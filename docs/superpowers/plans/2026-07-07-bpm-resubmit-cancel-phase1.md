# BPM Runtime Resubmit + Cancel Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在当前 `hunyuan-pro` BPM runtime 上补齐“发起人取消实例 + 退回后重新提交”窄闭环，不推翻现有运行时投影设计。

**Architecture:** Hunyuan 继续以 `t_bpm_instance` 作为实例真相，Flowable 只作为内部引擎。`WAIT_RESUBMIT` 不尝试把旧 Flowable 实例硬拉回运行态，而是保留同一个 Hunyuan `instanceId`，重新启动一轮新的 Flowable 实例并回绑到该实例记录上；这样既借鉴了参考仓“从旧实例重开草稿”的用户体验，又保留了 Hunyuan 自己的平台实例语义。

**Tech Stack:** Spring Boot 3.5.4, Java 17, Flowable 7.2.0, MyBatis-Plus, Vue 3, Element Plus, `@form-create/element-ui`, Vitest

## Global Constraints

- 遵循 `E:/my-project/hunyuan-pro/AGENTS.md`：一次只推进一个可验证增量。
- 不新增依赖；前端运行时表单渲染只能复用已存在的 `@form-create/element-ui`。
- `initialFormDataSnapshotJson` 永远不覆盖；重提只允许更新 `currentFormDataSnapshotJson`。
- `BpmInstanceRunStateEnum` 以当前后端枚举为准：`RUNNING=1`、`WAIT_RESUBMIT=2`、`FINISHED=3`、`CANCELLED=4`。
- `BpmInstanceResultStateEnum` 以当前后端枚举为准：`APPROVED=1`、`REJECTED=2`、`CANCELLED_BY_START_USER=3`、`CANCELLED_BY_ADMIN=4`。
- 第一阶段只做员工端 runtime 闭环；管理员取消语义在服务层预留，但不强制本轮补完整个管理端交互。
- Flowable 原生对象不能泄漏到 `hunyuan-bpm` 外部 API。

---

## Scope Verdict

当前仓已经真实闭环的能力：

- `returnToInitiator` 已经把实例打到 `WAIT_RESUBMIT`
- `approve / reject / transfer` 已经有后端真实请求与运行验收
- `my-instance / my-todo / my-done / detail` 已经有员工端页面和 contract test

当前仓仍然缺失的能力：

- App 侧没有 `cancel instance` API 与按钮
- App 侧没有 `resubmit` API 与按钮
- `startable-list.vue` 仍然用固定 `formDataJson: '{}'` 发起，无法承接“修改后重提”
- 详情轨迹还不认识 `RESUBMITTED` / `INSTANCE_CANCELLED`
- 管理端实例列表的状态标签仍停留在旧三态，不认识 `WAIT_RESUBMIT` / `CANCELLED=4`

本计划只解决这些真实缺口，不重做 BPM 全模块。

## Task 1: Backend Cancel Lifecycle

**Files:**
- Modify: `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/FlowableProcessInstanceGateway.java`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmInstanceController.java`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java`

**Interfaces:**
- Consumes:
```java
public class BpmInstanceCancelForm {
    private Long instanceId;
    private String cancelReason;
}
```
- Produces:
```java
public ResponseDTO<String> cancelMyInstance(BpmInstanceCancelForm cancelForm);
public void cancel(String engineProcessInstanceId, String reason);
```

- [ ] **Step 1: 先补失败用例，锁定取消语义**

```java
@Test
void cancelMyInstanceShouldTerminateEngineAndClosePendingTasks() {
    // given: instance.runState = RUNNING, startEmployeeId = current actor
    // when: bpmInstanceService.cancelMyInstance(form)
    // then: runState = CANCELLED, resultState = CANCELLED_BY_START_USER
    // and: finishedAt / cancelledAt written
    // and: pending task rows become taskState = CANCELLED, taskResult = INSTANCE_CANCELLED
    // and: action log contains INSTANCE_CANCELLED
}
```

