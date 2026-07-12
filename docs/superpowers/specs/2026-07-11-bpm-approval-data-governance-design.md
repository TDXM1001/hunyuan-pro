# BPM 审批数据治理模块设计

- 设计日期：2026-07-11
- 适用仓库：`E:\my-project\hunyuan-pro`
- 工作分类：平台或语义变更
- 状态：历史字段治理设计已实施并通过仓库验收；不代表当前 M3 已关闭
- 依赖基线：`docs/superpowers/specs/2026-07-10-bpm-development-baseline.md`

> 当前 M3 已扩展为审批对象、路由事实、流程工作数据和动作证据四类数据面的完整治理，设计入口为 `docs/superpowers/specs/2026-07-11-bpm-module-03-approval-data-governance-design.md`。本文只保留字段权限、版本、审计和重提语义的历史证据。

## 1. 结论

本交付块建设一个完整的 **BPM 审批数据治理模块**，使 Hunyuan BPM 从“保存并展示表单 JSON”升级为“按发布节点权限安全读取、修改、校验、追踪并回写审批数据”。

模块一次闭环五条业务线：

```text
设计配置 -> 发布冻结 -> 节点授权处理 -> 数据版本与变更审计 -> 退回重提及业务回写
```

本模块是一项平台级纵向交付。设计器、DSL、发布校验、定义快照、运行时任务、实例数据、审计、前端工作台、业务回调和真实验收属于同一个交付块。实施批次只表示连续执行顺序，不形成新的设计、范围或批准循环。

## 2. 当前事实与缺口

### 2.1 已有基础

- 表单已经通过 `formSchemaJson` 建模，并随定义发布为 `formSchemaSnapshotJson`。
- 实例已经保存 `initialFormDataSnapshotJson` 和 `currentFormDataSnapshotJson`。
- 定义节点已经保存 authored 和 compiled 节点快照。
- 员工任务详情已经校验当前处理人归属，管理端详情权限保持独立。
- 发起、退回重提、单人审批、顺序审批组和 `parallelAll` 已有明确运行边界。
- 实例详情、trace、通知、回调、人工补偿和业务结果回写已经存在。
- 样板费用申请已经能够证明 BPM 与业务单据之间的发起和结果回调链路。

### 2.2 核心缺口

- 审批节点不能定义字段可见、只读、可编辑和节点必填规则。
- 员工任务详情没有权限裁剪后的表单上下文，只能查看基础任务信息。
- 审批动作不能原子提交获授权的表单修改。
- 后端没有字段级越权校验，前端禁用也不能构成安全边界。
- 当前表单快照没有版本号，无法可靠拒绝旧页面覆盖新数据。
- trace 不能回答谁在什么节点修改了哪些字段。
- 业务回调没有明确携带最终数据版本和最终表单快照。

## 3. 目标与非目标

### 3.1 目标

1. 设计器按 `userTask` 配置字段 `READONLY`、`EDITABLE`、`HIDDEN` 和节点必填约束。
2. 权限随定义节点发布冻结，运行实例不读取模型草稿或新表单版本。
3. 员工任务接口在服务端裁剪隐藏字段，并只接受获授权字段修改。
4. 表单修改、字段审计和任务通过在同一事务内成功或回滚。
5. 实例使用单调递增的数据版本拒绝旧页面覆盖。
6. 顺序审批能把前一成员的修改传递给后一成员。
7. 退回重提基于最新快照继续演进，并保留完整版本历史。
8. trace 以业务字段语义展示变更记录。
9. 最终数据通过现有可靠回调边界回写业务单据，失败重试保持幂等。
10. 用样板费用申请完成设计、发布、发起、修改、审批、追踪和回写的真实闭环。

### 3.2 非目标

- 条件、并行、包容等通用网关。
- 或签、比例审批或 Flowable multi-instance。
- 并行会签成员协同编辑、字段合并或实时冲突解决。
- 跨表关联表达式、脚本字段和通用低代码数据源。
- BPM 直接修改业务模块数据库表。
- 审批过程中的独立表单草稿保存；第一版只在“通过”动作中原子提交修改。
- 通过拒绝、退回、转办、委派、加签或减签提交字段修改。
- 对已经结束的历史实例批量构造字段变更记录。

