# BPM P1 菜单权限运行验收记录

## 验收范围

- P1 代码已经合入 `main`。
- 表结构 SQL 已执行：
  - `数据库SQL脚本/mysql/sql-update-log/v3.38.0.sql`
- 菜单和权限 SQL 已执行：
  - `数据库SQL脚本/mysql/sql-update-log/v3.39.0.sql`
- SQL 执行后，后端和前端都已经重启。
- 新增集成监控页面已经接入后台菜单和权限体系：
  - `流程引擎 / 集成监控 / 回调记录列表`
  - `流程引擎 / 集成监控 / 命令记录列表`
- 新增集成权限在管理员运行会话中可用：
  - `bpm:integration:query`
  - `bpm:integration:update`

## 运行环境

- 仓库：`E:/my-project/hunyuan-pro`
- 分支：`main`
- HEAD：`7035a93 feat: 接入BPM集成监控菜单权限`
- 前端地址：`http://127.0.0.1:5788`
- 后端地址：`http://127.0.0.1:1024`
- 浏览器账号：`admin / 123456`
- 验收方式：使用 Playwright MCP 访问已重启的本地前后端服务。

## 验收结果

- 命令：`Test-NetConnection 127.0.0.1 -Port 5788`
  - 结果：`TcpTestSucceeded = True`
- 命令：`Test-NetConnection 127.0.0.1 -Port 1024`
  - 结果：`TcpTestSucceeded = True`
- 浏览器访问：
  - 打开 `http://127.0.0.1:5788`
  - 使用 `admin / 123456` 登录成功
  - 登录后进入 `/system/home`
  - 展开 `流程引擎`
  - 确认可以看到 `集成监控`
  - 确认可以看到 `回调记录列表`
  - 确认可以看到 `命令记录列表`
- 回调记录列表：
  - 页面地址：`/system/bpm/integration/callback-record-list`
  - 页面标题：`回调记录列表 - Hunyuan System`
  - 网络请求：`POST /api/bpm/integration/callback/query`
  - 结果：HTTP `200`
  - 响应体：`code=0`，`ok=true`，`total=0`，`list=[]`
- 命令记录列表：
  - 页面地址：`/system/bpm/integration/command-record-list`
  - 页面标题：`命令记录列表 - Hunyuan System`
  - 网络请求：`POST /api/bpm/integration/command/query`
  - 结果：HTTP `200`
  - 响应体：`code=0`，`ok=true`，`total=0`，`list=[]`
- 重试接口权限和路由探针：
  - 请求：`POST /api/bpm/integration/callback/retry/-1`
  - 结果：HTTP `200`
  - 响应体：`code=30001`，`ok=false`，业务提示为 `回调记录不存在`
  - 解释：`retry` 接口可以访问，不是 `403` 或 `404`；这里的错误是不存在回调记录 ID 的预期业务结果。

## 已有合同检查

- 合入 `main` 后的前端 BPM 合同测试：
  - 命令：`pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom`
  - 结果：通过，2 个文件，36 个测试。
- 合入 `main` 后的后端 P1 定向测试：
  - 命令：`mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm "-Dtest=BpmDefinitionGovernanceServiceTest,BpmAdminInterventionServiceTest,BpmBusinessProcessApiTest,BpmBusinessIntegrationRecordServiceTest,BpmBusinessCallbackServiceTest,BpmTaskAdvancedActionServiceTest" test`
  - 结果：通过，20 个测试，0 个失败。

## 边界说明

- 本次运行验收没有修改业务代码。
- 验收期间唯一看到的脏工作区项是未跟踪的 `.tmp/`，本次没有处理它。
- 当前回调记录列表为空，没有真实失败回调行，所以无法从表格中点击 `重试` 按钮完成完整业务验收。
- 已使用回调 ID `-1` 安全探测 `retry` 接口；这可以证明菜单、权限、路由和接口可达，但不能替代真实失败回调数据下的按钮级业务验收。

## 结论

P1 的菜单、权限、前端路由、后端路由和管理员运行态访问已经在 `main` 上验收通过。

当前 P1 可以作为已合入、可运行的基线。唯一剩余的运行验收跟进项是：等出现真实失败回调记录后，再点击该行的 `重试` 按钮补一次完整业务闭环验收。