- [ ] **Step 2: 运行后端单测，确认当前缺口真实存在**

Run: `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest test`

Expected: 失败，原因是当前没有 `cancelMyInstance` 命令与 Flowable 实例取消网关。

- [ ] **Step 3: 实现取消命令，但只做当前仓真正需要的语义**

```java
// FlowableProcessInstanceGateway
public void cancel(String engineProcessInstanceId, String reason) {
    runtimeService.deleteProcessInstance(engineProcessInstanceId, reason);
}

// BpmInstanceService
@Transactional(rollbackFor = Exception.class)
public ResponseDTO<String> cancelMyInstance(BpmInstanceCancelForm cancelForm) {
    // 1. 校验实例存在
    // 2. 校验当前人为发起人
    // 3. 仅允许 RUNNING / WAIT_RESUBMIT 取消
    // 4. 取消 Flowable 当前实例（WAIT_RESUBMIT 时若引擎已结束则跳过）
    // 5. 批量关闭 t_bpm_task 中该实例的待办投影
    // 6. 更新 instance.runState/resultState/cancel* 字段与结束时间
    // 7. 写 INSTANCE_CANCELLED 动作日志
    // 8. 返回 ok
}
```

实现约束：

- 取消时同时写 `finishedAt` 与 `cancelledAt`，因为当前实例列表只展示 `finishedAt`
- 如果实例已经是 `FINISHED` 或 `CANCELLED`，直接返回参数错误，不做幂等吞掉
- 只关闭 `task_state = PENDING` 的任务投影，已办记录保持原样

- [ ] **Step 4: 再跑后端单测，确认取消命令闭环**

Run: `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest test`

Expected: `cancelMyInstanceShouldTerminateEngineAndClosePendingTasks` 通过。

- [ ] **Step 5: Commit**

```bash
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/FlowableProcessInstanceGateway.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmInstanceController.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java
git commit -m "feat: add bpm instance cancel lifecycle"
```

## Task 2: Backend Resubmit Semantics And Draft Contract

**Files:**
- Create: `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmInstanceResubmitForm.java`
- Create: `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmRuntimeStartDraftVO.java`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmStartController.java`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmInstanceController.java`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java`

**Interfaces:**
- Consumes:
```java
public class BpmInstanceResubmitForm {
    private Long instanceId;
    private String formDataJson;
    private String summary;
    private String title;
}
```
- Produces:
```java
public ResponseDTO<BpmRuntimeStartDraftVO> getStartDraft(Long definitionId);
public ResponseDTO<BpmRuntimeStartDraftVO> getResubmitDraft(Long instanceId);
public ResponseDTO<Long> resubmitMyInstance(BpmInstanceResubmitForm resubmitForm);
```

- [ ] **Step 1: 先补失败用例，锁定“同一个 Hunyuan instanceId，新一轮 Flowable run”语义**

```java
@Test
void resubmitMyInstanceShouldReusePlatformInstanceButStartNewEngineRun() {
    // given: instance.runState = WAIT_RESUBMIT
    // when: bpmInstanceService.resubmitMyInstance(form)
    // then: same instanceId remains
    // and: engineProcessInstanceId is replaced by newly started flowable id
    // and: initialFormDataSnapshotJson unchanged
    // and: currentFormDataSnapshotJson updated
    // and: runState returns to RUNNING
    // and: action log contains RESUBMITTED
    // and: bpmTaskProjectionService.syncActiveTasksForInstance(instanceId) is invoked
}
```

- [ ] **Step 2: 运行单测，确认当前仓还没有重提命令**

Run: `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest test`

Expected: 失败，原因是缺少 `BpmInstanceResubmitForm`、draft VO 与 resubmit service/controller。

- [ ] **Step 3: 实现 draft 查询与重提命令**

```java
// BpmRuntimeStartDraftVO
private Long definitionId;
private String definitionName;
private String formNameSnapshot;
private String formSchemaSnapshotJson;
private String title;
private String summary;
private String formDataJson;
private Long sourceInstanceId; // fresh start 时为空，resubmit 时为原实例

