# BPM 顺序多人审批组投影与异常路径闭环设计

- 设计日期：2026-07-11
- 适用仓库：`E:\my-project\hunyuan-pro`
- 工作分类：平台或语义变更
- 状态：已完成对话级设计确认，待书面复核
- 依赖基线：`docs/superpowers/specs/2026-07-10-bpm-development-baseline.md`

## 1. 结论

本交付块扩展现有 Hunyuan 审批组投影，使其同时承载 `sequential` 和 `parallelAll`，但两种模式继续保持独立的执行语义。

顺序多人审批仍编译为连续的单处理人 `userTask`，不引入 Flowable multi-instance，不改变 Simple Model DSL 的节点类型，也不增加通用网关。新增能力只负责把同一个 authored 顺序节点展开出的真实任务归入稳定审批组，并向待办、已办、任务详情、实例详情和 trace 提供结构化组进度。

同时，本交付块修正“退回发起人”的共同引擎语义：退回必须终止当前 Flowable 流程实例，不能通过完成当前任务推进到后续节点，再由 Hunyuan 将实例置为 `WAIT_RESUBMIT`。该修正覆盖普通任务、顺序审批组和并行审批组，避免旧引擎实例残留活动任务。

## 2. 当前事实

### 2.1 已有能力

- 顺序多人审批已经按 authored 员工顺序展开为 `<nodeKey>_1 -> <nodeKey>_2 -> ...`。
- 编译快照已经保存 `authoredNodeKey`、`authoredNodeName`、`sequentialIndex` 和 `sequentialTotal`。
- `parallelAll` 已经具备 `t_bpm_approval_group`、任务关联字段、组状态、组进度、成员详情、动作收敛、前端面板和真实运行态验收。
- 任务、实例和 trace 公共契约已经可以承载审批组摘要和详情。
- 普通任务支持通过、拒绝、退回、转办、委派、加签、减签和实例级撤回；`parallelAll` 成员禁止加签、减签。

### 2.2 明确缺口

- 顺序任务没有 `approval_group_id`，前端只能从展开后的 `taskKey` 和任务名中的序号理解顺序。
- `BpmTaskProjectionService` 只识别 `parallelAll` 审批组快照。
- `BpmApprovalGroupService` 的快照解析、成员排序和动作日志均绑定并行语义。
- 后端多处使用 `approvalGroupId != null` 等价判断“并行会签成员”。顺序任务归组后，该判断会错误地改变动作语义。
- 前端 `approvalMode` 类型固定为 `parallelAll`，并以“是否存在审批组”决定是否隐藏加签、减签。
- 当前普通退回路径调用 Flowable `complete` 后再进入 `WAIT_RESUBMIT`；多节点流程可能因此推进出新的活动任务。

## 3. 目标与非目标

### 3.1 目标

1. 一个 authored 顺序审批节点在一次 Flowable 运行中对应一个稳定审批组。
2. 同组顺序任务共享 `approvalGroupId`，并输出组名、模式、总人数、进度、结果和成员任务。
3. 顺序成员通过、拒绝、退回、转办、委派、加签、减签、撤回和取消的语义清晰且可自动化验证。
4. 退回发起人后旧 Flowable 实例不存在活动任务；重提产生新的引擎实例和审批组。
5. 运行中的旧顺序实例可以在投影同步时安全归组，不从错误的 `0/N` 开始。
6. 普通单人任务和现有 `parallelAll` 行为保持兼容。

### 3.2 非目标

- 或签、比例审批和 Flowable multi-instance。
- 动态增加、删除或替换顺序审批成员。
- 角色、部门、岗位或表达式动态展开为顺序成员。
- 通用并行网关、条件网关、子流程、定时器和任意路由。
- 为尚未激活的顺序审批人创建虚假任务或独立成员计划表。
- 批量回填已经结束的历史顺序实例。
- 新增业务单据模块、通用业务回调平台或新的流程页面。

## 4. 方案比较

### 4.1 方案 A：扩展现有审批组投影（采用）

复用 `t_bpm_approval_group`、`t_bpm_task.approval_group_id`、现有 VO、实例详情、trace 和前端审批组面板。审批组服务解析通用多人审批快照，再按 `approvalMode` 执行不同动作语义。

优点：

- 有稳定审批组 ID 和持久化进度。
- 与现有 `parallelAll` 公共契约一致。
- 不新增业务表和依赖。
- 可以通过现有唯一约束保证同一次引擎运行只创建一个 authored 组。

代价：

- 需要清理当前代码中“有审批组即并行”的隐含假设。
- 需要为运行中旧实例增加任务归组和计数恢复逻辑。

