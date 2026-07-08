# BPM 实例详情与流转轨迹 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 BPM 实例详情在管理员端和员工端统一展示基础信息、当前待办、表单快照和动作轨迹。

**Architecture:** 后端继续以 `BpmInstanceService#getDetail(Long instanceId)` 作为实例详情聚合入口，在现有 `BpmInstanceDetailVO` 上增加 `currentTasks`，并通过 `BpmTaskDao` 查询当前待办任务。前端以现有 `bpm-instance-detail-drawer.vue` 作为统一详情抽屉，员工端和管理员端实例列表复用同一组件，任务详情保留本地任务弹层。

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, Maven, Vue 3, TypeScript, Element Plus, Vitest, Vben ArtTable primitives

## Global Constraints

- 以 Hunyuan 自有运行表和动作日志为事实来源，不把 Flowable 内部对象暴露给前端业务页面。
- 先复用现有 `BpmTaskActionLogVO`，只在现有字段不足时增加 Hunyuan 语义字段。
- 管理员端和员工端共用同一套详情展示语义，权限由入口接口和菜单控制。
- 保持增量改造，不重做现有 BPM runtime，不引入新依赖。
- 所有新增行为必须有 Maven 或 Vitest 契约测试。
- 本阶段不做 BPMN 图高亮、会签、或签、加签、减签、委派、管理员跳转节点。
- 不复制 Yudao/RuoYi 的 API 路径、页面壳、权限模型或 Flowable 对象结构。
- 使用 UTF-8 保存中文文档、测试数据和提交信息。
- 不提交 `.superpowers/`、`test-results/`、`hunyuan-design/pnpm-workspace.yaml`、`lefthook.yml`、`hunyuan-system-home-snapshot.md` 等非本功能噪声。

---

## File Structure

- `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceDetailVO.java`
  - 实例详情返回对象，新增 `currentTasks`。
- `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmTaskDao.java`
  - 新增实例维度当前待办查询接口。
- `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmTaskMapper.xml`
  - 新增 `queryCurrentTasksByInstanceId` SQL，稳定排序当前待办。
- `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
  - 在实例详情聚合中填充 `currentTasks`。
- `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeDetailServiceTest.java`
  - 扩展实例详情单测，覆盖当前待办、动作轨迹和不存在实例。
- `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskDetailServiceTest.java`
  - 扩展任务详情不存在实例测试，并修正测试中文样例。
- `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
  - 前端实例详情类型增加 `currentTasks`。
- `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue`
  - 增强统一详情抽屉，展示当前待办、状态、动作轨迹扩展信息。
- `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/instance/instance-list.vue`
  - 管理员实例列表改用统一实例详情抽屉。
- `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
  - 扩展前端契约测试，锁住统一详情组件复用和当前待办展示。

---

### Task 1: 后端实例详情返回当前待办

**Files:**
- Modify: `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeDetailServiceTest.java`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceDetailVO.java`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmTaskDao.java`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmTaskMapper.xml`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`

**Interfaces:**
- Consumes: `BpmTaskStateEnum.PENDING.getValue()` equals `1`; existing `BpmTaskVO`.
- Produces: `BpmInstanceDetailVO#getCurrentTasks(): List<BpmTaskVO>` and `BpmTaskDao#queryCurrentTasksByInstanceId(Long instanceId)`.

- [ ] **Step 1: Write the failing backend detail test**

In `BpmRuntimeDetailServiceTest`, add imports:

```java
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO;
import java.time.LocalDateTime;
```

Update `getDetailShouldReturnInstanceAndActionLogs` setup to inject `BpmTaskDao`:

```java
BpmTaskDao taskDao = Mockito.mock(BpmTaskDao.class);
setField(service, "bpmTaskDao", taskDao);
```

Add this test method:

