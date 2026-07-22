# A3 平台能力迁移路线图

## 1. 阶段定位

A3 面向“尚未确定具体业务，但继续建设通用管理平台”的近期目标。它不新增虚构业务模块，而是盘点现有平台能力，明确数据所有权、公开接口和迁移顺序，并在真实变更发生时逐个关闭遗留边界。

A3 的第一个可实施纵切确定为 **A3.1 员工与账号管理**。选择它不是因为员工页面最容易改，而是因为它处于组织目录、登录认证、角色授权、岗位目录和多个历史业务消费者的交汇处，适合验证平台身份能力的完整迁移方法。

本路线图只确定范围和执行顺序。A3.1 的代码、Flyway、权限数据与运行态验收应作为独立实施批次完成，不能把本文件视为实现已开始或已完成的证明。

## 2. 决策依据

### 2.1 已完成前置条件

- A0/A1 已建立迁移账本、Flyway 基线、架构守卫和隔离测试边界。
- A2 已以组织目录完成第一个新架构纵切。
- A2.1 已退役旧部门页面、`/department/*` API 和 `system:department:*` 授权，组织目录成为部门数据的唯一所有者。
- 当前仍采用模块化单体、单一后端应用和主要数据库，不因 A3 拆分微服务或 Maven 工程。

### 2.2 现状代码证据

当前 `t_employee` 同时保存：

- 人员标识与基本资料：`employee_uid`、姓名、性别、手机、邮箱、头像。
- 登录账号与密码：`login_name`、`login_pwd`。
- 组织关系：`department_id`、`position_id`。
- 平台状态：`disabled_flag`、`deleted_flag`、`administrator_flag`。

当前 `EmployeeService` 直接协作：

- `EmployeeDao` 和 `EmployeeManager`。
- `OrganizationDepartmentFacade`。
- `PositionDao`。
- `RoleEmployeeDao` 与角色分配服务。
- `SecurityPasswordService`。
- `LoginService`。
- `IFileStorageService`。

现有登录、数据范围和 OA 代码还直接引用员工 Service、DAO 或 Entity。现有员工前端页面虽然已经使用 `@hunyuan/feature-organization` 获取部门目录，但员工、岗位和角色仍由应用内 `src/api/system/organization.ts` 与 `views/system/employee` 承载。

因此，A3.1 必须处理员工与账号的所有权和公开协作边界，不能只把旧 Controller 改成 REST 路径，也不能在员工模块内继续拥有角色授权规则。

### 2.3 codebase-memory-mcp 的使用边界

本次盘点使用本地 `codebase-memory-mcp` 项目 `E-my-project-hunyuan-pro-current` 辅助识别调用簇、热点和跨模块路径。代码图显示员工与登录形成强关联簇，员工、角色和部门形成另一组高连接协作关系，`EmployeeDao.getEmployeeId` 等查询位于高复用路径。

代码图用于提高侦察效率，不替代以下事实来源：

- 当前 Git checkout 中的源码和测试。
- Flyway 迁移与实际 `flyway_schema_history`。
- OpenAPI、直接 API 和权限行为。
- 浏览器中的真实菜单、按钮和业务流程。

代码图与源码不一致时，以当前源码和运行态验证结果为准；大规模重构或删除前必须刷新索引并再次核对。

## 3. 平台能力盘点

