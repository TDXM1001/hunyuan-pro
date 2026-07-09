# BPM P2.4 业务样板闭环设计

## 结论

P2.4 采用 **最小费用申请样板 + P2.3 回调执行器验收闭环**。

本轮不继续扩展 BPM 底座能力，也不把费用申请建设成完整业务系统。目标只做一个足够真实、足够小的业务样板，证明 Hunyuan BPM 的业务接入契约已经能支撑：业务单据创建、发起流程、审批结果事件、回调执行器回写业务状态、失败可观测、手动重试可恢复。

一句话目标：用一张费用申请样板单据证明“业务从 BPM 发起到审批结果回写”是真闭环，不是只在 BPM 内部自测。

## 当前证据

- `BpmBusinessProcessApi.start` 已能用 `businessType/businessId/definitionKey` 发起业务流程，并通过命令记录保证 `START:{businessType}:{businessId}:{definitionKey}` 幂等。
- `BpmBusinessProcessApi.publishResultEvent` 已按 `eventId` 幂等写入 `t_bpm_callback_record`，状态为待处理。
- P2.3 已新增 `BpmBusinessCallbackHandler`、`BpmBusinessCallbackExecutor`、自动扫描、手动重试、人工补偿状态和前端可靠性展示。
- 当前审批完成路径只在 `BpmTaskService.completeTask` 中更新实例终态，还没有在真实审批结束时发布 `BpmBusinessResultEvent`；P2.4 必须补齐这一处最小发布点，不能只在测试里手动调用 `publishResultEvent`。
- 当前后端只有 `hunyuan-admin`、`hunyuan-base`、`hunyuan-bpm` 三个模块，没有现成费用或采购业务模块可以承载样板。
- P2 总设计明确要求样板不进入 BPM 底座核心表、不强制成为所有业务模块模板，只验证接入契约、幂等、回调、补偿和监控。

## 方案取舍

### 方案 A：BPM 内 sample expense 模块（采用）

在 `hunyuan-bpm` 内新增 `module/sampleexpense`，作为 BPM 验收样板模块。

优点：

- 与 `BpmBusinessProcessApi`、`BpmBusinessCallbackHandler`、回调执行器同模块，测试门禁集中。
- 不需要引入新后端模块或修改 `hunyuan-admin` 业务模块结构。
- 可以用包名、表名和文档明确标注为 sample，避免污染 BPM 核心模型。

代价：

- 费用申请不是 BPM 的真实核心领域，实施时必须保持 `sampleexpense` 边界清晰。

### 方案 B：放入 `hunyuan-admin/module/business`

优点是更像真实业务模块；缺点是 P2.4 会扩大成跨模块业务系统建设，涉及 admin controller、菜单、权限和前端完整页面，超过“业务样板闭环”的最小验收目标。

### 方案 C：只做测试 fake handler

优点是最小；缺点是不能证明业务状态表、业务 API、回调处理器和监控重试的真实闭环，不满足 P2.4 验收口径。

## 范围

### 本轮包含

- 新增 `t_bpm_sample_expense` 样板费用申请表。
- 新增 `sampleexpense` 后端边界，包含实体、DAO、表单、VO、service 和回调 handler。
- 提供最小管理端 API，用于创建样板单据、发起流程、查询详情、设置下一次回调失败。
- 样板通过 `BpmBusinessProcessApi.start` 发起流程，不直接调用 Flowable。
- 审批终态落库后，对有 `businessType/businessId` 的实例发布一次 `BpmBusinessResultEvent`，让真实审批路径进入回调记录。
- 样板通过 `BpmBusinessCallbackHandler` 接收审批结果回调并回写业务状态。
- 通过 `callback_fail_flag` 制造一次回调失败，证明失败记录、手动重试和恢复成功。
- 后端集成测试覆盖正常回写、失败注入、重试恢复和幂等。
- 前端合同测试只固定样板 API 与现有可靠性入口，不在本轮建设复杂业务页面或新增菜单。
- 验收记录保存到 `docs/superpowers/specs/`。

### 本轮不包含

- 不建设通用费用报销产品。
- 不新增采购、合同、资产等真实业务模块。
- 不新增 MQ、通用 HTTP 回调平台或外部任务调度。
- 不把样板字段合并进 BPM 实例、任务、回调核心表。
- 不让样板 controller、VO 或前端类型暴露 Flowable 原生对象。
- 不重做 P2.1/P2.2/P2.3 已完成的 trace、通知、回调执行器能力。
- 不建设独立事件总线或通用业务事件平台；本轮只补审批通过/拒绝终态的 BPM 结果事件发布。