### 4.2 方案 B：查询时动态聚合

不写审批组事实，查询时根据定义节点快照和任务历史临时构造顺序组。

不采用原因：没有稳定 `approvalGroupId`，任务列表、任务详情和实例详情需要重复聚合；运行中转办、重提和历史版本会增加推导复杂度，公共契约容易出现不同查询入口返回不一致。

### 4.3 方案 C：新建通用审批阶段和成员表

新增 `approval_stage`、`approval_stage_member`，统一顺序、并行和未来的或签、比例审批。

不采用原因：它会提前引入尚未确认的通用审批平台模型，扩大数据库、状态机、迁移和兼容范围，违反当前基线的最小闭环原则。

## 5. 领域模型

### 5.1 审批组身份

审批组继续使用以下唯一身份：

```text
(engine_process_instance_id, approval_group_key)
```

- `approval_group_key` 等于 authored 节点 key。
- `approval_group_name` 等于 authored 节点名称快照。
- 同一次 Flowable 运行中的后续顺序任务复用原审批组。
- 重提产生新的 `engine_process_instance_id`，因此创建新审批组；旧组保留为历史事实。

### 5.2 审批模式

```text
sequential
parallelAll
```

`approvalMode` 是动作分流和展示分流的权威字段。代码不得再通过 `approvalGroupId != null` 判断任务是否属于 `parallelAll`。

### 5.3 组状态和结束原因

组状态继续复用：

```text
PENDING
APPROVED
REJECTED
RETURNED
CANCELLED
```

结束原因继续复用：

```text
ALL_APPROVED
MEMBER_REJECTED
MEMBER_RETURNED
INSTANCE_RECALLED
INSTANCE_CANCELLED
```

这些名称对顺序和并行都成立，不新增模式专属状态。

### 5.4 成员和进度

- `totalMemberCount` 来自冻结编译快照中的 `sequentialTotal` 或 `parallelTotal`。
- `processedMemberCount` 表示 authored 成员任务中已经处理的数量。
- `approvedMemberCount`、`rejectedMemberCount` 只统计 authored 成员任务。
- 成员详情只返回已经由 Flowable 创建并投影到 `t_bpm_task` 的真实任务。
- 尚未激活的顺序成员不创建占位任务，也不在运行时重新读取组织身份；前端通过 `totalMemberCount - members.size()` 显示“后续 N 人待激活”。
- 加签任务不设置 `approval_group_id`，不计入 authored 组人数和进度。

## 6. 编译快照

顺序展开节点在现有字段基础上补齐：

```json
{
  "approvalMode": "sequential",
  "approvalGroupKey": "finance_review",
  "approvalGroupName": "财务复核",
  "authoredNodeKey": "finance_review",
  "authoredNodeName": "财务复核",
  "sequentialIndex": 1,
  "sequentialTotal": 3,
  "employeeId": 101
}
```

编译生成的 BPMN 仍然是：

```text
finance_review_1 -> finance_review_2 -> finance_review_3
```

本轮不修改 Simple Model authored JSON 结构，不新增设计器字段，也不改变已有定义的编译结果。只有重新发布的定义获得新的顺序审批组快照字段。

## 7. 运行时数据流

### 7.1 新实例首次归组

1. Flowable 生成第一个顺序任务。
2. `BpmTaskProjectionService` 读取定义节点编译快照。
3. 快照解析器确认 `approvalMode=sequential`、组 key、序号和总数合法。
4. `BpmApprovalGroupService` 按引擎实例和组 key 查询审批组。
5. 不存在时创建 `PENDING` 组，计数初始化为 `0/N`。
6. 当前任务写入该 `approvalGroupId`。
7. 待办和详情通过现有审批组摘要/详情契约返回结构化进度。

### 7.2 后续成员归组

前一成员通过后，Flowable 生成下一任务。投影服务使用相同引擎实例和 authored 组 key 找到已有组，给新任务写入同一个 `approvalGroupId`。

### 7.3 运行中旧实例恢复

部署前已经运行的顺序实例可能存在没有 `approvalGroupId` 的任务。为避免第二个成员才创建组并错误显示 `0/N`，投影同步必须支持恢复：

1. 已存在的活动任务若缺少组 ID，不能直接因 engine task 已投影而返回。
2. 根据其定义节点快照创建或定位审批组。
3. 查询同一 Hunyuan 实例、同一 `engineProcessInstanceId` 中定义节点 `authoredNodeKey` 相同的既有任务。
4. 将这些真实任务关联到该组。
5. 根据任务状态和结果重新计算组计数和状态。
6. 恢复过程必须幂等，重复 resync 不重复计数。