| 能力 | 当前主要实现 | 目标所有权 | 决策 | A3 批次 |
| --- | --- | --- | --- | --- |
| 组织目录 | `module.organization.department`、`t_department`、`@hunyuan/feature-organization` | `organization` | KEEP | 已由 A2/A2.1 完成 |
| 员工与账号 | `system.employee`、`t_employee`、员工页面 | `identity.employee` | ADAPT | A3.1 |
| 登录认证与会话 | `system.login`、Sa-Token、登录缓存 | `identity.authentication` | KEEP / ADAPT | A3.1 先消费公开账号接口，认证机制不重写 |
| 角色与员工授权 | `system.role`、`t_role`、`t_role_employee` | `access.role` | ADAPT | A3.2 |
| 菜单与能力授权 | `system.menu`、`t_role_menu`、`api_perms` | `access.capability` | ADAPT | A3.2 |
| 数据范围 | `system.datascope`、`t_role_data_scope` | `access.data-scope` | ADAPT | A3.2 |
| 岗位目录 | `system.position`、`t_position` | `organization.position` | ADAPT | A3.3 |
| 密码安全 | `hunyuan-base` 的安全保护服务 | `platform-security` | KEEP / HARDEN | A3.1 复用，后续按公共能力收敛 |
| 文件与头像 | 文件存储服务、员工头像字段 | `platform-file` | KEEP | A3.1 仅通过公开服务消费 |
| 消息、帮助、反馈、任务等支持能力 | `system.support` 与历史菜单 | `platform-support` 或 RETIRE | INVENTORY | A3.4 |
| 商品、分类示例 | `business.goods`、`business.category` | 未确定 | RETIRE 候选 | A3.4，除非先有明确业务采用 |
| OA 通知与企业 | `business.oa` | 未确定 | EVALUATE | A3.4，决策前不扩建 |
| 历史 BPM、部门和 module-bridge 残留 | 页面、路由或兼容引用 | 无 | RETIRE 候选 | A3.4，仅在可达性与消费者归零后删除 |

`KEEP / ADAPT / RETIRE` 的含义：

- `KEEP`：能力和所有权方向成立，保持并补齐必要契约。
- `ADAPT`：能力保留，但需要重建边界、API、权限或前端装配。
- `RETIRE`：没有真实平台价值或业务采用证据，完成消费者审计后退役。
- `EVALUATE / INVENTORY`：证据不足，不扩建、不批量删除，先逐项盘点。

## 4. A3 执行顺序

### A3.0：能力账本与边界冻结

目标：

- 对现有 `system`、`support`、`business` 能力补齐 owner、表、API、菜单、权限、消费者和处置决定。
- 将员工、账号、角色、岗位、菜单和数据范围从“同一系统目录”拆解为明确业务责任。
- 为 A3.1 冻结旧调用清单和关闭条件，禁止新增对 `EmployeeDao`、`EmployeeEntity`、`EmployeeService` 的跨模块直接依赖。

关闭条件：

- 每项平台能力都有 `KEEP / ADAPT / RETIRE / EVALUATE` 决策。
- A3.1 的内部与外部消费者清单可审计。
- codebase-memory 图与源码搜索结果已经交叉核对。

### A3.1：员工与账号管理

建立 `identity.employee` 纵切，迁移员工管理页面、Admin API、能力码和公开协作接口。保留 `t_employee` 为唯一数据源，不复制员工表，不建立新旧双写。

本批次的详细范围见本文第 5 至第 11 节。

### A3.2：角色、能力、菜单授权与数据范围

目标：

- 由 `access` 拥有 `t_role`、`t_role_employee`、`t_role_menu` 和 `t_role_data_scope`。
- 把角色生命周期、员工角色分配、菜单/能力授权和数据范围作为相互关联但可独立测试的用例。
- 迁移 `system:role:*` 等历史权限码，并建立能力码与后端校验的一致性守卫。
- 登录只通过 access 公开接口装载授权结果，不直接调用角色 Service/DAO。

A3.1 只允许通过最小兼容端口读取或提交角色分配，不提前重写 A3.2 的授权模型。

### A3.3：岗位目录

目标：

- 由 `organization.position` 拥有 `t_position`。
- 建立稳定 Admin API、能力码和前端 feature。
- 增加岗位名称等必要约束。
- 删除岗位前检查员工引用，禁止当前“直接删除但员工仍引用”的行为。

岗位在 A3.1 中只作为只读引用数据，不与员工迁移同时扩展岗位规则。

### A3.4：平台支持能力与示例模块退役

目标：

- 逐项盘点消息、帮助、反馈、定时任务、文件、安全等支持能力，确定平台 owner。
- 对商品、分类、OA 和其他历史示例模块做真实采用审计。
- 对确认无消费者的页面、菜单、API、表和代码按独立迁移退役。

本批次禁止“整个目录看起来像示例所以一次删除”。每项退役必须有数据备份、消费者归零、Flyway、权限迁移和运行态证据。

## 5. A3.1 用户与任务

### 5.1 主要用户

