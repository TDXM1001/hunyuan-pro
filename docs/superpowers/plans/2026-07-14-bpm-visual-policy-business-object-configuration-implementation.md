# BPM 可视化审批规则与业务对象配置实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将审批策略和业务契约从面向开发者的 JSON 编辑界面升级为业务管理员可使用的独立可视化配置中心，同时保留技术管理员只读协议、版本冻结、v1 兼容和真实 Graph 运行闭环。

**Architecture:** 前端只编辑类型化业务模型，后端负责身份校验、风险计算、自然语言摘要、规范化和唯一 canonical JSON 持久化；策略与业务对象分别使用列表、独立草稿编辑页和只读详情页，Graph 只引用已启用精确版本。现有 v1、已发布 Graph 和运行实例不原地迁移，新配置使用 Schema v2，v1 通过“升级为可视化草稿”生成新版本。

**Tech Stack:** Java 17, Spring Boot 3.5, MyBatis-Plus, MySQL, Flowable 7.2, JUnit 5, Mockito, AssertJ, Vue 3, TypeScript, Element Plus 2.14, Art Hooks, form-create, Vitest, vue-tsc, pnpm 11.

## Global Constraints

- 在当前 `main` 分支现有工作树实施；保留并忽略与本计划无关的未提交文件，尤其是 `docs/bpm-manual-verification-guide.md`。
- 所有 Java、TypeScript、Vue、SQL、提交信息和文档使用 UTF-8；PowerShell 验证中文文件时显式使用 `-Encoding UTF8`。
- 不新增 Maven、pnpm 或前端运行依赖，不引入第二套 UI、低代码、JSON Schema Form、画布、状态管理或拖拽库。
- 下一增量 SQL 固定为 `数据库SQL脚本/mysql/sql-update-log/v3.62.0.sql`，只做增量 DDL、菜单、权限和兼容回填，不修改全量初始化脚本，不改写历史 canonical JSON。
- 业务管理员不能查看或编辑 canonical JSON、内部枚举、裸员工/角色/部门 ID、digest 或技术诊断；技术接口必须具有独立服务端权限。
- 双模式共享同一业务详情和后端事实源：业务模式展示中文配置，技术模式只追加授权的只读协议与诊断，不维护第二套编辑器。
- 技术管理员只能查看只读 canonical JSON；任何变更都必须复制或升级为新草稿，已启用和已退休版本禁止原地编辑。
- 前端提交类型化业务配置，后端生成自然语言摘要、计算风险、规范化并保存唯一 canonical JSON；不得保存两套可独立编辑事实。
- Graph 发布继续冻结精确策略/业务对象版本、Schema、canonical payload 和 digest；运行时不查询目录最新版本。
- 现有 Schema v1、已发布 Graph 和运行实例继续运行；v1 只通过复制升级生成 v2 草稿，不做原地转换。
- 每项行为先建立失败测试并确认 RED，再写最小实现；每个任务结束运行聚焦门禁并使用中文 UTF-8 提交信息。

---

## 文件职责映射

| 区域 | 文件/目录 | 责任 |
| --- | --- | --- |
| 策略业务模型 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/domain/visual/` | 三类可视化输入、结构化详情、诊断、摘要和模拟结果 |
| 策略编译与目录 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/service/` | 类型化模型到 canonical v2、风险计算、摘要、草稿 CAS、模拟 |
| 策略管理接口 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmPolicyCatalogController.java` | 业务列表/详情、草稿保存、校验、模拟、技术详情与权限 |
| 业务对象模型 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/businesscontract/domain/visual/` | 业务对象 v2、字段、显示、明细、附件和修改策略 |
| 业务对象目录 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/businesscontract/service/` | v1/v2 分派、规范化、摘要、升级、草稿 CAS 和引用查询 |
| Graph 发布 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/M2M3GraphPublicationDependencyResolver.java` | 精确版本冻结、业务摘要元数据和引用查询 |
| 通用申请 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/approvaldata/` | v2 申请、明细、附件和服务端字段校验 |
| 前端 API | `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/{policy,business-contract}.ts` | 业务 DTO、技术 DTO、模拟、升级与生命周期接口 |
| 身份选择 | `hunyuan-design/apps/hunyuan-system/src/components/bpm/identity/` | 员工、角色、部门、岗位和用户组名称选择与回显 |
| 策略页面 | `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/policy/` | 列表、独立编辑、详情、摘要、模拟和技术只读视图 |
| 业务对象页面 | `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/business-contract/` | 列表、编辑、详情、字段/明细设计和真实预览 |
| Graph 前端 | `hunyuan-design/apps/hunyuan-system/src/components/bpm/graph/` | 已启用版本选择、中文摘要和风险展示 |
| 路由 | `hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts` | 配置编辑/详情隐藏路由与菜单 activePath |
| 增量 SQL | `数据库SQL脚本/mysql/sql-update-log/v3.62.0.sql` | 可检索展示列、技术权限、保存/模拟权限、菜单名称和角色授权 |
| 验收 | `docs/superpowers/specs/2026-07-14-bpm-visual-policy-business-object-configuration-acceptance.md` | 自动化、实库、浏览器、权限和兼容证据 |

## Task 1: 策略 Schema v2 类型化模型、摘要与风险计算

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/domain/visual/BpmPolicyVisualDraft.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/domain/visual/{CandidatePolicyVisualDocument,ApprovalPolicyVisualDocument,StartVisibilityPolicyVisualDocument,PolicyScopeVisualDocument,PolicyIdentityReference,PolicyValidationFinding,PolicyBusinessValidationResult,PolicyVisualCompilation,PolicyRiskAssessment}.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/service/{PolicyVisualDocumentMapper,PolicyBusinessSummaryService,PolicyRiskAssessmentService}.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/service/PolicyDocumentValidator.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/domain/model/PolicyValidationResult.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate/PolicyVisualDocumentMapperTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate/PolicyBusinessSummaryServiceTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate/PolicyRiskAssessmentServiceTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate/PolicyVisualFixtures.java`

**Interfaces:**
- Produces `PolicyVisualCompilation compile(BpmPolicyVisualDraft draft)` containing `canonicalPayload`, `digest`, `businessSummary`, `calculatedRiskLevel`, and `findings`.
- Preserves `PolicyDocumentValidator.validate(PolicyType, Integer, String)` for Schema v1 and frozen runtime compatibility.
- Defines `PolicyValidationFinding(String code, String severity, String fieldPath, String message, String suggestion)` for frontend field定位.

- [ ] **Step 1: Write failing v2 round-trip and summary tests**

```java
@Test
void candidateRoleRuleShouldCompileToCanonicalV2AndReadableSummary() {
    BpmPolicyVisualDraft draft = PolicyVisualFixtures.roleCandidate(
            "finance-reviewer", "费用审批人", 8L, "财务经理");

    PolicyVisualCompilation result = mapper.compile(draft);

    assertThat(result.canonicalPayload()).contains(
            "\"schemaVersion\":2", "\"resolverType\":\"ROLE\"", "\"roleId\":8");
    assertThat(result.businessSummary()).isEqualTo(
            "任务到达时，由“财务经理”角色成员审批；发起人不能自审；无人可处理时阻断流程。");
    assertThat(result.calculatedRiskLevel()).isEqualTo("LOW");
}

@Test
void clientRiskMustBeIgnoredAndAutoApproveMustCalculateHighRisk() {
    BpmPolicyVisualDraft draft = PolicyVisualFixtures.autoApproveFallbackWithClientRisk("LOW");
    assertThat(riskService.assess(draft).level()).isEqualTo("HIGH");
}
```