## 4. 领域模型

### 4.1 节点字段权限 DSL

每个 authored `userTask` 可增加：

```json
{
  "nodeKey": "finance_review",
  "name": "财务复核",
  "type": "userTask",
  "fieldPermissions": [
    {
      "fieldKey": "expenseAmount",
      "permission": "EDITABLE",
      "required": true
    },
    {
      "fieldKey": "applicantName",
      "permission": "READONLY",
      "required": false
    },
    {
      "fieldKey": "internalRemark",
      "permission": "HIDDEN",
      "required": false
    }
  ]
}
```

规则：

- 权限枚举固定为 `READONLY`、`EDITABLE`、`HIDDEN`。
- `required=true` 只允许和 `EDITABLE` 组合。
- `fieldKey` 必须存在于发布时表单 schema。
- 同一节点中的 `fieldKey` 必须唯一。
- 未配置字段默认 `READONLY`，保证旧定义升级后不会获得隐式写权限。
- 没有绑定表单的模型不能配置字段权限。
- 顺序展开任务继承 authored 节点的同一权限。
- `parallelAll` 只能配置 `READONLY` 或 `HIDDEN`，发布阶段拒绝 `EDITABLE`。

### 4.2 运行时表单上下文

员工任务详情增加：

```text
formContext.dataVersion
formContext.formSchemaJson
formContext.formDataJson
formContext.permissions[]
```

其中 `permissions[]` 包含 `fieldKey`、`permission` 和 `required`。后端必须先执行权限裁剪：

- `HIDDEN` 字段从 schema 和 data 同时移除。
- `READONLY` 字段保留在 schema 和 data 中。
- `EDITABLE` 字段保留并允许在通过动作中提交。
- schema 中不存在但 data 中存在的字段不向员工任务接口泄露。

管理端实例详情继续通过管理权限查看完整快照，不复用员工端裁剪结果。

### 4.3 数据版本

`t_bpm_instance` 增加 `form_data_version BIGINT NOT NULL DEFAULT 1`。

- 首次发起成功时版本为 `1`。
- 审批通过实际修改字段时版本加一。
- 重提实际修改字段时版本加一。
- 没有字段变化的审批通过不增加版本。
- 客户端提交的版本与数据库版本不同则拒绝，不执行 Flowable 动作。

### 4.4 字段变更审计

新增 `t_bpm_form_data_change`：

```text
change_id
instance_id
task_id nullable
definition_node_id nullable
node_key_snapshot
change_source
actor_employee_id
actor_name_snapshot
before_version
after_version
changed_fields_json
before_values_json
after_values_json
create_time
```

`change_source` 固定为：

- `INSTANCE_STARTED`
- `TASK_APPROVED`
- `INSTANCE_RESUBMITTED`

审批和重提只记录实际变化字段。首次发起记录字段集合和初始值，用于建立版本历史起点。

## 5. 发布治理

### 5.1 设计器

模型编辑器在节点属性区域提供字段权限矩阵。字段来源只取当前绑定表单 schema，不允许自由输入字段 key。表单被替换或字段被删除后，旧配置保留为可见错误，保存或发布时明确阻断，不能静默丢弃。

### 5.2 后端校验

`BpmSimpleModelPublishValidator` 扩展字段权限校验，并将问题写入现有发布检查报告：

- `FIELD_PERMISSION_FIELD_NOT_FOUND`
- `FIELD_PERMISSION_DUPLICATED`
- `FIELD_PERMISSION_INVALID_MODE`
- `FIELD_PERMISSION_REQUIRED_NOT_EDITABLE`
- `FIELD_PERMISSION_FORM_MISSING`
- `FIELD_PERMISSION_PARALLEL_EDIT_FORBIDDEN`

### 5.3 快照

权限同时保存在定义 `simpleModelSnapshotJson` 和定义节点 authored/compiled 快照。运行时根据任务的定义节点身份读取节点快照，不读取模型表，也不根据任务名称或 key 后缀推断权限。

## 6. 任务处理数据流

### 6.1 加载任务工作台

1. 员工调用现有 `/app/bpm/task/detail/{taskId}`。
2. 后端确认任务属于当前员工。
3. 根据任务和定义节点加载冻结权限。
4. 读取实例当前数据及版本。
5. 按字段权限裁剪 schema 和 data。
6. 返回任务、审批组、动作轨迹和 `formContext`。