已经结束且没有再次同步的历史实例不批量回填，继续返回空审批组字段或列表。

## 8. 动作语义

### 8.1 顺序成员通过

1. 按“审批组 -> 当前任务”顺序加锁。
2. 复查组为 `PENDING`、任务为待处理且属于当前员工。
3. 完成当前 Flowable 任务。
4. 更新任务为 `COMPLETED/APPROVED`，写入普通 `APPROVED` 动作日志。
5. 组计数增加。
6. 非最后成员保持 `PENDING`；最后成员关闭为 `APPROVED/ALL_APPROVED`。
7. 同一事务后同步下一活动任务；无活动任务时结束实例。

顺序成员不使用 `PARALLEL_MEMBER_APPROVED` 日志类型。

### 8.2 顺序成员拒绝

1. 按统一锁顺序锁定组和任务。
2. 取消当前 Flowable 流程实例，不生成后续顺序任务。
3. 当前任务更新为 `COMPLETED/REJECTED`，写入普通 `REJECTED` 日志。
4. 审批组关闭为 `REJECTED/MEMBER_REJECTED`。
5. 关闭同一实例仍待处理的 Hunyuan 任务投影。
6. 实例结束为拒绝。

### 8.3 退回发起人

退回是实例级重启语义，不是“完成当前节点”。普通任务、顺序成员和并行成员统一遵守：

1. 校验当前任务和操作人。
2. 取消当前 Flowable 流程实例。
3. 当前任务写入 `RETURNED` 结果；普通任务和顺序成员使用 `RETURNED_TO_INITIATOR` 日志，并行成员继续使用既有 `PARALLEL_MEMBER_RETURNED` 日志，公共实例语义保持一致。
4. 当前审批组关闭为 `RETURNED/MEMBER_RETURNED`；无审批组时跳过。
5. 关闭实例下其余待处理任务投影，包括不属于 authored 组的加签投影。
6. Hunyuan 实例进入 `WAIT_RESUBMIT`，活动任务数归零。

禁止先调用 `FlowableTaskGateway.complete` 再进入 `WAIT_RESUBMIT`。

### 8.4 转办和委派

- 保持当前任务 ID、定义节点 ID、组 ID、成员序号和组总数不变。
- 更新实际处理人及员工快照。
- 不改变组计数和组状态。

### 8.5 加签和减签

- `parallelAll` 成员继续由后端硬拒绝，前端隐藏操作。
- `sequential` 成员继续使用普通任务的加签、减签能力。
- 加签任务不复制 `approvalGroupId`，因此不参与 authored 组进度。
- 减签只处理现有加签投影，不改变顺序组成员总数。

### 8.6 撤回、取消和重提

- 撤回和取消继续关闭当前实例下所有 `PENDING` 审批组和任务投影。
- 重提启动新的 Flowable 实例，创建新的顺序审批组。
- 旧审批组保持 `RETURNED` 或 `CANCELLED`，不得因重提重新打开或复用。

## 9. 服务职责

### 9.1 `SimpleModelBpmnCompiler`

- 为顺序展开节点补齐组 key 和组名。
- 保持现有串行 BPMN 拓扑。
- 不改变 authored DSL。

### 9.2 `BpmTaskProjectionService`

- 识别 `sequential` 和 `parallelAll` 两种审批组节点。
- 为新任务分配审批组。
- 对已投影但缺少组 ID 的活动任务执行幂等恢复。
- 不从任务名称或 task key 后缀推断组语义。

### 9.3 `BpmApprovalGroupService`

- 将当前 `ParallelGroupSnapshot` 泛化为模式感知的审批组节点快照。
- 创建、查找、恢复审批组并维护计数。
- 按模式执行独立动作状态机。
- 按 `parallelIndex` 或 `sequentialIndex` 排序真实成员任务。
- 对外继续返回现有摘要和详情 VO。

不新增通用规则引擎或新的万能 service；模式分流必须在审批组领域边界内完成。

### 9.4 `BpmTaskService`

- 通过审批组 `approvalMode` 判断动作路径。
- 不再使用“组 ID 非空”作为并行会签判断。
- 统一普通任务和审批组任务的退回引擎终止语义。
- 保持抄送、动作日志、实例结束和错误响应的现有接口。

### 9.5 查询与前端