## 业务模型

### 样板表

新增 SQL 增量 `数据库SQL脚本/mysql/sql-update-log/v3.42.0.sql`，创建 `t_bpm_sample_expense`。

核心字段：

- `expense_id bigint`：样板费用申请 ID。
- `title varchar(100)`：申请标题。
- `amount decimal(12,2)`：申请金额。
- `applicant_employee_id bigint`：申请人员工 ID。
- `approval_status int`：业务审批状态。
- `instance_id bigint`：关联 BPM 实例 ID。
- `callback_event_id varchar(100)`：最近一次回调事件 ID。
- `callback_fail_flag bit`：下一次回调是否故意失败。
- `approved_at datetime`：审批通过回写时间。
- `rejected_at datetime`：审批拒绝回写时间。
- `create_time datetime`、`update_time datetime`：审计时间。

状态语义：

| 值 | 名称 | 含义 |
| --- | --- | --- |
| `0` | 草稿 | 样板单据已创建，尚未发起流程 |
| `1` | 审批中 | 已通过 BPM 业务 API 发起流程 |
| `2` | 已通过 | BPM 审批通过后由回调 handler 写回 |
| `3` | 已拒绝 | BPM 审批拒绝后由回调 handler 写回 |

`callback_event_id` 用于样板侧幂等。handler 收到同一个 `eventId` 时，如果已经处理过，则直接返回成功摘要，不重复推进业务状态。

## 后端设计

### 审批终态结果事件发布

P2.4 先补齐真实审批路径的最小发布点。

发布规则：

- 在任务审批通过且实例没有剩余活动任务时，或任务审批拒绝导致实例结束时发布。
- 仅当实例存在 `businessType` 和 `businessId` 时发布，纯 BPM 内部流程不产生业务回调。
- `eventId` 使用稳定幂等键：`RESULT:{instanceId}:{resultState}`。
- `resultState` 沿用 `BpmInstanceResultStateEnum`：`1` 为审批通过，`2` 为审批拒绝。
- 发布事件时不要设置自定义 `payloadJson`，让 `t_bpm_callback_record.request_payload_json` 保存完整 `BpmBusinessResultEvent` 包络，便于业务 handler 解析 `resultState`。
- 该发布点只负责写入回调记录，不直接调用样板 service，也不同步等待业务回写成功。

测试必须证明事件来自 `BpmTaskService` 的审批完成路径，而不是测试代码直接调用 `BpmBusinessProcessApi.publishResultEvent`。

### 包结构

新增包：

- `com.hunyuan.sa.bpm.module.sampleexpense.dao`
- `com.hunyuan.sa.bpm.module.sampleexpense.domain.entity`
- `com.hunyuan.sa.bpm.module.sampleexpense.domain.form`
- `com.hunyuan.sa.bpm.module.sampleexpense.domain.vo`
- `com.hunyuan.sa.bpm.module.sampleexpense.service`

命名使用 `SampleExpense` 前缀，明确这是 BPM 验收样板，不是平台费用报销模块。

### Service

新增 `BpmSampleExpenseService`。

职责：

- `create(BpmSampleExpenseCreateForm form)`：创建草稿样板单据。
- `start(Long expenseId)`：构造 `BpmBusinessStartCommand` 并调用 `BpmBusinessProcessApi.start`。
- `detail(Long expenseId)`：查询样板单据详情。
- `markNextCallbackFailed(Long expenseId)`：设置 `callbackFailFlag = true`，用于验收失败路径。
- `handleCallback(BpmBusinessCallbackContext context)`：由 handler 调用，解析结果事件并回写样板状态。

发起流程的固定业务参数：

- `businessType = "sample_expense"`
- `businessId = expenseId`
- `definitionKey = "sample_expense_apply"`
- `title = 样板费用申请标题`
- `summary = 金额和申请人摘要`
- `formDataJson = {"expenseId":1001,"amount":1280.50}`

### Callback Handler

新增 `BpmSampleExpenseCallbackHandler implements BpmBusinessCallbackHandler`。

职责：

- `businessType()` 返回 `sample_expense`。
- `handle(context)` 委托 `BpmSampleExpenseService.handleCallback(context)`。
- handler 不直接写复杂业务逻辑，便于单测和后续替换真实业务模块。

