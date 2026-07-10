# BPM 并行全员会签设计

- 状态：设计已确认，等待书面规格审阅
- 日期：2026-07-10
- 适用仓库：`E:\my-project\hunyuan-pro`
- 参考边界：Yudao 前端与 RuoYi 后端只用于理解交互和机制；所有实现、契约、SQL、测试和验收留在 Hunyuan

## 结论

本模块为 Hunyuan BPM 增加第一种真正的并行协同审批能力：**并行全员会签**。

对外领域名统一为“审批组”。Flowable 的并行网关只留在 Hunyuan BPM 编译器和运行时网关内部；接口、页面、通知、数据库投影和业务语义均使用 Hunyuan 的审批组、任务、员工和实例概念。

首期只支持一个精确定义的模式：

```text
approvalMode = parallelAll
```

一个审批组由至少两名指定员工组成。成员同时收到各自独立的待办；所有成员通过后流程才能继续；任何一个成员拒绝时立即终止实例；任何一个成员退回发起人时立即停止整个审批组、取消其他成员待办，并将实例转为 `WAIT_RESUBMIT`。

这不是 Flowable multi-instance，不支持或签、比例审批、动态成员、子流程或定时器。

## 为什么现在作为一个大模块推进

当前 Hunyuan BPM 的编译器只生成线性结构：

```text
startEvent -> userTask... -> endEvent
```

顺序多人审批通过把一个 authored 节点展开为多个连续的单处理人任务实现。并行会签会改变同一时刻产生多少任务、何时允许向下游流转、一个成员终止操作如何影响其他成员，以及详情页如何解释当前状态，因此不能拆成“先加一个前端按钮”或“先加一条接口”。

本设计将它作为一个完整模块交付，但按五个机制批次实现和验证，避免每次只关闭一个零散页面点。

## 目标与边界

### 本模块完成后

- 流程设计器可配置“并行全员会签”节点。
- 该节点仅可选择至少两名、互不重复、有效的指定员工。
- 发布前完成模型、员工和编译后节点 key 的校验。
- 编译器生成并行网关分叉、多个独立 user task、并行网关汇聚的 BPMN。
- 每个成员仍拥有自己的 Hunyuan `t_bpm_task` 任务投影。
- 实例中有可查询、可锁定、可展示的审批组投影，能够表达成员、进度、结果和结束原因。
- 我的待办、任务详情、实例详情、trace 和通知均使用结构化审批组契约展示会签，不解析内部 JSON 拼页面。
- 转办、委派、撤回与会签的关系清晰；会签成员级加签、减签被前后端同时拒绝。
- 一条真实三人会签活体链路证明并发待办、全员通过、任一拒绝、退回发起人和高级动作边界均符合语义。

### 明确不做

- 或签：任一成员通过即流转。
- 比例审批：按人数或百分比决定通过。
- Flowable multi-instance。
- 角色、部门、岗位或表达式自动展开为多人成员。
- 运行中动态增加、删除或替换会签成员。
- 条件、包容或任意通用网关 DSL。
- 子流程、定时器、超时自动流转。
- 通用 BPMN 图编辑器、图形运行态高亮或脚本表达式。

## 领域模型

### 设计器 DSL

仍使用现有 `userTask`，不新增节点类型。新模式复用指定员工的 `employeeIds`：

```json
{
  "nodeKey": "finance_review",
  "name": "财务会签",
  "type": "userTask",
  "approvalMode": "parallelAll",
  "candidateResolverType": "EMPLOYEE",
  "employeeIds": [101, 102, 103],
  "listeners": []
}
```

设计器和后端都执行以下约束：

1. `parallelAll` 只允许 `candidateResolverType = EMPLOYEE`。
2. `employeeIds` 必须是至少两个正整数构成的数组。
3. 同一员工不得重复出现。
4. 发布前逐一通过 Hunyuan 组织身份网关确认员工存在且可用。
5. 现有单人和顺序多人审批保持原有契约与行为。
6. 切换到 `parallelAll` 时，设计器使用现有远程员工多选能力，不允许手工输入 ID 作为正常配置路径。
7. 前端校验用于即时反馈；后端校验是发布与运行的最终权威。

前端类型 `BpmProcessNodeDraft.approvalMode` 扩展为：

```ts
'single' | 'singleOnly' | 'sequential' | 'parallelAll'
```