- [ ] **Step 2: Run RED for missing visual domain**

Run from `hunyuan-backend`:

```powershell
mvn -pl hunyuan-bpm -am "-Dtest=PolicyVisualDocumentMapperTest,PolicyBusinessSummaryServiceTest,PolicyRiskAssessmentServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: test compilation fails because visual documents, compilation result, summary and risk services do not exist.

- [ ] **Step 3: Implement the minimal typed documents and compiler**

```java
public record BpmPolicyVisualDraft(
        PolicyType type,
        String policyKey,
        String policyName,
        String description,
        Integer schemaVersion,
        Long catalogRevision,
        CandidatePolicyVisualDocument candidate,
        ApprovalPolicyVisualDocument approval,
        StartVisibilityPolicyVisualDocument startVisibility
) {
    public Object requireMatchingDocument() {
        return switch (type) {
            case CANDIDATE -> Objects.requireNonNull(candidate, "审批人规则不能为空");
            case APPROVAL -> Objects.requireNonNull(approval, "审批方式规则不能为空");
            case START_VISIBILITY -> Objects.requireNonNull(startVisibility, "发起范围规则不能为空");
        };
    }
}

public record PolicyValidationFinding(
        String code, String severity, String fieldPath, String message, String suggestion
) {}
```

`PolicyVisualDocumentMapper` must serialize a server-owned map with stable field order, call `PolicyDocumentValidator`, calculate risk without reading a client risk field, then use `PolicyCanonicalizer` for canonical JSON and digest.

- [ ] **Step 4: Add structured validation failures**

```java
return new PolicyBusinessValidationResult(
        findings.stream().noneMatch(it -> "BLOCKING".equals(it.severity())),
        risk.level(),
        summaryService.summarize(draft),
        List.copyOf(findings),
        canonicalPayload,
        digest
);
```

Map invalid role/employee scope, missing fallback, invalid ratio, unsupported action and invalid self-approval to stable `fieldPath` values such as `candidate.identityReference`, `candidate.emptyCandidatePolicy`, and `approval.ratioPercent`.

- [ ] **Step 5: Run GREEN plus existing v1 validator regression**

```powershell
mvn -pl hunyuan-bpm -am "-Dtest=PolicyVisualDocumentMapperTest,PolicyBusinessSummaryServiceTest,PolicyRiskAssessmentServiceTest,PolicyDocumentValidatorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: all selected tests pass; existing Schema v1 JSON validation remains green.

- [ ] **Step 6: Commit the typed policy core**

```powershell
cd E:\my-project\hunyuan-pro
git add -- hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate
git commit -m "feat(bpm): 增加可视化审批规则模型"
```

## Task 2: 策略草稿 CAS、业务/技术接口与增量 SQL

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/domain/entity/{BpmCandidatePolicyVersionEntity,BpmApprovalPolicyVersionEntity,BpmStartVisibilityPolicyVersionEntity}.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/dao/{BpmCandidatePolicyVersionDao,BpmApprovalPolicyVersionDao,BpmStartVisibilityPolicyVersionDao}.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/domain/form/{BpmPolicyVisualDraftForm,BpmPolicyTechnicalDiffForm}.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/domain/vo/{BpmPolicyCatalogSummaryVO,BpmPolicyBusinessDetailVO,BpmPolicyTechnicalDetailVO,BpmPolicyTechnicalDiffVO}.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/service/BpmPolicyCatalogService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/BpmDefinitionReferenceQueryService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/vo/BpmDefinitionReferenceVO.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmPolicyCatalogController.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate/{BpmPolicyCatalogServiceTest,PolicyCatalogControllerContractTest}.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/schema/BpmVisualConfigurationSchemaSourceTest.java`
- Create: `数据库SQL脚本/mysql/sql-update-log/v3.62.0.sql`

**Interfaces:**
- Produces `BpmPolicyBusinessDetailVO saveVisualDraft(PolicyReference, long expectedRevision, BpmPolicyVisualDraft, long actorEmployeeId)` and error code `CATALOG_REVISION_CONFLICT`.
- Produces `deleteDraft(PolicyReference, long expectedRevision)` only when lifecycle is DRAFT and no Graph reference exists.
- Produces technical `diff(left, right)` and `exportCanonical(reference)` under `bpm:policy-catalog:technical`.
- Business list/detail never returns `canonicalPayload` or `digest`.
- Technical detail requires `bpm:policy-catalog:technical` and returns read-only payload.

- [ ] **Step 1: Write failing catalog save and response-boundary tests**

```java
@Test
void saveShouldUpdateOnlyDraftWithExpectedRevision() {
    BpmPolicyBusinessDetailVO saved = service.saveVisualDraft(
            reference("finance-reviewer", 2), 3L, visualDraft(), 1L);
    assertThat(saved.catalogRevision()).isEqualTo(4L);
    verify(candidateDao).saveDraftVisual(eq("finance-reviewer"), eq(2), eq(3L), any());
}

@Test
void staleSaveMustFailWithoutOverwriting() {
    when(candidateDao.saveDraftVisual(anyString(), anyInt(), eq(2L), any())).thenReturn(0);
    PolicyReference reference = new PolicyReference(PolicyType.CANDIDATE, "finance-reviewer", 2);
    assertThatThrownBy(() -> service.saveVisualDraft(reference, 2L, visualDraft(), 1L))
            .hasMessageContaining("CATALOG_REVISION_CONFLICT");
}

@Test
void businessControllerMustNotExposeCanonicalPayload() {
    assertThat(BpmPolicyCatalogSummaryVO.class.getDeclaredFields())
            .extracting(Field::getName)
            .doesNotContain("canonicalPayload", "digest");
}

@Test
void deleteMustRejectActiveOrReferencedDraftAndDiffMustNotMutateEitherVersion() {
    assertThatThrownBy(() -> service.deleteDraft(activeReference(), 1L))
            .hasMessageContaining("只有未引用草稿可以删除");
    BpmPolicyTechnicalDiffVO diff = service.technicalDiff(v1Reference(), v2Reference());
    assertThat(diff.changedPaths()).contains("candidate.identityReference");
    verify(candidateDao, never()).update(any(), any());
}
```

- [ ] **Step 2: Run RED for missing save and summary DTOs**

```powershell
mvn -pl hunyuan-bpm -am "-Dtest=BpmPolicyCatalogServiceTest,PolicyCatalogControllerContractTest,BpmVisualConfigurationSchemaSourceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: compilation fails for the missing save API, business/technical DTOs and v3.62 migration.

- [ ] **Step 3: Add rerunnable v3.62 policy columns and permissions**

`v3.62.0.sql` must add nullable or safely defaulted columns to all three policy version tables:

```sql
`policy_name` varchar(128) NULL COMMENT '业务规则名称',
`description` varchar(500) NULL COMMENT '业务说明',
`business_summary` varchar(1000) NULL COMMENT '后端生成的业务摘要',
`calculated_risk_level` varchar(16) NULL COMMENT '后端计算风险等级'
```

Add button permissions under menu 342 for:

```text
bpm:policy-catalog:save
bpm:policy-catalog:simulate
bpm:policy-catalog:technical
bpm:policy-catalog:delete
```

Rename menu 342 to “审批规则”, preserve its path/component, and grant business save/simulate to role 1. Grant technical permission only to role 1 in the baseline migration; future roles must opt in explicitly.

