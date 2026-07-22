# A3.2 访问控制契约与消费者冻结

## 1. 状态与目标

截至 2026-07-22，A3.1 已正式关闭，A3.2 进入实施。A3.2 由 `access` 统一拥有角色、员工角色分配、菜单与能力授权、数据范围以及登录授权装载边界。

本阶段保持 `t_role`、`t_menu`、`t_role_employee`、`t_role_menu`、`t_role_data_scope` 为唯一数据源，不创建镜像表，不建立新旧双写。

## 2. 所有权冻结

| 对象 | 目标 owner | 决策 |
| --- | --- | --- |
| `t_role` | `access.role` | 角色生命周期和稳定角色编码 |
| `t_role_employee` | `access.role` | 员工角色分配 |
| `t_menu` | `access.capability` | 导航菜单、功能点和能力码目录 |
| `t_role_menu` | `access.capability` | 角色菜单与能力授权 |
| `t_role_data_scope` | `access.data-scope` | 角色数据范围 |
| Sa-Token 会话与登录缓存 | `identity.authentication` | 只消费 access 公开授权快照 |

## 3. 现有入口冻结

| 能力 | 当前入口 | 当前权限 | 处置 |
| --- | --- | --- | --- |
| 角色生命周期 | `/role/add`、`/role/update`、`/role/delete/{id}`、`/role/get/{id}`、`/role/getAll` | `system:role:add/update/delete` | 迁移到 `/api/admin/v1/access/roles` |
| 员工角色分配 | `/role/employee/*` | `system:role:employee:*`，部分查询无显式权限 | 迁移并消除 `EmployeeVO` |
| 角色菜单授权 | `/role/menu/*` | `system:role:menu:update`，查询无显式权限 | 迁移到 access capability 用例 |
| 数据范围 | `/dataScope/list`、`/role/dataScope/*` | `system:role:dataScope:update`，查询无显式权限 | 迁移到 access data-scope 用例 |
| 菜单目录 | `/menu/*` | `system:menu:*`，部分查询无显式权限 | 建立稳定 Admin API 和能力码 |
| 登录授权装载 | `LoginService`、`LoginManager` 直接调用角色 Service | 无独立公开契约 | 第一批改为 `AccessAuthorizationFacade` |

前端当前由 `apps/hunyuan-system/src/views/system/role/index.vue` 和 `views/system/menu/menu-list.vue` 消费上述入口；API 仍集中在 `api/system/organization.ts` 与 `api/system/menu.ts`，后续迁入独立 access feature。

## 4. 内部消费者

| 消费者 | 当前依赖 | 迁移决定 |
| --- | --- | --- |
| `system.login.LoginService` | 角色与菜单 Service | 已改用 `AccessAuthorizationFacade` |
| `system.login.LoginManager` | 角色与菜单 Service | 已改用 `AccessAuthorizationFacade` |
| `system.datascope.DataScopeViewService` | `AccessDataScopeFacade` | P2 第二子批已完成，不再直接依赖角色 DAO/实体 |
| `identity.employee.OrganizationDepartmentScopeAdapter` | `AccessDepartmentScopeFacade` | P2 第三子批已完成，不再直接依赖旧 DataScope Service/枚举 |
| `system.role.RoleController` | `AccessRoleLifecycleFacade` | P2 第四子批已完成，兼容入口与稳定 Admin API 共用角色生命周期用例 |
| `system.role.RoleMenuController` | `AccessCapabilityGrantFacade` | P2 第五子批已完成，兼容入口与稳定 Admin API 共用角色能力授权用例 |
| `system.role.AccessAuthorizationFacadeAdapter` | `AccessCapabilityQueryFacade` | P2 第六子批已完成，登录授权菜单查询不再依赖旧菜单模型或 `RoleMenuService` |
| `system.menu.MenuController` | `AccessMenuCatalogFacade` | P2 第七子批已完成，兼容入口与稳定 Admin API 共用菜单目录生命周期用例 |
| `system.role.RoleEmployeeController` | `AccessRoleMembershipFacade`、`AccessRoleAssignmentFacade` | P2 第八子批已完成，兼容入口只负责旧响应转换，不再依赖旧角色员工 Service |
| `system.role.AccessAuthorizationFacadeAdapter` | `AccessRoleMembershipFacade` | P2 第八子批已完成，登录授权角色查询不再依赖旧角色员工 Service |
| 角色员工查询 | access `AccessRoleMember` | P2 第八子批已完成，稳定边界不依赖 identity，且不投影密码字段或已删除员工 |
| `identity.employee.EmployeeAdministrationApplicationService` | `AccessRoleAssignmentFacade` | P2 第一子批已完成，通过正式命令替换员工角色 |

仓库外消费者沿用 A3.1 的事实判断：系统尚未投入生产，也未开放正式仓库外集成，因此当前外部消费者审计为 N/A。首次生产或外部集成前仍必须建立消费者登记、调用方标识和访问日志。

## 5. 稳定能力码草案

| 分组 | 能力码 |
| --- | --- |
| 角色 | `access.role.read/create/update/delete` |
| 员工角色 | `access.role.employee.read/assign/remove` |
| 菜单与能力 | `access.capability.read/grant` |
| 菜单目录 | `access.menu.read/create/update/delete` |
| 数据范围 | `access.data-scope.read/update` |

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

2026-07-22，P0 已完成，P1 第一批和 P2 前八个子批已落地：

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

当前结论：**A3.2 已正式启动，P0、P1 第一批和 P2 前八个子批完成；A3.2 尚未关闭。下一步继续 P2，盘点并收口剩余跨边界 Service/DAO 消费。**
