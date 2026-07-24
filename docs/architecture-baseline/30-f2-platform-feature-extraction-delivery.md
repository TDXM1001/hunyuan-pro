# F2 配置、文件与账号 Feature 归位交付记录

## 1. 交付结论

截至 2026-07-25，F2 已完成配置、字典、文件和个人账号页面的 feature 归位，当前状态为 `F2_CLOSED`。

应用层不再拥有对应的业务 API 或完整页面，只负责将已有请求客户端注入 feature，并通过薄入口和 App Kernel 注册表装配页面。后端菜单继续保留历史 `component` 作为未迁移菜单的兼容输入，但本批三个菜单已优先使用稳定 `routeId`。

## 2. 本批边界

| feature | 迁移内容 | 应用保留边界 |
| --- | --- | --- |
| `identity-account` | 个人资料、密码、安全和消息提醒页面，以及账号资料、头像、密码策略客户端 | `/profile` 壳层路由和单个页面薄入口 |
| `platform-configuration` | 参数配置、数据字典、字典值抽屉和对应客户端 | 请求客户端注入与两个页面薄入口 |
| `platform-file` | 文件管理页面和对应客户端 | 请求客户端注入与单个页面薄入口 |

应用唯一注册表仍位于 `apps/hunyuan-system/src/app-kernel/feature-registry.ts`。它声明三个 feature 的稳定路由加载器，不扫描 feature 目录，也不让 feature 导入应用别名、应用请求单例或应用页面。

## 3. 稳定路由与数据库

`V3_80_0__f2_platform_feature_route_ids.sql` 只回填已迁移菜单的稳定标识：

| 菜单 ID | 菜单路径 | routeId |
| ---: | --- | --- |
| 109 | `/config/config-list` | `platform.configuration.parameters` |
| 110 | `/setting/dict` | `platform.configuration.dictionary` |
| 193 | `/support/file/file-list` | `platform.file.management` |

本机开发库已执行该迁移：Flyway 版本为 `3.80.0`，迁移记录成功，三条 `t_menu.route_id` 均与上表一致。运行时路由优先顺序保持不变：`menu.routeId -> App Feature Registry -> 应用薄入口 -> feature 页面`。未迁移菜单仍可通过历史 `component` 兼容读取，F2 不删除双读逻辑。

## 4. 结构验证

本批使用新的 Codebase Memory 项目 `E-my-project-hunyuan-pro-f2-20260725` 完整重建索引，结果为 15,921 个节点、42,018 条关系。图谱和源码交叉核对结果如下：

1. `identity-account`、`platform-configuration`、`platform-file` 到 `apps/hunyuan-system` 的导入为 0。
2. 应用通过 feature 注册表和页面薄入口单向装配 feature。
3. 已删除的 `api/core/account`、`api/system/config`、`api/system/dict`、`api/system/file` 及旧页面路径没有生产消费者；全文命中仅存在于架构负向守卫测试，用于防止旧入口回流。
4. `login-adapter.ts` 在菜单携带已注册 `routeId` 时解析 feature 组件；缺失或未知标识才进入兼容组件路径或桥接页。

图谱用于结构导航和消费者审计；Flyway 版本、菜单实值、构建和浏览器结果均由各自的直接验证提供证明。

## 5. 验证证据

自动化与构建门禁：

- 全量前端单元测试：68 个文件、456 项通过。
- `@hunyuan/system` TypeScript 检查通过。
- `@hunyuan/system` 生产构建通过。
- Maven `hunyuan-admin -am -DskipTests package` 通过。
- Flyway `3.80.0` 迁移成功，目标菜单 `route_id` 实值已复查。

管理员真实浏览器验收使用新的本地开发服务完成，避免复用变更前的 Vite 进程：

- `/config/config-list` 显示参数配置的查询区、操作入口和真实列表数据。
- `/setting/dict` 显示数据字典查询区、字典值入口和真实列表数据。
- `/support/file/file-list` 显示文件筛选条件和真实文件列表数据。
- `/profile` 显示账号 feature 提供的基本设置、安全设置、修改密码和消息提醒页签。
- 当前验收服务的页面控制台无错误。

## 6. 后续边界

F3 继续迁移审计、通知和安全功能。F3 不得删除 `component` 双读、模块桥接页或历史页面扫描；这些兼容能力应保留到 F6 完成全部菜单稳定路由迁移、浏览器验收和独立 Flyway 关闭后再统一退役。
