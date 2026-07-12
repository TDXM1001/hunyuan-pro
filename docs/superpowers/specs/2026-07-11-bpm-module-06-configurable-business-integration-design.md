# BPM 模块 M6：配置化业务接入设计

- 日期：2026-07-11
- 重审日期：2026-07-12
- 状态：当前模块设计，待重建标准接入主路径
- 优先级：P2
- 总体蓝图：`docs/superpowers/specs/2026-07-11-bpm-enterprise-blueprint.md`
- 前置条件：M3 审批对象、M4 统一任务命令稳定；异步接入使用 M5 事件语义

## 1. 结论

M6 让内部模块和外部系统通过配置与固定协议接入审批，不需要为每个采购、请假或合同流程新增 BPM 专用 Service/handler。

M6 负责来源系统、M3 业务契约的登记发布入口、流程绑定、外部应用身份、员工映射、开放 API、嵌入组件、连接器目录、事件订阅和可靠交付。业务契约的字段、快照、权限与变化语义仍归 M3，M6 不建立第二套契约实体；流程拓扑、审批运行和业务主数据分别仍属于 M1、M4 和业务系统。

## 2. 控制面契约

```text
SourceSystem
  code / auth / signing / publicKey / endpointRegistry / networkPolicy / status

BusinessContractVersion
  businessType / keyRule / fieldSchema / detailLayout / changePolicy

ProcessBindingVersion
  businessType / organizationScope / scenario / conditions / priority
  definitionVersion / effectivePeriod

EventSubscriptionVersion
  eventType / registeredEndpoint / signature / retry / deadLetter / scopes

ExternalEmployeeMapping
  sourceSystem / externalUserId / hunyuanEmployeeId / status / validity
```

所有外部引用必须版本化并在发布时冻结。业务绑定只能使用 M3 冻结路由事实上的类型化条件或登记规则，禁止脚本和实时跨库查询；它必须且只能命中一条有效规则，无匹配和同优先级多匹配均失败关闭。

## 3. 标准协议

- 发起、查询、业务对象变更、取消。
- 待办查询、任务详情、允许动作、任务动作。
- 过程与结果事件：开始、任务创建、退回、取消、通过、拒绝。
- 投递状态、重试、死信和人工补偿查询。

外部应用身份只能证明调用系统，不能代替员工完成任务。人工任务动作必须同时通过应用授权、员工映射和 M4 任务授权。

## 4. 端到端业务线

```text
登记来源系统和应用身份
-> 发布业务契约、详情布局和流程绑定
-> 外部系统按固定协议发起
-> M3 生成审批对象
-> M4 创建并处理任务
-> Hunyuan / 嵌入组件 / 外部页面共享统一命令
-> 终态写 Outbox
-> M6 签名投递、重试、死信或补偿
```

流程定义、绑定和订阅都不能直接填写任意 URL。端点、网络策略和凭据由登记目录管理，流程只保存版本化引用。

## 5. 失败闭环

- 未授权应用、签名失败、员工未映射、越权 scope 和过期凭据立即拒绝并审计。
- 业务键重复、绑定冲突、字段映射不完整和引用失效在发起或发布前阻断。
- `requestId`、`eventId` 和业务键提供分层幂等；重试不得重复创建实例或改变业务终态。
- 接收端故障使用 Outbox、指数退避、死信和人工重放，不能回滚审批终态。
- 外部副作用需要显式补偿契约；没有补偿能力时进入 M7 人工处置。

## 6. 完成定义

1. 一个内置通用申请和一个外部采购模拟器使用同一审批对象、任务和事件契约。
2. 新增采购字段、详情布局、流程绑定和结果订阅只需配置，不修改 BPM 业务代码。
3. 同一任务可在 Hunyuan 与外部入口读取同一版本快照并执行同一允许动作。
4. 重复发起、重复动作和重复事件只形成一个有效实例、动作和业务终态。
5. 来源故障时审批快照仍可阅读，结果可重试、死信和人工补偿。
6. 外部 API 不暴露 Flowable ID、BPMN XML、内部表或旧作者模型结构。
