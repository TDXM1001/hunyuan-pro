# BPM 顺序多人审批组投影与异常路径闭环 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` (recommended) or `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将同一 authored 顺序审批节点展开出的真实任务归入稳定审批组，提供结构化进度，并关闭退回、重提和高级动作的异常路径。

**Architecture:** 复用现有 `t_bpm_approval_group`、任务关联字段和公共 VO，把审批组快照从并行专属泛化为 `sequential | parallelAll`。顺序和并行动作在审批组领域内按模式分流，Flowable 顺序拓扑保持不变；运行中旧实例通过 projection resync 幂等恢复组关联和计数。

**Tech Stack:** Java 17、Spring Boot、MyBatis-Plus、Flowable 7.2.0、JUnit 5、Mockito、Vue 3、TypeScript、Element Plus、Vitest、MySQL 8。

## Global Constraints

- 所有生产代码、契约、SQL、测试和文档必须留在 `E:\my-project\hunyuan-pro`。
- 不新增 Maven、pnpm 或运行时依赖。
- 不修改 Simple Model authored JSON 结构，不新增节点类型，不引入 Flowable multi-instance 或通用网关。
- 普通任务保持 `approvalGroup=null`；已有 `parallelAll` 字段和值保持兼容。
- 尚未激活的顺序成员只显示数量，不创建占位任务或成员计划表。
- 加签任务不设置 `approvalGroupId`，不计入 authored 组进度。
- 退回发起人必须取消当前 Flowable 实例，禁止先完成任务再进入 `WAIT_RESUBMIT`。
- 不修改已经执行的 `v3.43.0.sql`；实施前确认 `v3.44.0.sql` 未被占用。
- 历史已结束顺序实例不批量回填；运行中实例只在当前 engine process 范围内恢复。
- 保留工作树中现有 `AGENTS.md`、运行手册和旧计划改动，不加入本交付提交。

---

## File Responsibility Map

### Compiler and migration

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompiler.java`：给顺序展开快照补充稳定组 key/name，不改变 BPMN 拓扑。
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompilerTest.java`：锁定顺序快照与串行拓扑。
- `数据库SQL脚本/mysql/sql-update-log/v3.44.0.sql`：把数据库注释从并行专属泛化为多人审批组。

### Projection and persisted group truth

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskProjectionService.java`：识别两种审批组模式，给新任务和已投影旧任务关联组。
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmApprovalGroupService.java`：解析通用组快照，创建/恢复组，维护计数并按模式排序成员。
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskProjectionServiceTest.java`：覆盖首任务、后续任务和 existing-task resync。
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmApprovalGroupServiceTest.java`：覆盖创建、恢复、幂等和模式冲突。

### Runtime actions

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`：按审批模式分流动作，统一退回引擎终止语义。
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmApprovalGroupService.java`：执行顺序成员状态机和组锁定。
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskServiceTest.java`：覆盖顺序审批组路由和实例完成。
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java`：覆盖普通任务退回取消引擎。
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskAdvancedActionServiceTest.java`：覆盖顺序/并行加签减签边界。

### Public contract and frontend

- `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`：增加 `BpmApprovalMode` 联合类型。
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-approval-group-panel.vue`：模式标签和待激活人数。
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-todo-list.vue`：仅对 `parallelAll` 隐藏加签、减签。
- `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`：锁定 TypeScript 公共契约。
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`：锁定面板和动作可见性。
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeDetailServiceTest.java`：锁定实例/任务详情中的顺序组。
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmInstanceTraceServiceTest.java`：锁定 trace 透传。

### Closure artifacts

- Create `docs/superpowers/specs/2026-07-11-bpm-sequential-approval-group-acceptance.md`：记录本次自动化和活体验收证据。
- Modify `docs/superpowers/specs/2026-07-10-bpm-development-baseline.md`：把顺序组移入已完成能力并更新当前优先级。

---

### Task 1: Freeze sequential compiler contract and SQL metadata

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompilerTest.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompiler.java`
- Create: `数据库SQL脚本/mysql/sql-update-log/v3.44.0.sql`

**Interfaces:**
- Consumes: existing `CompiledNodeSnapshot.compiledNodeSnapshotJson()`.
- Produces: sequential snapshots with `approvalGroupKey`, `approvalGroupName`, `sequentialIndex`, `sequentialTotal`; no BPMN topology change.

- [ ] **Step 1: Confirm the SQL version is free**

Run:

```powershell
Test-Path 'E:/my-project/hunyuan-pro/数据库SQL脚本/mysql/sql-update-log/v3.44.0.sql'
```

Expected: `False`. If it is `True`, stop and select the next unused version before editing any migration reference in this plan.

- [ ] **Step 2: Add the failing compiler contract**

Add to `SimpleModelBpmnCompilerTest`:

```java
@Test
void compileShouldGiveSequentialMembersAStableApprovalGroupIdentity() {
    CompiledDefinitionArtifact artifact = compiler.compile(
            "expense_apply",
            "费用审批",
            "{\"nodes\":[{\"nodeKey\":\"finance_review\",\"name\":\"财务复核\","
                    + "\"type\":\"userTask\",\"approvalMode\":\"sequential\","
                    + "\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[101,102,103]}]}",
            "{\"type\":\"ALL\"}",
            "{}"
    );

    assertThat(artifact.nodeSnapshots()).hasSize(3);
    assertThat(artifact.nodeSnapshots().get(0).compiledNodeSnapshotJson())
            .contains("\"approvalGroupKey\":\"finance_review\"")
            .contains("\"approvalGroupName\":\"财务复核\"")
            .contains("\"sequentialIndex\":1")
            .contains("\"sequentialTotal\":3");
    assertThat(artifact.nodeSnapshots().get(2).compiledNodeSnapshotJson())
            .contains("\"approvalGroupKey\":\"finance_review\"")
            .contains("\"sequentialIndex\":3");
    assertThat(artifact.bpmnXml())
            .contains("sourceRef=\"finance_review_1\" targetRef=\"finance_review_2\"")
            .contains("sourceRef=\"finance_review_2\" targetRef=\"finance_review_3\"")
            .doesNotContain("parallelGateway");
}
```

- [ ] **Step 3: Run the compiler test and confirm RED**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=SimpleModelBpmnCompilerTest' test
```

