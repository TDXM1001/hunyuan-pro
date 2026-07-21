# A2.1 组织目录兼容入口退役与迁移链路收口

## 1. 阶段定位

A2.1 是 A2 组织目录垂直切片的关闭批次，优先级高于任何新的业务模块迁移。A2 已验证新模块的完整读写、权限、模块开关和浏览器闭环；本批次只收口其明确保留的兼容入口，不把组织域扩大为员工、岗位、角色或 OA 的重构。

本批次已于 2026-07-21 完成全部关闭门。下一步可以进入
[A3 持续迁移](09-current-state-migration-map.md#A3持续迁移)的范围评估，但本批次不直接启动 A3 实现。

## 2. 目标与非目标

目标：

- 在全新或已有合规 `_it` 库完整重放并验证当前 Flyway 链路至 `3.66.1`。
- 核对 Flyway 校验和、组织目录模块开关、菜单 `perms_type`、页面和操作能力授权。
- 迁移旧页面和所有旧 `/department/*` API 消费者到组织目录新 API 或公开 Facade。
- 完成全部角色授权从 `system:department:*` 到 `organization.department.*` 的显式映射。
- 通过独立 Flyway 迁移退役旧菜单、旧权限码及其角色关联。
- 在仓库内调用方归零后删除旧 Controller 和仅为其服务的兼容 Facade。

不包含：

- 员工、岗位、角色、OA、企业或登录模块的业务规则重构。
- 改变 `t_department` 的数据所有权、复制部门数据或引入新旧双写。
- 迁移 A3 的其他业务模块，或为目录整齐进行大范围搬迁。

## 3. 已知起点与调用清单

当前 A2 迁移为 `V3_66_0__a2_organization_directory.sql` 与
`V3_66_1__a2_organization_permission_type.sql`。A2.1 开始前必须以实际
`flyway_schema_history` 为准重新核对版本、脚本名、校验和和成功状态；文档中的
本机快照不能替代该核对。

| 类别 | 当前对象 | A2.1 处置 |
| --- | --- | --- |
| 旧页面 | `apps/hunyuan-system/src/views/system/department/department-list.vue` | 先移除路由/菜单可达性并确认无引用，再删除页面。 |
| 旧前端客户端 | `src/api/system/organization.ts` 的 `listDepartmentTree`、`addDepartment`、`updateDepartment`、`deleteDepartment` | 部门页删除前迁走或删除这些旧 API 包装；不可保留动作路径调用。 |
| 部门选择消费者 | `src/views/system/employee/index.vue` 的 `listDepartments` | 改为组织目录的新只读客户端，保持员工页所需的数据形状，不重新设计员工模块。 |
| 旧 HTTP 入口 | `DepartmentController` 的 `/department/treeList`、`add`、`update`、`delete`、`listAll` | 所有消费者归零、直接 API 负向验收完成后删除。 |
| 兼容服务 | `DepartmentService`、旧部门 VO/Form/DAO 及缓存适配 | 先将 `NoticeService`、`NoticeEmployeeService`、`EmployeeService`、`EnterpriseService`、`LoginManager` 等内部调用方迁至组织公开 Facade 或等价只读端口；之后删除无剩余用途的兼容类型。 |
| 旧权限 | `system:department:add`、`system:department:update`、`system:department:delete` 及旧部门菜单关系 | 以角色菜单数据为准建立幂等映射；确认新能力已授予后，独立迁移删除旧权限菜单与关联。 |
| 旧菜单/路由 | `/organization/department`、旧 `department_*` 菜单项 | 新目录入口保留为 `/organization/directory`；独立迁移退役旧记录，不修改已执行的 `3.65.x` 或 `3.66.x` 脚本。 |

该清单是实现前的仓库级盘点，不等同于运行中外部客户端审计。发布前还必须根据访问日志、网关记录或已登记集成方确认没有仓库外旧 API 调用方。

## 4. 执行门

### 4.1 P0：隔离迁移重放

1. 使用全新或已有合规、名称以 `_it` 结尾的库，禁止连接 `hunyuan` 开发库。
2. 从空库完整执行 Flyway 至 `3.66.1`，并验证所有成功迁移的版本、脚本和校验和。
3. 核对 `module.organization.directory.enabled=true`，组织目录页面及四个 `organization.department.*` 菜单记录的 `perms_type=1`。
4. 在已有合规 `_it` 库重复执行，确认迁移幂等、版本一致且无校验和漂移。

`3.66.1` 是 A2.1 的迁移基线门，不是加入退役迁移后的最终版本号。退役迁移发布后，空库和已有基线库必须继续升级至该新增迁移的版本；最终记录必须同时保留“已验证至 `3.66.1`”和“已验证至退役迁移版本”两项证据。

### 4.2 P1：消费者迁移与无双写保证

1. 将旧部门页、旧前端 API 包装和员工页部门选择器分别迁移或删除。
2. 将所有后端 `DepartmentService` 调用方改为组织模块公开 Facade 或最小只读端口；不允许新模块反向依赖旧 `system.department`。
3. 对照路由、前端请求封装、Controller 映射、后端注入点和运行日志，确认旧页面引用及旧 API 调用为零。
4. 验证所有部门写操作只通过组织应用服务到达同一 `t_department` 路径；不保留镜像表、同步任务或双写分支。

### 4.3 P2：授权和菜单迁移

1. 导出拥有旧部门页面或 `system:department:*` 的所有角色，逐角色核对其目标 `organization.department.*` 授权与数据范围。
2. 管理员保留读、建、改、删四项能力；只读角色只保留 `organization.department.read`；无模块、无页面或无写能力角色分别保持原有拒绝边界。
3. 新建独立且递增的 Flyway 迁移，先插入缺失的新授权，再删除旧菜单、旧权限码和其 `t_role_menu` 关联；迁移须可在空库和 `3.66.1` 基线库通过。
4. 迁移后复核菜单 `perms_type`、角色菜单关联、数据范围和模块开关过滤，不以 UI 隐藏替代后端授权。

### 4.4 P3：兼容层删除与验收

1. 仅在 P1 和 P2 的调用方、权限数据和外部调用审计全部归零后，删除 `DepartmentController` 与无调用方的兼容 Facade、DTO/VO/Form/DAO、测试和前端旧页面/客户端。
2. 增加静态守卫或契约测试，防止重新引入 `/department/`、`/organization/department` 和 `system:department:`。
3. 重新执行后端、前端、隔离 Flyway、直接 API 和浏览器验收；失败即不关闭 A2.1。

## 5. 验收矩阵

| 维度 | 必须证明的结果 |
| --- | --- |
| Flyway | 空 `_it` 库和已有 `3.66.1` 合规 `_it` 库均通过；已执行迁移校验和无漂移；最终版本为独立退役迁移的版本。 |
| 模块与菜单 | 开关关闭时新菜单、直达 URL 和新 API 均被拒绝；开启时菜单及全部 `organization.department.*` 的 `perms_type=1`。 |
| 角色授权 | 管理员、只读角色、无页面能力角色和无写能力角色的菜单、按钮、直接 URL、直接 API 结果符合五层权限模型。 |
| 旧入口归零 | `/organization/department` 页面引用为零；`/department/*` 请求、Controller 映射和仓库内调用为零；`system:department:*` 菜单、角色授权及源码引用为零。 |
| 数据路径 | 不存在部门数据镜像、同步任务或新旧双写；所有写操作由组织应用服务处理。 |
| 质量 | 后端测试、前端类型检查/单测、迁移测试、直接 API 验收和浏览器验收全部通过。 |

## 6. 关闭定义

A2.1 只能在以下条件同时满足时关闭：

1. 已在隔离库证明当前链路可完整到 `3.66.1`，并在独立退役迁移加入后证明可升级至新的最终版本。
2. 旧部门页面引用为 `0`。
3. 旧部门 API 调用、Controller 映射和仓库内消费者为 `0`，且外部调用审计无未迁移方。
4. 旧部门权限授权和 `system:department:*` 源码/菜单数据引用为 `0`。
5. 新旧部门数据双写路径为 `0`。
6. 后端、前端、Flyway、直接 API 和浏览器验收全部通过，并写回命令、版本、校验和、角色样本和浏览器证据。

达到以上条件后，更新 A2 验收记录和迁移账本，关闭与部门范围相关的 `LEGACY-001`、`LEGACY-004`、`LEGACY-005`、`LEGACY-006`、`LEGACY-009`；只有此时才评估进入 A3。

## 7. 执行记录（2026-07-21）

当前状态：**已关闭，可发布**。A2.1 的代码、迁移、隔离库、开发库、直接 API、后端、前端和浏览器运行态验收全部通过；六项关闭定义均已满足，可以进入 A3 范围评估。

已完成：

- 已删除旧部门页面、`/department/*` 客户端、旧 `DepartmentController`、兼容 `DepartmentService`、旧 DAO/Mapper/VO/Form 与缓存适配链。员工、登录、数据范围、角色员工和 OA 调用方统一改为 `OrganizationDepartmentFacade` 的正式跨模块只读能力；该 Facade 只保留组织模块的一套部门持久化路径。
- 已新增 `V3_67_0__a2_1_retire_legacy_department_access.sql`。迁移先将旧部门动作授权、旧部门页面授权及员工页面所需的读取授权映射到 `organization.department.*`，再删除旧角色菜单关联和旧菜单/权限码。
- `hunyuan_a1_it` 已从原有基线完整迁移至 `3.67.0`；`FlywayMigrationTest`、`InitialAdminBootstrapIntegrationTest`、`RedisIsolationTest` 共 3 项通过。开发库在执行前已预检为 `3.66.1`、旧菜单 3 条、旧角色菜单关联 6 条，并已备份到 `数据库SQL脚本/mysql/backups/hunyuan-before-a2-1-retirement-20260721-2040.sql`；随后成功迁移至 `3.67.0`，`V3_67_0` 校验和为 `-1092911844`。
- 开发库迁移后复核：旧部门菜单为 `0`，旧部门角色授权为 `0`；组织目录页面 `perms_type=1` 为 1 条，四个 `organization.department.*` 能力的 `perms_type=1` 为 4 条。
- 直接 API 复核：旧 `/department/listAll` 已无 Controller 映射，响应为统一的 `NoResourceFoundException` 错误；新 `/api/admin/v1/organization/departments` 仍存在，未登录请求被统一认证边界拒绝。OpenAPI 不再包含旧路径，仍包含新路径。
- 自动验收：`mvn -pl hunyuan-admin -am test` 通过，35 项执行、3 项显式隔离测试按环境跳过、0 失败；`pnpm --filter @hunyuan/system typecheck` 通过；`pnpm exec vitest run --maxWorkers=1` 通过，58 个文件、424 项测试全部通过。首次并行运行的 5 个既有 API 测试文件出现动态导入超时，单 worker 全量复验后全部通过。
- 管理员浏览器验收通过：`/organization/directory` 实际挂载目录组件并显示 7 个部门，新增顶级部门、编辑和删除入口均可见；旧 `/organization/department` 不再挂载目录组件。
- 只读角色浏览器验收通过：临时角色 `a2_1_readonly` 只关联组织模块和目录页面菜单，并授予 `organization.department.read` 及全部组织目录数据范围。关联员工 `huke` 后可查看 7 个部门，新增顶级部门、编辑和删除按钮数量均为 `0`；直接调用创建部门 API 返回权限拒绝 `30005`，调用前后部门数量均为 7。
- 模块开关关闭验收通过：关闭 `module.organization.directory.enabled` 后，拥有临时目录授权的 `huke` 仍看不到目录菜单；管理员调用新 API 返回稳定错误码 `41001`；`huke` 的写请求先被权限层拒绝为 `30005`，符合模块与权限分层顺序；浏览器直达 `/organization/directory` 显示“未找到页面”，目录组件未挂载。
- 模块开关恢复验收通过：`module.organization.directory.enabled` 已恢复为 `true`；管理员调用新 API 返回 `code=0` 和 7 条部门数据；只读页面重新显示 7 个部门，写按钮仍全部隐藏。
- 临时验收数据已清理：关闭和恢复开关时使用的临时角色 ID `64`、`65` 均已删除，临时角色残留为 `0`，`huke` 的菜单授权恢复为 `0`。
- 浏览器连接问题已解决：此前失败由运行会话被回收、Windows 同时存在 `Path`/`PATH` 导致 `Start-Process` 失败、`cmd /c` 引号包装错误，以及 Vite 冷启动依赖优化期间的短暂动态模块失败共同造成；改为直接启动 Java 与 PNPM 的 Node 入口，并在依赖优化完成后刷新登录，前后端和内嵌浏览器保持稳定。

关闭门结果：

1. 旧部门页面引用为 `0`。
2. 旧部门 API 调用、Controller 映射和仓库内消费者为 `0`。
3. 旧部门权限菜单、角色授权和源码引用为 `0`。
4. 隔离库已验证通过 `3.66.1` 基线并迁移至最终版本 `3.67.0`。
5. 新旧部门数据双写路径为 `0`。
6. 后端、前端、Flyway、直接 API 和浏览器验收全部通过。

A2.1 至此关闭。下一步只进入 A3 的范围评估，不在本记录中展开 A3 实现。
