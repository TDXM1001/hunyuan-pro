# A3.1 员工契约与消费者冻结

## 1. 文档定位

本文是 A3.1 的 P0 执行基线，冻结员工与账号管理迁移开始前的：

- `t_employee` 数据语义和所有权。
- 旧员工 API、权限与前端入口。
- 后端、前端和数据库消费者。
- 员工模块对组织、岗位、角色、认证、安全和文件能力的依赖。
- 超级管理员、软删除、唯一性和一次性密码交付规则。
- 禁止新增遗留直连的自动守卫。

P0 不新增新 API，不修改菜单、权限或数据库，不改变当前登录和员工管理运行态。后续 P1 至 P4 必须以本文为迁移输入；发现遗漏消费者时先补充账本，再继续删除或切换入口。

## 2. 事实来源与刷新规则

本次盘点同时使用：

- 当前 Git checkout 中的 Java、Vue、TypeScript、Mapper 和 Flyway 文件。
- 本地 `codebase-memory-mcp` 项目 `E-my-project-hunyuan-pro-current` 的架构簇、符号和依赖查询。
- 现有 ArchUnit 冻结存储。

`codebase-memory-mcp` 用于快速发现高连接调用簇和候选消费者，源码搜索用于确认具体导入、路由和 SQL。两者不一致时，以当前源码、数据库迁移和运行态证据为准。

以下情况必须刷新索引并重新执行源码盘点：

- 开始 P1 前当前 checkout 已发生员工、登录、角色、岗位、数据范围或 OA 变更。
- 准备删除旧 API、旧页面或旧类型。
- ArchUnit 冻结基线出现意外新增或减少。
- 仓库外消费者登记发生变化。

## 3. `t_employee` 所有权与字段语义

`t_employee` 保持员工与账号的唯一数据源和唯一写路径。A3.1 不创建镜像表，不做新旧双写，不拆分历史数据。

| 字段 | 当前语义 | A3.1 决策 |
| --- | --- | --- |
| `employee_id` | 数据库主键 | 保持内部稳定标识 |
| `employee_uid` | 员工 UUID，也是现有密码加盐输入 | 保持不可变；不得向通用摘要暴露其密码用途 |
| `login_name` | 管理后台登录名 | 全表唯一，软删除后也不允许自动复用 |
| `login_pwd` | 加盐后的密码摘要 | 仅认证和密码安全协作可访问，不进入公开 DTO |
| `actual_name` | 员工显示姓名 | 员工资料字段，不作为唯一身份标识 |
| `avatar` | 文件引用 | 由员工保存引用，通过文件公开服务解析 |
| `gender` | 性别枚举 | 保持现有枚举语义 |
| `phone` | 手机号 | 应用层校验唯一；P2 暂不新增数据库唯一约束，待真实业务证据确认 |
| `email` | 邮箱 | 应用层校验唯一；P2 暂不新增数据库唯一约束，待真实业务证据确认 |
| `department_id` | 当前所属部门 | 必填，只引用 `organization.department` |
| `position_id` | 当前岗位 | 可空，只引用 `organization.position` |
| `disabled_flag` | 禁止登录与使用账号 | 显式启用/禁用动作，不再使用状态翻转接口 |
| `deleted_flag` | 兼容软删除 | 不物理删除，不自动释放登录名、手机号或邮箱 |
| `administrator_flag` | 超级管理员兜底身份 | 普通员工管理用例只读，不允许修改 |
| `remark` | 管理备注 | 仅管理场景可见 |
| `create_time`、`update_time` | 审计时间 | 保持数据库维护语义 |

当前表已有 `employee_uid` 与 `login_name` 唯一约束；`phone` 和 `email` 继续依赖应用层先查后写，并且创建、更新使用 JVM 内 `synchronized`。P2 增加目录查询组合索引，但未在缺少业务证据时新增手机号、邮箱唯一约束；不得把 `synchronized` 当作并发唯一性保证。

## 4. 状态与安全决策

### 4.1 超级管理员保护

当 `administrator_flag = 1` 时：

- 普通员工管理 API 不得删除、禁用或修改该标记。
- 不得通过普通角色分配流程使超级管理员失去平台兜底访问能力。
- 密码重置只允许另一个已认证超级管理员执行；若不存在第二个超级管理员，使用受控 bootstrap 恢复流程。
- 修改登录名、手机号、邮箱或组织归属必须记录操作审计。

这些规则必须在 application 用例中执行，不能只通过前端隐藏按钮实现。

### 4.2 软删除

- 删除继续表示 `deleted_flag = 1`，不执行物理删除。
- 删除前检查超级管理员保护和已登记关联。
- 删除成功后立即清理登录缓存并注销现有会话。
- 已删除账号不能登录、不能出现在默认目录查询中。
- 历史登录名、手机号和邮箱默认不复用；若未来真实业务要求复用，必须另立带审计和冲突处理的决策。