Expected: FAIL because sequential snapshots do not contain `approvalGroupKey` and `approvalGroupName`.

- [ ] **Step 4: Add stable group identity to both multiple-employee modes**

Replace the mode-specific snapshot block in `SimpleModelBpmnCompiler.compileNode` with:

```java
compiledNodeObject.put("employeeId", employeeIds.getLongValue(memberIndex));
compiledNodeObject.put("authoredNodeKey", nodeKey);
compiledNodeObject.put("authoredNodeName", nodeName);
compiledNodeObject.put("approvalGroupKey", nodeKey);
compiledNodeObject.put("approvalGroupName", nodeName);
if (isParallelAll(nodeObject)) {
    compiledNodeObject.put("parallelIndex", memberIndex + 1);
    compiledNodeObject.put("parallelTotal", employeeIds.size());
} else {
    compiledNodeObject.put("sequentialIndex", memberIndex + 1);
    compiledNodeObject.put("sequentialTotal", employeeIds.size());
}
```

- [ ] **Step 5: Create the additive SQL metadata migration**

Create `v3.44.0.sql` with exactly:

```sql
-- BPM 多人审批组：将既有并行专属数据库注释泛化为顺序/并行共用语义
ALTER TABLE `t_bpm_approval_group`
  MODIFY COLUMN `approval_group_key` varchar(128) NOT NULL COMMENT 'authored审批节点业务标识',
  MODIFY COLUMN `approval_group_name` varchar(255) NOT NULL COMMENT 'authored审批节点名称快照',
  COMMENT = 'BPM多人审批组';

ALTER TABLE `t_bpm_task`
  MODIFY COLUMN `approval_group_id` bigint NULL COMMENT '多人审批组ID';
```

- [ ] **Step 6: Run GREEN and the validator regression**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=SimpleModelBpmnCompilerTest,SimpleModelValidatorTest' test
```

Expected: PASS; sequential BPMN remains serial and existing parallel compiler assertions remain green.

- [ ] **Step 7: Commit the compiler contract**

```powershell
git add -- 'hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompiler.java' 'hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompilerTest.java' '数据库SQL脚本/mysql/sql-update-log/v3.44.0.sql'
git commit -m 'feat(bpm): 补充顺序审批组编译契约'
```

### Task 2: Persist and recover sequential approval groups

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmApprovalGroupServiceTest.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskProjectionServiceTest.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmApprovalGroupService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskProjectionService.java`

**Interfaces:**
- Consumes: Task 1 snapshot fields.
- Produces: `ApprovalGroupNodeSnapshot`, unchanged `assignApprovalGroup(...)`, new `isParallelAllGroup(Long)`, and idempotent recovery for existing tasks.

- [ ] **Step 1: Add failing group creation and recovery tests**

Add the following tests and helper methods:

```java
@Test
void assignApprovalGroupShouldCreateSequentialGroup() {
    BpmDefinitionNodeEntity node = buildSequentialNode(11L, "finance_review_1", 1, 3, 101L);
    BpmInstanceEntity instance = buildInstance("process-8");
    BpmTaskEntity task = buildSequentialTask(21L, 11L, "process-8", 101L, null);
    when(groupDao.selectByEngineProcessInstanceIdAndGroupKey("process-8", "finance_review"))
            .thenReturn(null);
    when(groupDao.insert(any(BpmApprovalGroupEntity.class))).thenAnswer(invocation -> {
        BpmApprovalGroupEntity group = invocation.getArgument(0);
        group.setApprovalGroupId(31L);
        return 1;
    });
    when(taskDao.selectList(any())).thenReturn(List.of());

    Long groupId = service.assignApprovalGroup(instance, node, task);

    assertThat(groupId).isEqualTo(31L);
    ArgumentCaptor<BpmApprovalGroupEntity> captor =
            ArgumentCaptor.forClass(BpmApprovalGroupEntity.class);
    verify(groupDao).insert(captor.capture());
    assertThat(captor.getValue().getApprovalMode()).isEqualTo("sequential");
    assertThat(captor.getValue().getApprovalGroupKey()).isEqualTo("finance_review");
    assertThat(captor.getValue().getTotalMemberCount()).isEqualTo(3);
}

@Test
void recoverSequentialGroupShouldUseOnlyCurrentEngineProcessTasksAndBeIdempotent() {
    BpmInstanceEntity instance = buildInstance("process-new");
    BpmApprovalGroupEntity group = buildPendingGroup(31L, 3);
    group.setApprovalMode("sequential");
    group.setEngineProcessInstanceId("process-new");
    BpmTaskEntity approved = buildSequentialTask(21L, 11L, "process-new", 101L, null);
    approved.setTaskState(BpmTaskStateEnum.COMPLETED.getValue());
    approved.setTaskResult(BpmTaskResultEnum.APPROVED.getValue());
    BpmTaskEntity current = buildSequentialTask(22L, 12L, "process-new", 102L, null);
    BpmTaskEntity oldRun = buildSequentialTask(9L, 11L, "process-old", 101L, null);
    when(groupDao.selectByEngineProcessInstanceIdAndGroupKey("process-new", "finance_review"))
            .thenReturn(group);
    when(taskDao.selectList(any())).thenReturn(List.of(approved, current, oldRun));
    when(definitionNodeDao.selectBatchIds(any())).thenReturn(List.of(
            buildSequentialNode(11L, "finance_review_1", 1, 3, 101L),
            buildSequentialNode(12L, "finance_review_2", 2, 3, 102L)
    ));

    service.assignApprovalGroup(
            instance,
            buildSequentialNode(12L, "finance_review_2", 2, 3, 102L),
            current
    );

    verify(taskDao).updateById(argThat(task ->
            task.getTaskId().equals(21L) && task.getApprovalGroupId().equals(31L)));
    verify(taskDao, never()).updateById(argThat(task -> task.getTaskId().equals(9L)));
    verify(groupDao).updateById(argThat(group ->
            group.getProcessedMemberCount() == 1
                    && group.getApprovedMemberCount() == 1
                    && "PENDING".equals(group.getGroupState())));
}
```

Add a `BpmDefinitionNodeDao definitionNodeDao` mock to the test fixture, inject it as `bpmDefinitionNodeDao`, and add these helpers:

```java
private BpmDefinitionNodeEntity buildSequentialNode(
        Long definitionNodeId,
        String nodeKey,
        int index,
        int total,
        Long employeeId
) {
    BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
    node.setDefinitionNodeId(definitionNodeId);
    node.setNodeKey(nodeKey);
    node.setCompiledNodeSnapshotJson(("{\"approvalMode\":\"sequential\","
            + "\"approvalGroupKey\":\"finance_review\","
            + "\"approvalGroupName\":\"财务复核\","
            + "\"authoredNodeKey\":\"finance_review\","
            + "\"sequentialIndex\":%d,\"sequentialTotal\":%d,\"employeeId\":%d}")
            .formatted(index, total, employeeId));
    return node;
}

private BpmTaskEntity buildSequentialTask(
        Long taskId,
        Long definitionNodeId,
        String engineProcessInstanceId,
        Long employeeId,
        Long approvalGroupId
) {
    BpmTaskEntity task = new BpmTaskEntity();
    task.setTaskId(taskId);
    task.setInstanceId(8L);
    task.setDefinitionId(2L);
    task.setDefinitionNodeId(definitionNodeId);
    task.setEngineTaskId("task-" + taskId);
    task.setEngineProcessInstanceId(engineProcessInstanceId);
    task.setAssigneeEmployeeId(employeeId);
    task.setApprovalGroupId(approvalGroupId);
    task.setTaskState(BpmTaskStateEnum.PENDING.getValue());
    return task;
}
```

- [ ] **Step 2: Add the existing-task resync RED test**

Add to `BpmTaskProjectionServiceTest`:

```java
@Test
void syncShouldAttachGroupToExistingSequentialTaskWhenGroupIdIsMissing() {
    BpmTaskEntity existing = new BpmTaskEntity();
    existing.setTaskId(22L);
    existing.setDefinitionNodeId(12L);
    existing.setEngineTaskId("engine-task-2");
    existing.setEngineProcessInstanceId("process-1");
    BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
    node.setDefinitionNodeId(12L);
    node.setNodeKey("finance_review_2");
    node.setCompiledNodeSnapshotJson("{\"approvalMode\":\"sequential\","
            + "\"approvalGroupKey\":\"finance_review\","
            + "\"approvalGroupName\":\"财务复核\","
            + "\"sequentialIndex\":2,\"sequentialTotal\":3}");
    when(bpmTaskDao.selectOne(any())).thenReturn(existing);
    when(bpmDefinitionNodeDao.selectOne(any())).thenReturn(node);
    when(bpmApprovalGroupService.assignApprovalGroup(any(), same(node), same(existing)))
            .thenReturn(31L);

    service.syncActiveTasksForInstance(8L);

    verify(bpmTaskDao).updateById(argThat(update ->
            update.getTaskId().equals(existing.getTaskId())
                    && update.getApprovalGroupId().equals(31L)));
}
```