### 编译快照

部署时的编译节点快照必须保留 authored 节点与并行展开节点之间的关系。每个会签成员的 compiled node 至少包含：

- `authoredNodeKey`、`authoredNodeName`
- `approvalMode = parallelAll`
- `approvalGroupKey`，值为 authored node key
- `approvalGroupName`，值为 authored node name
- `parallelIndex`、`parallelTotal`
- 已冻结的 `employeeId`
- 编译后的 `nodeKey`、`name`、候选人解析信息和现有运行时规则快照

这些字段是后端建立任务投影和组详情的事实来源。前端不读取或解析编译快照 JSON。

### 审批组投影

新增表 `t_bpm_approval_group`，表示一次实例运行中的一个并行全员会签节点。它不是 Flowable 表的副本，也不取代成员任务投影。

核心字段：

| 字段 | 含义 |
| --- | --- |
| `approval_group_id` | 审批组主键 |
| `instance_id`、`definition_id` | Hunyuan 实例与定义归属 |
| `engine_process_instance_id` | 内部运行时定位，仅在 BPM 边界内使用 |
| `approval_group_key`、`approval_group_name` | authored 节点的稳定业务标识和展示名 |
| `approval_mode` | 首期固定为 `parallelAll` |
| `group_state` | `PENDING`、`APPROVED`、`REJECTED`、`RETURNED`、`CANCELLED` |
| `close_reason` | `ALL_APPROVED`、`MEMBER_REJECTED`、`MEMBER_RETURNED`、`INSTANCE_RECALLED`、`INSTANCE_CANCELLED` |
| `total_member_count`、`processed_member_count`、`approved_member_count`、`rejected_member_count` | 结构化进度投影 |
| `closed_at`、审计时间字段 | 结束与审计信息 |

建立唯一约束：

```text
(engine_process_instance_id, approval_group_key)
```

同一个 Hunyuan 实例重新提交后会拥有新的 Flowable process instance，因此不会把一次重新发起误并入旧审批组；同一次并行分叉产生的多个成员任务则会稳定归入同一组。

`t_bpm_task` 新增可空字段：

```text
approval_group_id
```

现有单人、顺序多人和加签投影保持 `NULL`。会签成员任务引用所属审批组，仍保留现有任务状态、结果、处理人快照、动作记录和 Flowable 引擎任务 ID。

首期不新增独立的成员表：成员就是带有 `approval_group_id` 的独立任务投影；成员序号和配置来源由定义的编译快照提供，处理结果由任务和动作日志提供。

## BPMN 编译

### 拓扑

一个三人会签节点编译为下列受限结构：

```text
... -> gateway_finance_review_split
        -> finance_review_1 ->
        -> finance_review_2 -> gateway_finance_review_join -> ...
        -> finance_review_3 ->
```

实际 BPMN 使用一个 `parallelGateway` 分叉、三个带独立 `flowable:assignee` 变量的 `userTask`、一个 `parallelGateway` 汇聚。每个成员任务均是正常的单处理人 Flowable task，变量继续沿用现有形式：

```text
assignee_finance_review_1
```

例如：

```text
assignee_finance_review_1
assignee_finance_review_2
assignee_finance_review_3
```

所有成员完成时，汇聚网关自然放行后续线性节点；没有成员完成前，后续节点不得被创建。

### 编译器演进方式

`SimpleModelBpmnCompiler` 从“连续 user task 列表”扩展为仍然受限的线性片段编译器：

- 单人节点：追加一个 user task 片段。
- 顺序多人节点：追加多个连续 user task 片段。
- 并行全员会签：追加并行分叉、成员 task、并行汇聚片段。

这不是通用 BPMN 图 DSL。模型仍按 authored 节点数组顺序连接；只有 `parallelAll` 内部允许固定的分叉/汇聚拓扑。

校验器必须在生成 BPMN 前检查所有展开后的 task key、gateway key 与 sequence flow key 的格式、长度和全局唯一性。现有保留 ID 规则继续生效，新增生成 ID 不能与单人或顺序多人展开结果冲突。

## 运行时语义

### 创建与投影

Flowable 为会签成员创建 task 时，Hunyuan 任务投影服务根据 compiled node 快照执行以下工作：

