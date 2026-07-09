# BPM P2 收官活体验收设计

## 结论

P2 收官采用 **源级门禁复核 + 最小活体业务证明 + 验收记录补齐**。

本轮不新增 BPM 功能、不新增样板菜单、不改造页面。目标只补齐 P2 总设计完成定义中的最后一类证据：至少一条真实前后端/浏览器业务验收记录，证明 P2.1 到 P2.4 的可靠性链路不只是单元测试和源级合同成立，也能在本地运行环境中被管理员看见、解释和恢复。

一句话目标：用现有样板费用申请和现有可靠性页面证明“业务发起、审批结果、回调失败可见、手动重试恢复”在活体系统里成立。

## 当前证据

- P2.1 已把实例 trace 接入管理员实例详情抽屉，并能展示回调记录。
- P2.2 已把通知记录接入管理员实例 trace 和前端可靠性区域。
- P2.3 已实现统一回调执行器、自动到期重试、手动重试、人工补偿状态和前端回调记录列表。
- P2.4 已新增 `sample_expense` 样板费用申请，样板通过 `BpmBusinessProcessApi.start` 发起流程，并通过 `BpmSampleExpenseCallbackHandler` 回写审批结果。
- P2.4 源级验收已经通过：
  - `hunyuan-bpm` 模块测试 96 个通过。
  - 前端 BPM 合同测试 37 个通过。
  - `@hunyuan/system` 类型检查通过。
  - `BpmFlowableCompatibilityTest` 通过。
- P2 总设计完成定义仍要求至少一条浏览器业务验收记录；P2.4 验收记录明确说明浏览器级创建/审批演示尚未执行。

## 方案取舍

### 方案 A：最小活体验收，不新增功能（采用）

通过现有 API 创建样板费用申请、发起流程、设置一次回调失败；通过现有审批路径让实例结束；通过现有回调记录列表和实例详情可靠性区域查看失败；再用现有“重试”入口恢复成功并查询样板详情确认业务状态回写。

优点：

- 正好补 P2 完成定义的证据缺口。
- 不扩大 P2，不把样板变成产品页面。
- 能验证真实 controller、权限、前端路由、可靠性 UI、回调执行器和样板 handler 的组合。

代价：

- 依赖本地服务、可登录账号、可发起的 `sample_expense_apply` 流程定义和可处理待办的审批人数据。

### 方案 B：新增样板费用页面再验收

优点是演示更顺手；缺点是把收官验收扩大成新页面开发，违背 P2.4 “不新增样板菜单或复杂业务页面”的边界。

### 方案 C：只补一份人工说明文档

优点是最快；缺点是没有真实活体证据，不能满足 P2 总设计中的浏览器业务验收要求。

## 范围

### 本轮包含

- 检查当前代码和数据库是否具备活体验收前置条件。
- 使用现有样板 API 完成创建、失败注入、发起流程和详情查询。
- 使用现有员工运行端或后端审批接口完成审批终态。
- 使用现有回调执行路径制造一次失败记录。
- 使用现有回调记录列表查看失败状态、失败原因、重试次数和操作入口。
- 使用现有“重试”入口或 API 触发同一执行器恢复成功。
- 使用样板详情查询确认 `approvalStatus`、`callbackEventId` 和审批时间字段回写。
- 使用管理员实例详情抽屉确认可靠性区域可见回调记录和通知记录。
- 形成 `docs/superpowers/specs/2026-07-09-bpm-p2-live-acceptance.md` 活体验收记录。

### 本轮不包含

- 不新增样板费用菜单、页面、表单或路由。
- 不新增 BPM 事件账本、通知列表页、消息队列或外部调度。
- 不新增测试依赖或浏览器运行依赖。
- 不提交 Playwright 截图、网络日志、浏览器 profile 或 runtime 输出。
- 不把 Yudao/RuoYi 的页面、接口或权限模型迁入 Hunyuan。
- 不把样板费用申请包装成生产报销模块。

## 活体验收前置条件

本轮实施前必须确认：

- 后端服务 `http://127.0.0.1:1024` 可用。
- 前端服务 `http://127.0.0.1:5788` 可用。
- 可登录管理员账号具备 `bpm:integration:query` 与 `bpm:integration:update` 权限。
- 可登录员工或可用后端调用路径能处理样板流程产生的待办。
- 数据库已应用 `v3.38.0.sql`、`v3.40.0.sql`、`v3.41.0.sql`、`v3.42.0.sql`。
- 存在可发起的流程定义 `sample_expense_apply`。
- 持久 Playwright MCP controller 优先使用 `http://localhost:8934`；如未运行，按 `AGENTS.md` 从 `G:\code-mcp\playwright-mcp-temp` 启动。

