# A3.3 岗位目录契约与消费者冻结

## 1. 状态与结论

截至 2026-07-22，A3.3 的 P0-P5 已全部完成。稳定岗位边界、员工引用保护、悬空引用
修复、数据与权限迁移、前端纵切及旧兼容层退役均已通过源码、测试、数据库、构建产物、
HTTP/OpenAPI 和真实浏览器验收，A3.3 正式关闭。

当前结论：

- `organization.position` 已建立领域模型、应用服务、稳定 Admin API 和持久化适配器，
  并以现有 `t_position` 作为唯一数据源。
- 旧 `system.position` Controller、Service、DAO、Manager、Entity、Form、VO 和 MyBatis Mapper
  已删除，生产源码与干净构建产物中不再包含旧岗位兼容实现。
- identity.employee、登录、access 角色成员摘要和员工管理前端都消费岗位编号。
- 员工创建、更新、列表摘要和登录请求用户信息当前只透传 `positionId`；岗位名称由前端岗位选项表映射。
- 岗位单个删除和批量删除已增加未删除员工引用检查；被引用岗位拒绝删除，不自动清空或级联修改员工数据。
- 开发库当前存在 4 个有效岗位、9 条非空员工岗位引用，悬空岗位引用为 0。
- `V3.72.0` 只清空员工 66、67 的已停用账号悬空岗位引用，不猜测或自动改派岗位。
- `V3.72.1` 建立有效岗位名称唯一约束、4 个稳定能力码及旧授权映射；
  `V3.73.0` 已删除旧授权关系和旧岗位权限节点。
- P4 已建立岗位 feature、稳定客户端和应用薄入口；员工岗位选项 provider 已改用稳定岗位客户端。
- 旧前端 `api/system/organization.ts` 及其测试已删除；活跃前端不再请求旧 `/position/*`。
- 旧 `/position/*` 代表路径严格返回 HTTP 404，OpenAPI 只保留稳定岗位路径。
- A3.3 已正式关闭，下一步进入 A3.4 平台支持能力与示例模块退役。

本账本依据当前提交 `684740bf3f91659796e0d5806c3e52777299c232` 和
codebase-memory 索引
`E-my-project-hunyuan-pro-a3-3-p1-p2-20260722` 编制。P4 实现前使用
`E-my-project-hunyuan-pro-a3-3-p3-20260722` 反查前端消费者；P4 使用
`E-my-project-hunyuan-pro-a3-3-p4-20260722` 复核纵切结果；P5 关闭后的全量索引和
反查结果见第 10.2 节。图谱用于导航，关键中文事实均以 UTF-8 源码、测试、数据库、
构建产物和运行态复核。

## 2. 所有权冻结

| 对象 | 当前 owner | A3.3 目标 owner | P0 决策 |
| --- | --- | --- | --- |
| `t_position` | `system.position` | `organization.position` | 保留原表，迁移所有权，不建立镜像表和双写 |
| 员工的 `position_id` | `identity.employee` 数据行 | `identity.employee` 引用 `organization.position` | 员工继续保存岗位编号，不复制岗位名称 |
| 岗位目录查询 | `PositionService.queryList` | 稳定 organization 岗位查询契约 | 员工管理只能通过只读岗位提供器获取选项 |
| 岗位生命周期 | `PositionService` | organization.position 应用服务 | 新边界必须统一处理名称约束和引用保护 |
| 岗位菜单与能力码 | `t_menu` 中的旧 `system:position:*` | organization.position 能力目录 | 迁移前保留旧授权，禁止提前删除 |

A3.3 不改变岗位作为员工资料引用的语义，不把岗位编号改成岗位名称，也不在员工、登录或 access 中创建岗位副本。

## 3. 现有后端入口