### 4.3 启用与禁用

- 新 API 分离 `enable` 与 `disable`，不根据当前值翻转状态。
- 禁用后立即清理账号缓存并注销全部现有会话。
- 重复启用或重复禁用采用幂等成功语义。
- 已删除员工不能被普通启用动作恢复。

### 4.4 初始密码与管理员重置

- 创建和重置产生的随机密码只能在成功响应中向有权限的管理员展示一次。
- 明文密码不得写入数据库、日志、操作日志、异常、测试快照或文档。
- 新 API 不再以裸 `String` 作为长期响应契约，应返回明确的一次性凭据结果。
- 一次性凭据响应必须带禁止缓存语义，前端关闭展示后不能再次读取。
- “首次登录或重置后必须修改密码”的持久化状态暂不在 P2 增加；如真实业务确认需要，另立密码策略变更批次，认证模块只消费公开状态，不在 A3.1 重写认证协议。
- 密码生成、复杂度、历史重复校验和摘要计算继续复用 `SecurityPasswordService`。

## 5. 旧员工 API 冻结

| 方法与路径 | 当前用途 | 当前权限 | 目标处置 |
| --- | --- | --- | --- |
| `POST /employee/query` | 员工分页查询 | 无显式员工读权限 | 委托新查询用例，补 `identity.employee.read` |
| `POST /employee/add` | 创建员工并返回随机密码 | `system:employee:add` | 委托新创建用例 |
| `POST /employee/update` | 更新员工并同步角色 | `system:employee:update` | 员工更新与角色分配拆开 |
| `POST /employee/update/center` | 个人中心更新 | 登录用户 | 暂留兼容，不纳入员工管理页面迁移 |
| `POST /employee/update/avatar` | 更新本人头像 | 登录用户 | 暂留兼容，通过文件公开服务协作 |
| `GET /employee/update/disabled/{employeeId}` | 翻转启停状态 | `system:employee:disabled` | 由显式 enable/disable 替代 |
| `POST /employee/update/batch/delete` | 批量软删除 | `system:employee:delete` | 委托兼容删除用例 |
| `POST /employee/update/batch/department` | 批量调部门 | `system:employee:department:update` | 委托部门分配用例并补部门校验 |
| `POST /employee/update/password` | 本人修改密码 | 登录用户 | 暂留兼容，不与管理员重置混用 |
| `GET /employee/getPasswordComplexityEnabled` | 查询密码复杂度开关 | 登录用户 | 迁往安全能力公开接口 |
| `GET /employee/update/password/reset/{employeeId}` | 管理员重置密码 | `system:employee:password:reset` | 改为 POST 并执行超级管理员保护 |
| `GET /employee/getAllEmployeeByDepartmentId/{departmentId}` | 部门员工列表 | 无显式员工读权限 | 改由公开员工目录 Facade 或新读 API 提供 |
| `GET /employee/queryAll` | 全量员工列表 | 无显式员工读权限 | 消费者迁移后退役，禁止继续扩散 |

兼容期内旧 Controller 必须委托与新 API 相同的 application 用例。禁止维护两套校验、事务或写路径。

## 6. 历史权限冻结

| 历史权限 | 目标能力 | 处理阶段 |
| --- | --- | --- |
| `system:employee:add` | `identity.employee.create` | P2 映射，P4 退役 |
| `system:employee:update` | `identity.employee.update` | P2 映射，P4 退役 |
| `system:employee:disabled` | `identity.employee.enable`、`identity.employee.disable` | P2 拆分映射，P4 退役 |
| `system:employee:department:update` | `identity.employee.department.assign` | P2 映射，P4 退役 |
| `system:employee:password:reset` | `identity.employee.password.reset` | P2 映射，P4 退役 |
| `system:employee:delete` | `identity.employee.delete` | P2 映射，P4 退役 |

当前员工查询接口没有显式页面读取权限。P1 新查询 API 必须校验 `identity.employee.read`，并在 P2 将员工页面菜单映射到该能力。

角色分配不归员工能力组所有。员工表单中的 `roleIdList` 只作为历史兼容输入，P1 新员工更新契约不得继续把角色写入作为员工聚合内部行为。

## 7. 后端消费者冻结

### 7.1 直接依赖旧员工实现