- 平台管理员：维护员工、账号状态和组织归属。
- 授权管理员：维护角色分配，但规则和数据由 access 模块拥有。
- 普通员工：登录平台并使用自己的身份，不通过员工管理页面管理他人。
- 平台内部模块：查询稳定的员工摘要、验证员工是否存在或可用。

### 5.2 核心任务

- 按关键词、部门和启用状态查询员工。
- 查看员工详情及部门、岗位、角色摘要。
- 创建员工并初始化可登录账号。
- 修改人员资料、登录名和组织归属。
- 启用或禁用员工；禁用后立即使现有会话失效。
- 调整单个或批量员工的部门。
- 由管理员重置账号密码。
- 向登录、access、organization 和已登记业务消费者提供最小公开查询。

## 6. A3.1 范围与非范围

### 6.1 本批次包含

- 员工列表和详情。
- 员工创建、更新、启用、禁用和兼容删除行为。
- 部门归属校验和批量部门调整。
- 登录名、手机号、邮箱等唯一性规则。
- 账号密码初始化与管理员重置边界。
- 登录模块读取账号、状态变更后的缓存清理和会话失效。
- 员工模块公开 Facade，以及现有内部消费者迁移。
- `/api/admin/v1` 新接口、稳定能力码和 OpenAPI 契约。
- 独立前端 `feature-identity-employee`，由应用层薄装配。
- 旧员工页面、API 和权限的兼容迁移与最终关闭计划。

### 6.2 本批次不包含

- 重新设计角色、菜单、能力和数据范围模型。
- 把 `t_role_employee` 所有权交给员工模块。
- 岗位目录的完整迁移或岗位业务规则扩展。
- 登录方式、身份提供方、Sa-Token 或认证协议重写。
- 个人中心、头像、自助修改密码的全面迁移；仅保留兼容所需协作。
- OA 企业、人事、入转调离、考勤、薪酬或招聘等 HR 业务。
- 多租户、外部身份源同步、LDAP/AD、SSO 或 SCIM。
- 大规模包移动、Maven 模块拆分或数据库拆表。

## 7. A3.1 所有权与模块边界

### 7.1 目标所有权

| 对象 | Owner | 说明 |
| --- | --- | --- |
| `t_employee` | `identity.employee` | 人员身份、登录账号标识、状态和当前组织引用 |
| `t_department` | `organization.department` | A3.1 仅通过 `OrganizationDepartmentFacade` 校验和读取 |
| `t_position` | `organization.position` | A3.1 暂通过只读端口读取，A3.3 后由正式 Facade 替换 |
| `t_role_employee` | `access.role` | 员工页面可展示/提交角色选择，但写入必须经 access 公开用例 |
| 密码历史与安全策略 | `platform-security` | A3.1 不直接拥有公共安全策略 |
| 登录会话和缓存 | `identity.authentication` | 员工状态变化通过公开端口通知失效 |
| 头像文件 | `platform-file` | `t_employee.avatar` 只保存文件引用 |

### 7.2 后端目标结构

逻辑结构如下，不要求在 A3.1 开始前拆 Maven 工程：

```text
module.identity.employee
├─ api/
│  ├─ EmployeeDirectoryFacade
│  └─ dto/
├─ application/
│  ├─ EmployeeCommandService
│  └─ EmployeeQueryService
├─ domain/
│  ├─ Employee
│  ├─ EmployeeStatus
│  └─ rule/
├─ infrastructure/
│  ├─ EmployeeRepositoryAdapter
│  └─ persistence/
└─ interfaces/
   └─ admin/
```

约束：

- Controller 只进入 application 用例。
- 其他模块只依赖 `identity.employee.api`。
- `identity.employee` 不直接访问 role、position、login 的 DAO 或 Entity。
- 角色分配、岗位查询、会话失效和密码策略分别通过公开端口协作。
- 数据库迁移期间保持一条 `t_employee` 写路径。

### 7.3 最小公开 Facade

A3.1 至少提供以下语义，不直接暴露持久化 Entity：

```text
findEmployee(employeeId)
findAccountByLoginName(loginName)
listEmployeeSummaries(employeeIds)
listActiveEmployeesByDepartment(departmentId)
existsActiveEmployee(employeeId)
```

返回模型只包含调用方必需字段。密码摘要、管理员内部标记和未声明的个人信息不得进入通用员工摘要。