- `BpmTaskVO`、`BpmTaskDetailVO`、`BpmInstanceDetailVO` 和 `BpmInstanceTraceVO` 继续使用现有审批组字段。
- TypeScript `approvalMode` 扩展为 `'sequential' | 'parallelAll'`。
- 审批组面板根据模式显示“顺序审批”或“并行会签”。
- 待办操作区仅在 `approvalMode === 'parallelAll'` 时隐藏加签、减签。

## 10. 事务、并发与幂等

### 10.1 动作锁顺序

所有组内成员动作统一：

```text
普通查询取得 groupId
-> selectByIdForUpdate(approvalGroup)
-> selectByIdForUpdate(task)
-> 必要时锁定同组待办
```

顺序组理论上只有一个 authored 活动成员，但仍使用相同锁顺序，避免重复点击、转办与审批竞争导致重复计数。

### 10.2 组创建

- 唯一约束 `(engine_process_instance_id, approval_group_key)` 保持不变。
- 并发插入冲突后回查已创建组。
- 若已存在组的 `approvalMode`、总人数或定义身份与当前快照冲突，抛出明确异常并回滚，不静默覆盖。

### 10.3 计数幂等

- 只有从待处理状态成功转换为结束状态的 authored 任务才能改变计数。
- 恢复操作按数据库中真实任务状态重新计算，而不是在旧计数上累加。
- 重复动作返回“审批组已关闭或任务已处理”，不得再次驱动 Flowable。

## 11. 公共契约

不新增接口。现有契约中的审批组字段扩展语义：

```ts
type BpmApprovalMode = 'parallelAll' | 'sequential';
```

摘要继续包含：

```text
approvalGroupId
approvalGroupKey
approvalGroupName
approvalMode
groupState
totalMemberCount
processedMemberCount
approvedMemberCount
rejectedMemberCount
```

详情继续增加：

```text
closeReason
closedAt
members[]
```

兼容规则：

- 普通任务继续返回 `approvalGroup=null`。
- 普通实例继续返回空 `approvalGroups`。
- 已有 `parallelAll` 字段和值保持不变。
- 历史顺序实例没有审批组时仍返回空值，不动态伪造。

## 12. 前端体验

### 12.1 列表

- 顺序待办显示 authored 组名和 `已处理/总人数`。
- 当前成员任务名仍可保留 `（序号/总数）`，但页面不再依赖该文本推断进度。
- 已办列表通过组摘要展示同一 authored 节点的聚合状态。

### 12.2 任务和实例详情

- 面板标题展示组名和模式标签。
- 顺序组按 `sequentialIndex` 展示已经生成的成员任务。
- 存在未激活成员时显示“后续 N 人待激活”，不显示姓名占位。
- 已关闭组展示结束原因和结束时间。

### 12.3 操作区

- 转办、委派、撤回继续按现有权限和实例状态展示。
- `parallelAll` 隐藏加签、减签。
- `sequential` 不因存在审批组而隐藏加签、减签。
- 前端只负责可用性提示，后端继续作为动作边界权威。

## 13. 数据库与升级

预计新增 `v3.44.0.sql`。实施前必须再次确认版本号未被其他交付占用。

SQL 不新增表或业务列，只将以下数据库元数据从并行专属描述泛化为多人审批组：

- `t_bpm_approval_group` 表注释。
- `approval_group_key`、`approval_group_name` 列注释。
- `t_bpm_task.approval_group_id` 列注释。

不修改 `v3.43.0.sql`，因为该脚本已经执行并属于已关闭交付块。

部署要求：

1. 执行新 SQL 元数据迁移。
2. 同批部署后端与前端，避免旧前端继续把所有审批组都视为并行组。
3. 对需要立即归组的运行中顺序实例调用现有 projection resync；不做全库批量回填。

## 14. 错误处理

- 编译和发布阶段继续校验顺序成员数量、员工 ID、展开 key 和候选人身份。
- 运行时快照缺少模式、组 key、序号或总数时，不从任务名猜测；记录包含定义节点 ID 的告警，并保持任务为普通投影，避免写入错误组事实。
- 已有组与快照模式或总人数冲突时，事务失败并保留原数据。
- 组织身份在任务生成时继续通过现有身份网关解析；未激活成员不在详情查询时重新解析。
- 退回取消引擎失败时整个事务回滚，不能先把 Hunyuan 实例写成 `WAIT_RESUBMIT`。

## 15. 验证与验收矩阵

### 15.1 编译与投影

| 场景 | 预期 |
| --- | --- |
| 三人顺序定义编译 | 三个串行任务，快照共享组 key/name，序号为 1/3、2/3、3/3 |
| 首任务投影 | 创建 `sequential` 组，进度 `0/3`，任务关联组 ID |
| 后续任务投影 | 复用同组 ID，不创建第二个组 |
| 旧运行实例 resync | 历史和活动成员归入同组，计数按真实任务恢复 |
| 重复 resync | 组、任务关联和计数不重复 |