// BpmInstanceService.resubmitMyInstance
// 1. 校验实例存在、属于当前发起人、runState = WAIT_RESUBMIT
// 2. 校验 definition 仍是 CURRENT + STARTABLE
// 3. 调用 flowableProcessInstanceGateway.start(...) 重新拉起引擎实例
// 4. 复用同一个 instanceId，回写新的 engine ids/title/summary/currentFormDataSnapshotJson
// 5. 清空 finishedAt/cancelledAt/resultState，runState 改回 RUNNING
// 6. 写 RESUBMITTED 动作日志
// 7. syncActiveTasksForInstance(instanceId)
```

关键取舍：

- 不新建第二条 `t_bpm_instance` 记录，否则当前 `WAIT_RESUBMIT` 会变成“死旧实例 + 新生实例”双实例语义，和 Hunyuan 既有状态枚举冲突
- 也不尝试复活旧 Flowable 实例，因为当前 `returnToInitiator()` 已经不是基于显式引擎回退节点实现
- 借鉴 Yudao 的“从旧实例带出草稿再重新提交”体验，但保留 Hunyuan 的平台实例主语义

- [ ] **Step 4: 补 app draft 接口**

```java
@GetMapping("/app/bpm/start-draft/{definitionId}")
public ResponseDTO<BpmRuntimeStartDraftVO> startDraft(@PathVariable Long definitionId)

@GetMapping("/app/bpm/resubmit-draft/{instanceId}")
public ResponseDTO<BpmRuntimeStartDraftVO> resubmitDraft(@PathVariable Long instanceId)

@PostMapping("/app/bpm/instance/resubmit")
public ResponseDTO<Long> resubmit(@RequestBody @Valid BpmInstanceResubmitForm form)
```

- [ ] **Step 5: 再跑后端单测，确认重提闭环成立**

Run: `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest test`

Expected: 新增的重提测试通过，并且原有 `returnToInitiator` / `approve` / `reject` / `transfer` 用例不回归。

- [ ] **Step 6: Commit**

```bash
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmStartController.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmInstanceController.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmInstanceResubmitForm.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmRuntimeStartDraftVO.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java
git commit -m "feat: add bpm resubmit draft and restart lifecycle"
```

## Task 3: Frontend Runtime Start/Resubmit Form

**Files:**
- Create: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/start-form.vue`
- Create: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-renderer.vue`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/startable-list.vue`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-instance-list.vue`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue`

**Interfaces:**
- Consumes:
```ts
export interface BpmRuntimeStartDraftRecord {
  definitionId: number;
  definitionName: string;
  formNameSnapshot?: null | string;
  formSchemaSnapshotJson: string;
  formDataJson: string;
  sourceInstanceId?: number;
  summary?: null | string;
  title: string;
}
```
- Produces:
```ts
export async function getBpmStartDraft(definitionId: number)
export async function getBpmResubmitDraft(instanceId: number)
export async function cancelMyBpmInstance(params: BpmInstanceCancelForm)
export async function resubmitMyBpmInstance(params: BpmInstanceResubmitForm)
```

- [ ] **Step 1: 先补前端 source-level 用例，锁定新页面与新路由**

```ts
expect(runtimeApiSource).toContain('/app/bpm/start-draft/');
expect(runtimeApiSource).toContain('/app/bpm/resubmit-draft/');
expect(runtimeApiSource).toContain('/app/bpm/instance/cancel');
expect(runtimeApiSource).toContain('/app/bpm/instance/resubmit');
expect(routeSource).toContain('/system/bpm/runtime/start-form');
expect(myInstanceSource).toContain('重新提交');
expect(myInstanceSource).toContain('取消');
```

- [ ] **Step 2: 运行前端测试，确认 contract 还不存在**

Run: `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

Expected: 失败，提示缺少新 API contract、路由或页面文件。

- [ ] **Step 3: 实现 runtime 表单页，而不是继续用 confirm + '{}' 假发起**

```ts
// startable-list.vue
router.push({
  name: 'SystemBpmRuntimeStartFormRoute',
  query: { definitionId: row.definitionId },
});