- [ ] **Step 3: Run the two test classes and confirm RED**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmApprovalGroupServiceTest,BpmTaskProjectionServiceTest' test
```

Expected: FAIL because parsing is parallel-only and existing tasks return before group restoration.

- [ ] **Step 4: Generalize the frozen snapshot parser**

Replace `ParallelGroupSnapshot` with:

```java
private record ApprovalGroupNodeSnapshot(
        String groupKey,
        String groupName,
        String approvalMode,
        Integer memberIndex,
        Integer memberTotal
) {
    boolean isParallelAll() {
        return "parallelAll".equals(approvalMode);
    }

    boolean isSequential() {
        return "sequential".equals(approvalMode);
    }
}
```

Implement `parseApprovalGroupSnapshot` so it reads `parallelIndex/parallelTotal` for `parallelAll`, `sequentialIndex/sequentialTotal` for `sequential`, and returns `null` unless key/name are nonblank and `1 <= memberIndex <= memberTotal` with `memberTotal >= 2`. Before returning `null` for a nonblank compiled snapshot, call `LOGGER.warning("审批组编译快照无效，definitionNodeId=" + definitionNode.getDefinitionNodeId())`; never infer missing fields from task names.

Keep the public creation signature unchanged:

```java
public Long assignApprovalGroup(
        BpmInstanceEntity instance,
        BpmDefinitionNodeEntity definitionNode,
        BpmTaskEntity memberTask
)
```

Set `group.approvalMode` from the parsed snapshot. If an existing group has a different mode, total, instance ID or definition ID, throw `IllegalStateException("审批组快照与运行时事实不一致")`.

- [ ] **Step 5: Implement sequential recovery from real task rows**

Add private helpers with these exact signatures:

```java
private void restoreSequentialGroupMembers(
        BpmInstanceEntity instance,
        ApprovalGroupNodeSnapshot snapshot,
        Long approvalGroupId,
        BpmTaskEntity currentTask
)

private void recalculateSequentialGroup(
        BpmApprovalGroupEntity group,
        List<BpmTaskEntity> authoredTasks
)
```

`restoreSequentialGroupMembers` must:

1. Query `t_bpm_task` by both `instanceId` and `engineProcessInstanceId`.
2. Batch-load non-null `definitionNodeId` values.
3. Retain nodes whose parsed snapshot is `sequential` with the same group key.
4. Update only tasks whose `approvalGroupId` is null.
5. Include `currentTask` exactly once even when it has not been inserted yet.
6. Recalculate counts from task results; `APPROVED` closes only when approved count equals total, `REJECTED` and `RETURNED` take precedence, otherwise state remains `PENDING`.

Add:

```java
public boolean isParallelAllGroup(Long approvalGroupId) {
    if (approvalGroupId == null) {
        return false;
    }
    BpmApprovalGroupEntity group = bpmApprovalGroupDao.selectById(approvalGroupId);
    return group != null && "parallelAll".equals(group.getApprovalMode());
}
```

- [ ] **Step 6: Restore existing projected tasks before returning**

Refactor `insertTaskIfMissing` so it loads the definition node before the existing-task branch. Add:

```java
private void attachApprovalGroupIfMissing(
        BpmInstanceEntity instance,
        BpmDefinitionNodeEntity node,
        BpmTaskEntity task
) {
    if (task.getApprovalGroupId() != null || !isApprovalGroupNode(node)) {
        return;
    }
    Long groupId = bpmApprovalGroupService.assignApprovalGroup(instance, node, task);
    BpmTaskEntity update = new BpmTaskEntity();
    update.setTaskId(task.getTaskId());
    update.setApprovalGroupId(groupId);
    bpmTaskDao.updateById(update);
}
```

For new tasks, call `assignApprovalGroup` before insert. Rename `isParallelApprovalGroupNode` to `isApprovalGroupNode` and make it validate both frozen modes through the same field rules as the service parser.

- [ ] **Step 7: Run projection/group tests GREEN**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmApprovalGroupServiceTest,BpmTaskProjectionServiceTest' test
```

Expected: PASS, including existing parallel creation and concurrent unique-key recovery tests.

- [ ] **Step 8: Commit persisted projection behavior**

```powershell
git add -- 'hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmApprovalGroupService.java' 'hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskProjectionService.java' 'hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmApprovalGroupServiceTest.java' 'hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskProjectionServiceTest.java'
git commit -m 'feat(bpm): 投影并恢复顺序审批组'
```

### Task 3: Implement mode-aware actions and cancel-on-return semantics

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmApprovalGroupServiceTest.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskServiceTest.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskAdvancedActionServiceTest.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmApprovalGroupService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`

**Interfaces:**
- Consumes: Task 2 mode-aware group records.
- Produces: `handleMemberAction` dispatch for both modes, cancel-on-return for grouped and ordinary tasks, mode-aware add/reduce guards.

- [ ] **Step 1: Add failing sequential action tests**

Add cases to `BpmApprovalGroupServiceTest` that assert:

```java
@Test
void approveSequentialMemberShouldKeepGroupPendingUntilFinalMember() {
    BpmApprovalGroupActionResult result = service.handleMemberAction(
            21L,
            BpmApprovalMemberAction.APPROVE,
            employee(101L, "审批人甲"),
            "同意"
    );

    assertThat(result.processed()).isTrue();
    assertThat(result.groupState()).isEqualTo(BpmApprovalGroupStateEnum.PENDING);
    verify(taskGateway).complete("engine-task-1");
    verify(actionLogDao).insert(argThat(log -> "APPROVED".equals(log.getActionType())));
    verify(groupDao).updateById(argThat(group ->
            group.getProcessedMemberCount() == 1
                    && group.getApprovedMemberCount() == 1
                    && "PENDING".equals(group.getGroupState())));
}

