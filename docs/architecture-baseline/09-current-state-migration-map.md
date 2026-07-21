# 当前现状到目标架构迁移映射

## 1. 文档目的

本文是架构基线之后的 A0 执行账本。它描述当前仓库如何逐步靠近目标架构，不要求一次性重命名目录、拆分 Maven 工程或重写现有业务。

迁移采用“旧能力继续运行，新能力进入新边界”的方式。未完成业务确认前，不把示例模块自动升级为正式产品模块。

## 2. 当前基线快照

| 区域 | 当前实现 | 目标映射 | 处置 |
| --- | --- | --- | --- |
| 前端应用 | `hunyuan-design/apps/hunyuan-system` | `apps/admin-web` + `admin-shell` | KEEP / ADAPT |
| 前端公共包 | `hunyuan-design/packages` 中的 Vben 基础包 | `platform-*` | KEEP，逐步收紧依赖 |
| 前端业务页面 | `apps/hunyuan-system/src/views`、`src/api` | `packages/features/<feature>` | 新模块按目标组织，旧页面按变更迁移 |
| 后端启动应用 | `hunyuan-backend/hunyuan-admin` | `bootstrap` + `interfaces/admin-api` | KEEP / ADAPT |
| 后端公共工程 | `hunyuan-backend/hunyuan-base` | `shared-kernel` + `infrastructure` + 平台模块 | HARDEN，禁止继续扩大万能公共层 |
| 后端系统能力 | `admin.module.system` | identity/access、organization、platform-support | 按数据所有权拆分 |
| 后端业务能力 | `admin.module.business` | 真实业务模块 | 逐项确认 KEEP / ADAPT / RETIRE |
| 数据库结构 | `数据库SQL脚本/mysql/hunyuan.sql` + `v3.15.0` 至 `v3.64.0` 增量 | Flyway 版本迁移 | ADAPT，当前结构形成 `V3.64.0` 基线，历史脚本只保留审计 |
| 认证授权 | Sa-Token、Redis、菜单权限和数据范围 | Principal、capability、data scope 适配层 | KEEP / ADAPT |
| BPM | 代码和菜单已退役 | 不进入新模块目录 | RETIRE |

当前后端仍是一个 Spring Boot 应用，现有 Maven 模块是工程依赖边界，不直接等同于业务模块边界。第一阶段使用包级规则建立边界，只有编译隔离产生明确收益时才拆分 Maven 模块。

## 3. 迁移原则

1. 旧 API 和旧菜单在没有替代验收前保持可用。
2. 新业务模块必须拥有自己的用例、数据所有权、公开 Facade 和迁移脚本。
3. 跨模块只能依赖公开接口，不能直接依赖对方 DAO、Mapper 或 Entity。
4. 旧代码允许存在已登记例外，但不允许新增同类例外。
5. 数据库迁移从当前 `v3.64.0` 状态接管，不重写历史版本号。

## 4. 迁移批次

### A0：现状账本

- 完成本文映射。
- 对 `system`、`support`、`business` 的表、菜单、权限和接口建立模块所有权清单。
- 对示例业务逐项决定保留、适配或退役。
- 记录每个遗留例外的责任边界和关闭条件。

### A1：工程守卫

- Flyway 接管空库迁移和已有 `v3.64.0` 数据库的 baseline。
- ArchUnit 检查基础模块方向、领域层依赖和模块循环，并冻结当前已知例外。
- phpStudy MySQL 使用独立的 `hunyuan_it` 数据库，Redis 使用独立 DB；只有显式设置集成测试环境变量时才运行，禁止回退连接开发库。
- Maven、前端类型检查和前端单测作为 CI 必过项。

### A2：首个垂直模块

使用第一个真实业务模块验证完整链路：数据库迁移、应用用例、公开接口、OpenAPI、前端 feature、权限和浏览器验收。若业务模块尚未确定，可先用组织目录的一个完整读写用例做架构试点。