| 消费者 | 当前直连 | 所需最小公开语义 | 迁移顺序 |
| --- | --- | --- | --- |
| `system.login.service.LoginService` | `EmployeeService`、`EmployeeEntity` | 按登录名读取账号认证信息、读取状态 | P1 第一批 |
| `system.login.manager.LoginManager` | `EmployeeService`、`EmployeeEntity` | 按 ID 读取登录主体摘要 | P1 第一批 |
| `system.datascope.service.DataScopeViewService` | `EmployeeDao`、`EmployeeEntity` | 读取员工部门和有效状态 | P1/P2 |
| `system.role.controller.RoleEmployeeController` | `EmployeeVO` | 暴露角色下员工与候选员工的兼容响应 | A3.2 迁移 |
| `system.role.service.RoleEmployeeService` | `EmployeeVO` | 查询角色员工并装配部门名称 | A3.2 迁移 |
| `system.role.dao.RoleEmployeeDao` | `EmployeeVO` | 联表读取角色员工与候选员工 | A3.2 迁移 |
| `business.oa.notice.service.NoticeService` | `EmployeeDao`、`EmployeeEntity` | 批量校验员工并读取显示摘要 | P1 后评估 OA 去留 |
| `business.oa.notice.service.NoticeEmployeeService` | `EmployeeService`、`EmployeeEntity` | 读取当前员工摘要 | P1 后评估 OA 去留 |

上述类是 ArchUnit 冻结基线中的已知例外。允许逐项减少，不允许增加新的类或新的直接依赖。

### 7.2 员工模块内部对外直连

| 当前实现 | 直连对象 | 目标端口 |
| --- | --- | --- |
| `EmployeeService` | `RoleEmployeeDao` | `access` 的角色摘要与分配用例 |
| `EmployeeManager` | `RoleEmployeeDao`、`RoleEmployeeService`、`RoleEmployeeEntity` | `access` 的角色分配端口 |
| `EmployeeService` | `PositionDao`、`PositionEntity` | 岗位只读目录端口 |
| `EmployeeService` | `LoginService` | 账号缓存和会话失效端口 |
| `EmployeeService` | `OrganizationDepartmentFacade` | 保持公开组织 Facade |
| `EmployeeService` | `SecurityPasswordService` | 保持安全公开服务 |
| `EmployeeService` | `IFileStorageService` | 保持文件公开服务 |

P1 优先消除登录与 organization 对员工旧实现的反向依赖，并把员工用例对登录、岗位和角色的调用改为窄端口。角色模型和岗位模型的完整迁移仍分别属于 A3.2、A3.3。

### 7.3 数据库与启动消费者

| 消费者 | 当前行为 | 决策 |
| --- | --- | --- |
| `InitialAdminBootstrapService` | 直接查询和插入 `t_employee` | 保留为受控启动例外；P1 评估改用 bootstrap 专用 identity 端口 |
| `DepartmentPersistenceMapper` | 为部门负责人名称关联 `t_employee` | P1 改为部门数据与员工摘要分步装配，或登记只读 SQL 例外 |
| `RoleEmployeeMapper.xml` | 联表读取员工与角色 | A3.2 由 access 所有，先保持 |
| `EnterpriseEmployeeMapper.xml` | 企业员工关系查询 | A3.4 评估 OA 去留后处理 |
| Flyway 与集成测试 | 建表、seed、迁移和验证 | 属于结构与验收事实来源，不视为运行态跨模块消费者 |

直接 SQL 不受 Java 依赖守卫覆盖，因此删除或重命名字段前必须再次执行 `t_employee` 全仓搜索。

## 8. 前端入口与消费者冻结

当前员工能力仍位于：

- API 聚合文件：`apps/hunyuan-system/src/api/system/organization.ts`。
- 页面：`apps/hunyuan-system/src/views/system/employee`。
- 路由菜单：`/organization/employee`，组件 `/system/employee/index.vue`。

当前同一 API 文件还混合部门、岗位、角色、角色员工和员工请求。P3 建立 `@hunyuan/feature-identity-employee` 后：

- 员工类型、API 适配、页面、组件和权限判断迁入 feature。
- 应用只保留路由注册和薄装配。
- 部门选择复用 organization 公开客户端。
- 岗位和角色通过各自客户端或兼容端口获取。
- 旧页面测试在新 feature 验收完成前保留，P4 随旧页面一起退役。

## 9. 自动冻结守卫

`ArchitectureGuardTest` 增加规则：

```text
LEGACY_EMPLOYEE_DEPENDENCIES_MUST_NOT_GROW
```

规则语义：

- `system.employee` 包内部允许在兼容期继续协作。
- 包外对旧 `system.employee` 任意类型的直接依赖按当前明细冻结。
- 删除已有依赖是允许的。
- 新增消费者、新增旧类型依赖或把依赖转移到另一遗留类会使测试失败。
- 新公开接口应放入目标 `identity.employee.api` 边界，不加入旧包冻结例外。

冻结存储必须提交到 `src/test/resources/archunit-store`，并保持：

