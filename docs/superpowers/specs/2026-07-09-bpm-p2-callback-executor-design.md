# BPM P2.3 回调执行器与重试闭环设计

## 结论

P2.3 采用 **最小 Hunyuan 原生回调执行器 + 现有回调记录扩展**。

本轮不做完整事件账本、不引入消息队列、不建立通用 HTTP 节点平台，也不提前做 P2.4 业务样板。目标只补齐一件事：`t_bpm_callback_record` 已经能记录业务结果事件，但还不能稳定执行、失败重试、到顶补偿。P2.3 要把这张记录表从“可查 + 手动计数”推进到“可执行 + 可解释 + 可恢复”。

一句话目标：流程审批结果产生后，平台能把业务回调真正执行出去；失败时能看到原因、下次重试时间、重试次数，到达上限后能人工补偿。

## 当前证据

- `BpmBusinessProcessApiImpl.publishResultEvent` 已按 `eventId` 幂等写入 `t_bpm_callback_record`，状态为 `0`，并保存 `requestPayloadJson`。
- `BpmBusinessCallbackService.retry` 当前只读取记录、跳过成功记录、递增 `retryCount` 和 `updateTime`，没有执行回调 payload，也没有写成功、失败、响应、下次重试或补偿状态。
- `BpmBusinessIntegrationRecordService` 已能按分页和实例 ID 查询回调记录，并映射 `callbackStatus`、`failureReason`、`retryCount`、`nextRetryAt`。
- 管理端回调记录列表已有查询和“失败后重试”按钮，但按钮当前只触发后端计数。
- 管理员实例 trace 已包含 `callbackRecords` 和 P2.2 的 `notificationRecords`，适合继续承载可靠性区域，不需要另起一套排障页面。
- `AdminApplication` 已启用 `@EnableScheduling`，项目中已有 `@Scheduled` 使用点；P2.3 可以采用应用内定时扫描，不需要新增队列依赖。

## 范围

### 本轮包含

- 定义 BPM 回调状态常量或枚举，统一后端和前端语义。
- 新增回调处理器接口，让业务模块以后按 `businessType` 注册自己的回调处理逻辑。
- 新增统一回调执行器，负责读取记录、调用处理器、更新成功或失败结果。
- 自动扫描待处理和到期重试记录。
- 手动重试复用同一执行器。
- 达到最大重试次数后进入人工补偿状态。
- 管理端回调列表和实例可靠性区域展示下一次重试、补偿状态和必要操作。
- 用单元测试覆盖成功、失败、到期扫描、手动重试、补偿状态和幂等边界。

### 本轮不包含

- 不实现完整 BPM 事件流水表。
- 不实现通用 HTTP 回调节点平台。
- 不新增 MQ、Job 平台或第三方调度依赖。
- 不把业务样板提前塞入 BPM 底座；P2.4 再用样板业务证明真实回写。
- 不暴露 Flowable 原生对象给 controller、VO 或前端类型。
- 不重构命令记录、通知记录或实例 trace 的既有边界。

## 状态模型

沿用 `t_bpm_callback_record.callback_status`，扩展语义：

| 状态值 | 名称 | 含义 | 可操作 |
| --- | --- | --- | --- |
| `0` | 待处理 | 结果事件已记录，尚未执行或等待自动扫描 | 自动执行、手动执行 |
| `1` | 成功 | 业务处理器已确认处理成功 | 不可重试 |
| `2` | 失败 | 本次执行失败，但仍可重试 | 自动到期重试、手动重试 |
| `3` | 需人工补偿 | 已达到最大重试次数，需要管理员或业务人员线下处理 | 标记已补偿 |
| `4` | 已补偿 | 管理员确认业务侧已完成补偿处理 | 不可自动重试 |

失败记录的字段规则：

- `retryCount` 表示已经失败的执行次数；成功不递增。
- `failureReason` 保存最后一次失败原因，最多 1000 字符。
- `responsePayloadJson` 保存成功或失败的简短响应摘要，不保存大对象。
- `nextRetryAt` 在可重试失败时写入下一次自动重试时间；进入需补偿或成功后清空。
- `updateTime` 每次状态变化都更新。