```java
@Test
void getDetailShouldReturnCurrentPendingTasksInStableOrder() {
    BpmInstanceService service = new BpmInstanceService();
    BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
    BpmTaskDao taskDao = Mockito.mock(BpmTaskDao.class);
    BpmTaskActionLogDao actionLogDao = Mockito.mock(BpmTaskActionLogDao.class);
    setField(service, "bpmInstanceDao", instanceDao);
    setField(service, "bpmTaskDao", taskDao);
    setField(service, "bpmTaskActionLogDao", actionLogDao);

    BpmInstanceEntity instance = new BpmInstanceEntity();
    instance.setInstanceId(8L);
    instance.setInstanceNo("SN-2026-0001");
    instance.setTitle("请假申请");
    instance.setStartEmployeeNameSnapshot("张三");
    instance.setCurrentFormDataSnapshotJson("{\"days\":1}");

    BpmTaskVO firstTask = new BpmTaskVO();
    firstTask.setTaskId(18L);
    firstTask.setInstanceId(8L);
    firstTask.setInstanceNo("SN-2026-0001");
    firstTask.setInstanceTitle("请假申请");
    firstTask.setTaskName("部门审批");
    firstTask.setAssigneeNameSnapshot("李四");
    firstTask.setAssignedAt(LocalDateTime.of(2026, 7, 8, 9, 0));

    BpmTaskVO secondTask = new BpmTaskVO();
    secondTask.setTaskId(19L);
    secondTask.setInstanceId(8L);
    secondTask.setInstanceNo("SN-2026-0001");
    secondTask.setInstanceTitle("请假申请");
    secondTask.setTaskName("人事审批");
    secondTask.setAssigneeNameSnapshot("王五");
    secondTask.setAssignedAt(LocalDateTime.of(2026, 7, 8, 9, 5));

    when(instanceDao.selectById(8L)).thenReturn(instance);
    when(taskDao.queryCurrentTasksByInstanceId(8L)).thenReturn(List.of(firstTask, secondTask));
    when(actionLogDao.queryByInstanceId(8L)).thenReturn(List.of());

    ResponseDTO<BpmInstanceDetailVO> response = service.getDetail(8L);

    assertThat(response.getOk()).isTrue();
    assertThat(response.getData().getCurrentTasks()).extracting(BpmTaskVO::getTaskId)
            .containsExactly(18L, 19L);
    assertThat(response.getData().getCurrentTasks().get(0).getTaskName()).isEqualTo("部门审批");
    assertThat(response.getData().getActionLogs()).isEmpty();
}
```

Add this missing-instance test:

```java
@Test
void getDetailShouldReturnDataNotExistWhenInstanceMissing() {
    BpmInstanceService service = new BpmInstanceService();
    BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
    setField(service, "bpmInstanceDao", instanceDao);

    when(instanceDao.selectById(404L)).thenReturn(null);

    ResponseDTO<BpmInstanceDetailVO> response = service.getDetail(404L);

    assertThat(response.getOk()).isFalse();
    assertThat(response.getCode()).isNotBlank();
}
```

- [ ] **Step 2: Run the focused backend test and verify RED**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmRuntimeDetailServiceTest test
```

Expected: compilation fails because `BpmTaskDao#queryCurrentTasksByInstanceId` and `BpmInstanceDetailVO#getCurrentTasks` do not exist.

- [ ] **Step 3: Add the currentTasks VO field**

In `BpmInstanceDetailVO`, add:

```java
@Schema(description = "当前待办任务")
private List<BpmTaskVO> currentTasks;
```

No extra import is needed because `BpmTaskVO` is in the same package.

- [ ] **Step 4: Add DAO interface method**

In `BpmTaskDao`, add:

```java
/**
 * 查询流程实例当前待办任务。
 */
List<BpmTaskVO> queryCurrentTasksByInstanceId(@Param("instanceId") Long instanceId);
```

- [ ] **Step 5: Add MyBatis SQL with stable ordering**

In `BpmTaskMapper.xml`, after `queryPage`, add:

```xml
<select id="queryCurrentTasksByInstanceId" resultType="com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO">
    select
        task_id as taskId,
        instance_id as instanceId,
        task_name as taskName,
        instance_no as instanceNo,
        instance_title as instanceTitle,
        task_state as taskState,
        task_result as taskResult,
        assignee_name_snapshot as assigneeNameSnapshot,
        assigned_at as assignedAt,
        completed_at as completedAt
    from t_bpm_task
    where instance_id = #{instanceId}
      and task_state = 1
    order by assigned_at asc, task_id asc
</select>
```