如果某个前置条件不满足，验收记录必须明确写为阻塞项，而不是伪造通过结论。

## 数据流

### 正常主线

1. 管理端调用 `POST /bpm/sample/expense/create` 创建样板费用申请，得到 `expenseId`。
2. 管理端调用 `POST /bpm/sample/expense/markNextCallbackFailed/{expenseId}`，让下一次业务回调只失败一次。
3. 管理端调用 `POST /bpm/sample/expense/start/{expenseId}` 发起 BPM 实例，得到 `instanceId`。
4. 审批人完成待办审批，使实例进入通过或拒绝终态。
5. `BpmTaskService` 发布 `BpmBusinessResultEvent`。
6. `BpmBusinessProcessApi.publishResultEvent` 生成 `t_bpm_callback_record` 待处理记录。
7. 回调执行器首次处理 `sample_expense`，样板 handler 因 `callbackFailFlag` 返回失败，并清空失败标记。
8. 管理员在回调记录列表或实例详情可靠性区域看到失败记录。
9. 管理员触发重试，同一执行器再次调用样板 handler。
10. 样板费用申请回写为已通过或已拒绝，回调记录进入成功状态。
11. 管理员查询样板详情和实例详情，确认业务状态、回调状态、通知记录和实例终态一致。

### 失败处理

- 如果样板发起接口返回流程定义不存在，停止验收并记录 `sample_expense_apply` 缺失。
- 如果待办审批无法通过 UI 完成，但后端接口可证明同一真实待办能审批，可以记录“UI 证据 + 后端接口收口”，并说明 UI 自动化不稳定边界。
- 如果回调自动执行时机不可控，可以使用现有回调记录列表或接口确认记录后，通过手动重试推进同一执行器。
- 如果回调记录没有生成，优先检查实例是否具备 `businessType = sample_expense` 和 `businessId = expenseId`，不得跳过业务结果事件链路。
- 如果浏览器会话不可用，记录为环境阻塞，不把源级门禁冒充活体验收。

## 证据格式

活体验收记录至少保存：

- 环境信息：前端地址、后端地址、Playwright MCP controller 地址、验收时间。
- 样板业务标识：`expenseId`、`instanceId`、`businessType`、`callbackRecordId`。
- API 证据：创建、失败注入、发起、详情查询、回调查询、重试调用的状态码或响应摘要。
- 页面证据：回调记录列表能看到失败/成功状态，实例详情可靠性区域能看到回调记录和通知记录。
- 最终状态：样板 `approvalStatus` 与实例 `resultState` 一致，回调记录为成功。
- 非阻塞项：例如本机 Maven warning、Spring `MockBean` 过时提示、Playwright 自动化确认弹窗不稳定等。
- 阻塞项：只有真实阻塞才写，且不能得出 P2 活体验收通过结论。

Runtime 证据文件、截图和日志必须留在 `G:\code-mcp\playwright-mcp-temp\cache` 或 `G:\code-mcp\playwright-mcp-temp\runtime`，默认不提交。

## 验证策略

源级回归仍作为活体验收前后的护栏：

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

浏览器验收使用持久 MCP 会话，优先复用已有登录态；如果需要脚本辅助，脚本与输出放在 `G:\code-mcp\playwright-mcp-temp\runtime`，不写入当前仓库。

## 完成定义

- 当前分支工作树干净或只包含本轮验收文档变更。
- 活体验收前置条件已逐项确认。
- 至少一条样板费用申请完成发起、审批、回调失败、手动重试、业务状态回写。
- 管理端回调记录列表能看到失败到成功的变化。
- 管理端实例详情可靠性区域能展示与该实例相关的回调记录和通知记录。
- 样板详情最终状态与 BPM 实例终态一致。
- P2 收官活体验收记录已写入 `docs/superpowers/specs/` 并包含真实证据。
- 源级门禁重新通过或明确记录未运行原因。

## 理解校验

关键假设：

- P2 当前缺的是活体证据，不是新功能。
- 样板费用申请只是验收载体，不是产品模块。
- 浏览器验收的价值在验证现有页面和接口组合，不在生成截图包。
- 如果本地服务或流程定义不满足条件，正确输出是“阻塞且原因明确”，不是扩大实现范围。

人工必须重点审阅：

- 这条活体验收是否足以作为 P2 收官证据。
- 是否接受“样板 API + 现有可靠性页面”的验收入口，而不是新增样板页面。
- 如果审批 UI 自动化不稳定，是否接受“UI 可见证据 + 后端同一待办接口收口”的证据组合。