- [ ] **Step 4: Implement locked CAS updates and split DTOs**

Each DAO adds an explicit update statement equivalent to:

```sql
UPDATE t_bpm_candidate_policy_version
SET policy_name = #{policyName}, description = #{description},
    policy_json = #{canonicalPayload}, policy_digest = #{digest},
    business_summary = #{businessSummary}, calculated_risk_level = #{riskLevel},
    catalog_revision = catalog_revision + 1, update_time = #{updatedAt}
WHERE policy_key = #{policyKey} AND policy_version = #{policyVersion}
  AND lifecycle_state = 'DRAFT' AND catalog_revision = #{expectedRevision}
```

Controller endpoints:

```java
@PostMapping("/bpm/policy-catalog/visual-draft/save")
@SaCheckPermission("bpm:policy-catalog:save")
public ResponseDTO<BpmPolicyBusinessDetailVO> save(@Valid @RequestBody BpmPolicyVisualDraftForm form) {
    PolicyReference reference = new PolicyReference(
            form.getType(), form.getPolicyKey(), form.getPolicyVersion());
    return ResponseDTO.ok(bpmPolicyCatalogService.saveVisualDraft(
            reference,
            form.getCatalogRevision(),
            form.toVisualDraft(),
            bpmCurrentActorProvider.requireCurrentEmployeeId()
    ));
}

@GetMapping("/bpm/policy-catalog/technical-detail/{type}/{policyKey}/{policyVersion}")
@SaCheckPermission("bpm:policy-catalog:technical")
public ResponseDTO<BpmPolicyTechnicalDetailVO> technicalDetail(
        @PathVariable PolicyType type,
        @PathVariable String policyKey,
        @PathVariable Integer policyVersion
) {
    return ResponseDTO.ok(bpmPolicyCatalogService.technicalDetail(
            new PolicyReference(type, policyKey, policyVersion)));
}

@PostMapping("/bpm/policy-catalog/draft/delete")
@SaCheckPermission("bpm:policy-catalog:delete")
public ResponseDTO<String> deleteDraft(@Valid @RequestBody BpmPolicyCatalogLifecycleForm form) {
    bpmPolicyCatalogService.deleteDraft(lifecycleCommand(form));
    return ResponseDTO.ok();
}

@PostMapping("/bpm/policy-catalog/technical-diff")
@SaCheckPermission("bpm:policy-catalog:technical")
public ResponseDTO<BpmPolicyTechnicalDiffVO> technicalDiff(
        @Valid @RequestBody BpmPolicyTechnicalDiffForm form
) {
    return ResponseDTO.ok(bpmPolicyCatalogService.technicalDiff(
            form.toLeftReference(), form.toRightReference()));
}
```

Add a technical export endpoint under the same permission that returns the exact canonical payload as a UTF-8 attachment named `<policyKey>-v<version>.json`. The export service reads the stored immutable payload and never reserializes a client model.

- [ ] **Step 5: Run GREEN and schema source checks**

```powershell
mvn -pl hunyuan-bpm -am "-Dtest=BpmPolicyCatalogServiceTest,PolicyCatalogControllerContractTest,BpmVisualConfigurationSchemaSourceTest,BpmSchemaSourceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: DRAFT CAS update, non-DRAFT rejection, response separation, permission strings, display columns and menu rename tests pass.

- [ ] **Step 6: Commit catalog persistence and API**

```powershell
cd E:\my-project\hunyuan-pro
git add -- hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/BpmDefinitionReferenceQueryService.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/vo/BpmDefinitionReferenceVO.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmPolicyCatalogController.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/schema/BpmVisualConfigurationSchemaSourceTest.java 数据库SQL脚本/mysql/sql-update-log/v3.62.0.sql
git commit -m "feat(bpm): 支持审批规则草稿治理"
```

## Task 3: 身份选择数据与可信规则模拟

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/domain/form/BpmPolicySimulationForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/domain/vo/{BpmPolicySimulationVO,BpmPolicySimulationMemberVO,BpmIdentityOptionVO,BpmIdentityOptionPageVO}.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate/service/{BpmPolicySimulationService,BpmPolicyIdentityOptionService}.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmPolicyCatalogController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate/BpmPolicySimulationServiceTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate/BpmPolicyIdentityOptionServiceTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate/PolicySimulationFixtures.java`

**Interfaces:**
- `simulate(BpmPolicySimulationForm, long actorEmployeeId)` uses `CandidateResolutionService` and `StartVisibilityPolicyEvaluator`, never creates runtime facts.
- `queryIdentityOptions(kind, keyword, departmentId, pageNum, pageSize)` returns stable ID, display name, department, disabled state and type.

- [ ] **Step 1: Write failing no-side-effect simulation test**

```java
@Test
void simulateCandidateShouldUseRuntimeResolverWithoutCreatingFacts() {
    BpmPolicySimulationVO result = service.simulate(
            PolicySimulationFixtures.roleCandidateWithStarter(8L, 1L));

    assertThat(result.resolvedMembers())
            .extracting(BpmPolicySimulationMemberVO::employeeName)
            .containsExactly("胡克");
    verifyNoInteractions(instanceDao, taskDao, actionLogDao);
}

@Test
void disabledRoleMustReturnBlockingFindingWithBusinessName() {
    when(identityGateway.listEmployeeIdsByRoleId(8L)).thenThrow(new IllegalArgumentException("角色已禁用"));
    assertThat(service.simulate(form()).findings()).anySatisfy(finding -> {
        assertThat(finding.fieldPath()).isEqualTo("candidate.identityReference");
        assertThat(finding.message()).contains("财务经理", "已禁用");
    });
}
```

- [ ] **Step 2: Run RED**

```powershell
mvn -pl hunyuan-bpm -am "-Dtest=BpmPolicySimulationServiceTest,BpmPolicyIdentityOptionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: tests fail because simulation and identity option services are absent.

- [ ] **Step 3: Implement simulation and identity option endpoints**

```java
@PostMapping("/bpm/policy-catalog/simulate")
@SaCheckPermission("bpm:policy-catalog:simulate")
public ResponseDTO<BpmPolicySimulationVO> simulate(@Valid @RequestBody BpmPolicySimulationForm form) {
    return ResponseDTO.ok(simulationService.simulate(form, actorProvider.requireCurrentEmployeeId()));
}

@GetMapping("/bpm/policy-catalog/identity-options")
@SaCheckPermission("bpm:policy-catalog:detail")
public ResponseDTO<BpmIdentityOptionPageVO> identityOptions(
        @RequestParam String kind,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "20") Integer pageSize
) {
    return ResponseDTO.ok(identityOptionService.query(
            kind, keyword, departmentId, pageNum, pageSize));
}
```

Reuse `BpmOrgIdentityGateway` and existing organization queries. Do not add a second employee/role repository inside BPM.

- [ ] **Step 4: Run GREEN plus runtime resolver regression**

```powershell
mvn -pl hunyuan-bpm -am "-Dtest=BpmPolicySimulationServiceTest,BpmPolicyIdentityOptionServiceTest,CandidateResolutionServiceTest,StartVisibilityPolicyEvaluatorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: simulation resolves the same members as runtime, reports self-approval/fallback/empty diagnostics and performs no writes.

- [ ] **Step 5: Commit simulation**

```powershell
cd E:\my-project\hunyuan-pro
git add -- hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/candidate hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmPolicyCatalogController.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate
git commit -m "feat(bpm): 增加审批规则模拟解析"
```