## 8. A3.1 能力、API 与状态

### 8.1 稳定能力编码

模块编码：

```text
identity.employee
```

页面与操作能力：

```text
identity.employee.read
identity.employee.create
identity.employee.update
identity.employee.enable
identity.employee.disable
identity.employee.delete
identity.employee.department.assign
identity.employee.password.reset
```

其中 `identity.employee.delete` 在产品语义明确前保持兼容软删除，不承诺物理删除。角色分配能力不放入本组，A3.2 应定义 `access.role.employee.assign` 或等价稳定编码。

### 8.2 Admin API 方向

建议的新接口：

```text
GET    /api/admin/v1/identity/employees
GET    /api/admin/v1/identity/employees/{employeeId}
POST   /api/admin/v1/identity/employees
PUT    /api/admin/v1/identity/employees/{employeeId}
POST   /api/admin/v1/identity/employees/{employeeId}/enable
POST   /api/admin/v1/identity/employees/{employeeId}/disable
DELETE /api/admin/v1/identity/employees/{employeeId}
POST   /api/admin/v1/identity/employees/department-assignments
POST   /api/admin/v1/identity/employees/{employeeId}/password-reset
```

约束：

- 查询使用显式分页、过滤和排序参数。
- 启用与禁用是独立动作，不沿用“切换当前状态”的 GET 请求。
- 写操作不得使用 GET。
- 创建接口不得长期以明文密码作为普通响应字段；实施前必须确定一次性凭据展示或安全交付方案。
- OpenAPI 必须描述能力要求、错误码和状态语义。

### 8.3 业务状态规则

- 禁用员工后必须使其现有会话失效，并拒绝后续登录。
- 软删除员工必须先满足管理员保护、关联审计和登录失效规则。
- 超级管理员不能通过普通员工接口被删除、禁用或降级；具体保护规则在实施设计中固定并测试。
- 部门必须存在且模块启用；批量调整部门也必须执行同样校验。
- 岗位为空时允许保存；非空时必须通过岗位只读端口验证存在。
- 登录名与员工 UID 的唯一性由数据库约束与应用错误共同保护；手机号和邮箱在缺少真实业务证据时暂由应用层校验，不能把 `synchronized` 视为并发唯一性保证。

## 9. A3.1 兼容与迁移策略

### 9.1 单一写路径

- 保留现有 `t_employee`，由新的 repository adapter 接管。
- 不建立 `t_identity_employee` 镜像表。
- 不通过事件、定时任务或数据库触发器维护新旧双写。
- 旧 `/employee/*` Controller 在兼容期委托同一 application 用例。

### 9.2 消费者迁移

按以下顺序迁移：

1. 登录模块改为通过账号公开接口读取员工与账号状态。
2. 数据范围和角色模块改为通过员工摘要 Facade 获取人员信息。
3. OA 通知、企业等业务消费者改为公开 Facade，或在其自身处置决定中退役。
4. organization 的 `OrganizationDirectoryAdapter` 改为依赖员工公开查询，不直接访问 `EmployeeDao`。
5. 仓库内直接引用 `EmployeeDao`、`EmployeeEntity`、`EmployeeService` 的跨模块调用归零。

仓库外消费者原则上必须通过访问日志、网关记录或集成登记单独审计，不能仅凭代码搜索判定为零。若系统从未投入生产且从未开放正式仓库外集成入口，经部署事实确认后可将该项判定为不适用（N/A）；首次生产部署或首次开放外部集成前，必须补建消费者登记、调用方标识和访问日志或等效调用链机制。

### 9.3 前端迁移

- 新建 `@hunyuan/feature-identity-employee`。
- feature 内拥有页面、组件、API 适配、权限判断、类型和测试。
- 应用内只保留路由与装配，不继续扩展 `views/system/employee`。
- 部门选择器复用 `@hunyuan/feature-organization` 的公开客户端。
- 岗位和角色分别通过其公开客户端或兼容端口获取，不把多模块 API 继续堆入 `src/api/system/organization.ts`。

### 9.4 旧入口退役

旧范围包括：

- `/employee/query`
- `/employee/add`
- `/employee/update`
- `/employee/update/disabled/{employeeId}`
- `/employee/update/batch/delete`
- `/employee/update/batch/department`
- `/employee/update/password/reset/{employeeId}`
- `system:employee:*`
- 应用内旧员工页面和 API 封装