| 能力 | 当前入口 | 当前权限 | 当前实现 | P0 处置 |
| --- | --- | --- | --- | --- |
| 分页查询 | `POST /position/queryPage` | 无显式岗位权限 | `PositionController.queryPage` | 迁移到稳定岗位查询接口 |
| 新增 | `POST /position/add` | `system:position:add` | `PositionService.add` | 增加名称规范化和重复校验 |
| 更新 | `POST /position/update` | `system:position:update` | `PositionService.update` | 增加存在性、名称重复和并发边界 |
| 批量删除 | `POST /position/batchDelete` | `system:position:delete` | `PositionService.batchDelete` | 必须逐项检查员工引用 |
| 单个删除 | `GET /position/delete/{positionId}` | `system:position:delete` | `PositionService.delete` | 必须改为稳定删除命令，不再直接删除 |
| 不分页查询 | `GET /position/queryList` | 无显式岗位权限 | `PositionService.queryList` | 作为员工岗位选项的迁移输入 |

源码证据：

- Controller：`hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/system/position/controller/PositionController.java:36-75`
- Service：`hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/system/position/service/PositionService.java:40-104`
- DAO：`hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/system/position/dao/PositionDao.java:21-40`
- SQL：`hunyuan-backend/hunyuan-admin/src/main/resources/mapper/system/PositionMapper.xml:6-24`

## 4. 消费者冻结

| 消费者 | 消费内容 | 当前方式 | 迁移约束 |
| --- | --- | --- | --- |
| `identity.employee` 员工创建 | `positionId` | `EmployeeCreateDraft` -> `EmployeeRepositoryAdapter` 写入 `t_employee.position_id` | 只能引用存在且可选用的岗位 |
| `identity.employee` 员工更新 | `positionId` | `EmployeeProfileUpdate` -> `EmployeeRepositoryAdapter` 更新岗位编号 | 保持可空；更新时校验岗位存在性 |
| `identity.employee` 员工摘要 | `positionId` | `EmployeeSummary` 返回岗位编号 | 不在 identity 内复制岗位名称 |
| 登录 `LoginManager` | `positionId` | `EmployeeAuthenticationAccount` -> `RequestEmployee` | 登录只透传编号，不查询岗位 Service |
| access 角色成员 | `positionId` | `RoleEmployeeSummaryRow` -> `AccessRoleMember` | access 只保留摘要字段，不拥有岗位目录 |
| 员工管理页面 | 岗位编号、岗位名称 | `employeePositionProviderKey` 注入 `listPositions()` | 改用岗位 feature 的只读 provider |
| 员工表格和表单 | 岗位选项 | `PositionOption` 与 `positionNameMap` | 保留编号作为值，缺失岗位必须可见并可修复 |
| 岗位管理页面 | 岗位生命周期 | `views/system/position/position-list.vue` | 迁入独立岗位 feature 或稳定应用入口 |

关键源码证据：

- 员工持久化写入与摘要：`hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/identity/employee/infrastructure/EmployeeRepositoryAdapter.java:130-167,214-249`
- 登录透传：`hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/system/login/manager/LoginManager.java:75-105`
- access 摘要：`hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/system/role/service/AccessRoleMembershipFacadeAdapter.java:101-115`
- 员工 API 装配：`hunyuan-design/apps/hunyuan-system/src/views/system/employee/index.vue:1-28`
- 旧岗位 API：`hunyuan-design/apps/hunyuan-system/src/api/system/organization.ts:1-78`
- 员工岗位表单：`hunyuan-design/packages/features/identity-employee/src/employee/components/employee-form.vue:226-247`
- 员工岗位展示：`hunyuan-design/packages/features/identity-employee/src/employee/components/employee-table-panel.vue:78-101,155-164`

仓库外消费者仍沿用 A3.1、A3.2 的事实判断：系统尚未投入生产，也未开放正式仓库外集成，因此当前外部消费者审计为 N/A。首次生产或外部集成前，必须建立调用方登记、调用方标识和访问日志。

## 5. 数据与约束盘点

### 5.1 表结构

当前开发库 Flyway 版本为 `3.73.0`。

`information_schema` 只读核对结果：