## Task 4: 审批规则列表、独立编辑页、详情页与身份选择器

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/policy.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/components/bpm/identity/{bpm-identity-picker.vue,bpm-identity-display.vue,bpm-identity-picker.contract.test.ts}`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/policy/policy-catalog.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/policy/{policy-editor.vue,policy-detail.vue,policy-editor-model.ts,policy-editor-model.test.ts}`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/policy/components/{bpm-rule-type-selector.vue,bpm-scope-builder.vue,bpm-candidate-rule-editor.vue,bpm-approval-rule-editor.vue,bpm-rule-summary.vue,bpm-rule-simulation-panel.vue,bpm-technical-policy-panel.vue}`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/policy/policy-catalog.contract.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/policy-catalog.contract.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/router/bpm-visual-configuration-routes.test.ts`

**Interfaces:**
- Frontend `BpmPolicyVisualDraft` mirrors Task 1 types and never exposes editable `canonicalPayload`.
- `toPolicyVisualSaveParams(model)` produces the structure accepted by `/visual-draft/save`.
- `BpmIdentityPicker` is implemented by `bpm-identity-picker.vue` and emits `BpmIdentityReference[]` containing stable IDs plus display snapshots.
- Hidden routes preserve `activePath: '/system/bpm/policy/policy-catalog'`.

- [ ] **Step 1: Write failing frontend model and route tests**

```ts
it('builds a role candidate request without editable JSON or client risk', () => {
  const params = toPolicyVisualSaveParams(roleCandidateModel());
  expect(params.candidate?.identityReference).toEqual({ kind: 'ROLE', stableId: 8 });
  expect(params).not.toHaveProperty('policyJson');
  expect(params).not.toHaveProperty('riskLevel');
});

it('registers independent editor and detail routes', () => {
  expect(routeSource).toContain('/system/bpm/policy/editor');
  expect(routeSource).toContain('/system/bpm/policy/detail');
  expect(routeSource).toContain("activePath: '/system/bpm/policy/policy-catalog'");
});
```

- [ ] **Step 2: Run RED**

Run from `hunyuan-design`:

```powershell
pnpm test:unit -- apps/hunyuan-system/src/views/system/bpm/policy/policy-editor-model.test.ts apps/hunyuan-system/src/components/bpm/identity/bpm-identity-picker.contract.test.ts apps/hunyuan-system/src/router/bpm-visual-configuration-routes.test.ts
```

Expected: tests fail because visual models, picker and routes do not exist.

- [ ] **Step 3: Add API types and identity picker**

```ts
export interface BpmIdentityReference {
  kind: 'DEPARTMENT' | 'EMPLOYEE' | 'POSITION' | 'ROLE' | 'USER_GROUP';
  stableId: number;
  displayName?: string;
}

export interface BpmPolicyBusinessValidationResult {
  pass: boolean;
  calculatedRiskLevel: 'HIGH' | 'LOW' | 'MEDIUM';
  businessSummary: string;
  findings: Array<{ code: string; severity: string; fieldPath: string; message: string; suggestion: string }>;
}
```

`bpm-identity-picker.vue` uses existing organization APIs for employee, role, department and position. It shows names and disabled state, returns stable IDs, supports single/multiple selection and never shows a raw editable ID input.

- [ ] **Step 4: Implement independent list/editor/detail pages**

`policy-editor.vue` uses `ArtEditPage`, `ArtEditSection`, a top-level `ElForm`, server validation and a side summary/simulation surface. Header actions are exactly “返回”“保存草稿”“校验规则”“校验并启用”.

`policy-detail.vue` is read-only, shows business summary, lifecycle, references and version history. The technical panel is conditionally requested only when access contains `bpm:policy-catalog:technical`; it contains a read-only JSON viewer and no input.

`policy-catalog.vue` remains the dynamic-menu list entry and replaces the JSON dialog with navigation to editor/detail routes.

DRAFT rows expose “继续编辑”和“删除草稿”; delete requires a second confirmation and `bpm:policy-catalog:delete`. Technical detail exposes “版本对比”和“导出协议”; both call server-owned technical endpoints and never create an editable JSON surface.

When validation returns `HIGH`, “校验并启用” must require `bpm:policy-catalog:activate-high-risk`, open a confirmation dialog with risk reasons and a required confirmation reason, and call the existing high-risk activation endpoint. LOW/MEDIUM activation uses the ordinary lifecycle endpoint.

- [ ] **Step 5: Run focused GREEN and typecheck**

```powershell
pnpm test:unit -- apps/hunyuan-system/src/api/system/bpm/policy-catalog.contract.test.ts apps/hunyuan-system/src/views/system/bpm/policy apps/hunyuan-system/src/components/bpm/identity apps/hunyuan-system/src/router/bpm-visual-configuration-routes.test.ts
pnpm --filter @hunyuan/system typecheck
```

Expected: focused policy tests pass and vue-tsc exits 0.

- [ ] **Step 6: Commit the policy UI**

```powershell
cd E:\my-project\hunyuan-pro
git add -- hunyuan-design/apps/hunyuan-system/src/api/system/bpm/policy.ts hunyuan-design/apps/hunyuan-system/src/components/bpm/identity hunyuan-design/apps/hunyuan-system/src/views/system/bpm/policy hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts hunyuan-design/apps/hunyuan-system/src/router/bpm-visual-configuration-routes.test.ts
git commit -m "feat(bpm): 增加可视化审批规则页面"
```

## Task 5: Graph 已启用规则摘要与引用关系闭环

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/M2M3GraphPublicationDependencyResolver.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/GraphPublicationDependencySnapshot.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/BpmDefinitionReferenceQueryService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/vo/BpmDefinitionReferenceVO.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmPolicyCatalogController.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/definition/GraphDefinitionPublicationServiceTest.java`
- Modify: `hunyuan-design/apps/hunyuan-system/src/components/bpm/graph/{graph-process-designer.vue,graph-process-designer.contract.test.ts}`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/policy.ts`

**Interfaces:**
- Graph policy options contain `policyName`, `policyVersion`, `businessSummary`, `calculatedRiskLevel`, and exact reference.
- `GET /bpm/policy-catalog/references/{type}/{policyKey}/{policyVersion}` returns published/draft Graph references without raw snapshots.

- [ ] **Step 1: Write failing freeze and UI summary tests**

```java
@Test
void publicationShouldFreezeCanonicalPayloadButExposeOnlyBusinessMetadataToDesigner() {
    GraphPublicationDependencySnapshot snapshot = resolver.resolve(graphWithVisualPolicies());
    assertThat(snapshot.canonicalPayload()).contains("\"schemaVersion\":2");
    assertThat(snapshot.businessMetadata()).contains("费用审批人", "财务经理");
}
```

```ts
it('shows rule name version summary and risk without raw JSON', () => {
  expect(designerSource).toContain('businessSummary');
  expect(designerSource).toContain('calculatedRiskLevel');
  expect(designerSource).not.toContain('canonicalPayload');
});
```

- [ ] **Step 2: Run RED**

```powershell
mvn -pl hunyuan-bpm -am "-Dtest=GraphDefinitionPublicationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir ..\hunyuan-design test:unit -- apps/hunyuan-system/src/components/bpm/graph/graph-process-designer.contract.test.ts
```

Expected: tests fail because business metadata and reference endpoint are absent.

- [ ] **Step 3: Implement exact freeze plus safe designer metadata**