@Test
void returnSequentialMemberShouldCancelEngineAndCloseGroup() {
    BpmApprovalGroupActionResult result = service.handleMemberAction(
            22L,
            BpmApprovalMemberAction.RETURN,
            employee(102L, "审批人乙"),
            "补充材料"
    );

    assertThat(result.waitResubmit()).isTrue();
    verify(processGateway).cancel("process-8", "审批退回发起人");
    verify(taskGateway, never()).complete(anyString());
    verify(actionLogDao).insert(argThat(log ->
            "RETURNED_TO_INITIATOR".equals(log.getActionType())));
}
```

Add final-member and reject coverage:

```java
@Test
void approveFinalSequentialMemberShouldCloseGroupAndFinishInstance() {
    BpmApprovalGroupEntity group = buildPendingGroup(21L, 3);
    group.setApprovalMode("sequential");
    group.setProcessedMemberCount(2);
    group.setApprovedMemberCount(2);
    BpmTaskEntity currentTask = buildMemberTask(13L, 103L);
    prepareLockedAction(group, currentTask, List.of(currentTask));

    BpmApprovalGroupActionResult result = service.handleMemberAction(
            13L,
            BpmApprovalMemberAction.APPROVE,
            employee(103L, "审批人丙"),
            "同意"
    );

    assertThat(result.groupState()).isEqualTo(BpmApprovalGroupStateEnum.APPROVED);
    assertThat(result.finishInstanceResultState())
            .isEqualTo(BpmInstanceResultStateEnum.APPROVED);
    verify(groupDao).updateById(argThat(update ->
            "APPROVED".equals(update.getGroupState())
                    && update.getProcessedMemberCount() == 3
                    && update.getApprovedMemberCount() == 3));
}

@Test
void rejectSequentialMemberShouldCancelEngineAndCloseGroup() {
    BpmApprovalGroupEntity group = buildPendingGroup(21L, 3);
    group.setApprovalMode("sequential");
    BpmTaskEntity currentTask = buildMemberTask(12L, 102L);
    prepareLockedAction(group, currentTask, List.of(currentTask));

    BpmApprovalGroupActionResult result = service.handleMemberAction(
            12L,
            BpmApprovalMemberAction.REJECT,
            employee(102L, "审批人乙"),
            "不同意"
    );

    assertThat(result.groupState()).isEqualTo(BpmApprovalGroupStateEnum.REJECTED);
    assertThat(result.finishInstanceResultState())
            .isEqualTo(BpmInstanceResultStateEnum.REJECTED);
    verify(processGateway).cancel("process-8", "审批驳回");
    verify(taskGateway, never()).complete(anyString());
}
```

Add normal import `BpmInstanceResultStateEnum` and static Mockito imports `argThat` and `anyString`.

- [ ] **Step 2: Add the ordinary return characterization test**

Update `BpmRuntimeCommandServiceTest` so the ordinary return case includes another pending add-sign projection and asserts:

```java
BpmTaskEntity otherPendingAddSignTask = new BpmTaskEntity();
otherPendingAddSignTask.setTaskId(2L);
otherPendingAddSignTask.setInstanceId(8L);
otherPendingAddSignTask.setTaskState(BpmTaskStateEnum.PENDING.getValue());
otherPendingAddSignTask.setRuntimeAssignmentSnapshotJson(
        "{\"addSign\":true,\"sourceTaskId\":1}"
);
when(bpmTaskDao.selectList(any())).thenReturn(List.of(otherPendingAddSignTask));
verify(processGateway).cancel("process-8", "审批退回发起人");
verify(taskGateway, never()).complete("task-1");
assertThat(instanceCaptor.getValue().getRunState())
        .isEqualTo(BpmInstanceRunStateEnum.WAIT_RESUBMIT.getValue());
verify(bpmTaskDao).updateById(argThat(update ->
        update.getTaskId().equals(otherPendingAddSignTask.getTaskId())
                && update.getTaskState().equals(BpmTaskStateEnum.CANCELLED.getValue())
                && update.getTaskResult().equals(BpmTaskResultEnum.RETURNED.getValue())));
```

Expected RED: the current implementation completes the task instead of cancelling the process.

- [ ] **Step 3: Add mode-aware add/reduce tests**

In `BpmTaskAdvancedActionServiceTest`, promote the approval-group mock to a field:

```java
private BpmApprovalGroupService bpmApprovalGroupService;
```

Initialize and inject that field in `setUp`, then add:

```java
@Test
void addSignShouldRemainAvailableForSequentialApprovalGroupMember() {
    BpmTaskEntity taskEntity = buildPendingTask();
    taskEntity.setApprovalGroupId(31L);
    when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
    when(bpmApprovalGroupService.isParallelAllGroup(31L)).thenReturn(false);
    when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
    when(identityGateway().requireEmployee(100L)).thenReturn(
            new BpmEmployeeSnapshot(100L, "王主管", 7L, "业务部", null, null));
    when(identityGateway().requireEmployee(300L)).thenReturn(
            new BpmEmployeeSnapshot(300L, "赵六", 10L, "法务部", null, null));

    BpmTaskAddSignForm form = new BpmTaskAddSignForm();
    form.setTaskId(1L);
    form.setTargetEmployeeId(300L);
    form.setReason("请法务复核");

    ResponseDTO<String> response = bpmTaskService.addSign(form);

    assertThat(response.getOk()).isTrue();
    verify(bpmTaskDao).insert(argThat(task -> task.getApprovalGroupId() == null));
}