### 6.2 审批通过

`BpmTaskApproveForm` 增加：

```text
formDataVersion: Long
formDataPatchJson: String
```

两字段允许同时为空，以兼容无表单或旧定义的普通审批。只要提交 patch，就必须提交版本。

事务顺序：

```text
审批组（存在时） -> 当前任务 -> 流程实例 -> 权限和版本校验
-> 合并当前数据 -> 写实例快照和版本 -> 写字段审计
-> 完成 Flowable 任务 -> 更新任务/组/实例投影 -> 写动作日志和抄送
```

约束：

- patch 必须是 JSON object。
- patch 只能包含 `EDITABLE` 字段。
- patch 中出现隐藏、只读、schema 外字段时整个请求失败。
- `required=true` 的字段在合并后不能为空。
- 统一运行时表单数据校验器执行字段白名单、节点必填以及已支持组件的基础值类型校验；发起、重提和审批修改复用同一校验边界。
- 数据写入、审计和审批动作在同一事务内回滚。

### 6.3 其他任务动作

- 拒绝和退回忽略前端未提交的临时编辑，不接受 patch。
- 转办和委派后，新的当前处理人使用同一节点权限和当前数据版本。
- 加签任务继承来源任务的只读展示上下文，但第一版不允许加签任务编辑字段。
- 减签不改变实例数据。
- `parallelAll` 成员只读或隐藏，因此不存在并行写冲突。

## 7. 退回重提

- 退回终止当前 Flowable 实例并进入 `WAIT_RESUBMIT`，保留当前数据快照和版本。
- 重提草稿返回当前快照，不返回初始快照。
- 发起人按完整发布表单 schema 修改数据。
- 重提接口校验客户端版本，避免审批退回页面被其他操作更新后仍旧覆盖。
- 重提成功时按实际差异更新版本并写 `INSTANCE_RESUBMITTED` 审计。
- 新 Flowable 实例继续关联同一 Hunyuan 实例；旧任务、审批组和数据变更保持历史事实。

## 8. 业务回写

`BpmBusinessResultEvent` 增加：

```text
finalFormDataVersion
finalFormDataJson
formDataLastModifiedAt
```

业务模块仍然通过已有 handler/callback 边界消费结果，BPM 不直接更新业务表。回调记录冻结当次事件载荷；人工重试复用同一最终数据版本和快照，不重新读取可能已经变化的实例数据。

样板费用申请扩展 `approvedAmount` 和 `finalFormDataVersion`，用于证明：

1. 发起人提交申请金额。
2. 财务节点修改核定金额。
3. 后续节点看到最新金额。
4. 流程通过后回写核定金额和最终版本。
5. 模拟回调失败后，人工重试得到相同结果。
6. 重复事件幂等，冲突终态拒绝覆盖。

## 9. 前端体验

### 9.1 节点字段权限面板

- 复用模型编辑器的节点配置区域。
- 字段按表单顺序展示，包含字段名、key、组件类型、权限和节点必填。
- 权限使用单选/分段控件，必填使用开关。
- `parallelAll` 选择后禁用 `EDITABLE` 并显示对应校验结果。
- 未绑定表单时显示空状态，不生成虚假字段。

### 9.2 审批工作台

- 待办详情使用现有任务详情入口，不新增重复页面体系。
- 运行表单渲染器支持逐字段 hidden/disabled/required，而不再只有整表 `disabled`。
- 通过对话框展示实际变更摘要和当前版本。
- 版本冲突时保留用户输入，重新加载最新数据后由用户再次确认，不自动覆盖或自动合并。
- 实例详情和 trace 使用字段 label 展示差异；找不到历史 label 时回退到 `fieldKey`。

## 10. 安全、并发与错误处理

- 前端权限只用于体验，后端节点快照是唯一授权来源。
- 员工任务接口不得返回 `HIDDEN` 字段值。
- patch 使用白名单校验，不通过删除非法字段后继续执行。
- 审批组、任务和实例遵循固定加锁顺序，避免并行动作死锁。
- 版本冲突返回稳定业务错误 `FORM_DATA_VERSION_CONFLICT`。
- 定义节点权限快照缺失或损坏时严格失败关闭：员工任务接口不返回业务表单 schema 和数据，禁止提交字段修改，并记录包含实例、任务和定义节点的错误日志。管理端完整详情仍按独立管理权限查询。
- 审计写入失败时审批事务回滚。
- 回调载荷生成失败时流程结果事实保留，回调记录进入可重试失败状态。

