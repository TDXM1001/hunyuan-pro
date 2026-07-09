# BPM P2 收官活体验收记录

## 结论

P2 收官活体验收阻塞。

本轮已经完成环境前置检查和源级门禁复核：当前仓库干净，前端 `5788` 可达，后端 `1024` 可达，持久 Playwright MCP controller `8934` 可达，四个源级门禁均通过。

阻塞点在本地数据库业务数据：管理员登录成功后，`POST /bpm/definition/query` 使用 `definitionKey = sample_expense_apply` 查询返回 `total = 0`。因此样板费用申请无法发起真实 BPM 实例，也无法继续证明审批、回调失败可见、手动重试恢复和业务状态回写。

## 环境

- 前端：`http://127.0.0.1:5788`
- 后端：`http://127.0.0.1:1024`
- Playwright MCP controller：`http://localhost:8934`
- 验收时间：`2026-07-09T14:28:57.2696122+08:00`
- 前置检查证据文件：`G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-p2-live-preflight-20260709-142117-compact.json`
- 源级门禁证据文件：`G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-p2-live-gates-20260709-142339.json`
- 活体阻塞证据文件：`G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-p2-live-acceptance-20260709-142856.json`

## 前置检查

| 检查项 | 结果 | 证据 |
| --- | --- | --- |
| 当前分支 | `main` | `git branch --show-current` |
| 工作树 | 干净 | `git status --short` 无输出 |
| 前端服务 | 可达 | `http://127.0.0.1:5788` 返回 HTTP 200 |
| 后端服务 | 可达 | `http://127.0.0.1:1024` 返回 HTTP 200，响应体为 Hunyuan 统一错误结构 |
| MCP controller | 可达 | `http://localhost:8934/health` 返回 HTTP 200 |
| 前端代理 | 符合预期 | `/api` 代理到 `http://localhost:1024` 并去掉 `/api` 前缀 |

## 源级门禁

| 门禁 | 命令 | 结果 |
| --- | --- | --- |
| BPM 模块门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test` | PASS；96 tests，0 failures，0 errors |
| 前端 BPM 合同测试 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom` | PASS；2 files，37 tests |
| 前端类型检查 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck` | PASS；`vue-tsc --noEmit --skipLibCheck` exited 0 |
| Flowable 边界门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test` | PASS；1 test，0 failures，0 errors |

## 活体链路证据

| 步骤 | 结果 |
| --- | --- |
| 管理员登录 | PASS；`admin` 登录成功，`employeeId = 1`，`administratorFlag = true`，token 存在 |
| 查询样板流程定义 | BLOCKED；`definitionKey = sample_expense_apply` 返回 `total = 0`、`listCount = 0` |
| 查询定义第一页 | PASS；当前库存在 1 条定义，定义编码为 `codex-20260706224012-model` |
| 创建样板费用申请 | 因定义缺失停止 |
| 注入回调失败标记 | 因定义缺失停止 |
| 发起样板 BPM 实例 | 因定义缺失停止 |
| 审批待办 | 因定义缺失停止 |
| 查询失败回调记录 | 因定义缺失停止 |
| 手动重试回调 | 因定义缺失停止 |
| 查询样板最终状态 | 因定义缺失停止 |

## 页面证据

页面业务证据未生成。

原因是活体链路在流程定义查询阶段已经停止，没有生成 `expenseId`、`instanceId` 或 `callbackRecordId`。在没有真实实例和回调记录的情况下，不能用空页面、旧数据或源级门禁冒充回调记录列表和实例详情可靠性区域的验收。

## 边界说明

- 本轮没有新增样板费用页面、菜单或路由。
- 本轮没有新增 MQ、事件总线、HTTP 回调平台或外部调度。
- 本轮没有提交 Playwright runtime 输出、截图、网络日志或浏览器 profile。
- Hunyuan BPM 对外合同仍未暴露 Flowable 原生对象。
- 仓库中 `v3.42.0.sql` 只包含 `t_bpm_sample_expense` 表结构；未发现 `sample_expense_apply` 流程定义数据初始化脚本。

## 非阻塞项

- 本机 Maven 配置 `F:\maven\apache-maven-3.9.11\conf\settings.xml` 存在 line 235 解析警告，但本轮 Maven 门禁均退出 0。
- `BpmFlowableCompatibilityTest` 编译阶段存在 Spring `MockBean` 过时提示，但该门禁退出 0。
- 后端服务由本轮按 README 在 `hunyuan-backend/hunyuan-admin` 通过 `mvn spring-boot:run` 启动，启动日志保存在 `G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-admin-start-20260709-141216.*.log`。

## 阻塞项

- `BLOCKED_DEFINITION_MISSING`：本地数据库当前没有 `sample_expense_apply` 流程定义。
- 解除阻塞后，需要重新执行样板费用申请的创建、失败注入、发起、审批、失败回调查询、手动重试、样板详情查询和可靠性页面检查。