- [ ] **Step 6: Fill currentTasks in the instance detail service**

In `BpmInstanceService#getDetail`, after setting `finishedAt`, add:

```java
detail.setCurrentTasks(bpmTaskDao.queryCurrentTasksByInstanceId(instanceId));
```

Keep the existing line:

```java
detail.setActionLogs(bpmTaskActionLogDao.queryByInstanceId(instanceId));
```

- [ ] **Step 7: Run focused backend tests and verify GREEN**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmRuntimeDetailServiceTest test
```

Expected: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`.

- [ ] **Step 8: Run BPM module tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

Expected: all BPM module tests pass.

- [ ] **Step 9: Commit backend detail aggregation**

Run:

```powershell
git add -- `
  'hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeDetailServiceTest.java' `
  'hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceDetailVO.java' `
  'hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmTaskDao.java' `
  'hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmTaskMapper.xml' `
  'hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java'
git diff --cached --name-only
git -c i18n.commitEncoding=UTF-8 -c i18n.logOutputEncoding=UTF-8 commit -m "feat: 补齐BPM实例详情当前待办"
```

Expected staged files: only the five files listed above.

---

### Task 2: 任务详情测试补齐不存在任务与中文样例

**Files:**
- Modify: `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskDetailServiceTest.java`

**Interfaces:**
- Consumes: `BpmTaskService#getDetail(Long taskId)`.
- Produces: regression coverage for missing task and readable UTF-8 Chinese test fixtures.

- [ ] **Step 1: Write the failing missing-task assertion**

In `BpmTaskDetailServiceTest`, add:

```java
@Test
void getDetailShouldReturnDataNotExistWhenTaskMissing() {
    BpmTaskService service = new BpmTaskService();
    BpmTaskDao taskDao = Mockito.mock(BpmTaskDao.class);
    setField(service, "bpmTaskDao", taskDao);

    when(taskDao.selectById(404L)).thenReturn(null);

    ResponseDTO<BpmTaskDetailVO> response = service.getDetail(404L);

    assertThat(response.getOk()).isFalse();
    assertThat(response.getCode()).isNotBlank();
}
```

- [ ] **Step 2: Run focused task detail test**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmTaskDetailServiceTest test
```

Expected: test passes if missing-task handling already exists. If it fails with `NullPointerException`, continue to Step 4.

- [ ] **Step 3: Fix readable Chinese fixtures if the file still contains mojibake**

Run a UTF-8-safe scan for known mojibake code points in this test file:

```powershell
$path = 'hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskDetailServiceTest.java'
$text = Get-Content -LiteralPath $path -Encoding UTF8 -Raw
$badCodePoints = @(0x7487, 0x9435, 0x6d93, 0x6d5c, 0x9422, 0x95ae, 0xfffd)
foreach ($codePoint in $badCodePoints) {
  $char = [char]$codePoint
  if ($text.Contains($char)) {
    Write-Output "$path contains suspicious code point U+$('{0:X4}' -f $codePoint)"
  }
}
```

If it reports the existing assignee or actor name fixtures, replace those test values with readable Chinese:

```java
task.setAssigneeNameSnapshot("李四");
log.setActorNameSnapshot("王五");
```

- [ ] **Step 4: Minimal service fix only if Step 2 failed**

If `BpmTaskService#getDetail` does not already return `DATA_NOT_EXIST`, add this guard immediately after `selectById`:

```java
if (taskEntity == null) {
    return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
}
```

- [ ] **Step 5: Run focused task detail test again**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmTaskDetailServiceTest test
```

Expected: `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`.

- [ ] **Step 6: Commit task detail regression coverage**

Run:

```powershell
git add -- 'hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskDetailServiceTest.java'
git diff --cached --name-only
git -c i18n.commitEncoding=UTF-8 -c i18n.logOutputEncoding=UTF-8 commit -m "test: 补齐BPM任务详情缺失用例"
```

If Step 4 changed `BpmTaskService.java`, include it in `git add`:

```powershell
git add -- `
  'hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskDetailServiceTest.java' `
  'hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java'
```

---

### Task 3: 前端类型与统一实例详情抽屉增强

**Files:**
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue`