### 15.2 动作状态机

| 场景 | 预期 |
| --- | --- |
| 三人依次通过 | `0/3 -> 1/3 -> 2/3 -> APPROVED 3/3` |
| 第二人拒绝 | 组 `REJECTED`，实例拒绝，旧引擎无活动任务 |
| 第一或第二人退回 | 组 `RETURNED`，实例 `WAIT_RESUBMIT`，旧引擎无活动任务 |
| 退回后重提 | 新 engine process、新组 ID；旧组保持关闭 |
| 转办、委派 | 组 ID、序号、总数和计数不变，处理人快照更新 |
| 顺序成员加签、减签 | 操作可用；加签任务不计入 authored 进度 |
| 并行成员加签、减签 | 后端继续拒绝，前端继续隐藏 |
| 重复审批 | 不重复完成 Flowable，不重复计数 |

### 15.3 兼容回归

| 场景 | 预期 |
| --- | --- |
| 普通单人流程 | 审批组字段为空，全部动作保持原行为 |
| 现有 `parallelAll` 全员通过 | 原组收敛、日志、通知和详情保持不变 |
| `parallelAll` 拒绝、退回、撤回、取消 | 原关闭语义和其他成员取消行为保持不变 |
| 历史顺序实例 | 不批量回填，详情保持可读且不伪造组 |

### 15.4 验证门禁

- 后端编译器、投影、审批组、任务动作、实例详情和 trace 聚焦测试。
- `hunyuan-bpm` 模块全量测试。
- `BpmFlowableCompatibilityTest`。
- 前端 BPM API、设计器和运行时页面契约测试。
- `@hunyuan/system` 类型检查。
- 真实 API/浏览器验收覆盖全通过、拒绝、退回重提、动作边界和普通/并行回归。

历史通过结果只能作为参考，本交付块必须记录本次真实执行的命令、时间和结果。

## 16. 交付批次

1. 编译快照、审批组通用解析和投影恢复。
2. 顺序组动作状态机与统一退回语义。
3. 公共契约、查询聚合和前端展示/动作边界。
4. SQL 注释迁移、回归门禁、真实运行验收和基线回写。

批次是同一交付块的实施顺序，不形成新的设计或批准循环。

## 17. 完成定义

- 顺序 authored 节点在公共契约中表现为稳定审批组。
- 组进度只基于真实 authored 任务，且在正常流转和旧实例恢复中一致。
- 退回不会推进出下一任务，旧 Flowable 实例没有活动任务。
- 重提创建新组，旧组保留历史。
- 顺序、并行和普通任务的高级动作边界清晰且前后端一致。
- 自动化门禁和真实 API/浏览器验收全部通过。
- 新增独立验收记录并更新 BPM 开发基线。

## 18. 理解报告

### 18.1 关键假设

- 当前业务需要的是结构化顺序进度，不要求在任务激活前展示未来审批人姓名。
- 现有 `t_bpm_approval_group` 字段足以表达顺序组，不需要新增成员计划表。
- “退回发起人”应当终止当前引擎实例，而不是完成任务并沿 BPMN 继续流转。
- 加签投影不属于 authored 顺序成员，因而不影响顺序组总人数和进度。

### 18.2 已验证事实

- 编译快照已经包含 authored key/name 和顺序序号/总数。
- 当前任务投影只识别 `parallelAll`。
- 当前审批组表、任务关联字段和公共 VO 已存在。
- 当前后端和前端使用“存在审批组”近似判断并行会签动作边界。
- 当前普通退回路径调用 Flowable task complete 后进入 `WAIT_RESUBMIT`。

### 18.3 待实施验证

- 多节点普通/顺序流程退回后是否确实残留下一活动任务；先建立自动化失败证据，再进行真实 API 验证。
- 运行中旧顺序实例恢复时的任务关联、计数和重复 resync 幂等性。
- 顺序成员加签、减签与 authored 组进度在真实页面上的一致性。
- 后端先于前端部署时旧页面的动作显示风险；部署方案应按同批发布执行。

### 18.4 人工重点审阅

- 是否接受“未来成员只显示数量，不显示姓名”的范围边界。
- 是否接受把统一退回引擎终止语义纳入同一交付块。
- 是否接受历史已结束顺序实例不批量回填。
- 是否确认顺序成员继续允许现有加签、减签语义。