补偿信息本轮优先复用状态和失败原因表达，不新增复杂补偿表。若实施时确认需要审计操作者和补偿说明，则通过小 SQL 增量补充 `compensated_at`、`compensated_by`、`compensation_reason` 三个字段；不引入独立补偿流水。

## 后端设计

### 回调处理器接口

新增 `BpmBusinessCallbackHandler`，由业务模块按 `businessType` 实现。

接口职责：

- 判断自己支持的 `businessType`。
- 接收 Hunyuan BPM 回调上下文。
- 业务侧自行保证幂等：同一 `eventId` 重复处理不得重复推进业务状态。
- 返回简短处理结果，供 BPM 写入 `responsePayloadJson`。

P2.3 的 BPM 底座只定义接口和注册/查找机制，不绑定具体业务样板。没有找到处理器时视为失败，写入明确原因，进入重试或补偿。

### 回调执行器

新增 `BpmBusinessCallbackExecutor`，作为唯一执行入口。

执行入口：

- `execute(Long callbackRecordId, TriggerType triggerType)`：手动重试和测试使用。
- `executeDueRecords(LocalDateTime now, int batchSize)`：定时扫描使用。

执行流程：

1. 查询回调记录。
2. 若记录不存在，返回失败结果。
3. 若状态为成功或已补偿，直接跳过。
4. 若状态为需人工补偿，自动扫描跳过，手动入口也不重新执行。
5. 根据 `businessType` 查找处理器。
6. 调用处理器。
7. 成功：状态置为成功，写入响应摘要，清空失败原因和下次重试时间。
8. 失败：记录失败原因，递增失败次数，计算下次重试时间。
9. 达到最大重试次数：状态置为需人工补偿，清空下次重试时间。

`BpmBusinessCallbackService.retry` 改为调用 executor，不再直接递增 `retryCount`。

### 自动扫描

新增一个 BPM 内部 scheduled service。

扫描条件：

- `callbackStatus = 0`
- 或 `callbackStatus = 2 AND nextRetryAt <= now`

扫描限制：

- 单次最多处理固定批量，默认 50。
- 按 `callbackRecordId` 升序处理，避免长期饥饿。
- 只扫描待处理和到期失败记录，不扫描成功、需补偿、已补偿。
- 扫描异常不能中断整个应用启动；单条异常必须落到该回调记录。

重试退避策略：

- P2.3 使用固定阶梯退避：第 1 次失败后 1 分钟，第 2 次 5 分钟，第 3 次 15 分钟。
- 最大失败次数为 3。
- 达到第 3 次仍失败时进入需人工补偿。

该策略足够支持 P2.3 运维闭环，后续如果真实业务需要不同策略，再通过配置扩展。

## 前端设计

### 回调记录列表

继续使用现有 `callback-record-list.vue` 的列表页结构，遵守当前 list/table 标准，不新增解释性标题。

增强点：

- 状态下拉增加“需人工补偿”“已补偿”。
- 状态 tag 增加补偿状态的视觉区分。
- 表格增加 `nextRetryAt` 列，便于管理员看到自动重试时间。
- “重试”按钮只对 `失败` 状态显示。
- “标记补偿”按钮只对 `需人工补偿` 状态显示。
- 操作成功后刷新列表。

### 实例详情可靠性区域

现有实例详情抽屉已承载回调记录和通知记录。P2.3 只做可靠性信息补齐：

- 回调记录显示状态文案，而不是裸数字。
- 显示重试次数和下次重试时间。
- 对需补偿状态显示醒目的补偿文案。
- 员工运行端详情不展示平台级失败细节。

## API 设计

保留既有接口：

- `POST /bpm/integration/callback/query`
- `POST /bpm/integration/callback/retry/{callbackRecordId}`

新增接口：

- `POST /bpm/integration/callback/compensate/{callbackRecordId}`

补偿接口请求体包含：

- `reason`：人工补偿说明，必填，最多 500 字。

响应仍使用 `ResponseDTO<String>`，保持当前 admin controller 风格。

前端类型 `BpmCallbackRecordVO` 增强字段：

- `nextRetryAt`
- 如实施时新增补偿字段，则增加 `compensatedAt`、`compensatedBy`、`compensationReason`。

## 数据库设计