1. 以 `engine_process_instance_id + approval_group_key` 查找或创建审批组。
2. 首次创建时写入固定的总成员数、组名、模式和 `PENDING` 状态。
3. 创建成员任务投影，并填入 `approval_group_id`。
4. 以唯一约束和可重试的创建逻辑抵抗多个 Flowable task 创建事件同时到达。
5. 所有展示与动作服务只通过审批组和成员任务的结构化数据判断会签状态。

审批组计数是持久化投影。每次成员状态变化时，在组锁内基于成员任务的实际状态更新计数；不能依赖任务名后缀、页面侧计数或 `runtimeAssignmentSnapshotJson` 猜测进度。

### 状态转移

| 动作 | 组状态变化 | 当前成员任务 | 其他成员任务 | Flowable 与实例 |
| --- | --- | --- | --- | --- |
| 非最后一人通过 | `PENDING -> PENDING` | 完成，结果为通过 | 保持待办 | 当前分支完成，实例继续运行 |
| 最后一人通过 | `PENDING -> APPROVED`，原因 `ALL_APPROVED` | 完成，结果为通过 | 已全部完成 | 汇聚网关放行下游节点或结束实例 |
| 任一人拒绝 | `PENDING -> REJECTED`，原因 `MEMBER_REJECTED` | 完成，结果为拒绝 | 取消所有仍待办成员 | 取消同组活动引擎任务并按既有拒绝语义立即终止实例 |
| 任一人退回发起人 | `PENDING -> RETURNED`，原因 `MEMBER_RETURNED` | 完成，结果为退回 | 取消所有仍待办成员 | 取消同组活动引擎任务，实例转为 `WAIT_RESUBMIT` |
| 发起人撤回 | `PENDING -> CANCELLED`，原因 `INSTANCE_RECALLED` | 按撤回语义处理 | 取消所有仍待办成员 | 保持撤回的实例级 `WAIT_RESUBMIT` 语义 |
| 发起人取消 | `PENDING -> CANCELLED`，原因 `INSTANCE_CANCELLED` | 按取消语义处理 | 取消所有仍待办成员 | 保持既有取消实例语义 |
| 转办、委派 | 状态和计数不变 | 更新该成员处理人 | 不受影响 | 不发生分支完成或组收敛 |

`returnToInitiator` 不能复用当前单分支的“先完成一个 Flowable task，再更新实例”的实现。会签中的退回必须先以组为单位停止活动分支，取消同组剩余引擎任务与任务投影，然后把实例置为 `WAIT_RESUBMIT`；不得让并行汇聚等待遗留成员，也不得在实例已退回后留下可继续审批的引擎 task。

拒绝也采用同样的组级停止原则。外部调用方看不到 Flowable 的删除、终止或 execution 操作；这些仅由 Hunyuan 的 Flowable 运行时网关封装。

### 并发、幂等与锁顺序

会签的正确性以审批组为并发边界，而不是以页面按钮为并发边界。

1. 所有会改变会签成员状态的动作先锁审批组，再锁具体任务，锁顺序始终为 `approvalGroup -> task`。
2. 成员任务必须仍为 `PENDING`，审批组必须仍为 `PENDING`，才允许进入 Flowable 完成、拒绝或退回调用。
3. 两名成员同时通过时，组锁串行化计数与终态判断；只有确实最后完成的成员能够将组改为 `APPROVED`。
4. 通过与拒绝、退回并发时，先获得组锁的终止动作决定终态；后续请求发现组已关闭或任务已非待办，不得再次驱动 Flowable 流转。
5. 双击或网络重试不得产生第二次 Flowable completion、第二个下游任务或重复的组结束动作。
6. Flowable 事件回调、成员取消和实例级撤回路径也必须遵守相同的组锁顺序，避免跨成员死锁。

请求在已关闭组或已处理任务上的返回应沿用现有任务状态错误/幂等反馈风格，但任何反馈都不得隐藏已经发生的真实组终态。

### 高级动作边界

- 转办：允许对会签中的当前成员任务执行，属于成员级重新分配，不改变成员总数、进度或其他成员。
- 委派：允许，沿用 Hunyuan 当前“重新分配处理人”的语义，不暴露 Flowable 原生委派对象。
- 加签：禁止。后端只要发现 `approval_group_id` 非空即拒绝，前端不展示或不可操作。
- 减签：禁止。会签成员不是现有加签投影，前后端均不得把其视为可减签任务。
- 撤回：保持实例级语义；在会签期间撤回时必须关闭审批组并取消所有待办成员。
- 管理员转办、管理员委派：允许，但同样只能改变目标成员任务的处理人。