**Interfaces:**
- Consumes: `BpmInstanceDetailRecord` returned by `/app/bpm/instance/detail/{instanceId}` and `/bpm/instance/detail/{instanceId}`.
- Produces: frontend type `BpmInstanceDetailRecord.currentTasks?: BpmTaskRecord[]` and drawer sections for current tasks, form snapshot, action logs.

- [ ] **Step 1: Write failing frontend contract tests**

In `bpm-modules.test.ts`, add assertions to the existing runtime detail drawer test:

```ts
expect(detailSource).toContain('currentTasks');
expect(detailSource).toContain('当前待办');
expect(detailSource).toContain('暂无当前待办');
expect(detailSource).toContain('fromAssigneeEmployeeId');
expect(detailSource).toContain('toAssigneeEmployeeId');
```

Add a runtime API type assertion near the runtime API test:

```ts
expect(runtimeApiSource).toContain('currentTasks?: BpmTaskRecord[]');
```

- [ ] **Step 2: Run frontend contract tests and verify RED**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: tests fail because `currentTasks` and the current-task drawer labels do not exist yet.

- [ ] **Step 3: Add currentTasks to frontend API type**

In `runtime.ts`, update `BpmInstanceDetailRecord`:

```ts
export interface BpmInstanceDetailRecord extends BpmInstanceRecord {
  actionLogs: BpmTaskActionLogRecord[];
  currentFormDataSnapshotJson?: null | string;
  currentNodeSummaryJson?: null | string;
  currentTasks?: BpmTaskRecord[];
  startDepartmentNameSnapshot?: null | string;
  summary?: null | string;
}
```

- [ ] **Step 4: Normalize arrays and add current task display in drawer**

In `bpm-instance-detail-drawer.vue`, import `computed`:

```ts
import { computed, ref } from 'vue';
```

Add computed helpers:

```ts
const currentTasks = computed(() => detail.value?.currentTasks ?? []);
const actionLogs = computed(() => detail.value?.actionLogs ?? []);
```

Add current tasks block after the basic descriptions and before the form snapshot or action timeline:

```vue
<div class="bpm-instance-detail__section-title">当前待办</div>
<div v-if="currentTasks.length > 0" class="bpm-instance-detail__current-tasks">
  <div
    v-for="task in currentTasks"
    :key="task.taskId"
    class="bpm-instance-detail__current-task"
  >
    <strong>{{ task.taskName }}</strong>
    <span>{{ task.assigneeNameSnapshot || '-' }}</span>
    <span>{{ task.assignedAt || '-' }}</span>
  </div>
</div>
<ElEmpty v-else description="暂无当前待办" />
```

Replace direct action log use:

```vue
<ElTimeline v-if="actionLogs.length > 0" class="bpm-instance-detail__timeline">
  <ElTimelineItem
    v-for="log in actionLogs"
    :key="log.actionLogId"
    :timestamp="log.actionAt || ''"
  >
```

Within each action timeline item, add transfer metadata:

```vue
<p
  v-if="log.fromAssigneeEmployeeId || log.toAssigneeEmployeeId"
  class="bpm-instance-detail__comment"
>
  {{ log.fromAssigneeEmployeeId || '-' }} -> {{ log.toAssigneeEmployeeId || '-' }}
</p>
```

Use this title class for both current tasks and timeline titles:

```vue
<div class="bpm-instance-detail__section-title">动作轨迹</div>
```

Add styles:

```css
.bpm-instance-detail__section-title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
}

.bpm-instance-detail__current-tasks {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bpm-instance-detail__current-task {
  align-items: center;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  display: grid;
  gap: 8px;
  grid-template-columns: minmax(120px, 1fr) minmax(80px, 120px) minmax(140px, 180px);
  min-height: 36px;
  padding: 8px 10px;
}
```

- [ ] **Step 5: Run frontend contract tests and verify GREEN**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: targeted frontend contract tests pass.

- [ ] **Step 6: Run frontend typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: `vue-tsc --noEmit --skipLibCheck` exits 0.

- [ ] **Step 7: Commit frontend drawer enhancement**

Run:

```powershell
git add -- `
  'hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts' `
  'hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts' `
  'hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue'
