# BPM 模块 M4：核心审批运行与工作台设计

- 日期：2026-07-11
- 重审日期：2026-07-12
- 状态：当前模块设计，待按新定义与数据契约重建
- 优先级：P1，M1-M4 首个产品基线的关闭模块
- 总体蓝图：`docs/superpowers/specs/2026-07-11-bpm-enterprise-blueprint.md`
- 前置条件：M1、M2、M3 当前契约稳定

## 1. 结论

M4 将已发布定义、审批策略和审批对象转化为可执行、可审计、可恢复的审批实例，并向申请人、审批人和经办人提供统一工作台。

M4 负责实例生命周期、任务、审批组、路由事实、统一动作命令、抄送、运行投影和结果 Outbox。M4 不负责 timer、外部等待和子流程，这些高级能力属于 M5；不负责外部系统登记与协议，这些属于 M6。

## 2. 运行事实

```text
ProcessInstanceFact
  instanceId / definitionVersion / businessObjectKey / status / currentGeneration

InstanceVisibilitySnapshot
  policyVersion / canonicalDigest / organizationVersion / scopeAstDigest / createdAt

InstanceVisibilityGrantFact
  instanceId / employeeId / source(SCOPE|STARTER|PARTICIPANT|EXPLICIT) / grantedAt

TaskFact
  taskId / authoredNodeId / taskVersion / candidateSnapshot / status / allowedActions

ApprovalGroupFact
  policyVersion / members / progress / completionStatus

PreFrozenCandidateSnapshotFact
  preFrozenSnapshotId / instanceId / authoredNodeId / candidatePolicyDigest / snapshot / sourceFactsDigest

PreFrozenCandidateBindingFact
  preFrozenSnapshotId / generationId / engineExecutionId / stageInvocationId / approvalStageId

RouteDecisionFact
  gatewayNodeId / factVersion / matchedEdges / defaultUsed / reasons

TaskActionFact
  requestId / actor / action / evidence / beforeAfterState / occurredAt

CommandReceipt
  tenantId / instanceId / requestId / canonicalFingerprint / result / receiptState

BusinessResultOutbox
  eventId / terminalFactVersion / deliveryStatus

ParticipantNotificationFact
  recipient / channel / templateVersion / taskOrInstanceVersion / deliveryStatus
```

前端只能读取这些结构化事实，不能通过 Flowable task key、BPMN XML 或名称推断业务状态。

## 3. 实例与任务生命周期

主实例状态至少区分草稿外的运行中、退回待重提、已通过、已拒绝、已取消、异常待处置。任务状态至少区分待处理、已完成、已取消、被组策略终止和失效。

退回、重提、撤回和取消都是显式业务命令与新事实，不物理删除或伪造历史。指定节点退回只允许已发布图中的合法祖先审批节点，并建立新的执行代。

## 4. 统一动作命令

```text
TaskActionCommand
  requestId
  taskId / taskVersion
  action
  actorIdentity（服务端注入）
  approvalSubjectVersion
  processWorkingDataVersion
  comment / attachments
  clientChannel
```

客户端不能提交或覆盖实际处理人。服务端固定执行顺序：从登录会话或外部映射生成身份 -> 原子查询或占用 `CommandReceipt` -> 同指纹直接返回既有结果、异指纹返回冲突 -> 仅新 receipt 校验员工和任务授权、任务/数据版本 -> 写动作证据 -> 推进引擎 -> 写运行事实和投影 -> 写结果 Outbox。

引擎推进、领域事实、通知事实和 Outbox 必须有明确一致性边界。失败不能留下“Flowable 已推进但 Hunyuan 没记录”的静默分裂；无法自动恢复时进入 M7 异常队列。任务创建、退回、取消和终态通知按冻结模板版本投递，通知失败不回滚审批状态。

## 5. 工作台

- 申请人：发起、我的申请、撤回、补件/重提、进度与结果。
- 审批人/经办人：待办、已办、抄送、审批详情、允许动作和历史证据。
- 运行详情：authored 流程图、当前与历史节点、路由决定、审批组进度、数据版本和动作时间线。

任务详情必须至少展示 M3 审批对象快照。来源系统故障不能导致审批页面空白。

## 6. 失败闭环

- 重复 `requestId` 返回既有命令结果，不再次推进。
- 过期任务版本、数据版本冲突、任务已完成和越权动作均明确拒绝并审计。
- 并发审批组动作和终态竞争只能产生一个有效组终态、实例终态和结果事件。
- 引擎推进异常、投影失败和 Outbox 写入失败有可诊断状态和恢复命令。
- 业务结果投递失败不回滚已形成的流程终态，由 Outbox 重试和 M7 补偿处理。

## 7. 完成定义

1. 内置通用申请可发起、路由、生成单人/多人任务并到达正确终态。
2. 通过、拒绝、退回重提、撤回、取消、办理和抄送具备主路径与权限测试。
3. 重复提交、并发提交、过期版本和终态竞争不产生第二次有效推进。
4. 待办、已办、抄送、申请详情与 authored 运行图事实一致。
5. 任一实例可从定义版本、审批对象、任务、动作、路由、审批组和 Outbox 还原全过程。
6. 服务重启后未完成实例、未投递任务通知和未投递结果可继续处理。
7. M1-M4 通过一个完整内部审批业务流和关键异常矩阵，首个产品基线关闭。

## 8. M2 审批阶段协作契约

M4 为每个 M1 `ApprovalStageControl` 等待点创建唯一 `ApprovalStageControlFact`：`stageInvocationId / instanceId / authoredNodeId / compiledElementId / engineExecutionId / controlState`。`snapshotPhase=START` 的候选结果在实例创建事务内先写入唯一、不可变的 `PreFrozenCandidateSnapshotFact`；每个等待点激活按 `(preFrozenSnapshotId, generationId, engineExecutionId)` 原子创建或复用 `PreFrozenCandidateBindingFact`，并据此创建本次阶段、审批组和成员任务投影。相同执行的重试复用 binding；祖先退回的新执行代创建新的 binding 和阶段，但不重算候选人。`snapshotPhase=ACTIVATE` 才以完整定义依赖快照、租户、发起人身份和 M3 的已脱敏 `RoutingFactView` 调用 M2 并直接持久化候选快照、审批组和成员任务投影。M4 在实例创建时还持久化 M2 的 `InstanceVisibilitySnapshot` 与范围、发起人、参与者和显式授权 grant；M4 不直读策略目录或让页面解释策略 JSON。

成员动作的固定顺序为：注入服务端身份 -> 原子查询或占用 `CommandReceipt`（同指纹直接返回既有结果、异指纹冲突） -> 仅新 receipt 校验任务与数据版本 -> M2 参与者授权 -> 写成员和动作事实 -> M2 完成决策 -> 以 `instance -> approval group -> member -> task` 锁顺序声明一次 `ApprovalStageControl` 推进、关闭或创建执行代 -> 写运行投影与 Outbox。自动终态和多人策略都只能控制同一个阶段等待点；`ANY`、`RATIO` 的剩余成员只取消 M4 投影。阶段控制或投影失败进入可恢复异常状态，不能形成已推进但无领域事实的静默分裂。

M4 还必须幂等消费组织域的员工有效性变更并在恢复后扫描活动阶段；冻结成员失效时写 `INELIGIBLE`，按 M2 的可达性决定进入终态或 `EXCEPTION_PENDING`。恢复只能使用受权的成员转办命令，保留原成员、策略、阈值和审计事实，不得按当前组织重新解析整个阶段。
