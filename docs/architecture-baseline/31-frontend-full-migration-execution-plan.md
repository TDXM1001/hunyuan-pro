# 前端完整迁移执行与验收计划

## 1. 目标与当前基线

启动本计划时，F0、F1、F2 已关闭。26 个历史页面菜单中，部门、岗位、员工、角色、菜单、参数配置、数据字典和文件管理 8 个页面已通过稳定 `routeId` 装配；其余 18 个菜单需要迁移、转为外链或退役。

截至 2026-07-25，本计划中的 F3、F4 和前端侧 F6 已完成代码、开发库迁移、数据库实值、Codebase Memory、自动化门禁以及管理员和受限角色浏览器验收，交付证据见 [32-frontend-full-migration-delivery.md](32-frontend-full-migration-delivery.md)。当前状态为 `FRONTEND_FULL_MIGRATION_CLOSED`。

本计划的完成目标是：

1. `apps/hunyuan-system` 只保留启动、壳层、全局状态、守卫、请求能力和 feature 装配。
2. 所有完整平台任务的页面、客户端、类型和测试归属明确 feature。
3. 所有有效页面菜单使用稳定 `routeId`，不再存储或返回前端源码 `component`。
4. 删除应用业务页面扫描、历史 `component` 双读、模块桥接兼容和无消费者入口。
5. feature 到应用内部的反向依赖保持为 0。
6. Codebase Memory、自动化测试、类型检查、生产构建、Flyway、数据库实值和浏览器验收全部形成可复查证据。

完整后端 F5 模块归位不属于本计划；但菜单契约、Flyway 和运行时路由所需的最小后端变更属于前端完整迁移的必要配合。

## 2. 剩余菜单执行账本

| ID | 菜单 | owner | routeId / 决策 | 批次 |
| ---: | --- | --- | --- | --- |
| 143 | 登录登出记录 | platform-audit | `platform.audit.login-log` | F3 |
| 81 | 用户操作记录 | platform-audit | `platform.audit.operation-log` | F3 |
| 152 | 更新日志 | platform-audit | `RETIRE`，当前无前端页面且采用审计已确认无生产消费者 | F3 |
| 300 | 消息管理 | platform-notification | `platform.notification.message` | F3 |
| 306 | 短信模板 | platform-notification | `platform.notification.sms-template` | F3 |
| 307 | 发送日志 | platform-notification | `platform.notification.sms-send-log` | F3 |
| 250 | 安全基线设置 | platform-security | `platform.security.baseline-settings` | F3 |
| 214 | 登录失败锁定 | platform-security | `platform.security.login-failure` | F3 |
| 251 | 敏感数据脱敏 | platform-security | `platform.security.data-masking-validation` | F3 |
| 221 | 定时任务 | platform-runtime | `platform.runtime.job` | F4 |
| 130 | 单号管理 | platform-runtime | `platform.runtime.serial-number` | F4 |
| 133 | 缓存管理 | platform-runtime | `platform.runtime.cache` | F4 |
| 117 | Reload | platform-runtime | `platform.runtime.reload` | F4 |
| 206 | 心跳监控 | platform-runtime | `RETIRE`，无前端页面，后端只读能力不需要生产菜单 | F4 |
| 151 | 代码生成 | platform-devtools | `RETIRE`，当前前端页面缺失，不恢复未被采用的生产工具 | F4 |
| 85 | 组件演示 | platform-devtools | `RETIRE`，不得为悬空菜单新造生产页面 | F4 |
| 215 | 接口加解密 | platform-devtools | `platform.devtools.api-encrypt` | F4 |
| 234 | Swagger 文档 | platform-devtools | `EXTERNAL`，保留外链菜单，不注册本地页面组件 | F4 |

退役决定只处理菜单和授权入口，不删除仍被后端内部使用的能力。每个退役项必须通过源码消费者、角色菜单关系和运行时 URL 三方面复查。

## 3. 实施顺序

### F3：审计、通知和安全