```properties
freeze.store.default.allowStoreCreation=false
freeze.refreeze=false
```

不得通过开启自动重建或 refreeze 来绕过新增违规。

## 10. P1 输入与完成门

P1 首批实现顺序固定为：

1. 建立 `identity.employee.api` 的账号认证 DTO、员工摘要 DTO 和查询 Facade。
2. 建立 employee application 查询与命令用例，旧 Controller 开始委托。
3. 登录模块改为只消费账号公开接口。
4. organization 的员工查询适配改为只消费员工公开接口。
5. 建立 `/api/admin/v1/identity/employees` 的读接口和 `identity.employee.read` 校验。
6. 再实现创建、更新、显式启停、部门分配和管理员密码重置。

进入 P2 前必须证明：

- 新旧员工管理写入口共享同一 application 用例。
- 登录不再依赖 `EmployeeService` 或 `EmployeeEntity`。
- 新增代码没有扩大 ArchUnit 冻结基线。
- 新接口不暴露 `login_pwd`、密码盐用途、内部管理员标记或未声明个人信息。
- 超级管理员、禁用、软删除和一次性密码规则已有聚焦测试。

## 11. P0 完成结论

P0 完成的含义是“迁移边界已经可执行和可防扩散”，不是员工与账号管理已经迁移完成。

当前运行态仍使用旧员工页面、`/employee/*` API、`system:employee:*` 权限和 `system.employee` 实现。只有 P1 至 P4 的代码、Flyway、权限、API、直接调用和浏览器验收全部关闭后，A3.1 才能报告完成。

## 12. P1 首批后端纵切执行记录

执行日期：2026-07-21。

本批已完成：

- 建立 `identity.employee.api` 的 `EmployeeSummary`、专用 `EmployeeAuthenticationAccount`、`EmployeeDirectoryFacade` 和密码加盐协作工具。
- 建立 application 查询服务、domain repository 和基于 `t_employee` 的 MyBatis-Plus 持久化适配器；未新增表、镜像数据或双写。
- 新增 `POST /api/admin/v1/identity/employees/query`，校验 `identity.employee.read`，返回模型不含密码摘要、员工 UID、删除标记和超级管理员标记。
- `LoginService`、`LoginManager` 已改用认证账号公开契约，保留万能密码、密码盐格式、双因子邮箱、Sa-Token loginId、登录日志、缓存和角色菜单加载行为。
- organization 的 `OrganizationDirectoryPort` 实现迁入 identity 基础设施，通过公开 Facade 查询员工，不再直接访问旧 `EmployeeDao`。
- 部门占用统计继续统计全部未删除员工，禁用员工不会使部门变为可删除。
- 旧 `EmployeeService.generateSaltPassword` 委托统一密码盐实现，避免登录迁移后产生两套格式。

验证结果：

- `EmployeeDirectoryApplicationServiceTest`、`EmployeeDirectoryApiContractTest`、`EmployeePasswordSaltTest`、`LoginManagerIdentityEmployeeTest`、`LegacyEmployeeConsumerRetirementTest`：7 个测试全部通过。
- `ArchitectureGuardTest`：8 条规则全部通过。
- 合计 15 个测试通过，0 失败；ArchUnit 冻结明细已自然移除登录和 organization 的旧员工依赖，没有开启 refreeze。

本批未完成，因此 P1 仍为进行中：

- 旧 `/employee/*` Controller 尚未全部委托新 application 写用例。
- 创建、更新、显式启用/禁用、部门分配、删除和管理员密码重置尚未迁入新边界。
- `DataScopeViewService`、角色模块和 OA 通知消费者仍在冻结例外中。
- 新能力码的 Flyway 菜单/角色映射、OpenAPI 文件、前端 feature 和浏览器验收属于后续批次。

## 13. P1 第二批后端纵切执行记录

执行日期：2026-07-22。

本批已完成：

- 建立 `EmployeeAdministrationFacade` 与 `EmployeeAdministrationApplicationService`，覆盖创建、资料更新、显式启用、显式禁用、批量部门分配、软删除和管理员密码重置。
- 新更新契约不包含禁用状态、超级管理员标记或角色 ID；旧表单的 `disabledFlag` 和 `roleIdList` 只由兼容方法接收。
- 新增 `/api/admin/v1/identity/employees` 写接口及独立能力校验：`create`、`update`、`enable`、`disable`、`department.assign`、`delete`、`password.reset`。
- 旧 `/employee/add`、`/employee/update`、状态翻转、批量部门调整、批量删除和管理员密码重置已委托同一 application 用例；个人中心、头像和本人修改密码继续留在兼容边界。
- 创建和管理员重置返回明确的 `EmployeeOneTimeCredential`；新旧凭据响应均设置 `Cache-Control: no-store` 和 `Pragma: no-cache`。
- 超级管理员不能被普通员工管理用例禁用或删除；超级管理员密码只能由另一个未禁用、未删除的超级管理员重置。
- 禁用、软删除和密码重置通过 `EmployeeSessionPort` 清理登录缓存并注销现有会话；重复启用或禁用保持幂等成功；已软删除员工不能被重新启用。
- 角色兼容写入通过 `EmployeeRoleAssignmentPort` 协作，identity application 不依赖角色 DAO、Entity 或 Service；会话失效同样不依赖登录实现。
- 批量部门分配先校验部门存在，并要求全部目标员工存在且未删除；成功后统一更新并清理员工缓存。