关闭状态（2026-07-21）：A2 已正式选择 `organization` 组织目录架构试点，首个闭环为部门目录管理。实现、迁移、权限和浏览器运行态验收已完成；A2 阶段保留的旧入口已由 A2.1 完成退役。范围、稳定能力编码和完成定义见 [11-a2-organization-directory-vertical-slice.md](11-a2-organization-directory-vertical-slice.md)。

### A2.1：组织目录兼容入口退役与迁移链路收口

在开启新的业务模块前，先关闭 A2 保留的部门目录兼容边界。该批次只处理旧部门页面、`/department/*` API、`system:department:*` 授权和对应兼容代码，不扩展为员工、岗位、角色、OA 或其他组织域的重构。

执行顺序固定为：先在合规 `_it` 库重放并核验当前迁移链路至 `3.66.1`，再盘点和迁移全部消费者与角色授权，随后用新的独立 Flyway 迁移退役旧菜单及权限码，最后在引用归零后删除旧 Controller 和兼容 Facade。不得在仍有旧调用方时删除兼容入口，也不得以新旧双写作为过渡方案。

关闭状态（2026-07-21）：A2.1 已通过隔离迁移、旧入口归零、权限映射、自动测试、直接 API 和浏览器运行态全部关闭门。详细范围、执行证据和关闭定义见 [13-a2-1-organization-directory-compatibility-retirement.md](13-a2-1-organization-directory-compatibility-retirement.md)。当前可以进入 A3 的范围评估，但不因 A2.1 关闭而自动启动新的大模块实现。

### A3：持续迁移

后续只迁移正在发生业务变更的模块，禁止为了目录整齐进行大规模搬迁。每个模块关闭自己的例外后，再从 ArchUnit 冻结清单中移除对应记录。

## 5. 遗留例外台账

| 编号 | 当前例外 | 当前处置 | 关闭条件 |
| --- | --- | --- | --- |
| LEGACY-001 | 旧 Controller 直接调用 Service | 部门范围已关闭；其他模块继续冻结 | 其他模块下一次业务变更时引入 application 用例 |
| LEGACY-002 | MyBatis 注解实体位于旧 `domain` 包 | 冻结 | 新模块领域对象与持久化模型分离 |
| LEGACY-003 | `hunyuan-base` 同时承载平台、基础设施和公共工具 | 收敛 | 新公共能力至少有两个明确使用方，且归属已记录 |
| LEGACY-004 | 前端业务页面位于应用目录 | 部门范围已关闭；其他模块继续冻结 | 新模块通过 feature 包注册；其他旧页面按模块变更迁移 |
| LEGACY-005 | 旧 API 使用无版本动作路径 | 部门范围已关闭；其他模块保持兼容 | 新接口进入 `/api/admin/v1`，其他旧接口完成替代验收后再下线 |
| LEGACY-006 | 菜单、角色权限仍以历史编码为主 | 部门范围已关闭；其他模块保持兼容 | 其他模块完成能力和数据范围盘点后再调整编码 |
| LEGACY-007 | 空库只有结构，没有初始管理员和平台种子数据 | 已由 `V3.65.0` seed 与环境变量 bootstrap 关闭 | 保持凭据不进入 Flyway，持续执行隔离库验收 |
| LEGACY-008 | 历史支持、Demo、商品和 OA 菜单与当前前端能力不完全一致 | 不进入平台 seed，保留审计 | 对每组菜单完成 KEEP/RETIRE 决策并清理孤儿关系 |
| LEGACY-009 | 前端真实页面尚未系统使用 access code 隐藏无权按钮 | 部门范围已关闭；其他模块仍以 API 权限为强制边界 | 其他模块完成按钮权限映射与非管理员浏览器验收 |

## 6. A1 验收标准

