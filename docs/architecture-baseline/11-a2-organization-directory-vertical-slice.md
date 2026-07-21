# A2 组织目录垂直试点

## 1. 阶段结论

A2 首个垂直模块确定为 `organization`，首个闭环是“部门目录管理”。本批次用真实的平台组织数据验证目标架构，不把历史商品、OA 或 Demo 能力升级为正式产品模块。

本批次属于架构试点，不替代后续产品业务识别。若后续确定客户、项目、合同、资产等真实产品领域，仍需按用户、任务、生命周期、事务和数据所有权重新划分模块。

## 2. 用户与任务

主要用户：组织管理员、只有查看权限的组织成员。

完整任务：

```text
登录并加载模块与能力
  -> 查看权限范围内的部门树
  -> 新建部门
  -> 修改部门名称、负责人、上级和排序
  -> 删除空部门
  -> 遇到子部门或在职员工时得到明确业务拒绝
  -> 刷新后数据与可用操作保持一致
```

## 3. 本批次范围

包含：

- `t_department` 的目标数据所有权归属 `organization` 模块。
- 部门树查询、详情、新增、修改、移动和删除。
- 新版 Admin API：`/api/admin/v1/organization/departments`。
- OpenAPI 契约和前端 TypeScript 客户端。
- 独立前端 organization feature 的模块声明、页面和权限控制。
- 模块启用、页面能力、操作能力、数据范围和业务状态五层验收。
- 旧部门 API 与页面在替代验收完成前保持可用。

不包含：

- 员工账号、密码、角色和岗位的全生命周期重构。
- 历史商品、分类、OA、帮助文档和支持菜单的 KEEP/RETIRE 清理。
- Maven 模块拆分、微服务、微前端、动态插件和运行时建表。
- 未经业务确认的大规模目录搬迁。

## 4. 模块边界

目标后端结构：

```text
organization/
├─ api/              Admin API 和公开 Facade
├─ application/      部门管理用例与事务
├─ domain/           部门规则、模型和 Repository 端口
└─ infrastructure/   MyBatis 持久化与系统能力适配
```

依赖规则：

- API 只能进入 Application。
- Domain 不依赖 Spring Web、MyBatis、Controller 或基础设施实现。
- `organization` 不直接依赖 `system` 模块的 DAO、Mapper 或 Entity。
- 查询部门是否存在员工、负责人是否有效等能力，通过 `system` 暴露的公开 Facade 调用。
- 兼容期旧 Controller 调用新模块公开 Facade；禁止新旧两套业务逻辑分别写 `t_department`。

## 5. 业务规则

1. 部门名称去除首尾空白后长度为 1 至 50 个字符。
2. 上级部门必须存在；根部门使用 `parentId = 0`。
3. 部门不能成为自己的上级，也不能移动到自己的后代节点下。
4. 指定负责人时，负责人必须是有效员工。
5. 有子部门的部门不能删除。
6. 有未删除员工的部门不能删除。
7. 查询结果按 `sort`、`departmentId` 稳定排序。
8. 不存在的部门返回稳定的业务错误，不泄漏持久化异常。

## 6. 稳定标识

模块编码：`organization.directory`

能力编码：

```text
organization.department.read
organization.department.create
organization.department.update
organization.department.delete
```

兼容期保留旧编码：

```text
system:department:add
system:department:update
system:department:delete
```

新页面只使用新编码。旧入口替代验收完成后，再通过独立迁移决定旧编码是否下线。

## 7. API 契约

```text
GET    /api/admin/v1/organization/departments
GET    /api/admin/v1/organization/departments/{departmentId}
POST   /api/admin/v1/organization/departments
PUT    /api/admin/v1/organization/departments/{departmentId}
DELETE /api/admin/v1/organization/departments/{departmentId}
```

写请求使用 JSON；删除使用 HTTP `DELETE`。响应继续适配当前统一 `ResponseDTO`，但业务错误必须具有稳定错误码和可读消息。

## 8. 数据迁移与兼容

- 在 `V3.65.0` 之后新增独立 Flyway 迁移，不修改已执行迁移。
- 迁移只写模块、菜单、能力和必要授权，不复制开发库组织数据。
- 空库和已有 `3.65.0` 数据库都必须可迁移。
- 数据库迁移跟随产品版本执行，模块关闭时保留部门数据。
- 旧 API 在替代验收前继续可用，但转调同一应用用例；不建立第二张部门表。
- 新旧 API 行为对照通过后，菜单入口切换到新 feature；旧 API 下线另立批次。