验证结果：

- `EmployeeDirectoryApiContractTest`：4 个测试通过，覆盖新路径、能力码、更新契约和一次性凭据禁止缓存。
- `EmployeeAdministrationApplicationServiceTest`：10 个测试通过，覆盖创建、超级管理员保护、启停幂等、删除态、部门分配、删除和密码重置。
- `LegacyEmployeeConsumerRetirementTest`：2 个测试通过，除首批消费者守卫外，新增六个遗留写方法不得恢复 DAO、角色或会话直写的源码守卫。
- `EmployeeDirectoryApplicationServiceTest`、`EmployeePasswordSaltTest`、`LoginManagerIdentityEmployeeTest`：5 个测试通过。
- `ArchitectureGuardTest`：8 条规则全部通过；冻结存储保持 `freeze.refreeze=false`，未通过自动重建绕过新增违规。
- 合计 29 个测试通过，0 失败；`hunyuan-admin` 连同依赖模块编译成功。

本批完成后仍不能关闭 P1：

- `DataScopeViewService` 仍直接读取旧员工 DAO/Entity，需要迁入公开员工边界。
- 角色查询仍返回旧 `EmployeeVO`，按 A3.2 所有权处理；本批只有角色分配兼容端口，不代表角色模块已经迁移。
- OA 通知消费者仍在员工遗留依赖冻结明细中，需要结合 OA 去留决策迁移或退役。
- OpenAPI 文件尚未完成新员工管理契约收口。
- P2 能力数据与数据库迁移已经在后续批次完成；P3 前端 feature 和浏览器验收、P4 旧入口退役仍未完成。

当前结论：**截至本节记录时，A3.1 P1 第二批后端纵切完成；后续 P1 第三批与 P2 记录见下文。**

## 14. P1 第三批后端纵切执行记录

执行日期：2026-07-22。

本批已完成：

- `DataScopeViewService` 改用 `EmployeeDirectoryFacade` 读取员工协作摘要与部门员工 ID，不再直接依赖旧 `EmployeeDao` 或 `EmployeeEntity`；数据范围角色 DAO 仍留在 access 所有权内。
- `NoticeService` 改用公开员工协作摘要批量校验可见员工并装配姓名，保持已软删除但仍被历史通知引用的兼容读取语义。
- `NoticeEmployeeService` 改用公开员工协作摘要读取当前员工的部门与管理员标记，继续通过 organization 公开 Facade 获取本部门及下级部门。
- `LegacyEmployeeConsumerRetirementTest` 的守卫范围覆盖登录、organization、数据范围和 OA 通知，禁止这些已迁移消费者重新引入旧 `system.employee` 实现。
- 新 Admin API 的 OpenAPI 契约在仓库注解/反射测试层完成收口，覆盖版本化路径、operationId、能力码、操作说明、公开响应字段以及一次性凭据禁止 HTTP 缓存。
- ArchUnit 冻结明细自然缩减为角色查询对旧 `EmployeeVO` 的依赖；该边界属于 A3.2，本批未迁移角色查询或授权模型，也未开启 refreeze。

验证结果：

- 聚焦测试 `DataScopeViewServiceTest`、`NoticeServiceEmployeeCollaborationTest`、`NoticeEmployeeServiceCollaborationTest`、`EmployeeDirectoryApiContractTest`、`LegacyEmployeeConsumerRetirementTest`：13 项通过，0 失败。
- 完整执行 `hunyuan-admin -am test`：共执行 64 项，61 项通过、3 项按环境配置跳过、0 失败；`hunyuan-base` 与 `hunyuan-admin` 均构建成功。
- `ArchitectureGuardTest`：8 条规则全部通过；冻结配置继续保持 `freeze.store.default.allowStoreCreation=false` 和 `freeze.refreeze=false`。
- Maven settings 第 235 行仍有已知格式警告，不影响本次构建与测试结论。

P1 完成后的剩余边界：

