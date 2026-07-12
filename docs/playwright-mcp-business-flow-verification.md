# Playwright MCP 业务流程验证运行手册

## 1. 用途

本手册记录 Hunyuan Pro 在当前 Windows 开发环境中进行可见浏览器业务流验证的运行方式。它只承载本机路径、端口、启动命令、会话复用和证据管理，不替代页面契约测试、类型检查或领域验收标准。

只有变更涉及真实路由、登录态、权限、前后端联动、浏览器交互或运行时状态时，才需要浏览器证明。

## 2. 固定环境

| 项目 | 地址或路径 |
| --- | --- |
| Playwright MCP checkout | `G:\code-mcp\playwright-mcp-temp` |
| MCP HTTP server | `http://localhost:8933/mcp` |
| 持久 controller | `http://127.0.0.1:8934` |
| Controller 健康检查 | `http://127.0.0.1:8934/health` |
| 前端服务 | `http://127.0.0.1:5788` |
| 后端服务 | `http://127.0.0.1:1024` |

Playwright 的浏览器缓存、npm 缓存、profile、session、截图、日志和临时输出必须保留在 `G:\code-mcp\playwright-mcp-temp\cache\` 或 `G:\code-mcp\playwright-mcp-temp\runtime\`，不得写入 Hunyuan Pro 仓库。

## 3. 默认检查顺序

1. 先检查 `http://127.0.0.1:8934/health`。返回 `ok: true` 时直接复用现有 controller 和浏览器会话。
2. Controller 不可用时，检查 `8933` 端口；MCP server 已运行则只启动 controller。
3. MCP server 也未运行时，先启动 `local-scripts\start-http.ps1`，确认 `8933` 可连接后再启动 controller。
4. 检查业务所需的前端 `5788` 和后端 `1024`。只依赖静态前端行为时，可按实际范围省略后端。
5. 先运行与改动相关的聚焦契约测试或类型检查，再使用浏览器证明真实业务流。

可使用以下 PowerShell 命令检查状态：

```powershell
Invoke-RestMethod -Uri 'http://127.0.0.1:8934/health'
Test-NetConnection -ComputerName '127.0.0.1' -Port 8933
Test-NetConnection -ComputerName '127.0.0.1' -Port 5788
Test-NetConnection -ComputerName '127.0.0.1' -Port 1024
```

对 `http://127.0.0.1:8933/mcp` 直接执行普通 GET 得到 `403` 属于预期现象，只能说明端点正在监听；实际调用必须使用 MCP 协议。

## 4. 启动持久环境

在 MCP server 未运行时启动 HTTP 服务：

```powershell
Start-Process `
  -FilePath 'powershell.exe' `
  -ArgumentList @(
    '-NoProfile',
    '-ExecutionPolicy', 'Bypass',
    '-File', 'G:\code-mcp\playwright-mcp-temp\local-scripts\start-http.ps1'
  ) `
  -WorkingDirectory 'G:\code-mcp\playwright-mcp-temp' `
  -WindowStyle Hidden
```

确认 `8933` 已可连接后启动持久 controller：

```powershell
Start-Process `
  -FilePath 'node.exe' `
  -ArgumentList @(
    'G:\code-mcp\playwright-mcp-temp\runtime\persistent-mcp-controller.cjs'
  ) `
  -WorkingDirectory 'G:\code-mcp\playwright-mcp-temp' `
  -WindowStyle Hidden
```

启动后再次访问 `http://127.0.0.1:8934/health`，确认 controller 已连接 `http://localhost:8933/mcp`。

`local-scripts\start-stdio.ps1` 只用于用户明确要求的一次性 stdio 检查，不作为可见业务流验证的默认方式。

## 5. 会话复用规则

- 可见业务流检查必须优先通过持久 controller 复用同一浏览器上下文、页面和登录态。
- 不为每个检查创建新的临时 MCP 客户端；新客户端不能自然继承旧页面引用和会话状态。
- 不编写在验证结束时调用 `client.close()` 的一次性脚本，除非用户明确要求抛弃该次浏览器会话。
- 已有浏览器占用 profile 时，不再启动第二个相同 profile 的实例；先复用 controller，避免出现 `Browser is already in use`。
- 同一交付块中的导航、登录、操作、网络检查和结果确认应在同一持久会话中连续完成。

## 6. 业务验收证据

浏览器验证应围绕已确认的业务线收集最小充分证据：

- 页面、路由和菜单真实可达。
- 登录态、角色和权限与预期一致。
- 关键操作在界面上可执行，并出现正确反馈。
- 关键请求的 URL、方法、状态码和响应语义正确。
- 需要持久化的结果能够从后端接口、列表、详情或状态投影中再次读取。
- 关键失败路径返回业务错误，而不是被 `403`、`404`、脚本异常或假数据掩盖。

验收记录只描述本次真实执行过的步骤和结果。历史截图、旧日志或旧会话可以作为背景，不能冒充本次通过证据。

## 7. 证据与清理

- 截图、网络日志、console 日志、保存的 session、浏览器 profile 和临时输出都是运行证据，默认不提交。
- 需要保留证据时，将它留在 Playwright MCP checkout 的 `runtime\` 下，并在验收记录中说明证据位置和有效边界。
- 某些结果文件虽以 `.json` 命名，内容仍可能是带 `### Result` 包装的日志；解析前先提取实际结果段。
- 清理时只删除明确属于 `cache\` 或 `runtime\` 的可再生成产物，不删除 `local-scripts\`、controller 脚本或其他源码。
- 文件被占用时，先定位持有该文件的 Playwright MCP 进程，只停止明确匹配的进程，不做宽泛的 Node 进程清理。

## 8. 完成条件

浏览器业务流验证完成时，应能够明确回答：

1. 验证了哪条业务线和哪些关键异常路径。
2. 使用了哪些前端、后端和身份前置条件。
3. 浏览器、网络和持久化结果分别提供了什么证据。
4. 哪些范围已通过，哪些范围因环境或数据条件未覆盖。
5. 运行证据位于何处，以及是否按默认规则保持未提交状态。