## 9. 验收门

自动验收：

- Domain 规则单元测试覆盖环、自身父级、子部门和员工占用删除限制。
- Application 与真实 MySQL 集成测试覆盖完整读写和事务回滚。
- ArchUnit 验证分层方向以及无跨模块持久化访问。
- OpenAPI 生成可重复，前端生成客户端无未提交漂移。
- 前端类型检查、API 契约测试、feature 注册和按钮权限测试通过。
- 空隔离库和已有基线库迁移通过。

浏览器验收：

```text
管理员登录 -> 打开组织目录 -> 新建 -> 修改/移动 -> 删除 -> 刷新核对
只读用户登录 -> 可查看 -> 无写操作入口
无模块或无页面能力用户 -> 无菜单 -> 直接 URL 被拒绝
无写能力用户 -> 直接调用写 API 被拒绝
删除有子部门或在职员工的部门 -> 状态不变并显示业务错误
```

## 10. 完成定义

只有同时满足以下条件，A2 才能关闭：

1. 新接口、新 feature、迁移、权限和浏览器闭环均通过。
2. 旧部门入口已完成替代验收，没有新旧双写路径。
3. `organization` 模块没有新增架构冻结例外。
4. 与本切片相关的 `LEGACY-001`、`LEGACY-002`、`LEGACY-004`、`LEGACY-005`、`LEGACY-006` 和 `LEGACY-009` 已关闭或明确缩小到非部门范围。
5. 验收结果和仍保留的兼容边界写回本文件及迁移账本。

## 11. 实施与运行态验收记录（2026-07-21）

本批次已完成组织目录首个垂直切片的实现和运行态验收：

- 后端已落地 `organization` 的领域、应用、基础设施和 Admin API 分层；新写路径只有应用服务一条，旧 `system.department` 入口通过兼容 Facade 调用同一领域规则。
- 前端已落地 `@hunyuan/feature-organization`，应用层只保留薄装配入口；管理员按钮按 `organization.department.*` 能力码显示，负责人候选来自真实接口。
- 数据迁移为 `V3_66_0__a2_organization_directory.sql` 和递增修复 `V3_66_1__a2_organization_permission_type.sql`。后者修复复用旧菜单记录时 `perms_type` 为空导致非管理员读权限被忽略的问题；开发环境默认关闭 Flyway，启动验收服务时使用 `HUNYUAN_FLYWAY_ENABLED=true` 执行接管迁移。
- 管理员浏览器闭环已通过：部门目录、负责人下拉、新建父/子部门、修改名称/负责人/排序、移动到顶级、删除空部门和刷新核对均成功。
- 业务拒绝已通过：删除有子部门的部门提示“请先删除子部门”；删除有在职员工的部门提示“请先处理部门员工”，数据保持不变。前端统一错误解析已兼容 ResponseDTO 的 `msg` 字段。
- 只读账号 `huke` 已通过：可查看 7 个部门，写入口全部隐藏；直接写 API 返回权限拒绝 `30005`，没有产生数据。
- 模块开关已通过：关闭 `module.organization.directory.enabled` 后管理员新 API 返回稳定错误码 `41001`，非管理员登录菜单不再包含部门目录，直达 URL 落到未找到页面；开关已恢复为 `true`。
- 验证结果：后端全量测试 22 项执行、3 项隔离测试按环境配置跳过、0 失败；前端单测 58 个文件/430 项通过；`@hunyuan/system` 类型检查通过；OpenAPI 返回 200，并暴露部门列表、详情、负责人候选、新增、更新、删除 6 个 operationId。

当前收口状态：A2.1 已完成旧部门 API、旧页面、旧权限码和兼容实现的代码与数据退役，并通过隔离库、开发库、直接 API、后端、前端和浏览器运行态验收。管理员可查看 7 个部门并拥有完整写入口；只读角色可查看 7 个部门但无写按钮，直接写 API 返回 `30005` 且数据不变；模块关闭时管理员新 API 返回 `41001`、目录菜单和直达页面均不可用，恢复开关后新 API 与只读页面恢复正常。临时角色和员工关联已全部清理。A2.1 已关闭，可以进入 A3 范围评估，但本批次不直接启动 A3 实现。详见 [13-a2-1-organization-directory-compatibility-retirement.md](13-a2-1-organization-directory-compatibility-retirement.md)。
