# A4.5 评估能力关闭或产品化

## 1. 状态与范围

截至 2026-07-24，A4.5 已完成帮助文档、意见反馈、事务邮件和数据追踪四项 `EVALUATE` 能力的采用审计与边界判定。本阶段只依据现行源码、前端消费者、菜单授权、本机开发库和运行责任作出决策，不把存在 Controller、数据表或历史菜单视为产品承诺。

本次先使用 Codebase Memory 项目 `E-my-project-hunyuan-pro-a4-5-evaluation-20260724` 建立实施前全量索引，共 16,626 个节点和 43,582 条关系。实施后使用新项目名 `E-my-project-hunyuan-pro-a4-5-closeout-20260724` 刷新为 16,126 个节点和 42,099 条关系，并写入 `.codebase-memory/graph.db.zst`。图结果用于定位路由和调用链，最终结论仍由源码、数据库和测试交叉确认。

## 2. 决策结论

| 能力 | 证据 | 决策 | 最终边界 |
| --- | --- | --- | --- |
| 帮助文档 | 前端页面和 API 均不存在；开发库仍有 2 条“企业信息”历史文档、4 个目录、2 条关系、0 条查看记录；菜单仍指向不存在的组件 | `RETIRE` | 删除生产代码、路由、菜单授权和四张历史表 |
| 意见反馈 | 前端页面和 API 均不存在；开发库 `t_feedback` 为 0 条；菜单仍指向不存在的组件 | `RETIRE` | 删除生产代码、路由、菜单授权和历史表 |
| 事务邮件 | 登录验证码经 `PlatformMailFacade` 真实调用；开发库保留 1 个 `login_verification_code` 模板 | `KEEP / INTERNAL` | 保留受控模板邮件发送边界，不新增 SMTP、模板管理页面或管理 API |
| 数据追踪 | 前端消费者和包外写入者均为 0；仅剩 5 条已退役商品类型记录；查询路由无权限和管理入口 | `RETIRE` | 删除生产代码、查询路由和历史表；正式审计继续由登录、操作和变更日志边界承担 |

## 3. Codebase Memory 交叉核对

Codebase Memory 找到了帮助、反馈和数据追踪的后端 Route 节点，也确认邮件服务存在登录消费者。图中曾把多个同名 `insert` 方法解析为数据追踪写入者；直接源码检索确认这些调用实际写入字典、任务、短信等各自 DAO，仓库中不存在数据追踪包外调用。该误匹配不作为保留能力的证据。

当前前端源码对 `helpDoc`、`feedback` 和 `dataTracer` 的消费者均为 0。开发库仍有帮助、反馈页面菜单及六个帮助权限节点，各有一个角色授权，但组件文件已经不存在，属于悬空菜单和历史授权。

## 4. 实施记录

1. 删除 `support.helpdoc`、`support.feedback`、`support.datatracer` 三个生产包、MyBatis 映射和旧帮助管理 Controller。
2. 删除对应 Swagger 标签、文件目录枚举和文件列表筛选项，避免退役语义继续暴露。
3. 新增 `V3_78_0__a4_5_retire_unadopted_support_capabilities.sql`，先清理角色菜单和权限，再按从属顺序删除帮助、反馈和数据追踪表。
4. 保留 `PlatformMailFacade`、登录验证码模板和内部邮件应用服务，不增加管理菜单、配置页面或通用发信 HTTP 入口。
5. 增加后端和前端退役守卫，禁止历史包、路由、权限、目录类型和消费者被恢复。

## 5. 数据与迁移边界

开发库迁移前必须单独备份以下六张退役表，以及两张菜单授权表中的相关行：

```text
t_help_doc
t_help_doc_catalog
t_help_doc_relation
t_help_doc_view_record
t_feedback
t_data_tracer
t_role_menu / t_menu 中的帮助与反馈节点
```

当前开发库已确认帮助和反馈附件均为 0，`t_file.folder_type IN (3, 4)` 的记录为 0。迁移不删除通用文件表数据，也不修改 `t_mail_template`。

## 6. 关闭条件

A4.5 只有同时满足以下条件才算关闭：

1. 退役生产源码、Mapper、路由和悬空前端语义全部归零。
2. Flyway 在隔离数据库完成升级，开发库在备份后升级至 `3.78.0`。
3. 后端聚焦测试、前端契约测试、TypeScript 检查和管理端构建通过。
4. OpenAPI 不再注册 `/helpDoc/*`、`/feedback/*` 和 `/dataTracer/*`，事务邮件不新增公共管理路由。
5. Codebase Memory 使用新项目名刷新并与直接源码搜索交叉核对。
6. 新增和修改文本通过严格 UTF-8 与 `git diff --check` 校验。