- `t_position` 共 4 条记录，4 条有效，0 条逻辑删除。
- 现有岗位：3 技术P7、4 技术P8、5 管理M5、6 管理M6。
- `t_position` 已增加生成列 `active_position_name` 和唯一索引 `uk_position_active_name`，
  只约束未逻辑删除岗位的规范化名称。
- `position_name` 为 `NOT NULL varchar(200)`；稳定应用服务同时执行非空、长度和有效岗位重名校验。
- `t_employee.position_id` 可空。
- 当前数据库没有引用 `t_position` 的外键，也没有 `t_position` 指向其他表的外键。

### 5.2 当前数据

迁移前岗位引用统计：

- 员工岗位编号总引用：11 条。
- 未逻辑删除员工的有效引用：11 条。
- 涉及岗位编号：5 个。
- 其中 4 个编号指向当前有效岗位，另有 2 条员工记录指向不存在的岗位编号。
- 当前悬空员工记录为 `employee_id` 66、67；这两条记录的岗位编号必须在 P1 数据修复方案中明确处理。
- 当前有效岗位名称没有重复值。

迁移后开发库保留 11 条员工记录，其中 9 条未删除员工岗位引用非空，悬空引用为 0。
本节数字来自 2026-07-22 对开发库 `hunyuan` 的迁移前后只读对账，不代表生产数据。

### 5.3 删除风险

当前 `PositionService.batchDelete()` 调用 `positionDao.deleteBatchIds(idList)`，`delete()` 调用 `positionDao.deleteById(positionId)`。实现没有查询 `t_employee.position_id`，因此删除被员工引用的岗位会产生悬空引用；数据库没有外键来阻止该行为。

已冻结并实现以下业务策略：

1. 有未删除员工引用时拒绝删除，并返回可定位的引用数量。
2. 不自动清空员工 `position_id`，不级联修改员工数据。
3. 先完成员工改派或解除岗位引用，再允许岗位删除。

批量删除先校验全部岗位是否存在，再检查全部员工引用；任一岗位不满足条件时，
本批次不执行任何岗位删除。

### 5.4 悬空引用隔离修复方案

当前 `employee_id` 66、67 均为已停用账号，原岗位 2 在当前岗位目录、基线 SQL 和
2026-07-21 备份中均不存在，无法证明应自动改派到哪个有效岗位。因此已冻结最小修复：
只清空这两条记录的悬空 `position_id`，不删除员工、不新建岗位、不猜测目标岗位。

数据修复必须：

1. 同时校验员工编号、登录名、姓名、停用状态、逻辑删除状态和原岗位编号。
2. 先在隔离库复制当前数据并执行修复演练，不在开发库或生产库直接试改。
3. 使用独立递增 Flyway 或受审计的数据修复脚本执行，不复用岗位结构迁移。
4. 仅在岗位 2 当前不存在有效记录时清空引用。
5. 开发库执行前必须备份，并保留执行前后员工数量、引用数量和悬空数量对账。

修复前只读验收 SQL：

```sql
SELECT employee_id, position_id
FROM t_employee
WHERE deleted_flag = 0
  AND position_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM t_position
      WHERE t_position.position_id = t_employee.position_id
        AND t_position.deleted_flag = 0
  )
ORDER BY employee_id;
```

开发库修复关闭条件是上述查询返回 0 行，并且员工总数、有效岗位引用总数与修复清单
对账一致。隔离演练通过不等于开发库修复关闭。

### 5.5 P2 修复准备产物

2026-07-22 已新增只读审计脚本：

`hunyuan-backend/hunyuan-admin/src/test/resources/db/audit/a3_3_position_reference_audit.sql`

脚本覆盖：

- 当前数据库和最新成功 Flyway 版本。
- 全部有效岗位候选清单。
- 未删除员工按岗位编号的引用汇总。
- 悬空引用员工明细，但不读取登录密码、手机号或邮箱。
- 修复前后员工总数、岗位引用数和悬空引用数对账。
- P2 关闭查询，要求 `dangling_reference_count = 0`。

`PositionReferenceAuditSqlTest` 将该脚本冻结为只读契约：每条可执行语句必须以
`SELECT` 开始，并禁止写入、DDL、存储过程调用、切库语句和敏感字段。

