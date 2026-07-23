# A3.4 Demo 生产隔离关闭记录

## 1. 关闭结论

截至 2026-07-23，A3.4 P4 按“无已确认业务，不继续扩展业务模块”的边界完成治理：

- `web-ele` 继续保留为独立工程验收应用，不接入 `hunyuan-system` 正式业务菜单。
- 数据脱敏能力保留为安全治理下的验证能力，接口要求 `support:protect:dataMasking:query` 权限。
- `backend-mock` 仅用于开发、组件验收和自动化测试，生产构建不加载 Nitro Mock 插件。

## 2. 变更与兼容

- 后端控制器命名从 Demo 调整为数据脱敏验证，接口路径暂保留
  `/support/dataMasking/demo/query`，避免已部署前端调用失效。
- 前端 API、数据类型和页面按钮移除 Demo 语义，继续使用兼容接口路径。
- 新增安全治理菜单和查询权限，平台管理员默认获得该权限；其他角色需显式授权。
- `web-ele` 的 Demo 路由和页面未删除，默认开发与预览入口保持不变。

## 3. 权限与运行态证据

2026-07-23 使用随机临时数据库、随机管理员账号和 Redis 15 号隔离库完成真实运行态
验收。临时凭据和 Token 未输出、未保存、未写入源码或文档，验收后临时数据库已删除。

| 验收项 | 结果 |
| --- | --- |
| Flyway | 随机空库完成全量迁移，最终版本为 `3.77.0` |
| 服务启动 | test profile JAR 在 `11025` 端口启动成功 |
| 未登录直连接口 | HTTP 200 统一响应，业务码 `30007`，`ok=false`，无业务数据 |
| OpenAPI | 包含 `/support/dataMasking/demo/query` |
| 管理员登录 | 使用仓库既有 SM4 与 Base64 协议登录成功 |
| 授权查询 | 返回 11 条验证数据 |
| 数据脱敏 | 手机号和密码字段均包含脱敏字符 |

控制器方法保留 `@SaCheckPermission("support:protect:dataMasking:query")`，迁移脚本
`V3_77_0__a3_4_data_masking_validation_permission.sql` 新增安全治理页面、查询权限，
并仅为 `platform_admin` 默认授予页面和查询能力。

## 4. 构建与代码图证据

- 后端完整 `clean verify` 通过：`hunyuan-base` 12 项、`hunyuan-admin` 155 项，
  0 失败、0 错误，3 项外部环境测试按配置跳过，ArchUnit 18 项通过。
- 前端数据脱敏 API 契约测试 1 项通过，`@hunyuan/system` 类型检查和生产构建通过。
- 生产构建生成数据脱敏页面资源，backend-mock 仅在开发或测试开关下启用。
- Codebase Memory 全量索引
  `E-my-project-hunyuan-pro-a3-4-p5-closed-20260723` 状态为 `indexed`，包含
  15,059 个节点和 37,284 条关系。
- 图检索确认 `AdminDataMaskingController` 存在，旧
  `AdminDataMaskingDemoController` 节点为 0；权限注解、兼容路由、Flyway 权限和
  前端 API 消费者均可反查。
- 新增和修改的 Java、TypeScript、Vue、SQL、Markdown 文件按 UTF-8 严格解码，
  `git diff --check` 通过。

## 5. 关闭结论与停止条件

P4、P5 状态均为 `CLOSED`，A3.4 正式关闭。关闭后不再主动开发 Demo 或 OA 业务。
后续只有在出现明确业务 owner、目标用户、流程和验收标准后，才新建业务需求和对应
实现范围；不得把保留的工程验收页、Mock 数据或数据脱敏验证接口直接解释为业务需求。