- 空 MySQL 可以从 Flyway 迁移到 `3.64.0`，并确认 BPM 表已移除。
- 已有 `v3.64.0` 数据库只建立 Flyway baseline，不重复执行历史 DDL/DML。
- ArchUnit 测试通过，新增目标模块违反依赖方向时构建失败。
- Spring 集成测试不读取开发机 MySQL、Redis、邮件或文件目录。
- 后端 `mvn test`、前端类型检查和现有前端单测均通过。
- 所有无法自动验证的遗留例外都登记在本文，不以“测试通过”代替架构验收。

本机隔离测试入口：

```powershell
cd hunyuan-backend
.\scripts\run-integration-tests.ps1
```

脚本只创建或复用名称以 `_it` 结尾的测试库，默认是 `hunyuan_a1_it`；不会删除数据库，也不会修改 `hunyuan` 开发库。

## 7. 菜单与权限后续盘点

菜单和角色权限不在 A0/A1 中直接重命名或重建。首个垂直模块开始前，按以下矩阵复核：

```text
模块启用 -> 页面能力 -> 操作能力 -> 数据范围 -> 业务状态
```

盘点结果需要同时覆盖后端 `api_perms`、前端路由/菜单可见性、角色菜单关联、数据范围策略和直接 URL/API 访问。任何一项缺失都不能视为权限迁移完成。

## 8. A0/A1 执行结果（2026-07-21）

状态：A0 迁移映射完成，A1 工程守卫完成。

- Flyway 已接入四套环境配置，默认关闭；已有数据库完成 `v3.64.0` 预检后通过 `HUNYUAN_FLYWAY_ENABLED=true` 接管。
- `V3_64_0__current_schema_baseline.sql` 从 phpStudy MySQL 8 当前结构导出，只包含 46 张业务表结构，不包含本机数据。
- phpStudy 隔离库 `hunyuan_a1_it` 已迁移到 `3.64.0`，BPM/Flowable 表数量为 0。
- Redis 隔离测试使用 DB 15，探针键在测试结束时删除。
- ArchUnit 共执行 5 条规则，覆盖基础模块方向、领域层 Web 依赖、模块循环和跨模块持久化访问；冻结存储已锁定，不能在构建时自动重建。
- 后端完整测试通过；基础模块 12 个测试通过，管理模块 10 个测试中 8 个通过、2 个显式隔离测试默认跳过。
- 显式执行 phpStudy MySQL/Redis 隔离测试时 2 个测试全部通过。
- 前端类型检查通过，业务前端 20 个测试文件、113 个测试全部通过。

后续进展：`V3.65.0` 已加入非敏感平台 seed，初始管理员改为环境变量驱动 bootstrap；具体范围、权限差异和未关闭边界见 [10-platform-seed-bootstrap-and-access-audit.md](10-platform-seed-bootstrap-and-access-audit.md)。

开发库接管进展：2026-07-21 已在仓库外完成备份，`hunyuan` 建立 `3.64.0` baseline 并成功迁移到 `3.65.0`；没有执行管理员 bootstrap，现有员工和管理员保持不变。完整记录和剩余安全决策见文档第 10 篇第 9 节。

A2 进展：`V3.66.0` 建立组织目录模块、菜单、能力和数据范围，`V3.66.1` 修复旧菜单记录的权限类型；开发库已迁移到 `3.66.1`，模块开关恢复为 `true`，临时只读验收角色和测试数据已清理。

A2.1 进展：`V3.67.0` 已在隔离库和开发库执行，旧部门页面、`/department/*` Controller/客户端、`system:department:*` 菜单及角色授权已退役，旧入口数据归零。后端和前端自动验收、隔离迁移、直接 API 与浏览器运行态验收全部通过：管理员可见完整目录与写入口；只读角色可见 7 个部门但写入口为 0，直接写 API 返回 `30005` 且数据不变；模块关闭时管理员新 API 返回 `41001`，菜单和直达页面不可用；开关恢复后功能正常，临时角色和关联数据残留为 0。A2.1 已关闭，当前可以进入 A3 范围评估，但尚未启动 A3 实现。证据见 [13-a2-1-organization-directory-compatibility-retirement.md](13-a2-1-organization-directory-compatibility-retirement.md)。