修复清单：

| 员工编号 | 登录名 | 姓名 | 原岗位编号 | 处理方式 | 隔离演练状态 |
| --- | --- | --- | --- | --- | --- |
| 66 | `luoyi` | 罗伊 | 2 | 已停用账号清空悬空引用 | 已通过 |
| 67 | `chuxiao` | 初晓 | 2 | 已停用账号清空悬空引用 | 已通过 |

已新增 `V3_72_0__a3_3_repair_dangling_position_references.sql`。迁移采用上述全部保护条件，
并通过 `NOT EXISTS` 再次确认岗位 2 不存在有效记录；不包含岗位 3 的自动改派，也不插入
或删除员工。该迁移已在全新 `hunyuan_a3_3_position_20260722_it` 隔离库中从 `3.71.0`
演练到 `3.72.1`，两条样本的岗位引用均被清空；随后已应用到开发库，最终悬空引用为 0。

## 6. 旧权限与前端边界

P5 迁移前开发库存在 3 个旧岗位能力节点，并有 1 个角色的 3 条授权关系：

- `system:position:add`
- `system:position:update`
- `system:position:delete`

P4 实施前旧前端集中在：

- `hunyuan-design/apps/hunyuan-system/src/api/system/organization.ts`
- `hunyuan-design/apps/hunyuan-system/src/views/system/position/position-list.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/employee/index.vue`

P4 已完成以下前端边界调整：

- `@hunyuan/feature-organization` 新增 `position-directory`，拥有岗位页面、契约、稳定客户端和依赖注入键。
- 岗位应用页面收缩为客户端注入与 `<OrganizationPositionDirectory />` 渲染薄入口。
- 员工应用入口通过 `employeePositionProviderKey` 注入稳定岗位客户端；员工 feature 仅在具备
  `organization.position.read` 能力时加载岗位选项。
- 稳定客户端只访问 `/admin/v1/organization/positions` 及其资源路径。
- 岗位新增、编辑、删除分别受 `organization.position.create/update/delete` 控制。
- 稳定接口没有批量删除契约，岗位页面不再提供逐项模拟的批量删除，避免部分成功。
- 旧 `api/system/organization.ts` 及其测试已删除。
- `organization-modules.test.ts` 已改为稳定岗位边界的正向契约和旧路径负向契约。

P5 已完成以下退役：

- 删除后端旧 `/position/*` Controller 及无剩余生产消费者的 Service、DAO、Manager、
  Entity、Form、VO 和 MyBatis Mapper。
- `V3.73.0` 先删除关联旧岗位权限的角色授权，再删除旧 `system:position:*` 权限节点。
- 保留岗位页面、`t_position` 数据和 `organization.position.*` 四个稳定能力。
- 旧岗位权限节点和授权关系均为 0，生产源码、OpenAPI 与构建产物中的旧入口均已归零。

## 7. P0 关闭结果

A3.3 P0 已于 2026-07-22 按以下结果关闭：

- 岗位数据 owner、员工引用 owner、登录透传边界和 access 摘要边界均已形成明确契约。
- 单个删除与批量删除采用“员工仍引用则拒绝删除”的策略，并形成稳定错误码、中文响应和事务边界。
- 当前 2 条悬空员工岗位引用已有不直接修改现有数据库的隔离修复方案和只读验收 SQL。
- 岗位名称按有效岗位去重；输入统一清理首尾空格，逻辑删除数据不参与应用层重名判断。
- 稳定 Admin API 和能力码已冻结；岗位前端 feature 留待 P4。
- 旧权限授权、旧路由和旧表均保留，没有提前退役。
- codebase-memory 旧索引已用于实现前反查；实现后复核必须标注索引是否刷新，不能用旧图谱冒充新增源码证据。

## 8. P1/P2 后端首批执行记录

本批次已完成：

