# BPM M2 身份组织与审批策略实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在新 Graph 定义上交付可版本冻结的身份组织与审批策略，使内置通用申请能完成单人、顺序、全员、或签和比例审批，并留下可审计的成员与阶段事实。

**Architecture:** M2 是策略目录、组织解析、成员冻结、参与者授权和完成决策的所有者；M1 只冻结完整 canonical 策略并编译一个 `ApprovalStageControl` 等待点；M4 持久化阶段、成员、任务与命令事实，并只推进该等待点一次。旧 `parallelAll` / `sequential` 编译片段和旧公开 VO 不作为新模型兼容目标。

**Tech Stack:** Java 17, Spring Boot 3.5, MyBatis-Plus, MySQL, Flowable 7.2, JUnit 5, Mockito, AssertJ, Vue 3, TypeScript, Element Plus, Vitest, pnpm.

## Global Constraints

- 在当前分支 `main` 的现有工作树实施；保留与 M2 无关的所有未提交修改，不创建 worktree，不回退其他模块。
- 不新增 Maven、pnpm 或运行时依赖；所有中文、SQL 与文档使用 UTF-8。
- 新增表、菜单、权限和字典均使用递增 SQL；当前最大版本为 `v3.53.0.sql`，M2 固定写入 `数据库SQL脚本/mysql/sql-update-log/v3.54.0.sql`。
- 仅允许登记的类型化候选来源和 M3 已脱敏路由事实；禁止 EL、SpEL、脚本、动态 SQL、自由 URL 与客户端提交的实际处理人。
- 策略、定义和运行成员分别冻结；运行时不按策略键读取最新版本，也不按当前组织重写成员快照。
- `HANDLE` 不能使用 `AUTO_APPROVE` / `AUTO_REJECT`；自动终态只适用于 `APPROVAL` 的单一阶段控制等待点。
- 每处生产行为先新增一个失败测试并确认失败，再写最小实现；每批结束执行列出的 Maven 或 pnpm 测试。

---

## 文件职责映射