@Test
void addAndReduceSignShouldRemainBlockedForParallelApprovalGroupMember() {
    BpmTaskEntity taskEntity = buildPendingTask();
    taskEntity.setApprovalGroupId(32L);
    when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
    when(bpmApprovalGroupService.isParallelAllGroup(32L)).thenReturn(true);

    BpmTaskAddSignForm addForm = new BpmTaskAddSignForm();
    addForm.setTaskId(1L);
    addForm.setTargetEmployeeId(300L);
    BpmTaskReduceSignForm reduceForm = new BpmTaskReduceSignForm();
    reduceForm.setTaskId(1L);

    assertThat(bpmTaskService.addSign(addForm).getMsg())
            .contains("并行全员会签成员不支持加签或减签");
    assertThat(bpmTaskService.reduceSign(reduceForm).getMsg())
            .contains("并行全员会签成员不支持加签或减签");
}
```

In the existing `reduceSignShouldCancelPendingAddSignTaskAndWriteActionLog`, set `approvalGroupId=31L` and stub `isParallelAllGroup(31L)` to return `false`; all existing cancellation assertions must stay unchanged.

- [ ] **Step 4: Run the action tests and confirm RED**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmApprovalGroupServiceTest,BpmTaskServiceTest,BpmRuntimeCommandServiceTest,BpmTaskAdvancedActionServiceTest' test
```

Expected: FAIL on sequential dispatch, ordinary cancel-on-return, and add/reduce mode checks.

- [ ] **Step 5: Dispatch group actions by `approvalMode`**

Keep the public method:

```java
public BpmApprovalGroupActionResult handleMemberAction(
        Long taskId,
        BpmApprovalMemberAction action,
        BpmEmployeeSnapshot actor,
        String commentText
)
```

After locking group then task, dispatch explicitly:

```java
if ("parallelAll".equals(group.getApprovalMode())) {
    return handleParallelMemberAction(group, currentTask, pendingTasks, action, actor, commentText, now);
}
if ("sequential".equals(group.getApprovalMode())) {
    return handleSequentialMemberAction(group, currentTask, action, actor, commentText, now);
}
throw new IllegalStateException("不支持的审批组模式: " + group.getApprovalMode());
```

Add these exact mode entry points:

```java
private BpmApprovalGroupActionResult handleParallelMemberAction(
        BpmApprovalGroupEntity group,
        BpmTaskEntity currentTask,
        List<BpmTaskEntity> pendingTasks,
        BpmApprovalMemberAction action,
        BpmEmployeeSnapshot actor,
        String commentText,
        LocalDateTime actionAt
) {
    return action == BpmApprovalMemberAction.APPROVE
            ? approveMember(group, currentTask, actor, commentText, actionAt)
            : closeByMember(group, currentTask, pendingTasks, action, actor, commentText, actionAt);
}

private BpmApprovalGroupActionResult handleSequentialMemberAction(
        BpmApprovalGroupEntity group,
        BpmTaskEntity currentTask,
        BpmApprovalMemberAction action,
        BpmEmployeeSnapshot actor,
        String commentText,
        LocalDateTime actionAt
) {
    return switch (action) {
        case APPROVE -> approveSequentialMember(group, currentTask, actor, commentText, actionAt);
        case REJECT, RETURN -> closeSequentialMember(
                group, currentTask, action, actor, commentText, actionAt);
    };
}
```

Add `approveSequentialMember(...)` and `closeSequentialMember(...)` with the same parameter order shown by their calls. `approveSequentialMember` completes one engine task, writes ordinary `APPROVED`, increments processed/approved counts, and returns `finishInstanceResultState=APPROVED` only when approved count equals total. `closeSequentialMember` cancels the engine process, writes ordinary `REJECTED` or `RETURNED_TO_INITIATOR`, closes the group with the matching state/reason, and returns either `finishInstanceResultState=REJECTED` or `waitResubmit=true`. Preserve all existing `PARALLEL_*` action types inside `approveMember` and `closeByMember`.

- [ ] **Step 6: Make ordinary return cancel the process**

In `BpmTaskService.returnToInitiator`, replace the ordinary `flowableTaskGateway.complete(...)` call with:

```java
flowableProcessInstanceGateway.cancel(
        taskEntity.getEngineProcessInstanceId(),
        "审批退回发起人"
);
```

After updating the current task, close all other pending task projections for the same instance. Add one mode-independent helper:

```java
private void closeOtherPendingTasks(
        Long instanceId,
        Long currentTaskId,
        BpmTaskResultEnum closeResult,
        LocalDateTime actionAt
)
```