- 新建 `organization.position` 的领域模型、命令、仓储边界、员工引用端口、应用 Facade 和应用服务。
- 新建 `/api/admin/v1/organization/positions` 稳定接口，冻结
  `organization.position.read/create/update/delete` 四个能力码。
- 复用 `t_position`，查询只返回 `deleted_flag = false` 的有效岗位。
- 岗位名称统一清理首尾空格并限制为 1-200 个字符，排序值不得小于 0，
  有效岗位名称在应用层执行重复校验。
- identity.employee 通过岗位公开 Facade 校验员工创建和更新中的非空 `positionId`。
- identity.employee 提供未删除员工岗位引用统计适配器，岗位删除不直接访问员工持久化内部实现。
- P1/P2 阶段旧 `PositionService` 的新增、更新、单删、批删和不分页查询委托稳定岗位
  Facade，旧分页查询暂时保留；上述兼容实现已在 P5 全部删除。

2026-07-22 焦点验证结果：

- `OrganizationPositionApplicationServiceTest`：6 项通过。
- `OrganizationPositionApiContractTest`：1 项通过。
- `EmployeeAdministrationApplicationServiceTest`：12 项通过。
- `PositionReferenceAuditSqlTest`：2 项通过。
- `ArchitectureGuardTest`：18 项通过。
- 合计 39 项，无失败、无错误。

本批次没有执行数据库写入，没有新增 Flyway，也没有修改权限、前端或旧 Controller 路径。

## 9. P2/P3 迁移执行记录

2026-07-22 已新增：

- `V3_72_0__a3_3_repair_dangling_position_references.sql`：按严格保护条件清空两条已停用账号的悬空岗位引用。
- `V3_72_1__a3_3_position_capability_and_constraints.sql`：增加有效岗位名称生成列与唯一索引，
  建立 `organization.position.read/create/update/delete` 四个稳定能力节点。
- 旧 `system:position:add/update/delete` 授权按一一映射复制到稳定能力；旧节点在该阶段
  保留，并已在 P5 迁移中正式删除。
- 平台管理员获得四个稳定能力；原岗位页面或旧岗位权限持有者继续获得稳定读取能力。

隔离验证使用全新数据库 `hunyuan_a3_3_position_20260722_it`，执行顺序为：

1. 从空库迁移到 `3.71.0`。
2. 插入员工 66、67 的最小悬空引用样本。
3. 继续迁移到 `3.72.1`。
4. 对账迁移版本、修复结果、能力节点、平台管理员授权和唯一索引。

验证结果：

- 最终 Flyway 版本为 `3.72.1`，迁移成功。
- 员工 66、67 的 `position_id` 均为 `NULL`，员工记录总数仍为 2。
- 稳定岗位能力节点共 4 个，平台管理员稳定授权共 4 条。
- `uk_position_active_name` 唯一索引存在。
- `FlywayMigrationTest` 1 项通过；相关迁移与审计契约测试 4 项通过。

开发库执行记录：

- 迁移前只读预检确认版本为 `3.71.0`、有效岗位 4 个、有效岗位名称无重复、
  岗位 2 不存在、员工 66/67 的保护字段与迁移条件完全一致。
- 迁移前备份：
  `数据库SQL脚本/mysql/backups/hunyuan-before-a3-3-p2-p3-20260722-232706.sql`。
- 备份文件大小为 328,356 字节，包含 48 张表的建表语句，
  SHA-256 为 `FBD4473024473110ED53C95E94C27973828BC9FD1F112707B1B19E7AA6A8627D`。
- P2/P3 阶段 Flyway 成功应用 `3.72.0`、`3.72.1`，开发库当时版本为 `3.72.1`。
- 员工总数保持 11，员工 66/67 记录保留且 `position_id` 均为 `NULL`，
  非空岗位引用为 9，悬空引用为 0。
- P2/P3 阶段稳定岗位能力节点 4 个，平台管理员稳定授权 4 条；旧岗位权限节点当时保留
  3 个，旧授权到稳定能力的缺失映射为 0，随后已由 P5 清零。
