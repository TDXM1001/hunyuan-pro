# BPM 模块 M2：审批人与多人审批策略设计

- 日期：2026-07-11
- 状态：模块设计基线
- 优先级：P1
- 前置条件：M1 统一节点契约稳定

## 1. 结论

本模块将当前分散在候选解析、编译器和审批组服务中的多人逻辑统一为四个概念：候选来源、成员冻结、完成策略和异常兜底。现有顺序审批和 `parallelAll` 保持兼容，在同一模型上增加或签和比例审批；岗位、用户组、多级主管等组织来源通过身份网关扩展。

不直接复制参考项目的 multi-instance 模型。Flowable 是否使用 multi-instance 是内部编译选择，公共语义始终是 Hunyuan 审批阶段和成员。

## 2. 当前事实

- 已支持六类候选策略：指定员工、部门主管、角色、发起人、发起人部门主管、发起时自选员工。
- 已有发布前候选预检和运行时身份校验。
- 顺序多人和 `parallelAll` 已拥有审批组投影、成员任务、进度、终态和结构化详情。
- 顺序审批组的未来成员尚未创建任务，成员计划主要存在于定义快照。
- 高级动作在普通、顺序和 `parallelAll` 上已有明确差异。

## 3. 参考借鉴与优化

| 参考能力 | 决策 | 优化方式 |
| --- | --- | --- |
| 岗位、用户组、多级部门主管 | `ADAPT` | 通过 Hunyuan 组织身份网关解析并冻结 |
| 表单人员、发起人选择、前序选择 | `ADAPT` | 统一为类型化 CandidateSpec |
| 顺序、或签、比例、随机 | 部分 `ADAPT` | 先实现确定性的顺序/全员/或签/比例，随机延期 |
| 审批人为空处理 | `ADAPT` | 默认阻断，自动终态需权限和风险警告 |
| 发起人自审策略 | `ADAPT` | 作为显式节点策略并进入审计 |
| Flowable multi-instance | 不直接采用 | 内部实现可评估，公共投影保持 Hunyuan 语义 |

## 4. 统一领域模型

### 4.1 候选来源

```text
CandidateSpec
  resolverType
  resolverParams
  resolutionPhase: PUBLISH | START | ACTIVATE
  emptyPolicy
  selfApprovalPolicy
```

目标 resolverType：

```text
EMPLOYEE
ROLE
DEPARTMENT_MANAGER
START_EMPLOYEE
START_DEPARTMENT_MANAGER
FORM_EMPLOYEE
POST
USER_GROUP
MULTI_LEVEL_DEPARTMENT_MANAGER
PREVIOUS_NODE_SELECTED
```

表达式候选人不接受任意 EL，只能使用登记解析器键。

### 4.2 审批策略

```text
ApprovalPolicy
  approveType: HUMAN | AUTO_APPROVE | AUTO_REJECT
  mode: SINGLE | SEQUENTIAL_ALL | PARALLEL_ALL | ANY | RATIO
  ratioThreshold
  rejectPolicy
  returnPolicy
  allowedActions
```

- `ANY`：第一个有效通过结束阶段；第一个拒绝是否终止由 rejectPolicy 明确。
- `RATIO`：使用冻结成员总数和整数阈值计算，不使用浮点累计。
- 成员弃权、取消、转办和委派是否影响分母必须固定在定义快照。

### 4.3 空集合和自审策略

```text
NoCandidatePolicy: BLOCK | ASSIGN_ADMIN | ASSIGN_EMPLOYEE | AUTO_APPROVE | AUTO_REJECT
SelfApprovalPolicy: ALLOW | SKIP_SELF | ASSIGN_DEPARTMENT_MANAGER | BLOCK
```

默认均为 `BLOCK`。自动通过、自动拒绝和管理员兜底需要独立发布权限、醒目警告、定义快照和动作审计。

### 4.4 拒绝与退回策略

```text
RejectPolicy: END_PROCESS | RETURN_INITIATOR | RETURN_ANCESTOR_TASK
```

`RETURN_ANCESTOR_TASK` 只能选择当前 authored 路径上已发布的祖先 user task，不能自由输入节点 key，也不能跳入未执行分支。运行时终止当前执行代，按冻结目标创建新的执行代和任务投影，保留旧任务、审批组、路由和数据历史；它不是直接移动 Flowable token 后覆盖历史。