// my-instance-list.vue
if (row.runState === 1) show cancel
if (row.runState === 2) show resubmit
```

页面规则：

- 新页面同时承接“首次发起”和“待重提实例再提交”两种入口
- fresh start 走 `getBpmStartDraft`
- resubmit 走 `getBpmResubmitDraft`
- 表单渲染复用 `@form-create/element-ui`
- 提交成功后跳回“我的申请”，并刷新列表

- [ ] **Step 4: 详情轨迹和状态标签一起补齐**

```ts
const labelMap = {
  APPROVED: '审批通过',
  REJECTED: '审批拒绝',
  RETURNED_TO_INITIATOR: '退回发起人',
  TRANSFERRED: '转办',
  RESUBMITTED: '重新提交',
  INSTANCE_CANCELLED: '实例取消',
};
```

额外修正：

- `my-instance-list.vue` 继续使用后端四态 `1/2/3/4`
- `start-form.vue` 成为唯一发起入口，`startable-list.vue` 不再直接发请求

- [ ] **Step 5: 前端 typecheck**

Run: `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck`

Expected: 通过，没有 runtime API 类型错误或路由类型错误。

- [ ] **Step 6: Commit**

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/startable-list.vue hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-instance-list.vue hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/start-form.vue hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-renderer.vue
git commit -m "feat: add bpm runtime start and resubmit form flow"
```

## Task 4: Contract Alignment And Regression Guard

**Files:**
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/instance/instance-list.vue`

**Interfaces:**
- Consumes:
```ts
// 现有 source-level 测试风格
const source = readFileSync(resolve(process.cwd(), path), 'utf8');
```
- Produces:
```ts
// 后端状态四态与前端标签统一
runState 1 => 流转中
runState 2 => 待重新提交
runState 3 => 已结束
runState 4 => 已取消
```

- [ ] **Step 1: 补 contract 测试针脚**

```ts
expect(runtimeApiSource).toContain('/app/bpm/instance/cancel');
expect(runtimeApiSource).toContain('/app/bpm/instance/resubmit');
expect(runtimeApiSource).toContain('/app/bpm/start-draft/');
expect(runtimeApiSource).toContain('/app/bpm/resubmit-draft/');
```

- [ ] **Step 2: 把管理端实例列表的状态映射修回后端真实枚举**

```ts
if (value === 1) return '流转中';
if (value === 2) return '待重新提交';
if (value === 3) return '已结束';
if (value === 4) return '已取消';
```

原因：

- 当前 `instance-list.vue` 仍把 `2` 当“已结束”、`3` 当“已取消”
- 一旦重提/取消上线，这个页面会直接展示错误语义

- [ ] **Step 3: 跑前端测试 + typecheck**

Run: `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

Expected: 两个测试文件通过。

Run: `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck`

Expected: 通过。

- [ ] **Step 4: Commit**

```bash
git add hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts hunyuan-design/apps/hunyuan-system/src/views/system/bpm/instance/instance-list.vue
git commit -m "test: align bpm runtime lifecycle contracts"
```

## Self-Review

**1. Spec coverage**

- `实例取消`：Task 1
- `退回后重新提交`：Task 2 + Task 3
- `前后端 contract`：Task 3 + Task 4
- `状态与轨迹一致`：Task 1 + Task 2 + Task 4
- `不照搬参考仓`：通过“同 instanceId + 新 engine run”的 Hunyuan 化语义实现

**2. Placeholder scan**

- 没有 `TODO` / `TBD`
- 每个任务都给出了实际文件路径、命令、接口签名和验证方式

**3. Type consistency**

- 后端命令统一使用 `BpmInstanceCancelForm` / `BpmInstanceResubmitForm`
- 前端 API 统一对齐 `/app/bpm/instance/cancel`、`/app/bpm/instance/resubmit`
- 运行态标签统一对齐后端四态枚举

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-07-bpm-resubmit-cancel-phase1.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