git diff --cached --name-only
git -c i18n.commitEncoding=UTF-8 -c i18n.logOutputEncoding=UTF-8 commit -m "feat: 增强BPM实例详情轨迹抽屉"
```

Expected staged files: only the three files listed above.

---

### Task 4: 管理员实例列表复用统一详情抽屉

**Files:**
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/instance/instance-list.vue`

**Interfaces:**
- Consumes: `BpmInstanceDetailDrawer` exposes `open(instanceId: number)`.
- Produces: admin instance list opens the same unified instance detail drawer as employee runtime pages.

- [ ] **Step 1: Write failing admin reuse contract test**

In `bpm-modules.test.ts`, update `keeps the admin instance page wired to a local detail dialog` into a unified-drawer assertion:

```ts
it('keeps the admin instance page wired to the unified bpm instance detail drawer', () => {
  const instanceSource = readSource(instancePagePath);

  expect(instanceSource).toContain('BpmInstanceDetailDrawer');
  expect(instanceSource).toContain('detailDrawerRef');
  expect(instanceSource).toContain('detailDrawerRef.value?.open(row.instanceId)');
  expect(instanceSource).not.toContain('ElDialog v-model="detailVisible"');
  expect(instanceSource).not.toContain('getBpmAdminInstanceDetail');
});
```

- [ ] **Step 2: Run frontend contract test and verify RED**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: test fails because admin instance page still uses local `ElDialog` and `getBpmAdminInstanceDetail`.

- [ ] **Step 3: Replace local admin detail dialog with unified drawer**

In `instance-list.vue`, remove imports that were only used by the local detail dialog:

```ts
import type { BpmInstanceDetailRecord, BpmInstanceRecord } from '#/api/system/bpm';
```

becomes:

```ts
import type { BpmInstanceRecord } from '#/api/system/bpm';
```

Remove these Element Plus imports if unused after the template edit:

```ts
ElDescriptions,
ElDescriptionsItem,
ElDialog,
ElEmpty,
ElTimeline,
ElTimelineItem,
```

Change API import:

```ts
import { getBpmAdminInstanceDetail, queryBpmInstancePage } from '#/api/system/bpm';
```

to:

```ts
import { queryBpmInstancePage } from '#/api/system/bpm';
```

Add unified drawer import:

```ts
import BpmInstanceDetailDrawer from '../runtime/components/bpm-instance-detail-drawer.vue';
```

Remove local detail refs:

```ts
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailData = ref<BpmInstanceDetailRecord>();
const detailLoadErrorMessage = ref('');
```

Add:

```ts
const detailDrawerRef = ref<InstanceType<typeof BpmInstanceDetailDrawer>>();
```

Replace `openDetailDialog` with:

```ts
function openDetail(row: BpmInstanceRecord) {
  void detailDrawerRef.value?.open(row.instanceId);
}
```

Update action button:

```vue
<ElButton link size="small" type="primary" @click="openDetail(row)">
  详情
</ElButton>
```

Delete the whole local detail dialog block that starts with:

```vue
<ElDialog v-model="detailVisible" title="流程实例详情" width="920px">
```

and ends with:

```vue
</ElDialog>
```

Then add the unified drawer before `</Page>`:

```vue
<BpmInstanceDetailDrawer ref="detailDrawerRef" />
```

Delete the complete CSS rule blocks for these selectors, because they are only used by the removed local dialog:

```css
.instance-page__detail
.instance-page__detail-error
.instance-page__detail code
.instance-page__timeline-title
.instance-page__timeline
.instance-page__timeline-row
.instance-page__comment
```

- [ ] **Step 4: Run frontend contract tests and verify GREEN**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: admin instance page reuse test passes.

- [ ] **Step 5: Run frontend typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: typecheck exits 0.

- [ ] **Step 6: Commit admin reuse change**

Run:

```powershell
git add -- `
  'hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts' `
  'hunyuan-design/apps/hunyuan-system/src/views/system/bpm/instance/instance-list.vue'
git diff --cached --name-only
git -c i18n.commitEncoding=UTF-8 -c i18n.logOutputEncoding=UTF-8 commit -m "feat: 统一BPM管理员实例详情抽屉"
```

Expected staged files: only the two files listed above.