Keep canonical payload/digest inside publication snapshot. Return only safe option metadata to the frontend. The selector label is `${policyName} v${version}` and the property panel renders the summary and risk tag below the selector.

- [ ] **Step 4: Run backend/frontend GREEN**

```powershell
mvn -pl hunyuan-bpm -am "-Dtest=GraphDefinitionPublicationServiceTest,M2M3GraphPublicationDependencyResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir ..\hunyuan-design test:unit -- apps/hunyuan-system/src/components/bpm/graph apps/hunyuan-system/src/api/system/bpm/policy-catalog.contract.test.ts
```

Expected: v1 and v2 exact versions freeze; designer shows only safe business metadata.

- [ ] **Step 5: Commit Graph policy integration**

```powershell
cd E:\my-project\hunyuan-pro
git add -- hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmPolicyCatalogController.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/definition hunyuan-design/apps/hunyuan-system/src/components/bpm/graph hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue hunyuan-design/apps/hunyuan-system/src/api/system/bpm/policy.ts
git commit -m "feat(bpm): 展示Graph审批规则摘要"
```

## Task 6: 业务对象 Schema v2、草稿 CAS、v1 升级与数据库列

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/businesscontract/domain/visual/{BpmBusinessObjectDraft,BusinessObjectField,FieldPresentation,BusinessKeyRule,LineItemSchema,AttachmentRule,DataChangeRule,BusinessObjectValidationFinding,BusinessObjectValidationResult}.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/businesscontract/service/{BusinessObjectV2DocumentMapper,BusinessObjectBusinessSummaryService}.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/BpmDefinitionReferenceQueryService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/businesscontract/service/BusinessContractDocumentValidator.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/businesscontract/service/BpmBusinessContractCatalogService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/businesscontract/domain/entity/BpmBusinessContractVersionEntity.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/businesscontract/dao/BpmBusinessContractVersionDao.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/businesscontract/domain/form/{BpmBusinessObjectDraftForm,BpmBusinessObjectTechnicalDiffForm}.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/businesscontract/domain/vo/{BpmBusinessObjectSummaryVO,BpmBusinessObjectDetailVO,BpmBusinessObjectTechnicalDetailVO,BpmBusinessObjectTechnicalDiffVO,BpmBusinessObjectReferenceVO}.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmBusinessContractController.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/businesscontract/BpmBusinessContractCatalogServiceTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/businesscontract/BusinessObjectV2DocumentMapperTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/businesscontract/BusinessObjectFixtures.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/schema/BpmVisualConfigurationSchemaSourceTest.java`
- Modify: `数据库SQL脚本/mysql/sql-update-log/v3.62.0.sql`

**Interfaces:**
- Produces `compile(BpmBusinessObjectDraft)` with canonical Schema v2, digest, business summary and structured findings.
- Produces `upgradeV1AsV2Draft(contractKey, contractVersion, actorEmployeeId)` without updating the source row.
- Produces `deleteDraft(contractKey, contractVersion, expectedRevision)` only for unreferenced DRAFT versions.
- Produces technical version diff, reference list and canonical UTF-8 export under the technical permission.
- Business endpoints omit canonical JSON; technical endpoint requires `bpm:business-contract:technical`.

- [ ] **Step 1: Write failing v2 compilation, CAS and upgrade tests**

```java
@Test
void v2ShouldKeepBusinessSemanticsSeparateFromPresentation() {
    BusinessObjectValidationResult result = mapper.compile(BusinessObjectFixtures.expense());
    assertThat(result.canonicalPayload()).contains(
            "\"fieldSchema\"", "\"presentation\"", "\"lineItemSchema\"");
    assertThat(result.businessSummary()).contains("费用申请", "申请金额", "费用明细");
}

@Test
void upgradeMustCreateNewV2DraftWithoutChangingV1() {
    BusinessContractCatalogVersion upgraded = service.upgradeV1AsV2Draft("expense", 1, 1L);
    assertThat(upgraded.schemaVersion()).isEqualTo(2);
    verify(contractDao, never()).updateSourceVersion(eq("expense"), eq(1), any());
}

@Test
void referencedDraftCannotBeDeletedAndTechnicalDiffIsReadOnly() {
    when(referenceQuery.countGraphReferences("expense", 2)).thenReturn(1L);
    assertThatThrownBy(() -> service.deleteDraft("expense", 2, 0L))
            .hasMessageContaining("草稿仍被 Graph 引用");
    BpmBusinessObjectTechnicalDiffVO diff = service.technicalDiff("expense", 1, 2);
    assertThat(diff.changedFieldKeys()).contains("approvalNote");
    verify(contractDao, never()).update(any(), any());
}
```

- [ ] **Step 2: Run RED**

```powershell
mvn -pl hunyuan-bpm -am "-Dtest=BusinessObjectV2DocumentMapperTest,BpmBusinessContractCatalogServiceTest,BpmVisualConfigurationSchemaSourceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: missing v2 types, compiler, save/upgrade service and contract display columns fail compilation/tests.

- [ ] **Step 3: Implement Schema v2 validation and v1 dispatch**

```java
public BusinessObjectValidationResult validate(Integer schemaVersion, String canonicalPayload) {
    return switch (schemaVersion) {
        case 1 -> v1Adapter.validateAndDescribe(canonicalPayload);
        case 2 -> v2Mapper.validateCanonical(canonicalPayload);
        default -> throw new IllegalArgumentException("不支持的业务对象 Schema 版本：" + schemaVersion);
    };
}
```

Validate cross-zone field key uniqueness, type/control compatibility, routing candidate usability, editable fields, line item bounds, attachment rules and sensitivity/presentation consistency.

- [ ] **Step 4: Add business object columns and permissions to v3.62**

Add to `t_bpm_business_contract_version`:

```sql
`object_name` varchar(128) NULL COMMENT '业务对象名称',
`description` varchar(500) NULL COMMENT '业务说明',
`business_summary` varchar(1000) NULL COMMENT '后端生成的业务摘要'
```

Add permissions under menu 351:

```text
bpm:business-contract:save
bpm:business-contract:technical
bpm:business-contract:upgrade
bpm:business-contract:delete
```

Rename menu 351 to “业务对象”, keep path/component stable, and preserve existing role grants.

- [ ] **Step 5: Implement draft save, upgrade and technical endpoints**

```java
@PostMapping("/bpm/business-contract/visual-draft/save")
@SaCheckPermission("bpm:business-contract:save")
public ResponseDTO<BpmBusinessObjectDetailVO> save(@Valid @RequestBody BpmBusinessObjectDraftForm form) {
    return ResponseDTO.ok(catalogService.saveVisualDraft(
            form.toVisualDraft(), actorProvider.requireCurrentEmployeeId()));
}

@PostMapping("/bpm/business-contract/upgrade-v2")
@SaCheckPermission("bpm:business-contract:upgrade")
public ResponseDTO<BpmBusinessObjectDetailVO> upgrade(
        @Valid @RequestBody BpmBusinessContractReferenceForm form
) {
    return ResponseDTO.ok(catalogService.upgradeV1AsV2Draft(
            form.getContractKey(), form.getContractVersion(),
            actorProvider.requireCurrentEmployeeId()));
}

@GetMapping("/bpm/business-contract/technical-detail/{contractKey}/{contractVersion}")
@SaCheckPermission("bpm:business-contract:technical")
public ResponseDTO<BpmBusinessObjectTechnicalDetailVO> technicalDetail(
        @PathVariable String contractKey,
        @PathVariable Integer contractVersion
) {
    return ResponseDTO.ok(catalogService.technicalDetail(contractKey, contractVersion));
}

@PostMapping("/bpm/business-contract/draft/delete")
@SaCheckPermission("bpm:business-contract:delete")
public ResponseDTO<String> deleteDraft(@Valid @RequestBody BpmBusinessContractLifecycleForm form) {
    catalogService.deleteDraft(
            form.getContractKey(), form.getContractVersion(), form.getCatalogRevision());
    return ResponseDTO.ok();
}
```

