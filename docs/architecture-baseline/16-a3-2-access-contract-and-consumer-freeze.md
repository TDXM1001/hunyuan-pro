# A3.2 访问控制契约与消费者冻结

## 1. 状态与目标

截至 2026-07-22，A3.1、A3.2 已正式关闭。A3.2 由 `access` 统一拥有角色、员工角色分配、菜单与能力授权、数据范围以及登录授权装载边界。

本阶段保持 `t_role`、`t_menu`、`t_role_employee`、`t_role_menu`、`t_role_data_scope` 为唯一数据源，不创建镜像表，不建立新旧双写。

## 2. 所有权冻结

| 对象                    | 目标 owner                | 决策                         |
| ----------------------- | ------------------------- | ---------------------------- |
| `t_role`                | `access.role`             | 角色生命周期和稳定角色编码   |
| `t_role_employee`       | `access.role`             | 员工角色分配                 |
| `t_menu`                | `access.capability`       | 导航菜单、功能点和能力码目录 |
| `t_role_menu`           | `access.capability`       | 角色菜单与能力授权           |
| `t_role_data_scope`     | `access.data-scope`       | 角色数据范围                 |
| Sa-Token 会话与登录缓存 | `identity.authentication` | 只消费 access 公开授权快照   |

## 3. 现有入口冻结

| 能力         | 当前入口                                                                           | 当前权限                                       | 处置                                   |
| ------------ | ---------------------------------------------------------------------------------- | ---------------------------------------------- | -------------------------------------- |
| 角色生命周期 | `/role/add`、`/role/update`、`/role/delete/{id}`、`/role/get/{id}`、`/role/getAll` | `system:role:add/update/delete`                | 迁移到 `/api/admin/v1/access/roles`    |
| 员工角色分配 | `/role/employee/*`                                                                 | `system:role:employee:*`，部分查询无显式权限   | 迁移并消除 `EmployeeVO`                |
| 角色菜单授权 | `/role/menu/*`                                                                     | `system:role:menu:update`，查询无显式权限      | 迁移到 access capability 用例          |
| 数据范围     | `/dataScope/list`、`/role/dataScope/*`                                             | `system:role:dataScope:update`，查询无显式权限 | 迁移到 access data-scope 用例          |
| 菜单目录     | `/menu/*`                                                                          | `system:menu:*`，部分查询无显式权限            | 建立稳定 Admin API 和能力码            |
| 登录授权装载 | `LoginService`、`LoginManager` 直接调用角色 Service                                | 无独立公开契约                                 | 第一批改为 `AccessAuthorizationFacade` |

前端当前由 `apps/hunyuan-system/src/views/system/role/index.vue` 和 `views/system/menu/menu-list.vue` 消费上述入口；API 仍集中在 `api/system/organization.ts` 与 `api/system/menu.ts`，后续迁入独立 access feature。

## 4. 内部消费者