回调处理规则：

1. 根据 `context.businessId` 查询样板单据。
2. 单据不存在，返回失败，失败原因写明样板单据不存在。
3. 从 `requestPayloadJson` 解析 `BpmBusinessResultEvent`；解析失败或缺少 `resultState` 时返回失败。
4. 如果 `callback_event_id` 已等于 `context.eventId`，返回成功摘要，不重复回写。
5. 如果样板单据已处于通过/拒绝终态：
   - 同结果状态的重复事件返回成功摘要，不更新时间字段。
   - 冲突结果状态返回失败，失败原因写明样板单据已终态且结果冲突。
6. 如果 `callbackFailFlag = true`，清空该标记并返回失败，模拟业务侧短暂不可用。
7. `resultState = 1` 时写为已通过并设置 `approvedAt`。
8. `resultState = 2` 时写为已拒绝并设置 `rejectedAt`。
9. 其他结果状态返回失败，失败原因写明未知审批结果。

### 管理端 API

新增 `AdminBpmSampleExpenseController`，路径使用 `/bpm/sample/expense/**`，权限沿用 BPM 查询/更新风格。

接口：

- `POST /bpm/sample/expense/create`
- `POST /bpm/sample/expense/start/{expenseId}`
- `GET /bpm/sample/expense/detail/{expenseId}`
- `POST /bpm/sample/expense/markNextCallbackFailed/{expenseId}`

这些接口只用于 P2.4 验收样板，不替代真实业务模块 API。

权限固定为：

- 查询详情：`bpm:integration:query`
- 创建样板、发起流程、设置失败注入：`bpm:integration:update`

本轮不新增样板菜单。验收入口采用“样板 API + 现有实例详情抽屉 + 回调记录列表”：通过 API 创建/发起/注入失败，通过既有实例 trace 和回调监控查看结果。

## 前端设计

前端本轮只做最小 API 接入，不建设复杂费用页面，也不新增样板菜单。

新增 API 文件：

- `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/sample-expense.ts`

导出：

- `createBpmSampleExpense`
- `startBpmSampleExpense`
- `getBpmSampleExpenseDetail`
- `markNextBpmSampleExpenseCallbackFailed`

`bpm-api.test.ts` 增加合同针脚，确保样板 API 绑定到后端路径。

可视化验收复用现有页面：

- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/integration/callback-record-list.vue`

本轮不新增 `expense-sample.vue`。如后续需要面向业务人员演示，再作为独立 UI 小卡推进。

## 数据流

### 正常闭环

1. 管理端创建样板费用申请，状态为草稿。
2. 调用样板发起接口。
3. 样板 service 调用 `BpmBusinessProcessApi.start`。
4. BPM 创建实例和命令记录。
5. 审批通过最后一个活动任务或审批拒绝导致实例结束。
6. 审批终态发布点发布 `BpmBusinessResultEvent`。
7. P2.3 回调记录进入待处理。
8. 回调执行器找到 `BpmSampleExpenseCallbackHandler`。
9. handler 将样板状态写为已通过或已拒绝。
10. 回调记录写为成功。
11. 管理员可在实例 trace 和回调记录列表看到成功回调。

### 失败与重试闭环

1. 发起前或审批结束前调用 `markNextCallbackFailed`。
2. 回调执行器首次调用 handler。
3. handler 清空 `callbackFailFlag` 并返回失败。
4. 回调记录写入失败原因、重试次数和下次重试时间。
5. 管理员在回调记录列表或实例 trace 看到失败。
6. 管理员手动重试。
7. handler 第二次处理成功，样板状态回写成功。
8. 回调记录进入成功状态。

## 错误处理

- 未创建样板单据时不能发起流程。
- 已发起流程的样板再次发起时返回已有 `instanceId`，不创建重复流程。
- 已通过或已拒绝的样板不允许再次发起。
- 回调失败不回滚 BPM 审批结果。
- 同一 `eventId` 重复回调不重复更新审批时间。
- 已终态样板收到同结果状态的不同 `eventId` 时不重复更新时间；收到冲突结果状态时返回失败并进入回调可靠性链路。
- 样板失败注入只失败一次，避免手动重试永远不能恢复。
- 样板 API 返回 `ResponseDTO`，保持现有 controller 风格。

## 测试策略

后端测试：

- `BpmBusinessResultEventPublishTest` 或扩展现有 `BpmRuntimeCommandServiceTest`
  - 审批通过最后一个活动任务后发布 `BpmBusinessResultEvent`。
  - 审批拒绝结束实例后发布 `BpmBusinessResultEvent`。
  - 没有 `businessType/businessId` 的实例不发布业务结果事件。
  - 发布事件的 `eventId` 为 `RESULT:{instanceId}:{resultState}`，且 `payloadJson` 为空以保留完整事件包络。
- `BpmSampleExpenseServiceTest`
  - 创建样板单据默认为草稿。
  - 发起流程调用 `BpmBusinessProcessApi.start`，写回 `instanceId` 和审批中状态。
  - 重复发起返回已有实例，不重复调用 start。
  - 回调通过时写为已通过并记录 `callbackEventId`。
  - 回调拒绝时写为已拒绝。
  - `callbackFailFlag` 为 true 时首次回调失败并清空标记。
  - 同一 `eventId` 重复回调保持幂等。
  - 已终态样板收到同结果状态重复事件不重复更新时间。
  - 已终态样板收到冲突结果状态时返回失败。
- `BpmSampleExpenseCallbackHandlerTest`
  - `businessType()` 返回 `sample_expense`。
  - handler 委托 service 并返回业务处理结果。
- 集成到现有 `BpmBusinessCallbackExecutorTest` 或新增样板执行器测试，证明真实 handler 可被 executor 按 `businessType` 找到并调用。
- 真实闭环测试必须准备 `definitionKey = sample_expense_apply` 的当前可发起流程定义，不能依赖本地数据库已经存在该定义。

前端测试：

- `bpm-api.test.ts` 固定样板 API 文件与 `/bpm/sample/expense/` 路径。
- `bpm-modules.test.ts` 继续固定实例详情抽屉和回调记录列表的可靠性区域，不新增样板页断言。

建议门禁：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmSampleExpenseServiceTest,BpmSampleExpenseCallbackHandlerTest,BpmBusinessCallbackExecutorTest,BpmBusinessProcessApiTest' test
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

- 样板费用申请可以通过 BPM 业务 API 发起实例。
- 审批通过或拒绝导致实例结束后，真实审批路径可以发布业务结果事件并进入 P2.3 回调执行器。
- 样板 handler 可以把通过或拒绝结果回写到样板业务状态。
- 制造一次回调失败后，失败记录能在回调监控或实例 trace 中看到。
- 手动重试后样板状态恢复为审批结果状态，回调记录进入成功。
- 样板代码不暴露 Flowable 原生对象，不污染 BPM 核心表和核心 VO。
- 验收记录明确列出后端、前端和 Flowable 边界门禁。

## 理解校验

关键假设：

- P2.4 的价值是证明业务接入契约，而不是建设一个完整费用管理系统。
- 当前仓库没有独立费用/采购模块，因此 sample 放在 `hunyuan-bpm/module/sampleexpense` 是最小污染路径。
- 失败注入用 `callbackFailFlag` 足够证明 P2.3 的失败、可观测和手动重试能力。
- 样板业务幂等以 `callbackEventId` 为准，BPM 回调记录幂等仍由 `eventId` 控制。

已验证依据：

- 当前仓库已有 `BpmBusinessProcessApi.start`、`publishResultEvent`、`BpmBusinessCallbackHandler` 和 `BpmBusinessCallbackExecutor`。
- P2.3 验收记录已证明回调状态、重试、补偿和前端可靠性区域可用。
- P2 总设计明确建议费用或采购申请作为样板，并要求样板只验证接入契约、幂等、回调、补偿和监控。

仍需实施时验证：

- 样板发起所需的 `sample_expense_apply` 流程定义是否通过测试数据构造，还是依赖本地已有定义。
- 前端浏览器验收通过现有实例详情抽屉和回调记录列表完成；样板创建与失败注入通过 API 完成。
- 真实浏览器验收是否需要启动本地前后端服务；若需要，按 AGENTS.md 使用持久 Playwright MCP 会话。

人工必须重点审阅：

- `sampleexpense` 放在 `hunyuan-bpm` 是否符合“样板不污染底座”的边界。
- `callbackFailFlag` 单次失败注入是否足够代表业务侧短暂不可用。
- P2.4 是否需要同时覆盖拒绝结果，还是先以通过结果作为主验收路径。
