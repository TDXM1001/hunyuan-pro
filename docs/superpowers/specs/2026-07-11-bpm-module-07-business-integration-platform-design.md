# BPM 模块 M7：业务接入与流程开放平台设计

- 日期：2026-07-11
- 状态：模块设计基线
- 优先级：P1
- 前置条件：M3；事件接入依赖 M4

## 1. 结论

本模块把当前样板费用和业务回调机制升级为可复用的业务接入平台。业务模块通过登记的业务类型、版本化发起契约、稳定业务键、结果事件和受控页面注册接入 BPM；BPM 管理审批过程，业务模块继续拥有业务单据生命周期。

模块完成必须由两个不同业务域证明：保留样板费用，同时新增采购申请样板，验证同一接入契约能支持不同表单、路由、审批、回写和补偿语义。

## 2. 当前事实

- 已有 `BpmBusinessResultEvent` 和 handler 边界。
- 已有业务集成记录、命令记录、回调记录和人工重试。
- 样板费用能够发起流程并消费结果。
- 审批数据治理正在补充最终数据版本和最终快照。
- 业务接入仍以模块内样板和具体服务为主，尚未形成明确注册、版本和通用验收契约。

## 3. 参考借鉴与优化

| 参考能力 | 决策 | Hunyuan 优化 |
| --- | --- | --- |
| OA 请假样板 | `ADAPT` | 使用两个业务域验证，而不是只做演示 CRUD |
| 自定义表单创建/详情路径 | `ADAPT` | 使用页面注册 key，不保存任意路由字符串 |
| 任务/执行监听器接业务 | 部分 `ADAPT` | 业务消费稳定领域事件，不依赖 Flowable listener API |
| HTTP trigger | `ADAPT` | 归入 M4 登记连接器和可靠等待 |
| 流程变量直接驱动业务 | 不直接采用 | 使用版本化业务命令和结果事件 |

## 4. 业务类型注册

```text
BusinessProcessRegistration
  businessType
  version
  allowedDefinitionKeys
  startSchema
  resultSchema
  pageRegistrationKey nullable
  handlerKey
  enabledState
```

注册由代码或受控配置提供，发布时校验流程定义与业务类型兼容。普通设计者不能把任意 handler、类名或页面路径写入模型。

## 5. 发起契约

```text
StartBusinessProcessCommand
  requestId
  businessType
  businessId
  businessVersion
  definitionKey
  startEmployeeId
  formData
  attachments
```

约束：

- `(businessType, businessId)` 与活动实例关系明确。
- `requestId` 全局幂等；重试返回同一实例。
- 业务单据先建立可审批状态，再发起 BPM。
- BPM 发起失败时业务模块得到明确失败，不留下虚假审批中状态。
- 发起成功返回 Hunyuan instanceId 和冻结定义版本。

## 6. 结果和过程事件

### 最终结果

```text
BusinessProcessResultEvent
  eventId
  businessType/businessId
  instanceId/definitionId
  result
  finalFormDataVersion
  finalFormData
  completedAt
```

### 可选过程事件

```text
PROCESS_STARTED
NODE_ENTERED
DATA_CHANGED
PROCESS_RETURNED
PROCESS_CANCELLED
PROCESS_COMPLETED
```

业务模块只订阅登记事件。事件记录冻结载荷，重试不重新读取变化中的实例；消费者以 eventId 幂等。

## 7. 业务状态机边界

业务模块定义自己的状态，例如采购申请：

```text
DRAFT -> APPROVING -> APPROVED -> EXECUTING -> COMPLETED
                   -> REJECTED
        -> CANCELLED
```

BPM 只返回审批事实，不直接把业务单据推进到采购执行、付款或完成。业务 handler 校验当前业务版本和允许转换，冲突终态拒绝覆盖并进入补偿记录。

## 8. 页面注册

业务自定义创建/详情页使用：

```text
pageRegistrationKey -> 前端静态注册组件和路由构建器
```

后端只返回登记 key、业务 ID 和允许动作。前端注册表决定实际组件，未知 key 失败关闭到通用详情，不动态导入任意路径。

## 9. 附件与业务证据

- 发起附件通过 Hunyuan 文件引用进入实例。
- 业务附件和审批证据保留各自所有权与权限。
- 结果事件只传文件引用和元数据，不复制二进制。
- 删除业务单据不得删除仍受审计保留期约束的流程证据。

## 10. 样板业务

### 样板费用

继续证明：审批人修改核定金额、数据版本、最终回写、失败重试和冲突终态。

### 采购申请

新增证明：

- 申请人提交采购类型、金额、供应商和明细。
- M1 按金额/类型选择不同审批路径。
- M2 使用角色、部门主管和多人策略。
- M3 控制财务核定字段和审计。
- M4 可等待预算系统回调或采购窗口时间。
- 最终事件只把业务单据推进到 `APPROVED`，后续采购执行仍归业务模块。

## 11. 可靠性和安全

- 发起、事件、回调和补偿都有幂等键。
- 业务 handler 在独立事务内校验版本和终态。
- BPM 记录投递结果，不假装业务更新成功。
- 敏感字段按业务注册 schema 和事件权限过滤。
- 业务模块不能直接修改 BPM 任务、实例或 Flowable 表。
- BPM 不能绕过业务服务直接更新业务表。

## 12. 接入文档和测试契约

平台提供：

- 业务类型注册说明。
- 发起命令契约测试。
- 结果事件消费者契约测试。
- 幂等与冲突终态测试夹具。
- 页面注册示例。
- 真实验收记录模板。

每个新业务模块必须证明发起、审批中、通过、拒绝、退回重提、取消、重复事件和失败补偿。

## 13. 交付块

1. 业务类型注册和版本化发起契约。
2. 结果/过程事件及消费者幂等契约。
3. 页面注册和附件边界。
4. 样板费用迁移到统一接入契约。
5. 采购申请样板和跨模块真实验收。
6. 接入文档、测试工具、运营处置和基线回写。

## 14. 完成定义

只有统一注册、发起、事件、页面、附件、安全、可靠性、样板费用迁移、采购申请接入和两个业务域真实验收全部闭环后，M7 才完成。只有一个 callback handler 或一个示例接口不算开放平台。
