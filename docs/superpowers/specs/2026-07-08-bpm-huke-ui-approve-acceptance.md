# BPM huke UI 审批通过验收记录

## 基本信息

- 验收日期：2026-07-08
- 验收对象：huke 账号在 BPM 员工运行端的纯 UI 审批通过链路
- 验收结论：通过
- 前端地址：`http://localhost:5788/`
- MCP 方式：长连接 Playwright MCP controller `http://localhost:8934`

## 背景

前一轮 BPM runtime 验收中，`huke` 侧“审批通过”在自动化下曾被标记为前端交互/自动化稳定性遗留问题。当时后端 `/app/bpm/task/approve` 已证明可用，但 huke 侧 UI 无法稳定完成“打开弹框 -> 填写意见 -> 点击确定 -> 命中 approve 接口”的完整链路。

本轮先补齐 huke 的 BPM 员工运行端菜单权限，再使用长连接 Playwright MCP 复测纯 UI 链路。

## 权限补齐

已通过增量 SQL 补齐 huke 可访问的 BPM 员工运行端权限：

- SQL 文件：`数据库SQL脚本/mysql/sql-update-log/v3.36.0.sql`
- 新增角色：`BPM运行端用户`
- 角色编码：`bpm_runtime_user`
- 绑定账号：`huke`
- 授权菜单：
  - `308`：流程引擎
  - `316`：可发起流程
  - `317`：我的申请
  - `318`：我的待办
  - `319`：我的已办

## 验收步骤

1. 使用 huke / 123456 登录前端。
2. 确认登录菜单包含 BPM runtime 路由。
3. 进入 `/system/bpm/runtime/my-todo-list`。
4. 在“我的待办”中定位实例 `DK20260707NO01191`。
5. 点击“通过”。
6. 在“审批通过”弹框中填写审批意见。
7. 点击“确定”。
8. 等待并捕获 `/api/app/bpm/task/approve` 网络请求。
9. 查询实例详情确认运行结果。

## 通过证据

本轮证据文件：

- `G:\code-mcp\playwright-mcp-temp\runtime\output\hunyuan-bpm-huke-approve-existing-http-2026-07-07T16-20-59-678Z.json`（Playwright MCP 结果日志，`### Result` 段包含本轮 JSON 证据）
- `G:\code-mcp\playwright-mcp-temp\runtime\output\huke-http-before-2026-07-07T16-20-50-916Z.png`
- `G:\code-mcp\playwright-mcp-temp\runtime\output\huke-http-prompt-2026-07-07T16-20-50-916Z.png`
- `G:\code-mcp\playwright-mcp-temp\runtime\output\huke-http-after-2026-07-07T16-20-50-916Z.png`

关键结果：

- huke 登录菜单包含：
  - `system/bpm/runtime/startable-list`
  - `system/bpm/runtime/my-instance-list`
  - `system/bpm/runtime/my-todo-list`
  - `system/bpm/runtime/my-done-list`
- UI 页面成功打开“审批通过”弹框。
- 真实网络请求命中：
  - `POST /api/app/bpm/task/approve`
  - HTTP 状态：`200`
  - 请求任务：`taskId=51`
- 实例详情：
  - `instanceId=48`
  - `instanceNo=DK20260707NO01191`
  - `runState=3`
  - `resultState=1`
  - `actionTypes=["TRANSFERRED","APPROVED"]`

## 结论

`huke` 侧“审批通过”纯 UI 链路已经闭环通过。

前一轮记录中的“huke approve UI 自动化不稳定”不再是当前 BPM runtime 的未完成测试项。后续若再次出现该问题，应优先检查两类边界：

- huke 是否拥有 BPM 员工运行端菜单权限。
- Playwright 测试是否复用长连接 MCP controller，而不是使用会关闭浏览器上下文的一次性脚本。