退役必须另立 A3.1.x 关闭批次，顺序为：

1. 新 API、feature、能力和角色授权上线并验收。
2. 仓库内消费者迁移归零。
3. 仓库外消费者审计完成；若系统从未生产且未开放正式外部集成，可按 9.2 的条件判定为不适用（N/A）。
4. Flyway 映射或清理旧菜单与权限。
5. 直接调用旧 API 的负向验收通过。
6. 删除旧 Controller、页面、客户端和无剩余用途的兼容类型。

## 10. A3.1 实施分解

### P0：契约与消费者冻结

- 冻结 `t_employee` 字段语义、现有 API、权限和消费者清单。
- 建立 ArchUnit 或源码契约测试，禁止新增跨模块 Employee DAO/Entity/Service 依赖。
- 确定一次性初始密码和重置密码的安全交付方式。
- 确定超级管理员保护、软删除与唯一性约束策略。

### P1：后端纵切

- 建立 `identity.employee` 分层和公开 Facade。
- 让登录、organization 及首批内部消费者改用公开接口。
- 建立新 Admin API、能力校验和 OpenAPI。
- 旧 Controller 委托同一 application 用例。

### P2：数据库与授权

- 用递增 Flyway 补齐必要唯一索引、菜单、能力和角色映射。
- 在空 `_it` 库和已有 `3.67.0` 合规 `_it` 库验证迁移。
- 校验管理员、只读角色、无页面角色和无操作能力角色。

### P3：前端 feature 与浏览器闭环

- 建立 `@hunyuan/feature-identity-employee`。
- 完成员工查询、创建、编辑、启停、部门调整和密码重置交互。
- 验证页面、按钮、直接 URL 和直接 API 权限。
- 验证组织模块关闭或依赖缺失时的明确失败行为。

### P4：兼容入口关闭

- 完成仓库内消费者审计；仓库外消费者完成运行证据审计，或按 9.2 的条件判定为不适用（N/A）。
- 迁移旧角色授权并退役 `system:employee:*`。
- 删除旧 API、旧页面和兼容代码。
- 从 ArchUnit 冻结台账移除已关闭的员工范围例外。

## 11. A3.1 验收与关闭定义

### 11.1 验收矩阵

| 维度 | 必须证明的结果 |
| --- | --- |
| 所有权 | `t_employee` 只有 `identity.employee` 写入；其他模块无 Employee DAO/Entity/Service 直连 |
| 数据 | 空库和已有 `3.67.0` 合规 `_it` 库可迁移；唯一约束、软删除和状态规则符合设计 |
| API | 新 `/api/admin/v1/identity/employees` 契约、校验、错误码和 OpenAPI 通过 |
| 权限 | 模块、页面、操作、数据范围和业务状态五层不能被直接 URL/API 绕过 |
| 登录 | 禁用、删除或关键账号状态变化后会话失效，后续登录被正确拒绝 |
| 协作 | organization、login、access 和已登记业务消费者只调用公开 Facade |
| 前端 | feature 注册、类型检查、单元/组件测试和关键浏览器流程通过 |
| 兼容 | 新旧入口共用一条业务写路径，无镜像表和双写 |
| 退役 | 旧页面、API、权限和源码引用归零后才删除兼容层 |

### 11.2 必测浏览器角色

- 平台管理员：完整查询和写操作可用。
- 员工只读管理员：可查看，无新增、编辑、启停、删除、调部门和重置密码入口。
- 部门范围角色：只能看到授权范围内员工，直接 API 也不能越权。
- 无员工模块角色：无菜单，直接 URL 和 API 均被拒绝。
- 被禁用员工：当前会话失效，不能重新登录。

### 11.3 关闭定义

A3.1 只有同时满足以下条件才能关闭：

