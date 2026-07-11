# BPM 模块 M4：时间、SLA 与事件驱动设计

- 日期：2026-07-11
- 状态：模块设计基线
- 优先级：P0
- 前置条件：M1 编排核心、M3 数据治理

## 1. 结论

本模块让 Hunyuan 流程从“只能由人工任务动作推进”升级为“可由时间、受控外部调用和相关事件推进”。它一次闭环任务 SLA、超时处理、延迟节点、出站连接器、外部回调等待、幂等恢复、异常处置和运行追踪。

参考项目的 timer、boundary event、HTTP request/callback 可以借鉴，但 Hunyuan 不允许设计者直接配置任意 URL、凭据或用户表达式。外部调用必须通过登记连接器，所有计时器和等待状态必须有 Hunyuan 结构化投影。

## 2. 当前事实

- 已有通知记录和投递状态。
- 已有业务命令、回调记录、失败重试和人工补偿。
- 已有任务动作日志和实例 trace。
- 尚无流程模型驱动的任务截止时间、提醒计划、自动终态、延迟节点和运行中外部等待。

这些基础意味着 Hunyuan 不需要复制参考项目的消息与 HTTP 工具，而应把已有可靠性能力提升为通用时间事件运行时。

## 3. 参考借鉴与优化

| 参考机制 | 决策 | Hunyuan 优化 |
| --- | --- | --- |
| 用户任务 timeout boundary event | `ADAPT` | Flowable 负责到期信号，Hunyuan 投影 SLA 和执行事实 |
| 自动提醒/同意/拒绝 | `ADAPT` | 自动终态需要高风险权限、发布警告和完整审计 |
| 固定时长/固定日期延迟 | `ADAPT` | 使用类型化时间表达，不接受任意 EL |
| HTTP request trigger | `ADAPT` | 登记连接器、凭据引用、出站白名单、重试策略 |
| HTTP callback receive task | `ADAPT` | Hunyuan 等待记录、相关键、签名、幂等和人工补偿 |
| 表单更新/删除触发器 | 部分 `REJECT` | 只允许版本化 patch；不提供任意删除动作 |

## 4. 模型契约

### 4.1 任务 SLA

```text
TaskSlaPolicy
  dueAfter
  reminderSchedule[]
  timeoutAction: NONE | REMIND_ONLY | AUTO_APPROVE | AUTO_REJECT | ASSIGN_ADMIN
  businessCalendarKey nullable
```

时间使用 ISO-8601 duration 或登记业务日历，不允许自由 cron/EL。自动审批和拒绝必须配置系统动作意见和风险级别。

### 4.2 延迟节点

```text
DelayNode
  mode: DURATION | FIXED_DATETIME | FORM_DATETIME
  value
  timezone
  overduePolicy
```

表单日期来自冻结 schema，运行时解析失败时进入异常状态，不自动跳过。

### 4.3 外部触发节点

```text
ExternalTriggerNode
  connectorKey
  operationKey
  requestMapping
  responseMapping
  waitMode: NO_WAIT | WAIT_CALLBACK
  timeoutPolicy
```

模型只保存登记键和字段映射，不保存真实密钥。连接器版本随定义冻结。

### 4.4 登记事件监听

流程、节点和任务允许绑定登记的事件订阅：

```text
ListenerBinding
  listenerKey/version
  eventType
  deliveryMode: IN_TRANSACTION | AFTER_COMMIT
  failurePolicy
  parameterMapping
```

普通设计者只能从启用目录选择 listenerKey。Hunyuan 不允许模型保存 Java class、delegateExpression、Spring EL 或任意回调 URL。事务内监听只用于必须与运行事实原子一致的内部投影；业务集成默认使用提交后可靠事件。

## 5. 运行投影

### 5.1 SLA 与计时器

新增 Hunyuan 计时器/SLA 事实，至少记录：实例、任务或节点、策略快照、计划时间、实际触发时间、状态、触发次数、最后错误、Flowable timer job 定位信息和业务幂等键。