The helper must query pending tasks by `instanceId`, skip `currentTaskId`, set state `CANCELLED`, result `closeResult`, `cancelledAt`, and `lastActionAt`, and never call Flowable complete.

Call it with `RETURNED` after ordinary return and after a grouped action returns `waitResubmit=true`. Call it with `REJECTED` after ordinary or grouped rejection. This closes group-external add-sign projections as well as any stale ordinary projection; already closed parallel siblings are not selected because the query only returns `PENDING` rows.

- [ ] **Step 7: Make advanced-action guards mode-aware**

Replace both raw `approvalGroupId != null` guards with:

```java
if (bpmApprovalGroupService.isParallelAllGroup(taskEntity.getApprovalGroupId())) {
    return ResponseDTO.userErrorParam("并行全员会签成员不支持加签或减签");
}
```

Do not copy `approvalGroupId` in `buildAddSignTask`.

- [ ] **Step 8: Run action tests GREEN**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmApprovalGroupServiceTest,BpmTaskServiceTest,BpmRuntimeCommandServiceTest,BpmTaskAdvancedActionServiceTest' test
```

Expected: PASS with ordinary and sequential return using process cancellation, while parallel action tests remain unchanged.

- [ ] **Step 9: Commit runtime action semantics**

```powershell
git add -- 'hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmApprovalGroupService.java' 'hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java' 'hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmApprovalGroupServiceTest.java' 'hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskServiceTest.java' 'hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java' 'hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskAdvancedActionServiceTest.java'
git commit -m 'fix(bpm): 闭环顺序审批组动作语义'
```

### Task 4: Expose the contract and mode-aware frontend experience

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeDetailServiceTest.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmInstanceTraceServiceTest.java`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-approval-group-panel.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-todo-list.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

**Interfaces:**
- Consumes: existing backend VO fields with `approvalMode=sequential`.
- Produces: `BpmApprovalMode`, mode labels, pending-activation count, mode-aware action visibility.

- [ ] **Step 1: Lock backend detail and trace passthrough**

Add assertions to existing detail/trace tests:

```java
assertThat(detail.getApprovalGroups()).singleElement().satisfies(group -> {
    assertThat(group.getApprovalMode()).isEqualTo("sequential");
    assertThat(group.getApprovalGroupKey()).isEqualTo("finance_review");
    assertThat(group.getProcessedMemberCount()).isEqualTo(1);
    assertThat(group.getTotalMemberCount()).isEqualTo(3);
    assertThat(group.getMembers()).extracting(BpmApprovalGroupMemberVO::getMemberIndex)
            .containsExactly(1, 2);
});
```

Trace must return the same group list from instance detail without reparsing task names.

- [ ] **Step 2: Add failing frontend contracts**

Extend `bpm-api.test.ts` and `bpm-modules.test.ts` with:

```ts
expect(runtimeApiSource).toContain(
  "export type BpmApprovalMode = 'parallelAll' | 'sequential'",
);
expect(runtimeApiSource).toContain('approvalMode: BpmApprovalMode');
expect(groupPanelSource).toContain("approvalMode === 'sequential'");
expect(groupPanelSource).toContain('后续');
expect(todoSource).toContain(
  "row.approvalGroup?.approvalMode !== 'parallelAll'",
);
expect(todoSource).not.toContain('v-if="!row.approvalGroup"');
```

- [ ] **Step 3: Run frontend RED**

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: FAIL because the union type, mode label, pending text and mode-aware guards do not exist.

- [ ] **Step 4: Extend the TypeScript contract**

Add before the summary interface:

```ts
export type BpmApprovalMode = 'parallelAll' | 'sequential';
```

Change the summary field to:

```ts
approvalMode: BpmApprovalMode;
```

- [ ] **Step 5: Add mode label and pending activation count**

In `bpm-approval-group-panel.vue`, add:

```ts
function getApprovalModeLabel(mode: BpmApprovalGroupDetailRecord['approvalMode']) {
  return mode === 'sequential' ? '顺序审批' : '并行会签';
}

function getPendingActivationCount(group: BpmApprovalGroupDetailRecord) {
  if (group.approvalMode !== 'sequential') return 0;
  return Math.max(group.totalMemberCount - group.members.length, 0);
}
```

Render the mode label beside the group name and render:

```vue
<span v-if="getPendingActivationCount(approvalGroup) > 0">
  后续 {{ getPendingActivationCount(approvalGroup) }} 人待激活
</span>
```

Do not add placeholder rows to the members table.

- [ ] **Step 6: Narrow action visibility to parallel mode**

For both add-sign and reduce-sign items use:

```vue
v-if="row.approvalGroup?.approvalMode !== 'parallelAll'"
```

Keep transfer, delegate and recall unchanged.

- [ ] **Step 7: Run backend detail and frontend GREEN gates**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmRuntimeDetailServiceTest,BpmInstanceTraceServiceTest' test
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: all commands exit `0`; existing parallel panel and action contract tests remain green.

- [ ] **Step 8: Commit public contract and frontend**

```powershell
git add -- 'hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeDetailServiceTest.java' 'hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmInstanceTraceServiceTest.java' 'hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts' 'hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts' 'hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-approval-group-panel.vue' 'hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-todo-list.vue' 'hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts'
git commit -m 'feat(bpm): 展示顺序审批组进度'
```