- 角色查询、角色员工候选和授权模型仍返回旧 `EmployeeVO`，由 A3.2 处理，不计入 A3.1 P1 的员工消费者迁移。
- P2 已落地 `V3.68.0`：保留并验证 `login_name`、`employee_uid` 的数据库唯一约束，增加员工目录查询组合索引；员工页面下补齐 `identity.employee.*` 能力菜单，平台管理员和既有 `system:employee:*` 角色授权已迁移映射，旧能力仍保留至 P4。手机号、邮箱没有在缺少业务证据时被强制设为全局唯一；员工状态字段也没有使用本机 MySQL 8.0.12 不可靠的 `CHECK` 强制语义。隔离 MySQL Flyway 回放由 `FlywayMigrationTest` 控制，需显式启用环境变量后验收。
- P3 的前端 feature、运行态 OpenAPI、直接 API 与浏览器验收尚未完成。
- P4 的旧 `/employee/*`、`system:employee:*`、旧页面和兼容类型退役尚未完成。

当前结论：**A3.1 P1 三批后端纵切与 P2 数据库/授权批次已完成；下一阶段进入 P3，A3.1 仍未关闭。**

## 15. P3 前端员工目录迁移执行记录

截至 2026-07-22，员工管理页面已完成第一阶段前端切换：

- 新增 `apps/hunyuan-system/src/api/system/identity-employee.ts`，对应 `/api/admin/v1/identity/employees` 的查询、创建、更新、显式启停、部门分配、批量删除和密码重置接口。
- 员工列表将新公开摘要中的 `disabled` 适配为页面状态字段 `disabledFlag`；一次性密码按 `temporaryPassword` 展示，不再假设旧接口返回裸字符串。
- 员工表单不再提交历史 `roleIdList`，角色分配继续留在 A3.2 边界；职位只作为员工资料的引用字段。
- 页面操作已接入 `identity.employee.*` capability code，包括创建、更新、启用、禁用、部门分配、删除和密码重置。
- 新 API payload 契约测试、员工表格契约测试和组织模块回归测试共 25 项通过；`@hunyuan/system` `vue-tsc --noEmit --skipLibCheck` 通过。

本批仍未关闭 P3：

- 当前先建立在应用侧的专用 API 边界，尚未抽取为独立 `@hunyuan/feature-identity-employee` 包。
- 运行态 OpenAPI、直接 API 权限矩阵、平台管理员/只读角色/无模块角色和浏览器流程尚未验收。
- P4 旧 `/employee/*`、旧页面兼容引用和 `system:employee:*` 退役仍未完成。

当前结论：**P3 前端第一阶段完成，A3.1 仍进行中；下一步是启动前后端并完成直接 API 与浏览器验收。**

## 16. P3 运行态管理员验收记录

执行日期：2026-07-22。

本批已完成：

- 在本地 `hunyuan` 数据库真实执行 `V3.68.0`。Flyway 历史记录为成功状态，员工能力菜单 `identity.employee.read/create/update/enable/disable/department.assign/delete/password.reset` 已落库；迁移兼容同一角色持有多个历史员工权限，并兼容首次失败后组合索引已经存在的恢复场景。
- 使用开发环境后端 `http://127.0.0.1:1024` 与前端 `http://127.0.0.1:5788` 完成真实登录和员工页面验收。重新登录后管理员会话获得新能力，员工列表加载 11 条数据。
- 浏览器确认页面展示 `新增员工`、`编辑`、`启用/停用`、`重置密码`、`删除`、`批量删除` 和 `批量转移部门` 等受能力控制的操作入口。
- 浏览器打开并核对新增员工、编辑员工和批量转移部门弹窗；所有弹窗均只做读取验收，没有提交创建、修改、删除、启停、密码重置或部门转移。
- 关键字查询以“胡克”返回 1 条；机构树选择“开发部”返回 4 条；筛选重置与列表重新加载正常。状态下拉可选择启用/停用，组合筛选无匹配数据时展示规范空状态。
- 后端聚焦执行员工迁移、目录 API、员工管理 application、持久化适配、登录身份、密码盐和遗留消费者退役测试，共 26 项通过、0 失败。
- 前端执行员工 API、组织 API、员工表格、组织模块和部门目录客户端测试，共 36 项通过、0 失败；`@hunyuan/system` 类型检查通过。
- 后端在验收期间稳定监听 1024 端口，启动日志确认 `hunyuan-admin started successfully`。员工查询和筛选请求成功；日志同时记录一次登录加密解密回退异常、IPv6 本机地址解析告警和缺失历史头像资源 404，均未中断本次员工页面验收，后续应在独立运行时治理批次处理。

本批尚未关闭全部 P3：