## 11. 数据库与兼容

预计使用 `v3.45.0.sql`；实施前必须再次确认该版本未被其他交付占用。

迁移内容：

- `t_bpm_instance.form_data_version`。
- 新表 `t_bpm_form_data_change` 及实例、任务索引。
- `t_bpm_sample_expense.approved_amount`。
- `t_bpm_sample_expense.final_form_data_version`。

兼容规则：

- 旧定义没有 `fieldPermissions` 时，全部 schema 字段默认只读。
- 无表单流程保持现有审批体验，不要求数据版本参数。
- 历史实例版本迁移为 `1`，不伪造历史变更。
- 普通单人、顺序审批组和 `parallelAll` 的流转与动作语义不改变。
- 现有 API 字段只增加可选值，不删除或重命名已有字段。

## 12. 连续实施批次

1. 领域契约、SQL、实体、DAO 和数据版本服务。
2. 发布校验、节点权限快照和设计器字段权限矩阵。
3. 任务表单上下文、服务端裁剪、原子审批和越权校验。
4. 退回重提、版本冲突、字段审计和 trace 展示。
5. 最终数据事件、样板费用回写、失败重试和幂等。
6. 全量回归、真实业务流验收、验收记录和基线回写。

这些批次是同一交付块的连续实现顺序，不拆分为六轮设计或批准。

## 13. 验收矩阵

| 验收面 | 必须证明的行为 |
| --- | --- |
| 发布 | 无效字段、重复权限、隐藏必填、并行可编辑全部被阻断 |
| 权限读取 | 隐藏字段不出现在员工接口，只读字段仍可查看 |
| 主路径 | 发起、审批修改、后续节点读取、最终通过和业务回写一致 |
| 越权 | 手工构造隐藏、只读或 schema 外字段 patch 被后端拒绝 |
| 并发 | 旧版本提交被拒绝，当前数据不被覆盖 |
| 顺序审批 | 前一成员修改后，后一成员看到新版本和新值 |
| 并行会签 | 成员只能读取，不能修改字段 |
| 拒绝 | 页面临时编辑不写入实例 |
| 退回重提 | 使用最新快照，重提差异形成新版本和审计 |
| 回调 | 最终快照随失败重试保持不变，重复事件幂等 |
| 审计 | 能回答谁、在哪个节点、何时、把什么从什么改成什么 |
| 兼容 | 旧定义默认只读，原有任务和审批组行为不退化 |
| 权限边界 | 管理端完整视图与员工裁剪视图相互独立 |
| 真实运行 | 浏览器完成设计、发布、发起、修改、审批、追踪和回写 |

## 14. 验证门禁

- 发布校验、表单数据变更、任务动作、重提、trace、业务事件和样板费用聚焦测试。
- `hunyuan-bpm` 模块全量测试。
- `BpmFlowableCompatibilityTest`。
- 前端 BPM API、设计器、运行表单和页面契约测试。
- `@hunyuan/system` 类型检查。
- 服务可用时复用持久 Playwright MCP 会话完成真实业务流。

历史结果只作为基线；验收记录必须写本次实际执行的命令、时间、结果和证据边界。

## 15. 完成定义

- 设计器能够配置并验证节点字段权限。
- 发布定义冻结权限快照。
- 员工任务接口不会泄露隐藏字段。
- 后端阻止所有未授权字段修改。
- 表单修改、审计和任务通过原子提交。
- 数据版本冲突不会静默覆盖。
- 顺序审批能传递上一成员的修改。
- 退回重提保持连续数据历史。
- trace 能展示字段级变化。
- 最终数据可靠回写业务单据并支持失败重试。
- 自动化门禁和真实浏览器/API 验收全部通过。
- 新增一份总体验收记录并更新 BPM 开发基线。

上述条件全部满足后，整个审批数据治理模块才算关闭；不以单个批次、单个接口或单个页面完成作为模块完成状态。