### Task 5: Run total gates, live acceptance, and close the baseline

**Files:**
- Create: `docs/superpowers/specs/2026-07-11-bpm-sequential-approval-group-acceptance.md`
- Modify: `docs/superpowers/specs/2026-07-10-bpm-development-baseline.md`

**Interfaces:**
- Consumes: Tasks 1-4 completed behavior.
- Produces: current-run verification evidence and updated durable baseline.

- [ ] **Step 1: Run the focused backend gate**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=SimpleModelBpmnCompilerTest,SimpleModelValidatorTest,BpmApprovalGroupServiceTest,BpmTaskProjectionServiceTest,BpmTaskServiceTest,BpmRuntimeCommandServiceTest,BpmTaskAdvancedActionServiceTest,BpmRuntimeDetailServiceTest,BpmInstanceTraceServiceTest' test
```

Expected: all listed tests pass with zero failures and errors. Record the actual test count and completion time.

- [ ] **Step 2: Run module and Flowable compatibility gates**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin '-Dtest=BpmFlowableCompatibilityTest' test
```

Expected: both commands report `BUILD SUCCESS`. Historical counts must not be copied into the acceptance note.

- [ ] **Step 3: Run frontend contract and type gates**

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts --dom
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: all files/tests pass and typecheck exits `0`.

- [ ] **Step 4: Execute the real runtime matrix**

Follow `docs/playwright-mcp-business-flow-verification.md`, reuse the persistent visible session, and store temporary evidence outside the repository. Use one three-member sequential definition and prove:

```text
S1 start: group sequential, 0/3, only member 1 task exists
S2 approve member 1: 1/3, only member 2 is active
S3 approve member 2: 2/3, only member 3 is active
S4 approve member 3: APPROVED 3/3, instance finished
S5 reject member 2: group REJECTED, no Flowable active task
S6 return member 1 and member 2: group RETURNED, WAIT_RESUBMIT, old engine has no active task
S7 resubmit: new engine process and new group ID; old group remains RETURNED
S8 transfer/delegate: same group ID/index/total, assignee snapshot changes
S9 sequential add/reduce available and excluded from authored progress
S10 parallelAll still hides/rejects add/reduce and converges normally
S11 ordinary single task returns approvalGroup=null and keeps existing behavior
```

Capture API responses for group IDs, progress, state, engine process ID, active task count, and action logs. Capture browser evidence for the list summary, detail panel, pending-activation text and action menu.

- [ ] **Step 5: Write the acceptance record**

Create the acceptance note with these concrete sections:

```markdown
# BPM 顺序多人审批组投影与异常路径闭环验收记录

## 交付范围
## 自动化门禁
## 真实验收对象
## S1-S11 运行态证据
## 普通任务与 parallelAll 回归
## 已知边界
## 结论
```

Populate only actual commands, counts, IDs, timestamps and outcomes from Steps 1-4. State explicitly that future members show only a count and historical completed sequential instances are not backfilled.

- [ ] **Step 6: Update the BPM baseline**

In `2026-07-10-bpm-development-baseline.md`:

1. Extend the sequential approval section with structured group identity, progress, action boundary and resync compatibility.
2. Remove “结构化 authored 审批组” from current candidates.
3. Keep business-document integration as the remaining independently evaluated candidate.
4. Add the new acceptance note to source navigation.
5. Replace historical test counts only by adding a new dated evidence bullet; do not rewrite old evidence as current.

- [ ] **Step 7: Verify docs and workspace boundaries**

```powershell
rg -n --encoding utf-8 'T[B]D|T[O]DO|F[I]XME|待[定]' 'E:/my-project/hunyuan-pro/docs/superpowers/specs/2026-07-11-bpm-sequential-approval-group-acceptance.md' 'E:/my-project/hunyuan-pro/docs/superpowers/specs/2026-07-10-bpm-development-baseline.md'
```

Expected: no output.

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors; staged intent must exclude pre-existing `AGENTS.md`, Playwright runbook and the old parallel implementation plan unless the user explicitly includes them.

- [ ] **Step 8: Commit closure documentation**

```powershell
git add -- 'docs/superpowers/specs/2026-07-11-bpm-sequential-approval-group-acceptance.md' 'docs/superpowers/specs/2026-07-10-bpm-development-baseline.md'
git commit -m 'docs(bpm): 记录顺序审批组验收结果'
```

## Final Review Checklist

- [ ] Every sequential expanded node contains group key/name and mode-specific index/total.
- [ ] Runtime never derives group semantics from task names or key suffixes.
- [ ] Group recovery filters by both Hunyuan instance and current engine process.
- [ ] Duplicate resync and duplicate actions do not double-count or re-drive Flowable.
- [ ] Ordinary/sequential return cancels the current engine process before `WAIT_RESUBMIT`.
- [ ] Sequential add/reduce remains available; parallel add/reduce remains rejected.
- [ ] Future members are represented only by a count.
- [ ] Ordinary and parallel compatibility gates pass.
- [ ] Current-run live evidence is archived and the baseline is updated.
- [ ] Pre-existing unrelated worktree changes remain untouched.