## 后端结构化契约

新增只属于 Hunyuan API 的 VO，不返回 Flowable task、execution 或 gateway ID。

### 审批组摘要

`BpmApprovalGroupSummaryVO` 至少包含：

- `approvalGroupId`
- `approvalGroupKey`
- `approvalGroupName`
- `approvalMode`
- `groupState`
- `totalMemberCount`
- `processedMemberCount`
- `approvedMemberCount`
- `rejectedMemberCount`

`BpmTaskVO` 增加可空字段：

```text
approvalGroup
```

待办、已办、管理端任务列表和实例当前任务都可通过它显示：

```text
财务会签，2/3 已处理
```

普通任务的该字段为 `null`，原有页面不因没有审批组而改变语义。

### 审批组详情

`BpmApprovalGroupDetailVO` 在摘要基础上增加：

- `closeReason`、`closedAt`
- `members`

每个 `BpmApprovalGroupMemberVO` 至少包含：

- `taskId`
- `memberIndex`、`memberTotal`
- 当前 `assigneeEmployeeId`、姓名和部门快照
- `taskName`、`taskState`、`taskResult`
- `assignedAt`、`completedAt`、`cancelledAt`
- 最后一次可展示动作及其意见

结构化详情由后端把任务投影、动作日志和编译快照组合而成。页面不得解析 `runtimeAssignmentSnapshotJson`、`currentNodeSummaryJson`、任务 key 后缀或任务名称来推断会签成员与进度。

### 接口承载位置

不增加为展示而存在的独立页面接口，优先扩展现有详情和列表契约：

| 现有返回 | 新字段 |
| --- | --- |
| `BpmTaskVO` | `approvalGroup: BpmApprovalGroupSummaryVO \| null` |
| `BpmTaskDetailVO` | `approvalGroup: BpmApprovalGroupDetailVO \| null` |
| `BpmInstanceDetailVO` | `approvalGroups: BpmApprovalGroupDetailVO[]` |
| `BpmInstanceTraceVO` | `approvalGroups: BpmApprovalGroupDetailVO[]` |

员工端与管理端读取同一套 Hunyuan 结构化语义，仍由既有实例、任务和权限边界控制可见范围。

## 前端体验

### 设计器

- `userTask` 属性中新增“并行全员会签”模式。
- 选择该模式后固定候选人类型为“指定员工”，展示现有远程员工多选。
- 未选满两人、选择重复员工或切换模式后残留非法配置时，保存和发布前给出明确校验。
- 顺序审批与并行会签同时使用 `employeeIds`，但展示文案、校验规则和发布后的拓扑语义不同，不能相互混淆。

### 运行时页面

- 我的待办和我的已办在任务名称附近显示审批组名称、已处理人数和总人数。
- 任务详情显示完整成员列表、每人当前处理状态、处理结果、意见、处理时间和组结束原因。
- 实例详情抽屉和 trace 显示审批组列表；当前正在进行的组优先展示进度，已关闭组展示结果与原因。
- 成员被拒绝、退回、撤回或取消后，其他成员不再显示为可处理待办；详情保留其取消状态和审批组结束原因。
- 操作区对会签成员隐藏或禁用加签、减签；转办、委派、撤回继续按后端可用性展示。
- 动作记录和通知文本增加可读标签，例如“会签成员通过”“会签成员拒绝，审批组已终止”“会签成员退回发起人，其他成员待办已取消”。

内部 JSON 快照仍可作为管理端诊断信息保留，但不得成为会签业务展示的数据源。

## 数据库与兼容性

新增 SQL 文件：

```text
数据库SQL脚本/mysql/sql-update-log/v3.43.0.sql
```

它包含：

1. 创建 `t_bpm_approval_group` 及其实例、状态、结束时间、唯一约束和查询索引。
2. 为 `t_bpm_task` 增加可空 `approval_group_id` 及索引。
3. 不回填历史任务，不改变已发布定义，也不重写现有 Flowable 数据。

兼容性规则：

- 已部署的线性、单人和顺序审批定义继续按原逻辑运行。
- 历史任务和实例返回空审批组字段或空审批组列表。
- 新的结构化字段为兼容性新增字段；既有接口路径、权限码和基础字段不改名。
- 不新增 Maven、pnpm 或前端运行时依赖。