Add `/references/{contractKey}/{contractVersion}`, `/technical-diff` and `/technical-export/{contractKey}/{contractVersion}`. Reference responses contain Graph name/version/state only; diff and export require `bpm:business-contract:technical`.

- [ ] **Step 6: Run GREEN and v1 regression**

```powershell
mvn -pl hunyuan-bpm -am "-Dtest=BusinessObjectV2DocumentMapperTest,BpmBusinessContractCatalogServiceTest,BpmVisualConfigurationSchemaSourceTest,BpmGenericApplicationServiceTest,GraphDefinitionPublicationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: v1 still validates/freezes; v2 compiles; stale saves fail; upgrade creates a new DRAFT only; SQL source assertions pass.

- [ ] **Step 7: Commit business object backend**

```powershell
cd E:\my-project\hunyuan-pro
git add -- hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/businesscontract hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/BpmDefinitionReferenceQueryService.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmBusinessContractController.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/businesscontract hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/schema/BpmVisualConfigurationSchemaSourceTest.java 数据库SQL脚本/mysql/sql-update-log/v3.62.0.sql
git commit -m "feat(bpm): 增加业务对象契约v2"
```

## Task 7: 业务对象列表、字段设计器、独立编辑与详情页

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/business-contract.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/components/bpm/business-object/{business-object-form-rules.ts,business-object-form-rules.test.ts}`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/business-contract/business-contract-catalog.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/business-contract/{business-object-editor.vue,business-object-detail.vue,business-object-editor-model.ts,business-object-editor-model.test.ts}`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/business-contract/components/{bpm-business-key-rule-editor.vue,bpm-schema-field-table.vue,bpm-schema-field-editor.vue,bpm-line-item-designer.vue,bpm-attachment-rule-editor.vue,bpm-change-policy-editor.vue,bpm-application-preview.vue,bpm-technical-contract-panel.vue,bpm-version-reference-panel.vue}`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/m3-pages.contract.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/router/bpm-visual-configuration-routes.test.ts`

**Interfaces:**
- `BpmBusinessObjectDraft` separates `applicationFields`, `routingFields`, `workingFields`, `lineItemSchema`, `attachmentRule`, `changePolicy`, and `presentation`.
- `toBusinessObjectSaveParams` never produces `contractJson`; canonical JSON is server-owned.
- Preview consumes the same `toBusinessObjectFormRules(BpmBusinessObjectDetail, BpmBusinessObjectPerspective)` mapper later used by runtime.

- [ ] **Step 1: Write failing field-model and route tests**

```ts
it('rejects duplicate keys across application routing and working fields', () => {
  const model = expenseObjectModel();
  model.routingFields.push({ ...model.applicationFields[0]! });
  expect(validateBusinessObjectModel(model)).toContainEqual(
    expect.objectContaining({ fieldPath: 'routingFields.0.key' }),
  );
});

it('sends typed v2 without contractJson', () => {
  const params = toBusinessObjectSaveParams(expenseObjectModel());
  expect(params.schemaVersion).toBe(2);
  expect(params).not.toHaveProperty('contractJson');
});

it('renders labels and controls from the business object presentation', () => {
  const rules = toBusinessObjectFormRules(expenseObjectDetail(), 'APPLICANT');
  expect(rules).toEqual(expect.arrayContaining([
    expect.objectContaining({ field: 'amount', title: '申请金额', type: 'inputNumber' }),
  ]));
});
```

- [ ] **Step 2: Run RED**

```powershell
pnpm test:unit -- apps/hunyuan-system/src/views/system/bpm/business-contract/business-object-editor-model.test.ts apps/hunyuan-system/src/views/system/bpm/m3-pages.contract.test.ts apps/hunyuan-system/src/router/bpm-visual-configuration-routes.test.ts
```

Expected: tests fail because business object model, components and routes are absent.

- [ ] **Step 3: Implement typed API and editor model**

```ts
export interface BpmBusinessObjectField {
  key: string;
  label: string;
  type: 'BOOLEAN' | 'DATE' | 'DATETIME' | 'DECIMAL' | 'DEPARTMENT_ID' | 'EMPLOYEE_ID' | 'INTEGER' | 'MULTI_SELECT' | 'SINGLE_SELECT' | 'STRING' | 'TEXT';
  required: boolean;
  sensitivity: 'CONFIDENTIAL' | 'INTERNAL' | 'PUBLIC' | 'RESTRICTED';
  presentation: { controlType: string; helpText?: string; order: number; placeholder?: string; span: 1 | 2 | 3 };
}
```

Keep frontend checks immediate, but always render backend findings as authoritative.

- [ ] **Step 4: Implement the independent business object pages**

`business-object-editor.vue` uses `ArtEditPage` and sections for basic info, number rule, application fields, routing fields, working fields, line items, attachments, change policy and preview.

`bpm-schema-field-editor.vue` is a focused drawer for one field. It uses a single-column `ElForm`, generates a suggested key, validates cross-zone conflicts through the parent model, and supports only backend-registered types.

Sorting uses icon buttons with tooltips and stable array moves; no drag dependency is added.

`business-object-detail.vue` is read-only and provides upgrade/copy/reference actions. The technical panel is loaded only with `bpm:business-contract:technical`.

DRAFT rows support continue editing and guarded deletion. Technical detail supports version comparison and canonical export. The reference panel always uses the safe business reference endpoint and does not require technical permission.

Create `toBusinessObjectFormRules(detail, perspective)` in the shared BPM component directory during this task and make the editor preview consume it immediately. Task 8 must reuse and extend this same function rather than creating another renderer mapping.

- [ ] **Step 5: Run focused GREEN and typecheck**

```powershell
pnpm test:unit -- apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/components/bpm/business-object apps/hunyuan-system/src/views/system/bpm/business-contract apps/hunyuan-system/src/views/system/bpm/m3-pages.contract.test.ts apps/hunyuan-system/src/router/bpm-visual-configuration-routes.test.ts
pnpm --filter @hunyuan/system typecheck
```

Expected: business object tests pass and vue-tsc exits 0.

- [ ] **Step 6: Commit business object UI**

```powershell
cd E:\my-project\hunyuan-pro
git add -- hunyuan-design/apps/hunyuan-system/src/api/system/bpm/business-contract.ts hunyuan-design/apps/hunyuan-system/src/components/bpm/business-object hunyuan-design/apps/hunyuan-system/src/views/system/bpm/business-contract hunyuan-design/apps/hunyuan-system/src/views/system/bpm/m3-pages.contract.test.ts hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts hunyuan-design/apps/hunyuan-system/src/router/bpm-visual-configuration-routes.test.ts
git commit -m "feat(bpm): 增加业务对象可视化配置"
```

## Task 8: 真实预览、通用申请 v2 与审批字段权限

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/components/bpm/business-object/business-object-form-rules.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/components/bpm/business-object/business-object-form-rules.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/business-contract/components/bpm-application-preview.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/generic-application.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/{bpm-runtime-form-renderer.vue,bpm-task-form-workbench.vue}`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/business-contract.ts`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/approvaldata/domain/form/BpmGenericApplicationSubmitForm.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/approvaldata/service/{BpmGenericApplicationService,BpmApprovalSubjectService,BpmApprovalSubjectViewService,BpmApprovalDataMutationService}.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/approvaldata/{BpmGenericApplicationServiceTest,BpmApprovalSubjectServiceTest,BpmApprovalSubjectViewServiceTest,BpmApprovalDataMutationServiceTest}.java`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/m3-pages.contract.test.ts`

**Interfaces:**
- `toBusinessObjectFormRules(detail, perspective)` is shared by editor preview, generic application and task workbench.
- v2 submit sends typed `fields`, `routingFacts`, `workingData`, `lineItems`, and `attachments`; server serializes frozen snapshots.
- Backend remains compatible with v1 string payload requests until the legacy UI/API boundary is retired separately.

- [ ] **Step 1: Write failing preview/runtime parity test**

```ts
it('enforces applicant, approver readonly and approver edit perspectives', () => {
  const applicant = toBusinessObjectFormRules(expenseDetail(), 'APPLICANT');
  const readonly = toBusinessObjectFormRules(expenseDetail(), 'APPROVER_READONLY');
  const editable = toBusinessObjectFormRules(expenseDetail(), 'APPROVER_EDIT');

  expect(applicant.find((rule) => rule.field === 'amount')?.props?.disabled).toBe(false);
  expect(readonly.every((rule) => rule.props?.disabled === true)).toBe(true);
  expect(editable.find((rule) => rule.field === 'approvalNote')?.props?.disabled).toBe(false);
  expect(editable.find((rule) => rule.field === 'amount')?.props?.disabled).toBe(true);
});
```

```java
@Test
void v2SubmitShouldValidateLineItemsAttachmentsAndFreezeCanonicalData() {
    GenericApplicationSubmitResult result = service.submit(v2ExpenseCommand());
    assertThat(subjectDao.selectById(result.approvalSubjectSnapshotId()).getFieldsJson())
            .contains("\"amount\":1000");
    assertThatThrownBy(() -> service.submit(v2WithForbiddenAttachment()))
            .hasMessageContaining("附件类型不允许");
}
```

- [ ] **Step 2: Run RED**

```powershell
mvn -pl hunyuan-bpm -am "-Dtest=BpmGenericApplicationServiceTest,BpmApprovalSubjectServiceTest,BpmApprovalSubjectViewServiceTest,BpmApprovalDataMutationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir ..\hunyuan-design test:unit -- apps/hunyuan-system/src/components/bpm/business-object/business-object-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/m3-pages.contract.test.ts
```

Expected: v2 typed submit and shared form-rule mapper are absent.

- [ ] **Step 3: Implement the shared rule mapper and preview perspectives**

```ts
export type BpmBusinessObjectPerspective = 'APPLICANT' | 'APPROVER_EDIT' | 'APPROVER_READONLY';