1. 新后端分层、公开 Facade、新 API、能力码和前端 feature 已落地。
2. `t_employee` 保持单一写路径，数据库迁移在隔离 MySQL 环境通过。
3. 登录、organization、access 和所有已登记业务消费者不再直连员工内部实现。
4. 管理员、只读、数据范围、无模块和禁用账号的 API 与浏览器验收全部通过。
5. 旧 `/employee/*`、`system:employee:*` 和旧页面在独立关闭批次中完成消费者归零与退役。
6. 后端测试、前端类型检查与测试、OpenAPI、Flyway、直接 API 和浏览器证据全部写回执行记录。
7. 仓库外消费者已有运行证据时完成独立审计；从未生产且未开放正式外部集成时，可记录事实依据并判定为不适用（N/A），同时把首次生产或外部集成前的审计机制列为生产就绪前置条件。

未满足任一项时，只能报告 A3.1 进行中，不能以“新页面可用”或“自动测试通过”宣称迁移完成。

## 12. 当前结论与下一步

当前状态（2026-07-22）：**A3.1 P1-P4 已完成并正式关闭；A3.2 已启动，P0 冻结、P1 第一批登录授权边界以及 P2 前八个子批已完成。**

已建立 `identity.employee` 的员工摘要、专用认证账号 DTO、公开查询与管理 Facade、application 用例和 `t_employee` 持久化适配器；登录、organization、数据范围和 OA 通知已改用公开边界。新 Admin API 已覆盖查询、创建、资料更新、显式启停、部门分配、软删除和管理员密码重置，旧 `/employee/*` 对应写入口已委托同一 application 用例。角色兼容分配与登录缓存/会话失效分别通过窄端口协作，没有把角色或登录实现反向引入 identity。新接口的路径、operationId、能力码、说明、公开响应模型和一次性凭据禁缓存已在仓库注解/反射契约层收口。

P2 已完成 `V3.68.0` 数据库与授权迁移；P3 已完成 `@hunyuan/feature-identity-employee` 前端纵切、权限交互、运行态 API 和浏览器闭环；P4 已通过 `V3.69.0` 退役旧 `system:employee:*` 权限、旧 `/employee/*` 管理入口、旧页面组件及无消费者兼容实现。旧管理路径严格返回 HTTP 404，新员工管理 API、四个个人自助入口、员工菜单和 `identity.employee.*` 能力保持有效。仓库外消费者审计基于系统从未投入生产、从未开放正式外部集成的事实判定为不适用（N/A）；首次生产或外部集成前须建立消费者登记、调用方标识和访问日志机制。`/role/employee/*` 与 `EmployeeVO` 保留给 A3.2 处理。完整冻结账本、分批执行与关闭证据见 [15-a3-1-employee-contract-and-consumer-freeze.md](15-a3-1-employee-contract-and-consumer-freeze.md)。

A3.2 的所有权、入口、消费者、能力码草案、分批计划和关闭条件见 [16-a3-2-access-contract-and-consumer-freeze.md](16-a3-2-access-contract-and-consumer-freeze.md)。P1 第一批已建立 `AccessAuthorizationFacade`，登录不再直接依赖角色 Service/DAO；P2 第一子批已建立 `AccessRoleAssignmentFacade`，旧 identity 角色分配端口已删除；P2 第二子批已建立 `AccessDataScopeFacade`，`DataScopeViewService` 不再直接依赖角色 DAO/实体；P2 第三子批已建立 `AccessDepartmentScopeFacade`，organization 数据范围适配器不再依赖旧 DataScope Service/枚举；P2 第四子批已建立 `AccessRoleLifecycleFacade` 和 `/api/admin/v1/access/roles`，旧角色入口统一委托 access 用例，`RoleService` 已删除；P2 第五子批已建立 `AccessCapabilityGrantFacade` 和 `/api/admin/v1/access/roles/{roleId}/capabilities`，旧角色菜单授权入口统一委托 access 用例；P2 第六子批已建立 `AccessCapabilityQueryFacade`，登录授权菜单与能力查询不再暴露旧菜单模型，`RoleMenuService` 已删除；P2 第七子批已建立 `AccessMenuCatalogFacade` 和 `/api/admin/v1/access/menus`，旧菜单管理入口统一委托 access 用例，`MenuService` 已删除；P2 第八子批已建立 `AccessRoleMembershipFacade` 和稳定角色成员 Admin API，旧角色员工控制器及登录授权聚合统一委托 access 公开边界，`RoleEmployeeService` 已删除。下一步盘点并收口剩余跨边界 Service/DAO 消费；A3.2 尚未关闭。