- 有效岗位名称生成列和 `uk_position_active_name` 唯一索引存在。
- 后端重新启动成功并监听 `1024`；新稳定岗位查询和旧兼容查询在未登录时均返回业务码 `30007`。

## 10. 后续批次

1. **P1/P2 后端首批（已完成）**：稳定岗位领域边界、员工岗位存在性校验、删除引用保护和悬空引用隔离方案。
2. **P2 数据修复关闭（已完成）**：严格保护的数据修复迁移已通过隔离演练和开发库对账，悬空引用归零。
3. **P3 数据与权限迁移（已完成）**：稳定能力码、授权映射和岗位名称唯一约束已通过隔离验证、开发库迁移和运行态验收。
4. **P4 前端纵切（已完成）**：已建立岗位 feature，迁移岗位管理页面和员工岗位选项 provider。
5. **P5 兼容退役（已完成）**：旧 `/position/*`、旧权限码及无消费者兼容实现已退役，最终回归通过。

### 10.1 P4 前端纵切执行记录

2026-07-22 已完成：

- 新增 `packages/features/organization/src/position-directory`，包含岗位契约、稳定客户端、
  注入键、真实页面和客户端契约测试。
- `@hunyuan/feature-organization` 已导出岗位 feature、客户端、注入键及契约类型，
  并提供 `./position-directory` 子路径。
- `views/system/position/position-list.vue` 已收缩为 21 行薄入口。
- 员工岗位 provider 已从旧组织 API 切换到 `createOrganizationPositionClient`。
- 旧前端 `apps/hunyuan-system/src/api/system/organization.ts` 及其测试已删除。
- 岗位列表通过稳定目录接口获取数据，在前端完成关键字筛选与分页；未引入不具备原子语义的批量删除。

验证结果：

- 前端完整单测：61 个测试文件、426 项测试全部通过。
- `pnpm --filter @hunyuan/system typecheck` 通过。
- `pnpm --filter @hunyuan/system build` 通过，并生成岗位页面独立 JS、CSS 产物。
- P4 改动文件 ESLint 通过。
- P4 新增和修改文件严格 UTF-8 解码通过；新增注释使用中文。
- 浏览器访问 `/organization/position` 成功，页面显示 4 个有效岗位，新增、编辑、删除按钮
  按管理员稳定能力正常出现；页面刷新后无控制台错误。
- 浏览器访问 `/organization/employee` 成功，员工列表正常显示岗位名称；编辑表单岗位下拉
  包含技术P7、技术P8、管理M5、管理M6 四个稳定岗位，页面无控制台错误。
- 浏览器工具未暴露网络请求明细；稳定请求路径由客户端契约测试、应用边界负向测试和
  运行态数据加载共同确认，不把不可观测的网络面板结果写成已直接抓取。

最终 codebase-memory 快速索引
`E-my-project-hunyuan-pro-a3-3-p4-20260722` 已建立，包含 14,074 个节点和
37,188 条关系，状态为 `ready`，索引写入外部缓存且未改动仓库内持久化产物。图谱反查确认：

- `createOrganizationPositionClient` 定义于岗位 feature，并由岗位薄入口和员工应用入口消费。
- `organizationPositionClientKey` 连接岗位页面与应用装配入口。
- `OrganizationPositionDirectory` 由岗位应用入口真实渲染。
- `employeePositionProviderKey` 由员工应用入口通过稳定岗位客户端提供。
- 前后端稳定路径分别冻结为 `/admin/v1/organization/positions` 和
  `/api/admin/v1/organization/positions`。

codebase-memory 当前对 Vue `<script setup>` 的部分调用边仍显示为零入度、零出度，因此
图谱结果只作为导航证据；具体 import、provide/inject 和模板渲染关系已由 UTF-8 源码、
契约测试与浏览器运行态交叉复核。

### 10.2 P5 兼容退役与关闭记录

2026-07-22 已完成：

- 删除旧 `system.position` Controller、Service、DAO、Manager、Entity、Form、VO 和
  `PositionMapper.xml`，稳定 `organization.position` 实现独立拥有持久化与 Admin API。