export function toBusinessObjectFormRules(
  detail: BpmBusinessObjectDetail,
  perspective: BpmBusinessObjectPerspective,
): FormRule[] {
  return visibleFields(detail, perspective).map((field) => ({
    field: field.key,
    title: field.label,
    type: controlType(field),
    props: { disabled: perspective === 'APPROVER_READONLY', placeholder: field.presentation.placeholder },
    validate: field.required ? [{ required: true, message: `${field.label}不能为空` }] : [],
  }));
}
```

Update `m3-pages.contract.test.ts` to assert that both `bpm-application-preview.vue` and `generic-application.vue` import `toBusinessObjectFormRules` from the shared module; the task workbench must consume the same mapper for approver perspectives.

- [ ] **Step 4: Implement server-owned v2 submission validation**

Dispatch by frozen Schema version. Validate types, required values, routing candidate whitelist, line item schema, attachment limits/types and node editable fields before snapshot or mutation. Never trust the generated frontend form rules as authorization.

- [ ] **Step 5: Run GREEN and Flowable compatibility**

```powershell
mvn -pl hunyuan-bpm -am "-Dtest=BpmGenericApplicationServiceTest,BpmApprovalSubjectServiceTest,BpmApprovalSubjectViewServiceTest,BpmApprovalDataMutationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn --% -pl hunyuan-admin -am -Dtest=BpmFlowableCompatibilityTest -Dsurefire.failIfNoSpecifiedTests=false test
pnpm --dir ..\hunyuan-design test:unit -- apps/hunyuan-system/src/components/bpm/business-object apps/hunyuan-system/src/views/system/bpm/m3-pages.contract.test.ts apps/hunyuan-system/src/views/system/bpm/runtime
pnpm --dir ..\hunyuan-design --filter @hunyuan/system typecheck
```

Expected: v1/v2 submission, field visibility/editability and real Flowable compatibility pass; frontend tests and typecheck pass.

- [ ] **Step 6: Commit runtime v2 integration**

```powershell
cd E:\my-project\hunyuan-pro
git add -- hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/approvaldata hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/approvaldata hunyuan-design/apps/hunyuan-system/src/components/bpm/business-object hunyuan-design/apps/hunyuan-system/src/views/system/bpm/business-contract/components/bpm-application-preview.vue hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime hunyuan-design/apps/hunyuan-system/src/views/system/bpm/m3-pages.contract.test.ts
git commit -m "feat(bpm): 打通业务对象v2运行表单"
```

## Task 9: 权限响应、菜单契约与全量自动化门禁

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/candidate/PolicyCatalogControllerContractTest.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/schema/BpmVisualConfigurationSchemaSourceTest.java`
- Create: `hunyuan-backend/hunyuan-admin/src/test/java/com/hunyuan/sa/admin/bpm/BpmVisualConfigurationPermissionTest.java`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/policy/policy-catalog.contract.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/m3-pages.contract.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/{policy-catalog.contract.test.ts,bpm-api.test.ts}`
- Modify: `数据库SQL脚本/mysql/sql-update-log/v3.62.0.sql`

**Interfaces:**
- Business responses contain no canonical payload, digest or internal identity IDs except stable references needed for subsequent saves.
- Direct technical endpoint access without technical permission returns the platform permission error.
- v3.62 is rerunnable and grants only intended roles.

- [ ] **Step 1: Add failing response-leak and permission tests**

```java
@Test
void businessListMustNotSerializeTechnicalFields() throws Exception {
    String json = objectMapper.writeValueAsString(
            controller.list(PolicyType.CANDIDATE, null, null).getData());
    assertThat(json).doesNotContain("canonicalPayload", "canonicalContractJson", "digest");
}

@Test
void businessUserMustBeDeniedTechnicalPolicyDetail() {
    runAsBusinessUser(() -> assertPermissionDenied(
            () -> controller.technicalDetail(PolicyType.CANDIDATE, "finance", 1)));
}
```

- [ ] **Step 2: Run RED**

```powershell
mvn -pl hunyuan-admin -am "-Dtest=BpmVisualConfigurationPermissionTest,PolicyCatalogControllerContractTest,BpmVisualConfigurationSchemaSourceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: missing permission test wiring or response leakage fails.

- [ ] **Step 3: Close SQL idempotency and permission boundaries**

Use `information_schema.columns` guards for new columns, `ON DUPLICATE KEY UPDATE` for menu rows, and `NOT EXISTS` for role grants. Ensure business roles never receive technical permissions through a broad parent-menu grant.