---

### Task 5: Final verification and closure record

**Files:**
- Verify only: all files changed by Tasks 1-4
- Create: `E:/my-project/hunyuan-pro/docs/superpowers/specs/2026-07-08-bpm-instance-timeline-acceptance.md`

**Interfaces:**
- Consumes: backend and frontend changes from Tasks 1-4.
- Produces: repo-local acceptance record with exact verification commands and residual boundaries.

- [ ] **Step 1: Run backend focused verification**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmRuntimeDetailServiceTest,BpmTaskDetailServiceTest test
```

Expected: both detail test classes pass.

- [ ] **Step 2: Run backend module verification**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

Expected: all BPM module tests pass.

- [ ] **Step 3: Run frontend contract verification**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: both BPM frontend contract test files pass.

- [ ] **Step 4: Run frontend typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: command exits 0.

- [ ] **Step 5: Run public API leak check**

Run:

```powershell
rg -n "org\\.flowable|Flowable" `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo `
  hunyuan-design/apps/hunyuan-system/src/api/system/bpm
```

Expected: no output and exit code 1, meaning no public controller/VO/frontend API contract leaks Flowable names.

- [ ] **Step 6: Run UTF-8 readability check**

Run:

```powershell
$paths = @(
  'hunyuan-backend/hunyuan-bpm/src/main/java',
  'hunyuan-backend/hunyuan-bpm/src/test/java',
  'hunyuan-design/apps/hunyuan-system/src',
  'docs/superpowers/specs/2026-07-08-bpm-instance-timeline-acceptance.md'
)
$badCodePoints = @(0x7487, 0x9435, 0x6d93, 0x5bee, 0x95c1, 0x942e, 0x95bb, 0x6d5c, 0x9422, 0x9359, 0x95ae, 0xfffd)
Get-ChildItem -LiteralPath $paths -Recurse -File |
  Where-Object { $_.FullName -notmatch '\\target\\' } |
  ForEach-Object {
    $text = Get-Content -LiteralPath $_.FullName -Encoding UTF8 -Raw
    foreach ($codePoint in $badCodePoints) {
      $char = [char]$codePoint
      if ($text.Contains($char)) {
        Write-Output "$($_.FullName) contains suspicious code point U+$('{0:X4}' -f $codePoint)"
      }
    }
  }
```

Expected: no output. If matches appear in files changed by this plan, fix them before committing.

- [ ] **Step 7: Write acceptance record**

Create `docs/superpowers/specs/2026-07-08-bpm-instance-timeline-acceptance.md`:

```markdown
# BPM 实例详情与流转轨迹验收记录

## 结论

通过。BPM 实例详情现在能够统一展示基础信息、当前待办、表单快照和动作轨迹；管理员端实例列表与员工端运行页复用同一套实例详情抽屉。

## 范围

- 后端实例详情返回 `currentTasks` 和 `actionLogs`。
- 员工端我的申请、我的已办继续打开统一实例详情抽屉。
- 管理员端实例列表改为打开统一实例详情抽屉。
- 任务详情仍保留本地任务详情弹层。

## 验证

- `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmRuntimeDetailServiceTest,BpmTaskDetailServiceTest test`
- `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test`
- `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom`
- `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck`
- public API leak check for `org.flowable|Flowable`
- mojibake source check

## 非目标

- 未做 BPMN 图高亮。
- 未新增会签、或签、加签、减签、委派、管理员跳转节点。
- 未复制 Yudao/RuoYi 的 API 路径、页面壳、权限模型或 Flowable 对象结构。
```

- [ ] **Step 8: Commit acceptance record**

Run:

```powershell
git add -- 'docs/superpowers/specs/2026-07-08-bpm-instance-timeline-acceptance.md'
git diff --cached --name-only
git -c i18n.commitEncoding=UTF-8 -c i18n.logOutputEncoding=UTF-8 commit -m "docs: 记录BPM实例轨迹详情验收"
```

Expected staged files: only the acceptance record.

- [ ] **Step 9: Final status check**

Run:

```powershell
git log --oneline -6
git status --short
```

Expected: new BPM commits are visible. Remaining uncommitted files are only pre-existing noise or explicitly unrelated drafts.