- 新增 `V3_73_0__a3_3_retire_legacy_position_access.sql`，先删除关联
  `system:position:*` 的角色授权，再删除旧权限节点；岗位页面、`t_position` 和四个稳定能力保留。
- 新增退役守卫测试，禁止旧源码包、旧路径和旧权限码回流，并确认稳定岗位 Controller 与能力集合存在。

数据库与迁移证据：

- 迁移前备份为
  `数据库SQL脚本/mysql/backups/hunyuan-before-a3-3-p5-retirement-20260722.sql`，
  文件大小 330,337 字节，包含 48 张表的建表语句，SHA-256 为
  `BED4F76B203BA52F10D117E3A74FC4222E71596C19CD957DC7DCA79242273FC8`。
- 全新隔离库 `hunyuan_a3_3_p5_20260722_it` 与
  `hunyuan_a3_3_p5_bootstrap_20260722_it` 完成 Flyway、管理员初始化和 Redis 隔离验证；
  Flyway 从 `3.71.0` 演练到 `3.73.0`，3 项集成测试全部通过。
- 开发库 `hunyuan` 已迁移到 `3.73.0`：有效岗位 4 个、非空员工岗位引用 9 条、
  悬空引用 0 条、稳定岗位能力 4 个、平台管理员稳定授权 4 条、旧岗位权限节点 0 个、
  旧岗位授权 0 条，岗位页面 `/organization/position` 保留 1 个。

构建、接口与前端证据：

- 完整 Maven `clean package` 通过：`hunyuan-base` 12 项测试，
  `hunyuan-admin` 153 项测试无失败、无错误、3 项按配置跳过，`ArchitectureGuardTest`
  18 项通过。
- 干净 JAR 与 `target/classes` 中旧 `system.position` 字节码均为 0；
  稳定岗位类分别为 16 个和 11 个，`V3.73.0` 已打包。
- 旧 `/position/queryPage`、`/position/queryList`、`/position/add`、
  `/position/update`、`/position/batchDelete` 和 `/position/delete/1`
  均严格返回 HTTP 404。
- OpenAPI 共 180 条路径，其中稳定岗位路径 2 条，旧 `/position*` 路径 0 条；
  稳定路径为 `/api/admin/v1/organization/positions` 和
  `/api/admin/v1/organization/positions/{positionId}`。
- 前端 61 个测试文件、426 项测试全部通过；`@hunyuan/system` 类型检查、生产构建和
  改动文件 ESLint 均通过。
- 真实浏览器确认岗位页显示技术P7、技术P8、管理M5、管理M6 四个岗位；员工页及管理员
  编辑弹窗使用同一稳定岗位目录，刷新后控制台无错误。

最终 codebase-memory 全量索引
`E-my-project-hunyuan-pro-a3-3-p5-closed-20260722` 已使用新项目名建立，状态为 `ready`，
包含 16,117 个节点和 40,526 条关系，索引写入外部缓存且未修改仓库内持久化产物。
精确图谱和源码反查确认：

- 旧 `PositionController` 和 `PositionDao` 图节点均为 0，旧
  `com.hunyuan.sa.admin.module.system.position` 生产包不存在。
- 稳定 `OrganizationPositionController` 图节点为 1，定位到当前
  `organization.position.api` 生产源码，并包含稳定列表、详情、新增、更新和删除方法。
- `OrganizationPositionController`、前端岗位 feature 和员工岗位 provider 均能检索到
  `organization.position.*` 能力与稳定岗位路径。
- 旧 `/position/*` 和 `system:position:*` 的剩余文本只属于历史 Flyway、权限映射与退役
  Flyway、负向守卫测试及关闭文档；活跃生产 Java/TypeScript 中没有旧管理入口或旧权限校验。

图谱结果继续只作为导航证据，最终关闭结论以源码、测试、数据库、构建产物和运行态
交叉验证为准。

**A3.3 已正式关闭，下一步进入 A3.4 平台支持能力与示例模块退役。**
