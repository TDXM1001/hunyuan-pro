# A3.4 OA 通知退役执行与关闭台账

## 1. 最终结论

截至 2026-07-23，A3.4 P3 已完成全部关闭门禁：

```text
A3.4 P3 = P3_CLOSED
OA 通知 = RETIRE_CLOSED
OA 整体 = RETIRE_CLOSED
A3.4 业务示例与 OA 退役范围 = RETIRE_CLOSED
```

本批采用直接退役路径，没有将 OA 通知数据、接口或公告语义迁移到平台 `message`。
未来如重新出现公告需求，应建立独立的平台公告契约；不得恢复本批旧路由或直接扩展
`message` 表模拟公告。

## 2. 范围与边界

本批退役对象：

- `business.oa.notice` 的 Controller、Service、Manager、DAO、DTO、Entity 和 VO。
- `NoticeMapper.xml` 及通知协作测试。
- 通知类型、通知主表、可见范围和查看记录四张表。
- 通知页面、操作权限、角色授权、代码生成配置和 `t_data_tracer.type = 2`。
- 已无其他子节点的 `功能Demo` 父菜单 `menu_id = 138`。
- OA 通知 Swagger 标签和数据追踪枚举。

本批明确不处理：

- 平台 `message`、短信和文件中心。
- `web-ele` 工程验收 Demo、数据脱敏诊断和 `backend-mock`。
- 与 A3.4 P3 无关的其他业务、平台能力及工作树修改。

## 3. 数据备份与恢复

退役前开发库数据：

| 对象 | 数量 |
| --- | ---: |
| `t_notice` | 12 |
| `t_notice_type` | 2 |
| `t_notice_view_record` | 0 |
| `t_notice_visible_range` | 1 |
| 通知附件引用 | 0 |
| `t_data_tracer.type = 2` | 0 |
| 通知菜单节点 | 8 |
| 通知角色授权 | 8 |
| `menu_id = 138` 子节点 | 1 |
| `t_message` | 68 |

完整备份：

```text
数据库SQL脚本/mysql/backups/
hunyuan-before-a3-4-p3-oa-notice-retirement-20260723-110237.sql
```

备份大小为 304,360 字节，SHA-256 为
`FB37B46EB9B19F9DFA0969330AE3DACB2FBF0A5000DCDD77255DED9300C0A035`。
备份已恢复到隔离库并完成迁移前数量对账。通知附件引用为 0，因此本批没有对象文件
删除动作；文件生命周期继续由平台文件能力负责。

## 4. Flyway 与数据库结果

本批新增：

```text
V3_76_0__a3_4_retire_oa_notice.sql
V3_76_1__a3_4_remove_retired_oa_notice_parent_grants.sql
```

`V3.76.0` 删除通知角色授权、权限与页面节点、代码生成配置、追踪记录和四张通知表；
仅在 138 没有子节点时删除该父菜单。开发库执行后发现 138 被删除但原父菜单角色授权
形成 1 条孤立记录，因此没有修改已执行迁移，而是通过递增的 `V3.76.1` 在 138
确实不存在时清理该授权；若 138 仍承载其他子节点，则不会误删其授权。

空库隔离迁移、备份恢复迁移和开发库迁移均通过。开发库最终对账：

| 验收项 | 结果 |
| --- | ---: |
| Flyway 版本 | `3.76.1` |
| 四张通知表 | 0 |
| 通知菜单和权限节点 | 0 |
| 通知角色授权 | 0 |
| `menu_id = 138` | 0 |
| `t_data_tracer.type = 2` | 0 |
| `t_message` | 68 |
| message 菜单 | 1 |

## 5. 路由与 OpenAPI

以下 12 条旧路由在正式 JAR 启动后均返回 HTTP 404：

```text
GET  /oa/noticeType/getAll
GET  /oa/noticeType/add/{name}
GET  /oa/noticeType/update/{noticeTypeId}/{name}
GET  /oa/noticeType/delete/{noticeTypeId}
POST /oa/notice/query
POST /oa/notice/add
POST /oa/notice/update
GET  /oa/notice/getUpdateVO/{noticeId}
GET  /oa/notice/delete/{noticeId}
GET  /oa/notice/employee/view/{noticeId}
POST /oa/notice/employee/query
POST /oa/notice/employee/queryViewRecord
```

`/v3/api-docs` 返回 200，共 133 条路径。OA 通知路径为 0、通知标签为 0；
平台 message 路径仍有 6 条，证明通知退役没有删除消息能力。

本次运行态验收使用 `--spring.main.lazy-initialization=true` 绕开仓库既有
`RedisCacheManager` 类型注入启动缺陷。该参数只用于验收，未修改缓存源码；
Flyway、路由注册、OpenAPI 和本批业务对象验收均来自正式构建 JAR。

## 6. 源码、构建与测试

- 通知生产 Java 包、Mapper、路由和权限引用均为 0。
- `AdminSwaggerTagConst.Business.OA_NOTICE` 与 `DataTracerTypeEnum.OA_NOTICE` 已删除。
- 正式 JAR 中通知 Java 条目和通知 Mapper 条目均为 0。
- 正式 JAR 与 OpenAPI 中平台 message Controller 和路由保持存在。
- Maven 完整 reactor 共执行 167 项测试：`hunyuan-base` 12 项、
  `hunyuan-admin` 155 项，0 失败、0 错误、3 项按环境跳过。
- Flyway 隔离测试、严格 UTF-8 解码和 `git diff --check` 均通过。

## 7. Codebase Memory

退役前索引 `E-my-project-hunyuan-pro-a3-4-p2-closed-20260723` 可检出
`NoticeController`、通知 Service/DAO 和 12 条通知 Route，同时可检出独立的平台
message 路由。

关闭索引使用持久化项目：

```text
E-my-project-hunyuan-pro-a3-4-closed-20260723
```

索引状态为 `indexed`，已生成持久化图谱产物，共 15,751 个节点和 38,943 条关系。
关闭反查结果：

| 图谱对象 | 数量 |
| --- | ---: |
| Notice Controller/Service/Manager/DAO | 0 |
| `/oa/notice*` Route | 0 |
| Enterprise/Bank/Invoice Controller/Service/DAO | 0 |
| `MessageController` | 1 |
| `AdminMessageController` | 1 |
| `MessageService` | 1 |
| message Route | 7 |

图谱中的 7 个 message Route 同时包含后端注解路由和前端客户端调用节点；运行态
OpenAPI 的后端 message 路径为 6。两种口径均证明平台消息能力仍然存在。

## 8. 关闭判定

P3 的关闭条件已经全部满足：

1. 历史数据完成可恢复备份和隔离恢复对账。
2. 通知生产实现、Mapper、共享常量和内部消费者归零。
3. 数据库表、菜单、权限、角色授权和追踪类型归零。
4. 12 条旧路由均为 404，OpenAPI 不再暴露通知路径和标签。
5. 平台 `message` 的数据、菜单、Controller 和路由均保留。
6. 短信、文件中心和工程 Demo 未进入本批修改范围。
7. 正式 JAR、完整测试、UTF-8 和 Codebase Memory 关闭索引通过。

最终判定：`P3_CLOSED`。商品、分类、OA 主数据与 OA 通知均已退出生产能力边界，
空 `功能Demo` 菜单外壳已删除；A3.4 业务示例与 OA 退役范围正式关闭。