| 区域 | 文件/目录 | 责任 |
| --- | --- | --- |
| M2 策略领域 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/` | 策略版本、canonical 文档、解析、授权、完成决策、管理服务 |
| M2 管理接口 | `.../controller/admin/AdminBpmCandidatePolicyController.java` 与 `module/candidate/domain/{form,vo}` | 版本草稿、模拟、启用、退休与只读详情 |
| 组织适配 | `hunyuan-backend/hunyuan-bpm/src/main/java/.../api/identity/`、`hunyuan-admin/.../adapter/AdminBpmOrgIdentityGateway.java` | 有效员工、角色、主管、岗位/用户组/主管链的受控查询 |
| M1 绑定与发布 | `engine/graph/GraphPublicationPrecheck.java`、`module/definition/service/M2M3GraphPublicationDependencyResolver.java` | Graph 引用校验、完整策略依赖快照、发布/退休线性化 |
| M1 编译 | `engine/compiler/graph/GraphBpmnCompiler.java`、`engine/internal/*Graph*Deployment*` | `APPROVAL` 到一个 `ApprovalStageControl` 等待点及稳定 mapping |
| M4 阶段运行 | `module/runtime/{dao,domain,service}/` | 阶段、成员、任务投影、一次性控制推进、恢复与查询 |
| 前端 | `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/`、`components/bpm/graph/`、`views/system/bpm/` | 策略目录、Graph 选择器、发布诊断、任务/实例阶段展示 |
| 增量 SQL | `数据库SQL脚本/mysql/sql-update-log/` | M2 目录、阶段、成员、索引及权限菜单 |
| 验收记录 | `docs/superpowers/specs/2026-07-11-bpm-module-02-assignment-approval-strategy-design.md`、新 M2 验收记录 | 当前事实、命令证据、浏览器验收和剩余边界 |

## Task 1: 版本化策略目录与发布租约

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/domain/entity/BpmCandidatePolicyVersionEntity.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/domain/model/{CandidatePolicyDocument,ApprovalPolicyDocument,StartVisibilityPolicyDocument,PolicyLifecycleState,PolicyPublicationLease}.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/service/{BpmPolicyCatalogService,PolicyCanonicalizer}.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate/BpmPolicyCatalogServiceTest.java`
- Create: `数据库SQL脚本/mysql/sql-update-log/v3.54.0.sql`

**Interfaces:**
- Produces `PolicyPublicationLease freezeForPublication(PolicyReference reference, String publicationRequestId)` whose payload is immutable canonical JSON and SHA-256 digest.
- Produces `PolicyLifecycleState` transitions `DRAFT -> ACTIVE -> RETIRED`; active records are never updated in place.

- [ ] **Step 1: Write failing catalog tests**

```java
@Test
void freezeShouldReturnCanonicalPayloadAndRejectRetiredVersion() {
    BpmPolicyCatalogService service = serviceWithActiveCandidate("finance-manager", 2, "{\"resolverType\":\"ROLE\"}");

    PolicyPublicationLease lease = service.freezeForPublication(
            new PolicyReference("finance-manager", 2), "publish-1");

    assertThat(lease.canonicalPayload()).contains("resolverType");
    assertThatThrownBy(() -> service.retireAndFreezeSameVersion("finance-manager", 2, "publish-2"))
            .isInstanceOf(PolicyPublicationException.class);
}
```

- [ ] **Step 2: Run RED**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=BpmPolicyCatalogServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Expected: test compilation fails because the catalog API and immutable lease do not exist.

- [ ] **Step 3: Add schema and minimal domain implementation**

Create separate immutable candidate, approval and start/visibility version records rather than overloading the current candidate row. Each version row contains `policy_key`, `policy_version`, `lifecycle_state`, `schema_version`, canonical JSON, digest and audit fields. Add a unique `(policy_key, policy_version)` constraint, lifecycle index and a publication lease/revision column. `freezeForPublication` locks or CAS-checks the active row, returns its complete payload and registers transaction completion to release the lease.

- [ ] **Step 4: Run GREEN and SQL source test**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=BpmPolicyCatalogServiceTest,BpmSchemaSourceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Expected: catalog lifecycle, canonical payload and schema source tests pass.

## Task 2: 组织身份网关与候选解析上下文

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/identity/BpmOrgIdentityGateway.java`
- Modify: `hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/bpm/adapter/AdminBpmOrgIdentityGateway.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/domain/model/{CandidateResolutionContext,RoutingFactView,ResolvedCandidateSnapshot,ResolvedCandidateMember,CandidateDiagnostic}.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/service/CandidateResolutionService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate/CandidateResolutionServiceTest.java`
- Modify: `hunyuan-backend/hunyuan-admin/src/test/java/com/hunyuan/sa/admin/module/bpm/adapter/AdminBpmOrgIdentityGatewayTest.java`

**Interfaces:**
- Consumes `CandidateResolutionContext(tenantId, definitionSnapshot, authoredNodeId, stageInvocationId, startEmployee, routingFactView, resolvedAt)`.
- Produces sorted, deduplicated `ResolvedCandidateSnapshot`; no service accepts raw form JSON.

- [ ] **Step 1: Write failing resolver tests**

```java
@Test
void roleResolutionShouldFilterInvalidEmployeesDeduplicateAndSort() {
    when(identityGateway.listEmployeeIdsByRoleId(8L)).thenReturn(List.of(30L, 20L, 30L));
    when(identityGateway.requireEmployee(20L)).thenReturn(employee(20L));
    when(identityGateway.requireEmployee(30L)).thenThrow(new IllegalArgumentException("已停用"));

    ResolvedCandidateSnapshot snapshot = resolver.resolve(rolePolicy(8L), context());

    assertThat(snapshot.members()).extracting(ResolvedCandidateMember::sourceEmployeeId).containsExactly(20L);
}
```

- [ ] **Step 2: Run RED**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=CandidateResolutionServiceTest,AdminBpmOrgIdentityGatewayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Expected: missing resolver/context types fail before production implementation.

- [ ] **Step 3: Implement registered resolver types**

Implement `EMPLOYEE`, `ROLE`, `DEPARTMENT_MANAGER`, `START_EMPLOYEE`, `START_DEPARTMENT_MANAGER`, `ROUTING_FACT_EMPLOYEE`, `POST`, `USER_GROUP` and `MANAGEMENT_CHAIN` through explicit gateway methods. Add tenant-aware sorting, deduplication, source fact digest, organization version, self-approval handling and `BLOCK` / typed fallback / auto-terminal outcomes. Parse only the canonical policy payload and `RoutingFactView.allowedKeys()`.

- [ ] **Step 4: Run GREEN**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=CandidateResolutionServiceTest,AdminBpmOrgIdentityGatewayTest,BpmCandidatePrecheckServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Expected: deterministic members, invalid identity diagnostics and routing-fact white-list checks pass.

## Task 3: Graph 绑定、静态预检与完整定义冻结

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/M2M3GraphPublicationDependencyResolver.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/graph/GraphPublicationPrecheck.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/graph/{GraphPublicationPrecheckResult,GraphPublicationFinding}.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/definition/M2M3GraphPublicationDependencyResolverTest.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/graph/GraphPublicationPrecheckTest.java`

**Interfaces:**
- `Graph.policies.startVisibilityPolicy` binds one exact start/visibility policy version.
- `APPROVAL` binds `candidatePolicy`, `approvalPolicy` and optional `returnTargetNodeId`; `HANDLE` binds only non-automatic candidate policy.
- Produces snapshot entries containing key, version, immutable ID, canonical payload, schema version, digest, risk level and return target.

- [ ] **Step 1: Write failing publication tests**

```java
@Test
void approvalNodeShouldFreezeBothPoliciesAndRejectAutomaticHandlePolicy() {
    HunyuanProcessDefinitionGraph graph = graphWithApprovalPolicies();
    GraphPublicationDependencySnapshot snapshot = resolver.resolve(graph);

    assertThat(snapshot.toSnapshotMap().get("candidatePolicies")).isNotNull();
    assertThat(snapshot.toSnapshotMap().get("approvalPolicies")).isNotNull();
    assertThatThrownBy(() -> resolver.resolve(graphWithAutomaticHandlePolicy()))
            .isInstanceOf(GraphPublicationDependencyException.class);
}
```

- [ ] **Step 2: Run RED**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=M2M3GraphPublicationDependencyResolverTest,GraphPublicationPrecheckTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Expected: tests fail because only candidate ID/version is currently frozen.

- [ ] **Step 3: Implement bindings and diagnostics**

Resolve all three policy types through Task 1 catalog leases. Emit `READY`, `RUNTIME_REQUIRED`, `WARNING` or `BLOCKING` findings with node/property paths; block missing active versions, invalid policy/node combinations, free-form return targets and unsafe automatic handling. Persist full canonical payload rather than querying the current catalog at runtime.

- [ ] **Step 4: Run GREEN**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=M2M3GraphPublicationDependencyResolverTest,GraphPublicationPrecheckTest,GraphDefinitionPublicationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Expected: policy freeze, retirement race and Graph diagnostics pass.

## Task 4: ApprovalStageControl 编译与部署映射

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/graph/GraphBpmnCompiler.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/graph/{GraphCompiledArtifact,GraphCompiledElementMapping}.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/{GraphFlowableDeployment,GraphFlowableDeploymentGateway}.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/graph/ApprovalStageControl.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/graph/GraphBpmnCompilerTest.java`

**Interfaces:**
- `ApprovalStageControl` accepts one `stageInvocationId`, supports `completeOnce` and `closeOnce`, and is the only engine control surface for an `APPROVAL` node.
- One authored approval node maps to exactly one stable compiled waiting activity; no Flowable multi-instance or public member task is emitted.

- [ ] **Step 1: Write failing compiler tests**

```java
@Test
void approvalShouldCompileToSingleStageControlWaitAndStableMapping() {
    GraphCompiledArtifact artifact = compiler.compile("expense", "费用", graphWithApproval());

    assertThat(artifact.bpmnXml()).contains("hunyuanApprovalStageControl");
    assertThat(artifact.mappings()).singleElement()
            .extracting(GraphCompiledElementMapping::compiledElementId)
            .isEqualTo("graph_stage_finance_review");
}
```

- [ ] **Step 2: Run RED**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=GraphBpmnCompilerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Expected: current ordinary `userTask` compilation does not provide stage control mapping.

- [ ] **Step 3: Implement stage control wait**

Compile `APPROVAL` to a single internal controlled wait activity with authored node ID and policy snapshot variables. Add a stable `graph_stage_<nodeId>` mapping and an engine adapter that atomically claims `stageInvocationId`; retain `HANDLE` as a normal controlled task. Ensure engine failure leaves an explicit recoverable runtime state rather than a silent split.

- [ ] **Step 4: Run GREEN**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=GraphBpmnCompilerTest,GraphDefinitionPublicationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Expected: exactly one waiting control is compiled and deployed per approval node.

## Task 5: 阶段、成员与任务投影持久化

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/{BpmApprovalStageEntity,BpmApprovalStageMemberEntity}.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/{BpmApprovalStageDao,BpmApprovalStageMemberDao}.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/{BpmApprovalStageService,BpmApprovalStageRecoveryService}.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskProjectionService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskProjectionServiceTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmApprovalStageServiceTest.java`
- Modify: `数据库SQL脚本/mysql/sql-update-log/v3.54.0.sql`

**Interfaces:**
- `BpmApprovalStageService.open(OpenApprovalStageCommand)` persists exactly one stage per `(instance_id, authored_node_id, generation)` and inserts frozen members.
- `BpmApprovalStageMemberEntity` stores source/current employee, order, state, result, task ID, snapshot digest and timestamps.

- [ ] **Step 1: Write failing persistence tests**

```java
@Test
void openShouldPersistOneStageAndFrozenMembersInDeterministicOrder() {
    ApprovalStageFact stage = service.open(commandWithMembers(30L, 20L));

    assertThat(stage.stageInvocationId()).isNotBlank();
    assertThat(memberDao.findByStage(stage.stageId()))
            .extracting(BpmApprovalStageMemberEntity::getSourceEmployeeId)
            .containsExactly(20L, 30L);
}
```

- [ ] **Step 2: Run RED**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=BpmApprovalStageServiceTest,BpmTaskProjectionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Expected: stage and member types are missing.

- [ ] **Step 3: Add runtime records and projections**

Add tables with unique stage invocation and member index constraints, optimistic/pessimistic lock query support and indexes for active employee lookup. M4 task projection creates member tasks only from frozen active members; sequential stages create only the first task. Do not derive member state from Flowable task key or JSON.

- [ ] **Step 4: Run GREEN**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=BpmApprovalStageServiceTest,BpmTaskProjectionServiceTest,BpmSchemaSourceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Expected: stage uniqueness, deterministic member order and sequential activation projection pass.

## Task 6: 授权、可用性与 SINGLE/SEQUENTIAL/ALL 决策

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/service/{ParticipantAuthorizationService,ApprovalCompletionService}.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/domain/model/{ApprovalMemberAction,ApprovalCompletionDecision,MemberAvailabilityDecision}.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/{BpmTaskService,BpmTaskActionPolicy,BpmApprovalStageService}.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate/{ParticipantAuthorizationServiceTest,ApprovalCompletionServiceTest}.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskActionPolicyTest.java`

**Interfaces:**
- `authorize(ActorSnapshot, ApprovalStageFact, MemberFact, BpmTaskAction)` rejects non-current, invalid and cross-tenant actors.
- `decide(ApprovalPolicyDocument, ApprovalStageFact, List<MemberFact>, ApprovalMemberAction)` returns member/phase state, next member activation and one engine effect.

- [ ] **Step 1: Write failing decision tests**

```java
@Test
void sequentialApprovalShouldActivateOnlyNextMemberAfterPreviousApproval() {
    ApprovalCompletionDecision decision = completion.decide(sequentialPolicy(), stageWithMembers("APPROVED", "PLANNED"), approveFirst());

    assertThat(decision.memberUpdates()).extracting(MemberUpdate::state)
            .containsExactly(MemberState.APPROVED, MemberState.ACTIVE);
    assertThat(decision.engineEffect()).isEqualTo(EngineEffect.NONE);
}
```

- [ ] **Step 2: Run RED**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=ParticipantAuthorizationServiceTest,ApprovalCompletionServiceTest,BpmTaskActionPolicyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Expected: decision and authorization APIs are absent.

- [ ] **Step 3: Implement fixed member state machine**

Implement `PLANNED`, `ACTIVE`, `APPROVED`, `REJECTED`, `RETURNED`, `TERMINATED`, `INELIGIBLE` and `CANCELLED`. `SINGLE` requires exactly one member; `SEQUENTIAL` activates in frozen order; `ALL` requires all approvals. An `INELIGIBLE` member never changes denominator and enters `EXCEPTION_PENDING` for these modes. Route normal task commands through authorization and decision services before any engine operation.

- [ ] **Step 4: Run GREEN**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=ParticipantAuthorizationServiceTest,ApprovalCompletionServiceTest,BpmTaskActionPolicyTest,BpmTaskServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Expected: unauthorized actors, invalid members and all three deterministic modes are covered.

## Task 7: ANY/RATIO、并发终态与恢复

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/service/ApprovalCompletionService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/{BpmApprovalStageService,BpmApprovalStageRecoveryService,BpmTaskService}.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/graph/ApprovalStageControl.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate/ApprovalCompletionServiceTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmApprovalStageConcurrencyTest.java`

**Interfaces:**
- `RATIO` computes `requiredApprovalCount = ceil(effectiveMemberCount * ratioPercent / 100)` with integers only.
- A terminal decision claims the stage lock, cancels remaining M4 member tasks and invokes `ApprovalStageControl` exactly once.

- [ ] **Step 1: Write failing edge and concurrency tests**

```java
@Test
void ratioShouldRejectWhenFrozenThresholdBecomesUnreachable() {
    ApprovalCompletionDecision decision = completion.decide(ratioPolicy(67), stageWithThreeMembers("APPROVED", "REJECTED", "INELIGIBLE"), noNewAction());

    assertThat(decision.stageState()).isEqualTo(ApprovalStageState.REJECTED);
    assertThat(decision.engineEffect()).isEqualTo(EngineEffect.CLOSE_ONCE);
}

@Test
void competingTerminalCommandsShouldAdvanceControlOnce() {
    CompletableFuture.allOf(approveFirstMember(), rejectSecondMember()).join();

    verify(stageControl, times(1)).completeOnce(anyString());
}
```

- [ ] **Step 2: Run RED**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=ApprovalCompletionServiceTest,BpmApprovalStageConcurrencyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Expected: ANY/RATIO and stage control claim behavior fail before implementation.

- [ ] **Step 3: Implement terminal and recovery rules**

For `ANY`, first valid approval succeeds and terminates remaining members. For `RATIO`, calculate frozen threshold and fail only when `approved + stillEligible < required`; never use floats or alter the denominator. Consume employee-validity events and run restart reconciliation; mark members `INELIGIBLE`, use deterministic reachability, and only allow audited transfer of the current employee. Use one locked transaction to record member facts, stage terminal state, member cancellation and `completeOnce`/`closeOnce` claim.

- [ ] **Step 4: Run GREEN**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=ApprovalCompletionServiceTest,BpmApprovalStageConcurrencyTest,BpmTaskServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Expected: threshold boundaries, competing actions, replayed request IDs and recovery paths pass.

## Task 8: 策略管理 API、Graph 设计器与发布诊断

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmCandidatePolicyController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/domain/{form,vo}/`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate/BpmCandidatePolicyControllerContractTest.java`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/graph.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/candidate-policy.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/components/bpm/graph/{graph-process-model.ts,graph-process-designer.vue,graph-process-designer.contract.test.ts,graph-process-model.test.ts}`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/candidate-policy/{policy-list.vue,policy-editor.vue,policy-editor.test.ts}`
- Modify: `数据库SQL脚本/mysql/sql-update-log/v3.54.0.sql`

**Interfaces:**
- Admin API exposes exact immutable versions, lifecycle actions and simulation; it never accepts an arbitrary resolver expression or actual assignee override.
- Designer sends `{ policyKey, policyVersion }`, `returnTargetNodeId` and renders server diagnostics instead of deriving policy validity locally.

- [ ] **Step 1: Write failing controller and TypeScript contract tests**

```java
@Test
void activateShouldRequireDraftVersionAndReturnImmutableVersion() throws Exception {
    mockMvc.perform(post("/bpm/candidate-policy/activate").param("policyKey", "finance").param("policyVersion", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.lifecycleState").value("ACTIVE"));
}
```

```ts
it('keeps exact candidate and approval policy versions on an approval node', () => {
  const next = updateGraphApprovalPolicy(graph, 'finance-review', { policyKey: 'finance', policyVersion: 2 });
  expect(next.nodes[1]?.properties?.approvalPolicy).toEqual({ policyKey: 'finance', policyVersion: 2 });
});
```

- [ ] **Step 2: Run RED**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=BpmCandidatePolicyControllerContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Run: `pnpm --dir hunyuan-design/apps/hunyuan-system vitest run src/components/bpm/graph/graph-process-model.test.ts`

Expected: controller and approval-policy graph helper do not yet exist.

- [ ] **Step 3: Implement API and UI**

Add policy list/editor with version creation, simulation, active/retired display and risk label. Add the Graph policy selectors for start/visibility, candidate and approval policies; disable automatic policies on `HANDLE`; show `READY`, `RUNTIME_REQUIRED`, `WARNING`, `BLOCKING` findings with path and reason. Add incremental menu and permissions for policy administration.

- [ ] **Step 4: Run GREEN**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=BpmCandidatePolicyControllerContractTest,GraphDefinitionPublicationControllerContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Run: `pnpm --dir hunyuan-design/apps/hunyuan-system vitest run src/components/bpm/graph/graph-process-model.test.ts src/components/bpm/graph/graph-process-designer.contract.test.ts src/views/system/bpm/candidate-policy/policy-editor.test.ts`

Expected: immutable versions and Graph binding contracts pass on both layers.

## Task 9: 运行详情、内置申请端到端与关闭记录

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/{vo,BpmInstanceTraceVO}.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/{BpmInstanceService,BpmInstanceTraceService}.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/{BpmInstanceTraceServiceTest,BpmRuntimeCommandServiceTest}.java`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/{bpm-approval-group-panel.vue,bpm-instance-detail-drawer.vue}`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-approval-stage-panel.test.ts`
- Create: `docs/superpowers/specs/2026-07-12-bpm-m2-assignment-approval-strategy-acceptance.md`
- Modify: `docs/superpowers/specs/2026-07-10-bpm-development-baseline.md`

**Interfaces:**
- Runtime DTO exposes stage invocation, immutable policy versions, source/current member, progress, threshold, state and terminal reason; it contains no Flowable ID or raw policy JSON.
- The acceptance record separates unit/integration results, browser evidence and any remaining M4/M5 dependency.

- [ ] **Step 1: Write failing runtime projection and UI tests**

```java
@Test
void traceShouldExposeFrozenStageFactsWithoutEngineIdentifiers() {
    BpmInstanceTraceVO trace = service.trace(11L);

    assertThat(trace.getApprovalStages()).singleElement()
            .extracting(BpmApprovalStageVO::getPolicyVersion).isEqualTo(2);
}
```

```ts
it('renders frozen threshold and original/current assignee from stage DTO', () => {
  const wrapper = mount(BpmApprovalGroupPanel, { props: { stage: ratioStage } });
  expect(wrapper.text()).toContain('2 / 3');
  expect(wrapper.text()).toContain('原审批人');
});
```

- [ ] **Step 2: Run RED**

Run: `mvn -pl hunyuan-bpm -am "-Dtest=BpmInstanceTraceServiceTest,BpmRuntimeCommandServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Run: `pnpm --dir hunyuan-design/apps/hunyuan-system vitest run src/views/system/bpm/runtime/components/bpm-approval-stage-panel.test.ts`

Expected: current DTOs do not expose the new stage fact model.

- [ ] **Step 3: Implement projection and execute acceptance matrix**

Map only structured stage/member fields to task, instance and trace DTOs. Update the drawer/panel to show candidate origin, current handler, progress/threshold, policy version and termination reason. Run the internal sample application through start range, blocked range, candidate resolution, all five modes, auto terminal, invalid member recovery, duplicate request and competing terminal commands. Capture browser/network/runtime proof outside the repository and write only factual outcomes to the acceptance record.

- [ ] **Step 4: Run final gates**

Run: `mvn -pl hunyuan-bpm -am test`

Run: `pnpm --dir hunyuan-design/apps/hunyuan-system vitest run`

Run: `git diff --check`

Expected: backend and frontend tests pass, SQL source checks include the new incremental script, and the acceptance record documents the real executed evidence.

## Plan Self-Review

- M2 design sections 3-10 map to Tasks 1-7; its management/experience section maps to Task 8; its full acceptance matrix maps to Task 9.
- No task asks for arbitrary expressions, legacy public-model compatibility or a new dependency.
- The only cross-module edits are required M1 Graph bindings, M3 routing-fact DTO consumption and M4 stage-control persistence; their ownership remains explicit.
- Every implementation task begins with an executable failing test and ends with a focused green gate; Task 9 adds full backend/frontend gates and actual runtime proof.