- 建立 `platform-audit`、`platform-notification`、`platform-security`。
- 页面和客户端使用 feature 内部相对导入；请求客户端由应用薄入口通过 Vue injection 提供。
- App Kernel 注册 8 个保留页面的稳定路由。
- Flyway 回填 8 个 `route_id`，退役更新日志菜单及其角色授权。
- 应用内对应完整页面、API 和测试归零。

### F4：运行时和开发工具

- 建立 `platform-runtime`、`platform-devtools`。
- 迁移任务、单号、缓存、Reload 和接口加解密页面及客户端。
- Swagger 保持外链；心跳、代码生成、组件演示正式退役。
- Flyway 回填 5 个本地页面 `route_id`，处理 3 个退役菜单。

### F6：历史装配退役

- 数据库确认所有有效本地页面菜单均有唯一 `route_id`。
- 后端菜单 DTO 停止输出 `component`，菜单维护契约不再要求前端源码路径。
- `login-adapter.ts` 只允许按 App Kernel 的 `routeId` 解析本地页面；未知标识明确进入不可用页。
- 删除应用内业务 `views/support`、业务 API 和无消费者薄入口。
- 将递减守卫升级为零遗留守卫。

## 4. 每批验证门

每批必须同时满足：

1. Codebase Memory 使用新项目名重建，图谱与源码扫描结果一致。
2. feature 到 `apps/hunyuan-system` 的反向导入为 0。
3. 应用内本批完整页面和客户端为 0。
4. 单元测试、TypeScript、生产构建和 `git diff --check` 通过。
5. Flyway 成功，目标菜单 `route_id`、退役标志和角色授权实值正确。
6. 管理员和受限角色的浏览器结果记录完整。

浏览器验收由人工执行，因此代码交付可以先达到 `BROWSER_ACCEPTANCE_PENDING`，但在浏览器记录全部通过前不得报告前端完整迁移最终关闭。

## 5. 人工浏览器验收操作

### 5.1 环境准备

1. 使用迁移后的开发库启动后端，确认 Flyway 为计划中的最终版本。
2. 关闭变更前的 Vite 进程，重新启动 `@hunyuan/system` 开发服务。
3. 准备管理员和一个只拥有部分 F3/F4 菜单授权的受限角色。
4. 打开浏览器开发者工具，保留 Console 和 Network 记录。

### 5.2 管理员验收

逐一从菜单进入并刷新以下页面：登录日志、操作日志、消息管理、短信模板、短信发送日志、安全基线、登录失败、数据脱敏、定时任务、单号、缓存、Reload、接口加解密。每页至少验证查询；存在新增、编辑、删除、启停或详情动作时，执行一个可恢复样本。

同时验证：

- 直接粘贴页面 URL 能够打开，刷新后仍能恢复。
- Swagger 外链打开目标地址，不进入本地组件解析。
- 更新日志、心跳、代码生成和组件演示不再出现在菜单中，旧 URL 不得渲染历史页面。
- Console 无错误，业务 API 无 4xx/5xx，页面无白屏或无限重定向。

### 5.3 受限角色验收

1. 登录后只显示已授权的 F3/F4 菜单。
2. 已授权页面可查询，按钮能力与权限码一致。
3. 未授权页面通过直接 URL 访问时被拒绝，不能短暂渲染敏感内容。
4. 退出再登录后菜单、标签页和刷新恢复结果一致。

### 5.4 记录模板

| 角色 | routeId / URL | 菜单可见 | 页面渲染 | 查询/动作 | 刷新 | Console/Network | 结果 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 管理员 |  |  |  |  |  |  |  |
| 受限角色 |  |  |  |  |  |  |  |

出现失败时记录时间、角色、URL、请求响应和控制台信息；修复后必须重新验证该页面及同 owner 的相邻页面。

## 6. 最终完成定义

只有代码、图谱、测试、构建、数据库实值和上述浏览器记录均通过，且 26 个历史菜单全部有已执行的迁移、外链或退役结果，才将状态写为 `FRONTEND_FULL_MIGRATION_CLOSED`。浏览器尚未执行时，最高状态只能是 `BROWSER_ACCEPTANCE_PENDING`。
