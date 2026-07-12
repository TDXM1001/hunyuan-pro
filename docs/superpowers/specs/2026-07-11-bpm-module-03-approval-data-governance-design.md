# BPM 模块 M3：审批对象与数据治理设计

- 日期：2026-07-11
- 重审日期：2026-07-12
- 状态：当前模块设计；旧字段治理验收作为算法与测试资产
- 优先级：P1
- 总体蓝图：`docs/superpowers/specs/2026-07-11-bpm-enterprise-blueprint.md`
- 前置条件：M1 定义引用契约稳定；可与 M2 并行

## 1. 结论

M3 负责审批的是什么、谁能看什么、流程中哪些数据可以变化，以及每次动作留下什么证据。它是业务系统与审批运行时之间的数据防火墙。

旧字段权限、数据版本、变更账本、退回重提和终态冻结语义可以吸收，但新模块不受旧表单 JSON、旧实体或旧回调字段约束。

## 2. 核心契约

```text
BusinessContractVersion
  sourceSystem / businessType / businessKeyRule
  fieldSchema / sensitivity / attachmentRules / detailLayout / changePolicy

ApprovalSubjectSnapshot
  title / summary / fields / lineItems / attachments / submitter

RoutingFactSnapshot
  validated facts allowed for binding, routing and candidate resolution

ProcessWorkingData
  versioned data editable under node field policy

TaskActionEvidence
  action / actor / comment / signature / attachments / beforeAfterVersions
```

业务对象使用 `(sourceSystem, businessType, businessKey)` 唯一定位，业务键为字符串。路由事实、工作数据和动作证据不能混入同一个可任意修改的 JSON。

## 3. 端到端业务线

```text
登记业务契约和详情布局
-> 提交业务对象与版本
-> schema、敏感级别和业务键校验
-> 生成审批对象快照、路由事实和初始工作数据
-> 按节点权限裁剪审批详情
-> 版本化修改工作数据
-> 记录动作证据
-> 终态冻结结果事实
```

Hunyuan 内置通用表单也必须通过同一契约生成审批对象，不能形成一套内部表单捷径和一套外部业务协议。

## 4. 数据变化策略

- `LOCKED`：审批中禁止业务对象修改。
- `VERSIONED`：允许新版本，展示差异并通知后续处理人。
- `RESTART_REQUIRED`：关键事实变化后终止当前审批并重新发起。
- `FIELD_CONTROLLED`：只允许白名单字段变化。

业务对象快照不可被后续实时数据覆盖。来源系统不可用时仍展示提交快照，实时查询只作为补充。

## 5. 失败闭环

- 来源系统或业务类型未登记、业务键重复、schema 不匹配、关键路由事实缺失时拒绝发起。
- 字段越权、敏感数据越界、非法类型和过期工作数据版本由服务端拒绝，前端隐藏不构成权限边界。
- 并发修改使用显式版本冲突，不允许后写覆盖先写。
- 业务变化不能静默修改审批依据，必须按契约锁定、展示差异或重新审批。
- 附件不可用、摘要生成失败和详情布局错误进入可诊断状态，不得让审批人面对空白任务。

## 6. 完成定义

1. 一个内置通用申请无需 BPM 专用业务代码即可登记契约、创建审批对象并展示完整详情。
2. 审批人能看到标题、摘要、字段、明细和附件，并按服务端权限裁剪。
3. 路由与候选只读取冻结、脱敏且允许使用的事实。
4. 工作数据的每次修改都有版本、操作者、原因和前后值证据。
5. 退回重提保持历史证据并产生新执行代或新数据版本。
6. 终态结果使用冻结的最终版本，来源系统故障不破坏审批历史。

M3 的旧仓库验收继续作为回归资产，但只有上述四类数据面和通用审批对象闭环通过后，新 M3 才能关闭。