## 5. 审批阶段和成员投影

现有 `t_bpm_approval_group` 保留，语义升级为一次 authored 审批阶段。新增 `t_bpm_approval_group_member` 作为冻结成员计划：

```text
approval_group_member_id
approval_group_id
member_index
source_employee_id
current_employee_id
member_state
member_result
activated_task_id nullable
resolution_snapshot_json
activated_at/completed_at/cancelled_at
```

这样可以稳定表达：

- 顺序审批尚未激活的未来成员。
- 或签、比例审批中被提前取消的成员。
- 转办后来源成员与当前成员。
- 冻结成员总数和比例分母。
- 组终止后没有生成任务的成员事实。

历史审批组继续按任务投影读取；新定义使用成员表。不得伪造历史成员记录。

## 6. 完成策略

将当前按模式分支的组状态逻辑收敛为：

```text
ApprovalCompletionPolicy
  onMemberApproved(context)
  onMemberRejected(context)
  onMemberReturned(context)
  calculateProgress(context)
  allowedActions(context)
```

固定锁顺序：

```text
instance -> approval group -> member -> task
```

每次动作重新基于成员事实计算终态，不信任页面计数。终态只能从 `PENDING` 单向进入，双击和并发请求不能重复推进 Flowable。

## 7. 编译和运行

- `SINGLE` 编译一个 user task。
- `SEQUENTIAL_ALL` 可继续编译连续任务，成员投影负责计划和激活。
- `PARALLEL_ALL` 继续使用受限并行片段。
- `ANY/RATIO` 首选编译为固定并行成员片段，由 Hunyuan 完成策略决定提前结束并取消剩余分支；是否在内部使用 multi-instance 必须通过取消、退回、历史和升级实验后单独决策。
- 候选集合在定义允许的阶段解析并冻结，运行中组织变化不重写已激活审批阶段。

## 8. 高级动作规则

| 动作 | 默认语义 |
| --- | --- |
| 转办 | 改变成员当前处理人，不改变成员数和完成阈值 |
| 委派 | 保持 Hunyuan 当前重新分配语义，后续可独立升级原生委派 |
| 加签 | 作为附加任务，不自动改变 authored 阶段阈值 |
| 减签 | 只撤销可撤销的附加任务 |
| 撤回/取消 | 关闭阶段、成员和任务，保留原终止原因 |
| 退回 | 按阶段策略关闭当前执行并进入 `WAIT_RESUBMIT` |

若未来需要“加签改变比例分母”，必须作为新的明示策略，不改变现有语义。

## 9. 前端体验

- 候选来源使用与类型匹配的选择器，不允许日常路径手输 ID。
- 多人模式显示成员解析时机、完成条件、拒绝/退回规则和风险提示。
- 比例审批使用整数百分比和实际人数预览。
- 发布预检展示模拟成员、空集合、自审处理和运行时依赖。
- 任务和实例详情显示计划成员、已激活成员、已处理成员和阶段终态。

## 10. 安全与兼容

- 组织身份网关是员工有效性的唯一来源。
- 员工、角色、岗位、用户组解析结果进入冻结快照。
- 普通员工不能配置自动终态策略。
- 历史定义和审批组继续按旧语义运行。
- 现有 `approvalGroup` API 只增字段，不删除或重命名。
- 成员表上线不回填无法证明的历史计划成员。

## 11. 交付块

1. CandidateSpec、空集合/自审策略和发布预检。
2. 审批组成员投影及现有顺序/parallelAll 兼容。
3. 完成策略抽象和现有模式回归。
4. 或签运行时、并发终止和真实验收。
5. 比例审批、阈值边界和动态取消验收。
6. 自动终态、指定祖先退回、岗位、用户组、多级主管及管理前端。

## 12. 验收矩阵

覆盖确定成员、运行时成员、空集合、自审、顺序、全员会签、或签、比例边界、转办、委派、加减签、并发通过/拒绝、撤回、取消、退回重提和历史兼容。

## 13. 完成定义

只有候选来源、成员冻结、五种审批模式、空集合和自审策略、审批阶段投影、高级动作边界、并发幂等、前端配置、真实业务流和历史兼容全部闭环后，M2 才完成。单独实现或签或岗位不算模块完成。