- 本次运行态权限验收覆盖当前管理员账号；只读角色、无员工模块角色以及独立 `platform_admin` 账号的权限矩阵仍需在具备可登录测试账号后补齐。
- 管理员密码重置入口已显示，但为避免产生或暴露一次性凭据，本批未提交重置动作；一次性凭据响应继续由 API 契约测试覆盖。
- 应用侧员工 API 尚未抽取为独立 `@hunyuan/feature-identity-employee` 包。
- P4 的旧 `/employee/*`、旧页面兼容引用和 `system:employee:*` 退役仍未开始。

当前结论：**P3 的管理员真实数据库、前后端与浏览器主流程验收已通过；完整角色权限矩阵和 feature 包抽取仍未完成，A3.1 尚未关闭。**

## 17. P3 三角色权限矩阵验收记录

执行日期：2026-07-22。

本批次完成管理员、员工只读、无员工模块三类角色的运行时与契约验收：

- 管理员登录后访问 `/organization/employee`，机构树、所属部门筛选和 11 条员工数据正常加载；新增、编辑、启停、批量转部门、删除、批量删除和重置密码入口均可见。本批次未提交任何员工写操作，也未触发密码重置。
- 员工只读角色仅授予 `identity.employee.read`。页面正常加载 11 条员工数据，不显示机构树和所属部门筛选，也不显示新增、编辑、启停、批量转部门、删除、批量删除和重置密码入口；页面不再因缺少 `organization.department.read` 而整体失败。
- 无员工模块角色登录后侧栏不显示员工管理入口，直接访问 `/organization/employee` 返回前端 404，员工页面组件未挂载。
- 浏览器隔离环境不提供可用的模块加载、`fetch` 或 `XMLHttpRequest`，因此没有在浏览器中读取会话凭据或绕过页面直接调用 API。直接 API 权限边界改由后端可重复测试固化：管理员允许查询和全部独立写能力；只读角色仅允许查询并拒绝全部写能力；无模块角色拒绝查询和全部写能力。
- `EmployeeDirectoryApiContractTest` 新增三角色权限矩阵和异常映射断言，共 7 项通过；所有拒绝统一映射到 `UserErrorCode.NO_PERMISSION = 30005`。
- 收尾聚焦回归同时执行 `EmployeeCapabilityMigrationContractTest`、`FlywayMigrationContractTest`、`EmployeeDirectoryApiContractTest`、`LegacyEmployeeConsumerRetirementTest` 和 `EmployeeAdministrationApplicationServiceTest`，共 21 项通过，0 失败。
- 前端聚焦测试覆盖员工 API、员工表格、只读角色独立于部门读取能力、组织模块回归，共 26 项通过；`pnpm -F @hunyuan/system run typecheck` 通过。

验收期间创建的临时角色已清理：

- `员工只读验收`（`a31_emp_ro`）
- `无员工模块验收`（`a31_emp_none`）

清理前已确认两个角色均无员工关联；随后在事务中按角色码精确删除 `t_role_employee`、`t_role_menu`、`t_role_data_scope` 和 `t_role` 数据。删除后四张表针对角色 ID `66`、`67` 的残留计数均为 0，浏览器刷新角色管理页后两个临时角色也不再显示。

当前结论：**P3 三角色权限矩阵已通过，临时验收数据已清理。下一步进入 `@hunyuan/feature-identity-employee` 抽取；P4 旧 `/employee/*`、旧页面兼容引用和 `system:employee:*` 能力退役仍未完成，因此 A3.1 继续保持进行中。**

## 18. P3 独立员工 feature 抽取记录

执行日期：2026-07-22。

本批次已完成：

- 新建 `packages/features/identity-employee`，包名为 `@hunyuan/feature-identity-employee`，员工 contract、请求 payload 归一化、HTTP client、页面和三个业务组件均迁入该包。
- feature 不再依赖应用别名 `#/` 或应用级 `requestClient`。应用通过 `createIdentityEmployeeClient(requestClient)` 创建员工客户端，并通过 Vue injection 提供员工、部门和岗位依赖。
- 部门目录通过最小 `ReadonlyDirectoryProvider<DepartmentOption>` 协议复用 `@hunyuan/feature-organization` 客户端；岗位通过最小 `ReadonlyDirectoryProvider<PositionOption>` 协议适配现有只读接口，没有把 organization 聚合类型带入员工 feature。
- 动态后端路由仍指向 `/system/employee/index.vue`。该文件保留 `SystemEmployeeIndex` 组件名，但已缩减为依赖装配和 `<IdentityEmployeePage />` 渲染，页面状态、权限判断和业务交互全部归 feature 所有。
- 只读权限边界保持不变：只有 `identity.employee.read` 时不请求部门目录，不渲染机构树和部门筛选；写操作继续由 `identity.employee.*` capability code 控制。
- 应用侧旧 `src/api/system/identity-employee.ts` 已退役，员工管理页面不再直接引用应用 API 文件。
- `pnpm-lock.yaml` 已登记新 workspace 包，`apps/hunyuan-system` 已建立 `@hunyuan/feature-identity-employee` workspace 依赖。