- [ ] **Step 4: Run complete automated gates**

From `hunyuan-backend`:

```powershell
mvn -pl hunyuan-bpm -am test
mvn --% -pl hunyuan-admin -am -Dtest=BpmFlowableCompatibilityTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl hunyuan-admin -am -DskipTests package
```

From `hunyuan-design`:

```powershell
pnpm test:unit -- apps/hunyuan-system/src/api/system/bpm apps/hunyuan-system/src/components/bpm apps/hunyuan-system/src/views/system/bpm apps/hunyuan-system/src/router
pnpm --filter @hunyuan/system typecheck
```

Expected: all commands exit 0; Maven reports `BUILD SUCCESS`; no tests are skipped when their evidence is required.

- [ ] **Step 5: Execute v3.62 twice on the controlled test database**

Before execution, back up the policy/contract version tables and `t_menu`/`t_role_menu`. Execute the UTF-8 script twice using the repository's configured MySQL client credentials.

Expected after both runs:

- All three policy tables contain the four display/risk columns.
- Business contract table contains the three display columns.
- Menus 342 and 351 show “审批规则”和“业务对象”.
- Save/simulate/technical/upgrade permissions exist exactly once.
- Existing v1 rows retain original JSON and digest.

- [ ] **Step 6: Commit permission and gate closure**

```powershell
cd E:\my-project\hunyuan-pro
git add -- hunyuan-backend/hunyuan-bpm/src/test hunyuan-backend/hunyuan-admin/src/test hunyuan-design/apps/hunyuan-system/src/api/system/bpm hunyuan-design/apps/hunyuan-system/src/views/system/bpm hunyuan-design/apps/hunyuan-system/src/router 数据库SQL脚本/mysql/sql-update-log/v3.62.0.sql
git commit -m "test(bpm): 收口可视化配置权限门禁"
```

## Task 10: 真实浏览器闭环、响应式验收与发布记录

**Files:**
- Create: `docs/superpowers/specs/2026-07-14-bpm-visual-policy-business-object-configuration-acceptance.md`
- Modify: `docs/superpowers/specs/2026-07-14-bpm-m1-m8-platform-release-baseline.md`
- Modify only if the actual accepted flow changes: `docs/bpm-manual-verification-guide.md`

**Interfaces:**
- Acceptance record distinguishes repository implementation, real browser closure and release status.
- Screenshots/logs stay outside the repository unless explicitly selected as durable evidence.

- [ ] **Step 1: Start fresh backend and frontend builds**

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-admin -am -DskipTests package
$jar = Get-ChildItem .\hunyuan-admin\target\hunyuan-admin-*.jar |
  Where-Object { $_.Name -notlike '*.original' } |
  Select-Object -First 1
if (-not $jar) { throw '未找到 hunyuan-admin 可执行包' }
java -jar $jar.FullName
```

In a second PowerShell:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-design
pnpm --filter @hunyuan/system dev
```

Expected: backend captcha endpoint returns HTTP 200 and the frontend URL from Vite loads the login page.

- [ ] **Step 2: Complete the business-admin policy flow**

Using `admin` or a dedicated business administrator:

1. Create “管理员可发起”.
2. Create “胡克审批人” with employee picker, never typing ID 2.
3. Create “全部通过”.
4. Simulate with admin and confirm the result is 胡克.
5. Enable all three rules.

Expected: no editable JSON, internal enum, raw database ID or regex is visible. Lists and details show names, versions, summaries and calculated risk.

- [ ] **Step 3: Complete the business-object and Graph flow**

1. Create “费用申请” Schema v2.
2. Add amount/reason application fields and approvalNote working field.
3. Add one line-item schema and attachment constraints.
4. Verify applicant, approver-readonly and approver-edit previews.
5. Enable the business object.
6. Create Graph `开始 -> 胡克审批 -> 结束`.
7. Bind three Chinese rule names and the business object version.
8. Publish and record version ID/digest evidence.

Expected: Graph property panel shows safe summaries and risks, not canonical payload.

- [ ] **Step 4: Complete the runtime and technical-permission flow**

1. Submit a generic application as admin.
2. Confirm the task is assigned to 胡克.
3. Approve as huke.
4. Confirm my application/todo/done/admin instance/admin task facts agree.
5. Open technical detail as technical admin and verify read-only JSON/digest/diagnostics.
6. Request the same technical endpoint as business user and verify permission denial.
7. Upgrade one existing v1 version to a v2 DRAFT and prove the v1 row/digest remain unchanged.

Expected: the full business flow closes without business users touching JSON; technical data is authorized and read-only.

- [ ] **Step 5: Verify desktop/mobile layout and browser errors**

Check at least `1440x900` and `390x844`:

- No page-level horizontal overflow.
- Long rule/field names do not overlap actions.
- Field tables scroll only inside their container.
- Header actions remain reachable.
- Technical JSON is collapsed by default on mobile.
- No new error-level browser console messages or non-expected 401/403/404/500 responses.

- [ ] **Step 6: Write the actual acceptance record and baseline update**

The acceptance document must record:

- Date, branch, exact commit and database migration.
- Actual command results and test counts.
- Real rule/business-object/Graph/instance/task IDs.
- Business-user and technical-user permission evidence.
- v1 compatibility and v2 upgrade evidence.
- Desktop/mobile evidence.
- Remaining gaps and either `RELEASABLE` or `NOT_RELEASABLE`.

Do not mark the baseline releasable if any required real flow, technical permission, migration replay or v1 compatibility evidence is missing.

- [ ] **Step 7: Run final repository checks and commit acceptance**

```powershell
cd E:\my-project\hunyuan-pro
git diff --check
git status --short
$design = Get-Content -Raw -Encoding UTF8 'docs/superpowers/specs/2026-07-14-bpm-visual-policy-business-object-configuration-acceptance.md'
$blockedWords = @(('T' + 'BD'), ('T' + 'ODO'), ('FIX' + 'ME'))
if ($blockedWords | Where-Object { $design.Contains($_) }) { throw 'Acceptance document contains placeholders' }
git add -- docs/superpowers/specs/2026-07-14-bpm-visual-policy-business-object-configuration-acceptance.md docs/superpowers/specs/2026-07-14-bpm-m1-m8-platform-release-baseline.md
git commit -m "docs(bpm): 验收可视化规则与业务对象配置"
```

Expected: UTF-8 read succeeds, no placeholder or whitespace error exists, and only durable acceptance/baseline files enter the documentation commit.

---

## 最终门禁

实施完成声明前必须取得以下当次证据：

1. `mvn -pl hunyuan-bpm -am test` 全绿。
2. `BpmFlowableCompatibilityTest` 实际执行并通过。
3. Admin reactor 打包成功。
4. BPM 前端聚焦 Vitest 全绿。
5. `@hunyuan/system` typecheck 退出码 0。
6. `v3.62.0.sql` 受控实库连续执行两次成功，历史 v1 JSON/digest 未变化。
7. 业务管理员不接触 JSON 完成规则、业务对象、Graph、发起和审批闭环。
8. 技术详情服务端权限、只读性和业务响应不泄漏均通过。
9. v1 运行兼容与 v1 到 v2 新草稿升级通过。
10. 桌面、移动端和浏览器控制台门禁通过。
11. 验收记录包含实际 ID、版本、提交、命令结果和剩余风险。

只有以上证据全部关闭，才能将本设计状态从“设计已确认”更新为“实现完成并可发布”。