## 实现批次

### 批次 1：DSL、校验、预检、SQL 与并行 BPMN 编译

- 扩展设计器类型、模式选择和员工多选校验。
- 扩展 `SimpleModelValidator` 与候选人发布预检。
- 编译器从线性任务列表演进为受限线性片段，输出并行网关拓扑和完整编译快照。
- 新增 `v3.43.0.sql` 的审批组表及任务关联字段。
- 覆盖单人、顺序、并行模式的互不回归测试。

### 批次 2：审批组投影、收敛与组级终止

- 新增审批组实体、DAO、服务和任务投影关联。
- 在成员 task 创建时稳定归组，在成员动作后更新结构化进度。
- 实现全员通过后的自然收敛。
- 实现任一拒绝、退回发起人、撤回、取消时的成员取消与引擎停止。
- 实现固定锁顺序、重复提交保护和并发动作保护。

### 批次 3：后端 VO/API 与前端展示

- 输出审批组摘要和详情 VO。
- 扩展任务列表、任务详情、实例详情、trace 的契约和 MyBatis 查询。
- 更新运行时 API TypeScript 类型。
- 更新待办、已办、任务详情、实例详情抽屉和 trace 展示。
- 确保页面不再依赖内部快照 JSON 推导会签。

### 批次 4：高级动作、动作轨迹与通知

- 将转办、委派、撤回接入会签成员和组级保护。
- 对加签、减签增加后端硬拒绝与前端操作保护。
- 补齐组结束动作、成员处理动作和通知文案。
- 复核管理员操作与员工操作的权限和展示边界。

### 批次 5：契约、兼容与真实验收

- 运行 BPM 模块、Flowable 兼容、前端契约和类型检查。
- 运行并发与幂等测试。
- 使用三个真实员工完成浏览器/API 活体验收。
- 写入独立验收记录，并更新 `docs/superpowers/specs/2026-07-10-bpm-development-baseline.md` 的已完成能力和平台边界。

## 验收矩阵

| 维度 | 必须验证的事实 |
| --- | --- |
| DSL 与发布 | `parallelAll` 只接受至少两名有效、不同的指定员工；非法配置无法发布 |
| BPMN | 生成一个分叉网关、N 个成员 task、一个汇聚网关；单人和顺序拓扑不回归 |
| 任务投影 | N 个独立 `t_bpm_task` 关联同一审批组；成员信息和进度可查询 |
| 全员通过 | 前 N-1 个成员通过后不创建下游节点；最后一人通过后仅流转一次 |
| 任一拒绝 | 一个成员拒绝后同组剩余待办被取消，实例立即结束，后续成员不能再处理 |
| 退回发起人 | 一个成员退回后同组剩余待办被取消，实例为 `WAIT_RESUBMIT`，无遗留活动引擎任务 |
| 并发与幂等 | 同时通过、通过与拒绝竞态、双击提交都不会重复流转或产生死锁 |
| 高级动作 | 会签成员可转办、委派、撤回；加签、减签在 API 与页面均被拒绝 |
| 页面 | 待办、详情、实例详情和 trace 显示结构化会签数据，不解析内部 JSON |
| 兼容 | 历史和非会签流程无审批组时仍能正常运行和展示 |

建议的最小门禁：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=SimpleModelValidatorTest,SimpleModelBpmnCompilerTest,BpmTaskAssignmentResolverTest,BpmApprovalGroupServiceTest,BpmTaskServiceTest' test
```

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin '-Dtest=BpmFlowableCompatibilityTest' test
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

服务可用时，复用持久 Playwright MCP 会话，在三个员工账号之间验证真实会签流程；运行时截图、网络日志和会话文件保留在 `G:\code-mcp\playwright-mcp-temp\runtime`，不提交到仓库。

## 完成定义

该模块只有在以下条件全部满足时才算完成：

1. 五个实现批次均有对应代码和聚焦验证。
2. 新旧流程都通过相关后端与前端门禁。
3. 三人真实会签的通过、拒绝、退回和高级动作边界已在活体环境中验证。
4. 结构化审批组契约覆盖任务、实例详情和 trace，前端未通过内部 JSON 推导会签。
5. 验收记录和 BPM 开发基线已按真实执行结果更新。