验证结果：

- 员工 client、员工表格、应用薄装配、组织模块和部门 client 共 5 个测试文件、30 项测试通过，0 失败。
- `pnpm -F @hunyuan/system run typecheck` 通过。
- 新 feature 使用独立 `tsconfig.json` 执行 `vue-tsc --noEmit --skipLibCheck` 通过。
- 本地后端 `http://127.0.0.1:1024` 与前端 `http://127.0.0.1:5788` 均返回 HTTP 200。
- 使用内置浏览器重新加载 `/organization/employee` 后页面正常挂载，无白屏；机构树、所属部门筛选、11 条员工数据、分页和管理员写操作入口均正常显示。
- Vite 依赖优化期间曾出现一次员工入口动态导入失败；依赖优化完成并重新打开页面后未再复现。
- feature 目录扫描未发现应用别名 `#/`；应用侧旧 `src/api/system/identity-employee.ts` 已不存在。

安全边界：

- 本批次没有执行员工新增、编辑、启停、部门转移、删除或密码重置。
- 一次性密码没有写入日志、文档、测试快照或命令输出。

当前结论：**`@hunyuan/feature-identity-employee` 抽取、静态验证和运行时浏览器回归均已完成，P3 已收口。下一步进入 P4，退役旧 `/employee/*`、旧页面兼容引用和 `system:employee:*` 能力；在 P4 完成前 A3.1 仍未关闭。**

## 19. P4 兼容入口关闭执行记录

执行日期：2026-07-22。

本地仓库已完成以下关闭项：

- 删除旧员工管理 API，仅保留个人中心资料、头像、本人改密和密码复杂度查询四个自助入口。
- 删除旧员工管理表单、Manager、无消费者 DAO/XML 查询和应用内旧员工页面组件。
- 前端员工页面继续由 `@hunyuan/feature-identity-employee` 装配，聚合 API 中仅保留 A3.2 仍需处理的 `/role/employee/*` 兼容边界。
- 增加 `V3.69.0`，先清理旧角色菜单授权，再删除 `system:employee:*` 菜单能力；员工页面菜单和 `identity.employee.*` 能力保持不变。
- 将关闭契约固化为自动化守卫：旧管理路径和旧权限不得重新进入主代码，新 `/api/admin/v1/identity/employees` 管理入口与四个个人自助入口必须保留。

关闭边界：

- 本地仓库内消费者、代码入口、页面组件和权限数据的关闭，以搜索、编译、测试、Flyway 和浏览器验收结果为准。
- 截至 2026-07-22，本系统从未投入生产，也未向仓库外系统开放正式集成入口，因此不存在可审计的生产消费者、网关访问日志或调用链；本批次仓库外消费者审计判定为不适用（N/A），不作为 A3.1 关闭阻塞项。
- 首次生产部署或首次开放仓库外集成前，必须建立消费者登记、调用方标识和网关访问日志或等效调用链机制；一旦存在正式外部调用，以上 N/A 结论立即失效，必须按实际运行证据重新审计。
- `/role/employee/*` 与 `EmployeeVO` 属于 A3.2 角色授权迁移范围，本批次不删除。

运行态验收结果：

- 开发库 Flyway 已执行到 `3.69.0`，旧 `system:employee:*` 权限数量为 0；员工页面菜单、8 个 `identity.employee.*` 能力和 11 条员工数据保持不变。
- 运行时 OpenAPI 包含 `POST /api/admin/v1/identity/employees/query` 与四个个人自助入口，不再包含 `/employee/query`、`/employee/queryAll` 等旧管理入口。
- 增加未匹配资源的 HTTP 404 映射；重打包并重启后，`POST /employee/query` 与 `GET /employee/queryAll` 均严格返回 HTTP 404。
- 内置浏览器在 `http://127.0.0.1:5788/organization/employee` 重新查询成功，页面显示共 11 条；新增、编辑、启停、批量转移部门和重置密码入口保持可见。本批次未执行任何员工写操作或密码重置。
- 后端聚焦测试 10 项通过；此前 P4 后端聚焦测试 19 项、前端 37 项、前端应用与独立 feature 类型检查、隔离库 Flyway 回放均已通过。

当前结论：**A3.1 P1-P4 已完成，员工与账号管理纵切在本地仓库、开发数据库和本地运行态正式关闭。仓库外消费者审计基于“从未生产、从未开放正式外部集成”的事实判定为不适用（N/A），不再保留待完成项；下一步进入 A3.2 角色与访问控制迁移。**