| 消费者                                                                                 | 当前依赖                                                   | 迁移决定                                                                               |
| -------------------------------------------------------------------------------------- | ---------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| `system.login.LoginService`                                                            | 角色与菜单 Service                                         | 已改用 `AccessAuthorizationFacade`                                                     |
| `system.login.LoginManager`                                                            | 角色与菜单 Service                                         | 已改用 `AccessAuthorizationFacade`                                                     |
| `system.datascope.DataScopeViewService`                                                | `AccessDataScopeFacade`                                    | P2 第二子批已完成，不再直接依赖角色 DAO/实体                                           |
| `identity.employee.OrganizationDepartmentScopeAdapter`                                 | `AccessDepartmentScopeFacade`                              | P2 第三子批已完成，不再直接依赖旧 DataScope Service/枚举                               |
| `system.role.RoleController`                                                           | `AccessRoleLifecycleFacade`                                | P2 第四子批已完成，兼容入口与稳定 Admin API 共用角色生命周期用例                       |
| `system.role.RoleMenuController`                                                       | `AccessCapabilityGrantFacade`                              | P2 第五子批已完成，兼容入口与稳定 Admin API 共用角色能力授权用例                       |
| `system.role.AccessAuthorizationFacadeAdapter`                                         | `AccessCapabilityQueryFacade`                              | P2 第六子批已完成，登录授权菜单查询不再依赖旧菜单模型或 `RoleMenuService`              |
| `system.menu.MenuController`                                                           | `AccessMenuCatalogFacade`                                  | P2 第七子批已完成，兼容入口与稳定 Admin API 共用菜单目录生命周期用例                   |
| `system.role.RoleEmployeeController`                                                   | `AccessRoleMembershipFacade`、`AccessRoleAssignmentFacade` | P2 第八子批已完成，兼容入口只负责旧响应转换，不再依赖旧角色员工 Service                |
| `system.role.AccessAuthorizationFacadeAdapter`                                         | `AccessRoleMembershipFacade`                               | P2 第八子批已完成，登录授权角色查询不再依赖旧角色员工 Service                          |
| 角色员工查询                                                                           | access `AccessRoleMember`                                  | P2 第八子批已完成，稳定边界不依赖 identity，且不投影密码字段或已删除员工               |
| `identity.employee.EmployeeAdministrationApplicationService`                           | `AccessRoleAssignmentFacade`                               | P2 第一子批已完成，通过正式命令替换员工角色                                            |
| `system.role.AccessCapabilityGrantFacadeAdapter`、`AccessCapabilityQueryFacadeAdapter` | `AccessMenuQueryFacade`                                    | P2 第十子批已完成，角色能力适配器不再直接依赖菜单 DAO、VO 或实体                       |
| `system.datascope` 运行时与配置                                                        | access `AccessDataScopeType`、`AccessDataScopeViewType`    | P2 第十一子批已完成，历史数值、排序、名称和描述保持不变，旧 datascope owner 枚举已删除 |
| `system.role.RoleMenuController`、`RoleMenuTreeVO`                                     | 角色 owner `RoleCapabilityTreeNodeVO`                      | P2 第十一子批已完成，角色兼容响应不再复用菜单 owner 的 `MenuSimpleTreeVO`              |

仓库外消费者沿用 A3.1 的事实判断：系统尚未投入生产，也未开放正式仓库外集成，因此当前外部消费者审计为 N/A。首次生产或外部集成前仍必须建立消费者登记、调用方标识和访问日志。

## 5. 稳定能力码草案

| 分组       | 能力码                                    |
| ---------- | ----------------------------------------- |
| 角色       | `access.role.read/create/update/delete`   |
| 员工角色   | `access.role.employee.read/assign/remove` |
| 菜单与能力 | `access.capability.read/grant`            |
| 菜单目录   | `access.menu.read/create/update/delete`   |
| 数据范围   | `access.data-scope.read/update`           |

最终 Flyway 映射必须保证旧授权不丢失，并增加“Controller 校验能力码必须存在于能力目录”的一致性测试。

## 6. 实施批次

1. **P0 冻结**：完成所有权、API、权限码、前端入口和消费者账本。
2. **P1 授权查询边界**：建立 `AccessAuthorizationFacade`，登录只通过公开接口装载角色码、能力码和菜单。
3. **P2 后端用例**：迁移角色生命周期、员工角色分配、菜单授权和数据范围；消除 `EmployeeVO` 与跨边界 DAO 依赖。
4. **P3 数据与权限**：新增 Flyway，迁移稳定能力码和角色授权，补数据库唯一约束与一致性守卫。
5. **P4 前端纵切**：建立独立 access feature，迁移角色与菜单管理页面和 API。
6. **P5 兼容退役**：消费者归零后删除旧 `/role/*`、`/menu/*`、`/dataScope/*` 与 `system:role:*`、`system:menu:*`。

## 7. 关闭条件

A3.2 只有同时满足以下条件才能正式关闭：

