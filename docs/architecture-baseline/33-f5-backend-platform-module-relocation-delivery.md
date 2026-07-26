# F5 后端平台模块归位交付记录

## 1. 结论与范围

截至 2026-07-25，F5 已完成七个平台 owner 的后端实现归位、`hunyuan-base` HTTP 入口清零、跨 owner 公开协议收口、架构守卫和独立运行态验收。

当前状态：`F5_CLOSED`。

本记录的分母是 F0 冻结的 10 个 base HTTP Controller，以及配置、文件、审计、通知、安全、运行时和开发工具七组平台能力的 Controller、ApplicationService、内部 Service、持久化模型与 Mapper。前端页面、菜单、权限、Flyway 和数据库数据不在本批变更范围内；这些契约没有变化，继续复用 [32-frontend-full-migration-delivery.md](32-frontend-full-migration-delivery.md) 的已关闭证据。

F5 与已关闭的前端 F0 至 F4、前端侧 F6 合并后，底座结构状态关闭为 `FOUNDATION_STRUCTURE_CLOSED`。这只表示底座结构门已满足，不表示首个真实业务纵切已经实施。

## 2. Owner 归位结果

平台实现统一归入 `hunyuan-admin` 的 `com.hunyuan.sa.admin.module.platform.<owner>`，`hunyuan-base` 只保留稳定 Facade、公开 DTO、通用基础设施和少量扩展协议。

| owner | 归位内容 | base 保留边界 |
| --- | --- | --- |
| `platform-configuration` | 配置、字典、公开与历史兼容 Controller、Service、DAO、Entity、Form/VO、Mapper | 配置值读取、配置与字典 Facade 及公开模型 |
| `platform-file` | 文件 Controller、应用服务、存储实现、DAO、Entity、Form/VO、Mapper | 文件 Facade、公开命令与视图 |
| `platform-audit` | 登录日志、操作日志、更新日志、审计切面、Controller、Service、DAO、Entity、Mapper | 登录审计与平台审计 Facade、公开查询和视图 |
| `platform-notification` | 消息、站内信、短信、邮件实现及其 Controller、Service、DAO、Entity、Mapper | 消息、短信、邮件 Facade 与公开协议 |
| `platform-security` | 验证码、登录防护、密码历史、安全策略、数据脱敏入口及持久化 | 安全 Facade、密码 codec 与公开状态模型 |
| `platform-runtime` | 任务、单号、Reload、心跳、表格列偏好及其 Controller、Service、DAO、Entity、Mapper | 任务扩展、运行时 Facade、注解和公开模型 |
| `platform-devtools` | 代码生成与接口加解密验证入口、内部实现、模板引用和 Mapper | 代码生成 Facade、公开命令/视图及四个校验枚举 |

最终源码中七个 owner 共 263 个 Java 文件，21 个平台 Mapper XML 全部位于 admin。299 个迁移类型的旧 FQN 搜索结果为 0；Mapper 的 namespace 和 resultType 均能解析到现有 Java 类型。

## 3. Base 收缩与依赖边界

F0 冻结的 10 个 base `@RestController` 已由 10 递减至 0，`BASE_MUST_NOT_EXPOSE_HTTP_ROUTES` 从冻结基线改为严格零容忍规则。base 不再拥有业务或平台 HTTP 入口。

`ArchitectureGuardTest` 对七个平台 owner 分别建立内部实现不外泄规则，并增加平台 owner 切片循环守卫。其他模块只能依赖 base 中的稳定协议，不能直接导入平台模块的 Service、DAO、Mapper、Entity 或历史 Form/VO；公开 Facade 继续禁止暴露内部持久化模型。

现有数据库表、事务边界和写入路径均未复制或双写。迁移只改变 Java 包和资源归属，不改变数据 owner。

## 4. 兼容与运行时修复

本批没有修改 URL、HTTP 方法、权限码、请求响应语义或数据库结构。公开 `/api/admin/v1/platform/**` 路由与历史兼容路由均保留原契约；表格列偏好的完整历史路径仍为：

- `POST /support/tableColumn/update`
- `GET /support/tableColumn/delete/{tableId}`
- `GET /support/tableColumn/getColumns/{tableId}`

代码生成模板 `tools.xml` 已同步新的 `CodeGeneratorTool` FQN，避免模板运行时继续引用已删除包名。