优先不改表结构也能完成最小闭环：状态、失败原因、重试次数、下次重试时间、响应摘要均已存在。

如果实施阶段决定保留人工补偿审计，则新增 SQL 增量 `v3.41.0.sql`：

- `compensated_at datetime NULL COMMENT '人工补偿时间'`
- `compensated_by bigint NULL COMMENT '人工补偿操作人ID'`
- `compensation_reason varchar(500) NULL COMMENT '人工补偿说明'`

不新增独立补偿表，不修改 `event_id` 幂等唯一键。

## 错误处理

- 业务处理器抛异常时，executor 捕获异常并写入失败记录。
- 找不到处理器时，按失败处理，失败原因写明 `未找到业务回调处理器`。
- 已成功记录重复触发手动重试时直接返回成功，不重复调用处理器。
- 自动扫描只处理到期记录，不能提前执行未来重试。
- 补偿接口只能处理需人工补偿状态；其他状态返回业务错误。
- 回调失败不能回滚已经完成的 BPM 审批动作。

## 测试策略

后端聚焦测试：

- 回调成功：处理器被调用，状态更新为成功，响应摘要写入，失败原因和下次重试清空。
- 回调失败：失败原因写入，`retryCount` 递增，`nextRetryAt` 设置。
- 最大失败次数：第 3 次失败后进入需人工补偿。
- 手动重试：`BpmBusinessCallbackService.retry` 复用 executor。
- 自动扫描：只处理待处理和到期失败记录，不处理未来重试或终态记录。
- 已成功幂等：不重复调用处理器。
- 补偿接口：需补偿记录可标记已补偿，其他状态拒绝。

前端合同测试：

- API 导出包含重试和补偿接口。
- 回调记录页面识别状态 `3/4`。
- 失败状态显示重试入口，需补偿状态显示补偿入口。
- 实例详情可靠性区域显示回调状态文案和下次重试时间。

建议门禁：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmBusinessCallbackServiceTest,BpmBusinessCallbackExecutorTest,BpmBusinessCallbackSchedulerTest,BpmBusinessIntegrationRecordServiceTest,BpmInstanceTraceServiceTest' test
```

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test
```

## 完成定义

- 业务结果事件生成的回调记录可以被统一 executor 执行。
- 手动重试和自动重试走同一套执行逻辑。
- 成功记录能标记成功并保存响应摘要。
- 失败记录能保存失败原因、递增失败次数、计算下次重试时间。
- 到达最大失败次数后进入需人工补偿状态。
- 管理员能在回调记录列表和实例可靠性区域看见重试/补偿信息。
- 重复 `eventId` 不产生重复记录，已成功记录不重复推进业务处理。
- 所有公开 API 和前端类型保持 Hunyuan BPM 边界，不暴露 Flowable 原生对象。

## 理解校验

关键假设：

- P2.3 的核心缺口是执行闭环，而不是查询能力或完整事件账本。
- 应用内定时扫描对当前阶段足够，不需要引入 MQ。
- 业务幂等应由业务处理器负责，BPM 底座负责记录、调度和可恢复。
- 现有回调记录字段已经覆盖最小闭环，补偿审计字段可在实施时小幅补充。

已验证依据：

- 当前回调记录创建、查询、实例 trace 和前端列表都已存在。
- 当前手动 retry 只递增次数，未执行业务回调。
- 当前 SQL 已有 `callback_status`、`failure_reason`、`retry_count`、`next_retry_at`、`response_payload_json`。
- 当前项目已启用 Spring scheduling。

仍需实施时验证：

- 是否新增补偿审计字段，取决于当前用户上下文获取方式和前端补偿说明是否必须落库。
- 处理器 registry 的具体注入方式需结合 Spring bean 扫描和测试便利性确定。
- 自动扫描批量更新是否需要并发锁，本轮默认单实例/低并发实现；多实例部署可后续用数据库条件更新增强。

人工必须重点审阅：

- 状态 `3/4` 是否符合运维习惯。
- 最大失败次数 3 次和 1/5/15 分钟退避是否适合当前部署。
- “找不到处理器”是否应直接需补偿，还是允许按失败自动重试。
- P2.4 业务样板是否需要为处理器接口提供第一个真实实现。