- 登录、identity、organization 和其他模块只依赖 access 公开接口，不直接调用角色/菜单/数据范围 Service 或 DAO。
- 新 Admin API、稳定能力码、前端 feature 和 Flyway 已完成。
- 角色生命周期、员工角色分配、菜单授权、数据范围和管理员特权都有聚焦测试。
- 能力目录、Controller 权限校验和角色授权数据通过一致性守卫。
- 旧入口、旧权限码、旧前端引用和旧 `EmployeeVO` 消费者归零并退役。
- 开发数据库、直接 API 和浏览器角色矩阵验收通过。

## 8. 当前执行记录

2026-07-22，P0 已完成，P1 第一批和 P2 十一个子批已落地：

- 新增 `access.authorization.api` 的公开授权快照契约。
- 旧角色实现通过适配器提供角色码、能力码和菜单，不向登录暴露角色 Service/DAO。
- `LoginService` 和 `LoginManager` 已改用 `AccessAuthorizationFacade`。
- 新增 ArchUnit 守卫，禁止登录模块重新依赖角色 Service/DAO。
- 新增 `AccessRoleAssignmentFacade` 以及分配、移除、替换员工角色命令，`EmployeeAdministrationApplicationService` 不再依赖 identity 专用角色分配端口。
- 删除 `EmployeeRoleAssignmentPort` 与 `IdentityEmployeeRoleAssignmentAdapter`，角色员工写入统一委托 access 公开命令边界。
- `/role/employee/*` 兼容入口暂时保留，但查询响应已改为 identity `EmployeeSummary`；SQL 不再读取 `login_pwd`，并过滤 `deleted_flag = false`。
- 新增角色分配适配器、角色员工服务、SQL 契约和架构边界测试；聚焦测试共 29 个用例通过，`hunyuan-admin` 聚焦编译通过。
- codebase-memory 新索引 `E-my-project-hunyuan-pro-a3-2-20260722` 确认角色与 identity 已无旧 `EmployeeVO` 消费，`DataScopeViewService` 仍直接调用 `RoleEmployeeDao.selectRoleIdByEmployeeId`。
- 新增 `AccessDataScopeFacade`，由过渡适配器聚合角色成员关系与 `t_role_data_scope`，对外只返回最终可见范围值。
- `DataScopeViewService` 已改用 access 数据范围公开查询边界，员工存在性、管理员特权和旧枚举映射行为保持不变。
- 新增数据范围适配器测试与 ArchUnit 守卫，禁止 `system.datascope` 重新依赖角色 DAO/实体；与前序批次联合运行 36 个聚焦用例全部通过。
- codebase-memory 新索引 `E-my-project-hunyuan-pro-a3-2-p2-2-20260722` 确认 `system.datascope` 对角色 DAO 的直接依赖为零。
- 新增 `AccessDepartmentScopeFacade` 与显式的 `AccessDepartmentScope` 快照，统一表达“全部部门”和“受限部门列表”，不再向消费者暴露旧空列表哨兵语义。
- `OrganizationDepartmentScopeAdapter` 已改用 access 部门范围公开边界，不再直接依赖 `DataScopeViewService`、`DataScopeTypeEnum` 或 `DataScopeViewTypeEnum`；匿名请求继续按无部门权限处理。
- 新增部门范围门面、组织范围适配器测试和 ArchUnit 守卫；联合组织目录、数据范围及前三个 access 适配器运行 34 个聚焦用例全部通过，`hunyuan-admin` 编译通过。
- 新增 `AccessRoleLifecycleFacade`、稳定角色模型、创建/更新命令及失败原因契约；角色生命周期实现直接复用现有 `t_role`、`t_role_employee` 和 `t_role_menu`，未建立镜像表或双写。
- 新增 `/api/admin/v1/access/roles` 查询、创建、更新和删除接口，分别使用 `access.role.read/create/update/delete`；旧 `/role/*` 入口继续保留旧权限码与错误码，但统一委托 access 生命周期用例。
- 删除已无消费者的 `RoleService`，新增 ArchUnit 守卫禁止 `RoleController` 重新依赖旧角色 Service；角色重复校验、成员删除保护、关联清理顺序、新旧 API 契约均有聚焦测试。
- P2 前四个子批联合运行 53 个测试全部通过，其中 ArchUnit 14 条；`hunyuan-admin` 编译 255 个主源码文件，`git diff --check` 通过。
- 新增 `AccessCapabilityGrantFacade`、角色能力授权快照、能力树节点和全量替换命令；继续复用 `t_role_menu` 与 `t_menu`，未建立镜像表或双写。
- 新增 `GET/PUT /api/admin/v1/access/roles/{roleId}/capabilities`，分别使用已冻结的 `access.capability.read` 与 `access.capability.grant`；旧 `/role/menu/*` 路径、`system:role:menu:update` 权限和角色不存在错误码保持不变。
- `RoleMenuController` 已统一委托 access 能力授权用例，新增 ArchUnit 守卫禁止其重新依赖旧角色 Service；`RoleMenuService` 收缩为登录授权菜单查询兼容服务。
- 角色授权全量替换、清空授权、能力树、组织模块关闭过滤以及新旧 API 契约均已增加聚焦测试；第五子批聚焦运行 25 个测试全部通过，P2 五个子批联合回归 64 个测试全部通过，其中 ArchUnit 15 条，`hunyuan-admin` 编译 263 个主源码文件。
- codebase-memory 新索引 `E-my-project-hunyuan-pro-a3-2-p2-5-final-20260722` 包含 13,672 个节点和 35,923 条边，确认稳定 GET/PUT 能力授权路由存在，`RoleMenuService` 只保留登录授权菜单查询职责。
- 新增 `AccessCapabilityQueryFacade`，登录授权聚合只接收稳定 `AccessMenuItem`；旧 `MenuVO`、菜单 DAO 和角色菜单 DAO 均被封装在 `AccessCapabilityQueryFacadeAdapter` 内。
- 管理员功能点查询、非管理员无角色空结果、旧菜单实体映射和组织模块关闭过滤均已迁移并增加聚焦测试；`AccessAuthorizationFacadeAdapter` 新增架构守卫，禁止重新依赖菜单模块或角色 DAO。
- 删除已无生产消费者的 `RoleMenuService` 及其旧测试；P2 六个子批联合回归 71 个测试全部通过，其中 ArchUnit 16 条，`hunyuan-admin` 编译 264 个主源码文件，`git diff --check` 通过。
- codebase-memory 新索引 `E-my-project-hunyuan-pro-a3-2-p2-6-final-20260722` 包含 15,743 个节点和 39,289 条边，确认登录授权调用链已改为 `AccessCapabilityQueryFacade`，`RoleMenuService` 不再存在生产定义或调用。
- 新增 `AccessMenuCatalogFacade`、稳定菜单模型、菜单树节点、创建与更新命令以及显式失败原因契约；继续复用 `t_menu` 和现有授权请求地址清单，未建立镜像表或双写。
- 新增 `/api/admin/v1/access/menus` 列表、详情、树、授权请求地址、创建、更新和批量删除接口，分别使用 `access.menu.read/create/update/delete`；旧 `/menu/*` 路径、HTTP 方法、`system:menu:*` 权限、响应模型和错误码保持不变。
- `MenuController` 已统一委托菜单目录公开用例；同级名称与前端权限串重复校验、更新校验顺序、自父级保护、递归逻辑删除、根节点可达列表、菜单树过滤和授权请求地址透传均已增加聚焦测试。
- 删除已无生产消费者的 `MenuService`，新增 ArchUnit 守卫禁止 `MenuController` 重新依赖旧菜单 Service；P2 七个子批联合回归 85 个测试全部通过，其中 ArchUnit 17 条，`hunyuan-admin` 编译 272 个主源码文件。
- 新增 `AccessRoleMembershipFacade`、access 自有的 `AccessRoleMember`、成员查询条件和员工角色选择快照，避免 access 反向依赖 identity 并形成模块环。
- 新增角色成员稳定 Admin API：角色成员分页查询、候选成员分页查询、全部成员查询、批量分配、批量移除和员工角色选择查询；分别使用 `access.role.employee.read/assign/remove`。
- `RoleEmployeeController` 已统一委托 `AccessRoleMembershipFacade` 与 `AccessRoleAssignmentFacade`，旧 `/role/employee/*` 路径、HTTP 方法、权限、响应模型和空 ID 校验保持兼容；`AccessAuthorizationFacadeAdapter` 的角色查询也已改用公开成员边界。
- 删除已无生产消费者的 `RoleEmployeeService` 及其旧测试；新增 API 契约、适配器、SQL 和 ArchUnit 守卫，确认生产代码不存在旧 Service 引用，access 与 identity 不形成依赖环。
- 第八子批聚焦运行 27 个测试全部通过，其中 ArchUnit 19 条；完整 Maven reactor 回归中 `hunyuan-base` 12 个测试、`hunyuan-admin` 125 个测试均无失败或错误，3 个集成测试按既有配置跳过；`hunyuan-admin` 编译 277 个主源码文件，`git diff --check` 通过。
- 新增 `AccessDataScopeManagementFacade`、稳定数据范围目录、角色配置快照、全量替换命令与显式失败原因；继续复用 `t_role_data_scope`，未建立镜像表或双写。
- 新增 `GET /api/admin/v1/access/data-scopes`、`GET/PUT /api/admin/v1/access/roles/{roleId}/data-scopes`，分别使用 `access.data-scope.read` 与 `access.data-scope.update`；角色不存在、非法枚举和重复配置均在稳定用例中显式拒绝。
- `RoleDataScopeController` 与 `DataScopeController` 已统一委托数据范围稳定管理边界，旧路径、旧写权限和旧空配置错误保持兼容；稳定全量替换允许空集合清空配置，并在同一事务中先删除旧关系再写入新关系。
- 删除已无生产消费者的 `RoleDataScopeService` 与 `DataScopeService`，新增两条 ArchUnit 守卫禁止兼容 Controller 重新依赖旧 Service；第九子批聚焦运行 34 个测试全部通过，其中 ArchUnit 21 条；完整 Maven reactor 回归中 `hunyuan-base` 12 个测试、`hunyuan-admin` 140 个测试均无失败或错误，3 个集成测试按既有配置跳过，`hunyuan-admin` 编译 285 个主源码文件。
- P2 第十子批新增只读 `AccessMenuQueryFacade`，由菜单 owner 统一提供“全部启用菜单”和“指定角色未删除菜单”查询，准确保留管理员与普通角色的既有授权语义；`AccessCapabilityGrantFacadeAdapter`、`AccessCapabilityQueryFacadeAdapter` 改为只消费 access `AccessMenu` 稳定模型。
- 角色菜单 DAO 删除跨 owner 返回 `MenuEntity` 的 `selectMenuListByRoleIdList`，对应联表查询迁入菜单 DAO；新增公开契约、菜单 owner 适配器测试及 ArchUnit 守卫，禁止两个角色能力适配器重新依赖菜单 DAO、VO 或实体。第十子批聚焦运行 38 个测试全部通过，其中 ArchUnit 22 条；完整 Maven reactor 回归中 `hunyuan-base` 12 个测试、`hunyuan-admin` 146 个测试均无失败或错误，3 个集成测试按既有配置跳过，`hunyuan-admin` 编译 287 个主源码文件。
- P2 第十一子批新增 access owner 的 `AccessDataScopeType` 与 `AccessDataScopeViewType`，数据范围运行时、注解、SQL 配置和策略统一使用公开枚举；历史枚举值、排序、名称和描述保持不变，旧 `DataScopeTypeEnum` 与 `DataScopeViewTypeEnum` 已删除。
- 角色菜单兼容响应新增角色 owner 自有的 `RoleCapabilityTreeNodeVO`，`RoleMenuController` 与 `RoleMenuTreeVO` 不再复用菜单 owner 的 `MenuSimpleTreeVO`；`RoleDataScopeEntity` 继续以整数保存类型值，不再通过文档引用 datascope owner 枚举。
- 新增两条非冻结 ArchUnit 守卫，分别禁止 `system.role` 依赖 `system.menu` 和 `system.datascope`；新增枚举值与排序契约测试，并扩展角色能力兼容树 API 契约断言。第十一子批聚焦运行 51 个测试全部通过，其中 ArchUnit 24 条；完整 Maven reactor 回归中 `hunyuan-base` 12 个测试、`hunyuan-admin` 150 个测试均无失败或错误，3 个既有集成测试按配置跳过，`hunyuan-admin` 编译 288 个主源码文件。
- codebase-memory 新索引 `E-my-project-hunyuan-pro-a3-2-p2-11-final-20260722` 包含 13,979 个节点和 37,474 条关系；源码关系复核确认 `system.role` 到 `system.menu`、`system.datascope` 的引用均为零，旧数据范围枚举节点为零，新 access 枚举、角色能力树模型及其消费者均可检索。
- P3 新增 `V3.70.0`，在现有角色管理和菜单管理页面下建立 15 个 `access.*` 稳定能力节点，统一使用 `perms_type = 1`，并保持 `api_perms` 与 `web_perms` 一致。平台管理员获得全部稳定能力；既有角色页面、菜单页面和 `system:role:*`、`system:menu:*` 操作授权按冻结映射复制到新能力，旧权限码继续保留至 P5。
- `t_role_menu(role_id, menu_id)` 与 `t_role_data_scope(role_id, data_scope_type)` 唯一约束已由 `V3.65.0` 建立；`V3.70.0` 增加历史重复数据清理与约束缺失时的幂等补建守卫，避免对已满足约束的数据库重复建索引。
- 新增 `AccessCapabilityDirectoryContractTest`，反射扫描 5 个稳定 access Controller 的 `@SaCheckPermission`，并与冻结的 15 个能力码以及 `V3.70.0` 能力目录做严格集合对账；Flyway 契约测试同步冻结迁移顺序、旧授权映射、唯一约束与“不提前删除旧权限”边界。
- P3 聚焦契约测试 3 个全部通过；完整 Maven reactor 回归中 `hunyuan-base` 12 个测试、`hunyuan-admin` 152 个测试均无失败或错误，3 个集成测试按默认配置跳过，ArchUnit 24 条通过。随后在全新的 `hunyuan_a3_2_p3_it` 隔离库执行真实集成验收，Flyway 成功迁移到 `3.70.0`，`FlywayMigrationTest`、`InitialAdminBootstrapIntegrationTest`、`RedisIsolationTest` 共 3 项全部通过。
- codebase-memory 新索引 `E-my-project-hunyuan-pro-a3-2-p3-final-20260722` 包含 16,050 个节点和 40,866 条关系；新迁移、能力目录一致性守卫和 5 个稳定 access Controller 均可检索。
- P4 新增 `@hunyuan/feature-access`，冻结 `access.management` feature 标识、角色与菜单两个应用路由以及 15 个稳定能力码；feature 统一拥有角色、能力授权、数据范围、角色成员、菜单页面、稳定契约类型和请求客户端。
- 应用内 `views/system/role/index.vue` 与 `views/system/menu/menu-list.vue` 已收缩为薄入口，仅负责注入由 `requestClient` 创建的 access 客户端并渲染 feature 页面；原有密集布局、角色权限矩阵、数据范围、员工列表、菜单树、层级保护和紧凑操作区均保留在 feature 内。
- 前端角色生命周期、能力授权、数据范围、角色成员和菜单生命周期统一调用 `/admin/v1/access/*`；DELETE 批量移除成员和菜单均通过请求 body 传递 ID 集合，角色、菜单文本字段和成员查询关键词在客户端边界裁剪，ID 集合在请求前去重。
- 删除旧 `apps/hunyuan-system/src/api/system/menu.ts`；`api/system/organization.ts` 删除角色、角色能力、数据范围和角色成员兼容封装，只保留岗位 API。应用与 access feature 不再引用旧 `/role/*`、`/menu/*`、`/dataScope/*` 前端路径。
- 新增 `packages/features/access/src/client.test.ts`，并更新岗位 payload 测试和组织模块页面边界测试；3 个测试文件共 21 项通过。`@hunyuan/system` 的 `vue-tsc --noEmit --skipLibCheck` 通过，P4 改动文件 ESLint 通过。
- codebase-memory 新索引 `E-my-project-hunyuan-pro-a3-2-p4-final-20260722` 包含 16,064 个节点和 40,920 条关系；图检索确认角色与菜单应用入口均为 17 行薄装配，access 生产前端只使用 `/admin/v1/access`，冻结能力目录严格包含 15 个 `access.*` 能力码。
- P5 删除 `RoleController`、`RoleEmployeeController`、`RoleMenuController`、`RoleDataScopeController`、`MenuController`、`DataScopeController` 六个旧 Controller，并删除无剩余生产消费者的旧角色、菜单和数据范围兼容 DTO、Service 与枚举；稳定 access Controller 和公开 Facade 保持唯一生产入口。
- 新增 `V3.71.0` 退役迁移，删除旧角色授权关系及旧 `system:role:*`、`system:menu:*` 权限节点。开发库 `hunyuan` 已迁移至 Flyway `3.71.0`，对账结果为稳定 access 能力 15 个、旧权限节点 0 个、旧授权关系 0 条、`platform_admin` 稳定授权 15 个。
- 旧 `/role/*`、`/menu/*`、`/dataScope/*` 代表路径运行验收均返回 HTTP 404；OpenAPI 中稳定 access 路由 13 条、旧路由 0 条，干净构建产物 `target/classes` 中旧 Controller 字节码为 0。
- 角色管理和菜单管理已在真实浏览器中通过运行验收。角色页可正常展示角色列表、功能权限、数据范围和员工列表；菜单页可正常查询、新增和展示真实菜单树数据。两个页面均无控制台错误。
- 工作区源包曾导致开发态加载多个 Vue 运行时，角色页出现 `inject() can only be used inside setup()`；应用 Vite 配置已通过 `resolve.dedupe` 统一复用 `vue` 与 `element-plus`，清理缓存并恢复工作区依赖后问题消失。
- 仓库内生产消费者已归零。旧名称和路径仅保留在退役负向契约测试、Flyway 历史迁移与关闭文档中；`apps/web-ele` 和 backend-mock 的 `/menu/*` 属于非目标 demo/mock 示例，不是 `@hunyuan/system` 生产消费者或兼容入口。
- 仓库外消费者继续基于“系统从未投入生产、未开放正式仓库外集成”的事实判定为不适用（N/A）；首次生产或外部集成前仍须建立消费者登记、调用方标识和访问日志机制。
- P5 最终回归中，前端 3 个相关测试文件共 21 项通过，`@hunyuan/system` 类型检查和改动文件 ESLint 通过；完整 Maven reactor 中 `hunyuan-base` 12 项、`hunyuan-admin` 140 项均无失败或错误，3 个外部环境集成测试按配置跳过，ArchUnit 18 项通过。
- 最终 codebase-memory 全量索引 `E-my-project-hunyuan-pro-a3-2-p5-closed-20260722` 已持久化到 `.codebase-memory/graph.db.zst`；反查确认生产稳定接口统一为 `/api/admin/v1/access/*`，旧 Controller、旧路径和旧权限码没有生产消费者。

当前结论：**A3.2 的 P0-P5 已全部完成并正式关闭。角色、员工角色分配、菜单与能力授权、数据范围及登录授权装载已统一收敛到 access 公开边界；下一步进入 A3.3 岗位目录迁移。**