干净 JAR 首次启动发现两项只有运行态才能暴露的问题，并已关闭：

1. 父 POM 曾从 Redisson 传递依赖中排除 Objenesis，但 Redisson 3.50.0 的 Kryo/Config 在生产运行态直接需要它。已删除错误 exclusion，不再依赖 Mockito 的 test scope 间接提供。
2. `LoginService` 的 `@Resource` Facade 字段沿用内部 Service 名称，Spring 会优先按名命中不兼容 Bean。验证码和登录防护字段已改用明确的 Facade 边界名称，并增加字段名与类型契约测试。

## 5. Codebase Memory 证据

使用新项目 `E-my-project-hunyuan-pro-foundation-f5-closed-20260725` 对最终代码执行 full 索引。外部缓存保留该索引；刷新过程曾改写工作区 `.codebase-memory` 产物，现已按刷新前记录的 SHA-256 精确恢复：

- `artifact.json`：`D92B2B2B1F6F887C87BEEA33926968FBA478D64993DED20C110A27B5FBD56556`。
- `graph.db.zst`：`5AD93E4B4464CAF0EB8EE5FFEF353759197590406E58C24CEE2EC5D68EDFE6C5`。
- 节点：16,296。
- 关系：43,043。
- `hunyuan-base/src/main/java` 中 Java HTTP Route：0。
- 七个平台 owner 源码文件：263。
- admin 主源码从平台 owner 外部直接导入平台内部实现：0。
- 索引继续排除 `.codex-runtime`、数据库备份和依赖产物目录。

## 6. 验证证据

| 门禁 | 结果 |
| --- | --- |
| 干净临时副本 `clean verify` | `hunyuan-base` 通过；`hunyuan-admin` 287 项执行，0 失败、0 错误、3 项环境条件跳过 |
| Spring Boot 可执行 JAR | 重打包成功 |
| 独立端口启动 | `11024` 启动成功，启动日志无 `ERROR`、`BeanCreationException` 或 `Application run failed` |
| OpenAPI | `/v3/api-docs` 返回 200，共 168 条路径 |
| 平台稳定路由 | `/api/admin/v1/platform/**` 共 50 条；扣除下项单独统计的 6 条代码生成路径后，其余 44 条覆盖 audit、configurations、devtools、dictionaries、files、message-inbox、messages、notifications、runtime 九组路径 |
| 代码生成稳定路由 | 6 条 `/api/admin/v1/platform/devtools/code-generator/**` 路径 |
| 表格列历史路由 | 3 条均在 OpenAPI 中，HTTP 方法保持不变 |
| 旧 FQN、Mapper、包路径 | 旧 FQN 0 残留；21 个 Mapper 类型有效；变更 Java 文件包名与路径一致 |
| UTF-8 与 diff | 变更文本严格 UTF-8；staged、unstaged `git diff --check` 均通过 |

运行态验收使用干净构建产物和独立端口，完成后只停止本次临时进程。原有 PID `10156` 继续监听 `1024`，未停止或替换；该进程启动于 F5 之前，不作为当前代码的运行态验收证据。

Maven 使用 F 盘 JDK、Maven、settings 和本地仓库。settings 第 235 行的零宽字符警告为既有环境问题，不影响本批构建结论。

## 7. 复用与排除

- 前端 F0 至 F4、前端侧 F6、26 个历史菜单处置、管理员与受限角色浏览器验收继续复用既有关闭记录。
- 本批没有前端契约变化，因此不重复运行前端全量测试或浏览器验收。
- 本批没有 Flyway、表结构、菜单、权限或持久化数据变化，因此不重复执行开发库迁移与数据验收。
- 三个环境条件跳过项没有被当作数据库验收证据；数据库直接证据仍以既有关闭记录为准。

## 8. 最终关闭

F0 冻结的 base HTTP 遗留已归零，七个平台 owner 的实现、持久化与 HTTP 入口均已归位，公开协议、依赖方向、构建、OpenAPI、运行态和代码图门禁全部通过。F5 关闭为 `F5_CLOSED`，底座结构关闭为 `FOUNDATION_STRUCTURE_CLOSED`。

后续可以回到 [26-first-real-business-vertical-slice-blueprint.md](26-first-real-business-vertical-slice-blueprint.md) 的选题准入与业务契约阶段；不得把本次结构关闭解释为某个真实业务模块已经完成。
