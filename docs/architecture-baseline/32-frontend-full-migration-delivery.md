# 前端完整迁移交付记录

## 1. 结论与范围

截至 2026-07-25，F3、F4 和前端侧 F6 的代码实现、稳定菜单契约、开发库迁移、Codebase Memory 图谱和自动化门禁均已完成。

当前状态：`FRONTEND_FULL_MIGRATION_CLOSED`。

本记录的分母是 [31-frontend-full-migration-execution-plan.md](31-frontend-full-migration-execution-plan.md) 中的 26 个历史页面菜单，以及应用内完整平台页面、客户端、历史组件装配和 feature 依赖边界。完整后端 F5 模块归位不在本次前端迁移范围内；管理员与受限角色的浏览器验收均已执行并通过，因此本次前端完整迁移关闭，但不得把本记录解释为完整底座关闭。

## 2. 已执行结果

### 2.1 F3：审计、通知与安全

- 新增 platform-audit、platform-notification、platform-security feature。
- 迁移登录日志、操作日志、消息、短信模板、短信发送日志、安全基线、登录失败和数据脱敏页面、客户端、类型与测试。
- 应用仅保留请求客户端注入和 App Kernel 路由装配。
- `V3_81_0__f3_audit_notification_security_route_ids.sql` 回填 8 个稳定 `routeId`，退役更新日志菜单及其角色授权。

### 2.2 F4：运行时与开发工具

- 新增 platform-runtime、platform-devtools feature。
- 迁移任务、单号、缓存、Reload 和接口加解密页面、客户端、类型与测试。
- `V3_82_0__f4_runtime_devtools_route_ids.sql` 回填 5 个稳定 `routeId`。
- Swagger 继续使用 `frameFlag/frameUrl` 外链；心跳监控、代码生成和组件演示菜单及其角色授权已退役，未为悬空入口补造页面。

### 2.3 前端侧 F6：历史装配退役

- `login-adapter.ts` 只按 App Kernel 的稳定 `routeId` 解析本地页面，未知或缺失标识进入现有未找到页。
- 登录授权 DTO 停止输出前端源码 `component`，适配器停止赋值该字段。
- `V3_83_0__frontend_legacy_component_retirement.sql` 清空所有已迁移有效页面的历史 `component`。
- 删除无消费者模块桥接；新增 foundation-acceptance 最小 feature，证明公开协议接入不依赖应用内部业务实现。
- `apps/hunyuan-system/src/views/support` 已删除；`src/api/system` 仅保留 2 个架构边界测试，原先位于 support 的 2 个页面边界测试已归入 `src/architecture`，生产平台页面和客户端为 0。

## 3. 数据库直接证据

执行前开发库最新成功版本为 `3.80.0`，目标菜单仍保存历史 component，4 个待退役入口共有 7 条角色菜单关系。迁移前已备份：

`数据库SQL脚本/mysql/backups/hunyuan-before-frontend-full-migration-20260725-092240.sql`

使用已通过 reactor 构建的 dev jar，并显式设置 `HUNYUAN_FLYWAY_ENABLED=true` 启动后端。Flyway 在 2026-07-25 09:22:58 连续成功应用：

| 版本 | 脚本 | 结果 |
| --- | --- | --- |
| `3.81.0` | `V3_81_0__f3_audit_notification_security_route_ids.sql` | success |
| `3.82.0` | `V3_82_0__f4_runtime_devtools_route_ids.sql` | success |
| `3.83.0` | `V3_83_0__frontend_legacy_component_retirement.sql` | success |

迁移后只读核对结果：

- 21 个有效本地页面全部具有非空稳定 `routeId`，缺失数为 0。
- 具有稳定 `routeId` 的有效页面中，非空 `component` 数为 0。
- 菜单 85、151、152、206 均为已删除、已禁用、不可见；其自身及子菜单角色授权数为 0。
- Swagger 菜单 234 仍为有效外链，`frame_flag=1`，`frame_url` 和兼容 component 均保持目标 URL，不参与本地页面注册。
- 后端在 `http://localhost:1024/` 成功启动；该运行态只用于迁移和直接证据，不替代浏览器验收。

## 4. Codebase Memory 证据

