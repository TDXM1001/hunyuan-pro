# BPM P2 收官活体验收记录

## 结论

P2 收官活体验收通过。

本轮从第一次阻塞点继续推进：本地库原先缺少 `sample_expense_apply` 样板流程定义，后续通过 `POST /bpm/sample/expense/prepareDefinition` 准备定义；运行态又发现旧样板定义虽然可发起，但没有待办通知监听器，导致实例 trace 中 `notificationRecords = 0`。本轮已补齐样板定义 seed 的 `MESSAGE` 待办监听器，并通过 `prepareDefinition` 发布 `definitionId = 3`、`definitionVersion = 2` 的新版样板定义。

最终活体链路已经覆盖样板费用申请创建、回调失败注入、BPM 实例发起、待办审批、回调失败可见、手动重试恢复、业务状态回写，以及实例 trace 中回调记录、命令记录、动作日志和通知记录的可靠性数据。

## 环境

- 前端：`http://127.0.0.1:5788`
- 后端：`http://127.0.0.1:1024`
- 后端进程：PID `21620`
- 验收时间：`2026-07-09T16:34:12.6785051+08:00`
- 前置检查证据文件：`G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-p2-live-preflight-20260709-142117-compact.json`
- 源级门禁证据文件：`G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-p2-live-gates-20260709-142339.json`
- 最终活体验收证据文件：`G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-p2-live-acceptance-20260709-163412-compact.json`
- 后端启动日志：`G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-admin-jar-start-20260709-162438.out.log`

## 前置修复

| 问题 | 处理 | 结果 |
| --- | --- | --- |
| 直接从 `hunyuan-admin` 子目录运行会加载旧本地 Maven 依赖 | 改为从根工程打包当前分支 jar 后运行 `hunyuan-admin-dev-3.0.0.jar` | `1024` 加载当前分支代码 |
| 本地库缺 `t_bpm_notification_record` | 应用仓库已有 `数据库SQL脚本/mysql/sql-update-log/v3.40.0.sql` | `GET /bpm/instance/trace/{id}` 不再因缺表失败 |
| 旧样板定义没有 `MESSAGE` 待办监听器 | 样板定义 seed 的 `sample_approve` 节点补齐 `listeners:[{"channel":"MESSAGE"}]`，并在旧定义缺监听器时发布新版 | 新实例 trace 生成 `notificationRecords` |

## 源级门禁

| 门禁 | 命令 | 结果 |
| --- | --- | --- |
| BPM 通知/trace/seed 聚焦回归 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmSampleExpenseDefinitionSeedServiceTest,BpmNotificationRecordServiceTest,BpmNotificationListenerServiceTest,BpmTaskProjectionServiceTest,BpmInstanceTraceServiceTest' test` | PASS；18 tests，0 failures，0 errors |
| BPM 模块门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test` | PASS；101 tests，0 failures，0 errors |
| 前端 BPM 合同测试 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom` | PASS；2 files，37 tests |
| 前端类型检查 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck` | PASS；`vue-tsc --noEmit --skipLibCheck` exited 0 |
| Flowable 边界门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin '-Dtest=BpmFlowableCompatibilityTest' test` | PASS；1 test，0 failures，0 errors |

## 活体链路证据

| 步骤 | 结果 |
| --- | --- |
| 管理员登录 | PASS；`employeeId = 1`，`administratorFlag = true` |
| 准备样板定义 | PASS；`definitionId = 3` |
| 查询样板定义 | PASS；选中 `definitionId = 3`，`definitionVersion = 2`，`startState = 1`，定义总数 `2` |
| 创建样板费用申请 | PASS；`expenseId = 3`，标题 `P2活体验收-20260709-163412` |
| 注入回调失败标记 | PASS；`expenseId = 3` |
| 发起 BPM 实例 | PASS；`instanceId = 54` |
| 查询并审批待办 | PASS；`taskId = 57`，待办名为样板审批，审批人管理员 |
| 生成回调记录 | PASS；`callbackRecordId = 3`，审批后初始状态 `0` |
| 触发失败态 | PASS；手动重试触发一次模拟失败，`callbackStatus = 2`，`retryCount = 1`，失败原因为样板费用申请模拟回调失败 |
| 再次重试恢复 | PASS；同一 `callbackRecordId = 3` 最终 `callbackStatus = 1` |
| 查询样板详情 | PASS；`approvalStatus = 2`，`callbackEventId = RESULT:54:1`，失败标记已清空 |
| 查询实例 trace | PASS；`callbackRecords = 1`，`commandRecords = 1`，`notificationRecords = 1`，`actionLogs = 1` |

## 关键 ID

| 字段 | 值 |
| --- | --- |
| `definitionId` | `3` |
| `definitionVersion` | `2` |
| `expenseId` | `3` |
| `instanceId` | `54` |
| `taskId` | `57` |
| `callbackRecordId` | `3` |
| `notificationRecordIds` | `[2]` |

## 实例 trace 可靠性区域

最终 `GET /bpm/instance/trace/54` 返回的可靠性数据摘要：

| 数据 | 数量 / 状态 |
| --- | --- |
| 回调记录 | `traceCallbackCount = 1` |
| 命令记录 | `traceCommandCount = 1` |
| 通知记录 | `traceNotificationCount = 1` |
| 动作日志 | `traceActionLogCount = 1` |
| 通知渠道 | `MESSAGE` |
| 通知事件 | `TASK_CREATED` |
| 通知接收人 | `receiverEmployeeId = 1` |
| 通知发送状态 | `sendStatus = 1` |

这证明通知记录已经进入实例 trace 数据合同；前端可靠性区域已具备同一数据源，不需要新增样板费用页面或额外通知列表页来完成本次 P2 收口。

## 边界说明

- 本轮没有新增样板费用页面、菜单或路由。
- 本轮没有新增 MQ、事件总线、HTTP 回调平台或外部调度。
- 本轮没有提交 Playwright runtime 输出、截图、网络日志或浏览器 profile。
- Hunyuan BPM 对外合同仍未暴露 Flowable 原生对象。
- 本轮新增的 seed 监听器只服务样板验收流程，不改变通用 BPM 定义发布边界。

## 非阻塞项

- 本机 Maven 配置 `F:\maven\apache-maven-3.9.11\conf\settings.xml` 仍有 line 235 解析警告，但本轮 Maven 门禁退出 0。
- Spring `MockBean` 编译阶段存在过时提示，但相关门禁退出 0。
- 定义查询中历史 `definitionId = 2` 与新版 `definitionId = 3` 都显示 `lifecycleState = 1`，疑似发布服务历史版本降级逻辑仍需单独排查；当前活体验收使用 `definitionId = 3` 的新版定义，未阻塞本轮收口。

## 阻塞项

无。