状态：

```text
SCHEDULED -> TRIGGERED -> SUCCEEDED
                     -> FAILED_RETRYABLE -> SUCCEEDED
                     -> FAILED_MANUAL
SCHEDULED -> CANCELLED
```

### 5.2 外部等待

新增外部等待记录：

```text
WAITING -> RESUMED
        -> TIMED_OUT
        -> CANCELLED
        -> FAILED_MANUAL
```

唯一相关键由 `tenant/business + instance + node + attempt` 组成，对外使用不可猜测 token 或签名相关键。回调原始载荷按敏感规则脱敏或加密保存。

## 6. 执行架构

```text
Flowable timer/event 到达
-> 进入 Hunyuan RuntimeCommandCoordinator
-> 加载并锁定实例/任务/等待记录
-> 验证状态和幂等键
-> 执行提醒、系统动作或恢复命令
-> 写时间/事件事实和动作日志
-> 推进 Flowable
-> 更新任务、实例和 trace 投影
```

业务线程不得直接调用 Flowable 完成等待任务；所有推进必须经过命令协调器。

## 7. 连接器治理

### 7.1 登记模型

```text
ConnectorDefinition
  connectorKey/version
  baseEndpointRef
  credentialRef
  allowedOperations
  timeout/retry/circuitPolicy
  request/response schemas
  enabledState
```

### 7.2 安全边界

- 只允许登记域名和协议。
- 阻止回环、链路本地和内部元数据地址。
- 凭据只通过安全引用加载，不进入模型和日志。
- 请求字段映射使用白名单 schema。
- 响应大小、类型和超时受限。
- 自动重试只用于登记为幂等的 operation。
- 管理页面按权限查看脱敏请求、响应和错误。

## 8. 自动终态风险控制

自动通过和自动拒绝不是普通提醒选项：

- 需要独立配置权限。
- 发布检查显示阻断级或高风险警告。
- 禁止用于法规、财务或高风险分类，除非分类策略明确允许。
- 系统动作使用独立 actor 类型和固定意见。
- 与人工动作并发时，固定锁顺序和先到终态胜出。
- 所有自动终态进入动作日志、通知和管理报表。

## 9. 前端与运营

- 设计器节点属性配置 SLA、延迟、连接器和超时策略。
- 发布检查可模拟计划时间、连接器可用性和字段映射。
- 任务显示截止时间、剩余时间、逾期状态和提醒次数。
- 实例详情显示延迟原因、外部等待、相关事件和下一次重试。
- 管理端提供失败计时器、外部等待和连接器调用处置列表。

## 10. 兼容与故障处理

- 旧定义没有 SLA/事件节点时行为不变。
- 模型停用连接器不影响已发布快照，但运行时可由安全开关紧急禁用。
- 服务重启后依靠 Flowable job 和 Hunyuan 记录恢复，不依赖内存定时器。
- 重复 timer/event/callback 只产生一个有效动作。
- 超时和人工动作并发不得形成双终态。
- 连接器永久失败不应丢失流程，应进入可人工重试或取消的等待状态。

## 11. 交付块

1. SLA 模型、计时器投影和提醒闭环。
2. 自动终态、并发和风险控制。
3. 延迟节点编译、运行和取消。
4. 登记连接器和 NO_WAIT 调用。
5. WAIT_CALLBACK、超时、签名、幂等恢复和人工补偿。
6. 登记事件监听、运营页面、报表、真实外部系统模拟和基线回写。

## 12. 验收矩阵

覆盖正常提醒、重复提醒、自动通过/拒绝、人工动作竞态、延迟到期、延迟取消、出站成功、超时重试、回调恢复、重复回调、伪造签名、实例取消、服务重启恢复、敏感信息脱敏和旧定义兼容。

## 13. 完成定义

只有任务 SLA、延迟、出站连接器、外部等待、超时、幂等、异常处置、结构化 trace、管理页面和真实事件驱动业务流全部闭环后，M4 才完成。单独做催办页面或定时任务不算完成。