使用项目 `E-my-project-hunyuan-pro-frontend-full-migration-20260725` 对最终工作区执行 full 索引并持久化产物：

- 节点：16,042。
- 关系：42,312。
- feature 到 `apps/hunyuan-system` 的反向 IMPORTS：0。
- `packages/features` 对旧 `#/api/system/` 的引用：0。
- 应用 `views/support` 已删除；应用 `api/system` 仅保留 2 个边界测试，原先位于 support 的 2 个页面边界测试已归入 `src/architecture`。
- 应用业务薄入口只负责请求能力注入，完整页面实现位于 feature；生产支持页面和客户端残留为 0。

## 5. 自动化门禁

| 门禁 | 结果 |
| --- | --- |
| `@hunyuan/system` TypeScript | 通过 |
| 全量 Vitest | 68 个文件、462 项通过 |
| `@hunyuan/system` production build | 通过，仅有既有 Rolldown 告警 |
| F 盘 Maven reactor package | `hunyuan-base`、`hunyuan-admin` 通过 |
| `AccessCapabilityQueryFacadeAdapterTest` | 4 项通过 |
| Flyway 开发库实际执行 | 3 条迁移成功，最新 `3.83.0` |
| `git diff --check` | 通过 |

Maven 使用 `F:\jdk17`、`F:\maven\apache-maven-3.9.11`、`settings2.xml` 和 `F:\maven\repository2`，未回退到用户目录仓库。`FlywayMigrationTest` 的环境条件跳过不作为迁移证据，本记录使用开发库中的 Flyway 历史和菜单实值作为直接证据。

## 6. 人工浏览器验收

人工验收的环境准备、管理员页面清单、受限角色权限检查、直达 URL、刷新恢复、Console/Network 检查和记录表，统一执行 [31-frontend-full-migration-execution-plan.md](31-frontend-full-migration-execution-plan.md) 第 5 节。

浏览器记录全部通过后，才可将本记录状态更新为 `FRONTEND_FULL_MIGRATION_CLOSED`。任一页面失败时，应记录角色、URL、时间、响应和控制台信息，修复后复验同 owner 的相邻页面。

### 6.1 Browser verification evidence (2026-07-25)

- Administrator session: all 21 valid local routeId pages opened directly without login redirect; page titles matched the target menu.
- Retired URLs `/support/change-log/change-log-list`, `/demonstration/index`, `/support/code-generator`, and `/support/heart-beat/heart-beat-list` all rendered the application 404 page and did not render historical business pages.
- Swagger external target opened successfully at `http://localhost:1024/swagger-ui/index.html` with title `Swagger UI`.
- No new browser Console errors were observed; only the existing StorageManager warning remained.
- The exhaustive API response listener timed out while cycling every page; completed navigations did not expose a business API 4xx/5xx, but this listener result is not treated as a passed gate.
- Restricted role `f1_route_restricted` (role 68) was disabled in the development database, so its retained account was not reused as a persistent login entry. Its capability query excluded the migrated F3/F4 page permissions before the final acceptance run.

### 6.2 受限角色浏览器关闭证据（2026-07-25）

- 使用一次性员工账号加入角色 68，仅临时授予“登录登出记录”和“查询登录日志”两个 F3 能力；未授予操作日志或定时任务能力。
- 一次性账号真实登录成功，直达 `/support/login-log/login-log-list` 后页面正常渲染并返回真实查询数据；点击查询后结果保持正常。
- 未授权的 `/support/operate-log/operate-log-list` 和 `/job/list` 均进入应用 404，未短暂渲染对应 F3/F4 页面。
- 已授权页面刷新后仍能恢复；退出并重新登录后，同一页面仍可访问和查询，权限结果保持一致。
- 验收期间未出现新增 Console error；仅保留既有 StorageManager warning。此前使用失效密码产生的一条登录错误不计入通过证据。
- 验收完成后已删除一次性员工账号、重新停用原 `f1_route_restricted` 员工账号，并撤销角色 68 临时增加的登录日志菜单与查询权限；未保留明文凭据或持续可登录的测试入口。

至此，代码、图谱、测试、构建、数据库实值、26 个历史菜单处置以及管理员和受限角色浏览器记录均已通过，状态关闭为 `FRONTEND_FULL_MIGRATION_CLOSED`。
