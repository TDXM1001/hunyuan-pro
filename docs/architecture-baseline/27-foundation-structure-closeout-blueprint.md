# 底座结构收口蓝图

## 1. 决策与状态

截至 2026-07-24，A0 至 A4.5 已完成既有能力的接口稳定化、兼容退役、数据迁移和若干纵切的模块化试点，但这些关闭结论不等于整套底座已经完成职责归位。

本项目的明确产品目标是形成一套可复用、可装配、可验证的管理后台底座。因此，应用目录中的完整平台功能、后端返回前端源码路径、前端模块声明未参与应用装配，以及 `hunyuan-base` 同时承载平台能力、接口和基础设施等问题，均属于必须关闭的产品问题，不再仅作为“以后发生业务变更时处理”的历史例外。

当前状态：`FOUNDATION_STRUCTURE_CLOSEOUT_REQUIRED`。

在本蓝图关闭前，不启动首个真实业务纵切。底座结构关闭后，再回到真实业务选题和纵切实施。

## 2. 现状证据

本次使用 Codebase Memory 项目 `E-my-project-hunyuan-pro-a4-5-closeout-20260724` 复核代码图，并与当前源码和架构文档交叉确认。

### 2.1 前端

| 范围 | Codebase Memory 结果 | 结论 |
| --- | --- | --- |
| `apps/hunyuan-system` | 709 个节点、1,189 条关系、121 个 File 节点 | 应用仍拥有大量完整功能 |
| `packages/features` | 179 个节点、374 条关系、28 个 File 节点 | 目前只有 organization、identity-employee、access 三组抽包成果 |
| 应用到 feature | 19 条导入 | 已形成正确的装配方向 |
| feature 到应用 | 0 条导入 | feature 未反向依赖应用，现有方向应保留 |

当前源码还存在以下结构性问题：

1. `apps/hunyuan-system/src/api/system` 仍有 30 个文件，拥有配置、字典、文件、消息、日志、短信、任务、序列号和运行时等完整客户端能力。
2. `apps/hunyuan-system/src/views/support` 仍有 24 个文件，拥有完整平台管理页面和业务交互。
3. 后端菜单继续返回 `component` 字段，`login-adapter.ts` 通过 `import.meta.glob('../../views/**/*.vue')` 和源码组件路径定位页面。
4. 三个现有 feature 已声明标识、路由和能力码，但应用没有统一消费这些模块声明；当前仍依赖应用内薄页面完成装配。
5. `_core` 中的登录、错误页、布局相关页面属于应用壳，但个人资料、密码和安全设置的业务所有权仍需与 `identity.account` 重新对账。

### 2.2 后端

| 范围 | Codebase Memory 结果 | 结论 |
| --- | --- | --- |
| `hunyuan-admin/.../module` | 2,115 个节点、4,582 条关系、188 个 Java 文件、157 个 Route 节点 | identity、access、organization 已形成模块试点，旧 system/support 仍并存 |
| `hunyuan-base/.../base` | 4,415 个节点、10,532 条关系、417 个 Java 文件、32 个 Route 节点 | 公共工程仍拥有 HTTP 入口和大量平台实现 |
| admin 到 base | 297 条导入 | 启动应用高度依赖万能公共工程 |
| base 到 admin | 0 条导入 | 工程依赖方向单向，但职责边界仍过宽 |

当前后端主要问题不是 Maven 依赖循环，而是 `hunyuan-base` 的职责过多：公共类型、Web 基础设施、数据库实现、平台功能、应用服务和 HTTP Route 同时存在。A4 已建立的 Facade 和 ApplicationService 是正确过渡边界，但仍需完成模块归属和接口适配层归位。

## 3. 目标前端结构

目标职责如下，目录名称允许结合现有 Vben 包调整，但职责不能重新合并回应用：

```text
hunyuan-design/
├─ apps/hunyuan-system/            # 管理后台组合入口
├─ packages/app-kernel/            # 生命周期、模块协议、模块注册和路由注册表
├─ packages/admin-shell/           # 布局、导航、标签页和后台外壳
├─ packages/platform-auth/         # 身份会话、能力上下文和登录基础协议
├─ packages/platform-http/         # 请求、错误和协议适配
├─ packages/platform-contracts/    # 稳定公共类型和小型协议
└─ packages/features/
   ├─ organization/
   ├─ identity-account/
   ├─ identity-employee/
   ├─ access/
   ├─ platform-configuration/
   ├─ platform-file/
   ├─ platform-audit/
   ├─ platform-notification/
   ├─ platform-runtime/
   ├─ platform-security/
   └─ platform-devtools/
```

`platform-*` 基础包不拥有具体管理页面。配置管理、审计日志和任务管理等具有完整用户任务的功能仍属于 `features/*`，不能因为名称中包含“平台”而进入通用基础包。

### 3.1 应用允许保留的内容

- 应用启动、环境配置和构建入口。
- Router 初始化、全局守卫和异常处理。
- 请求客户端、身份上下文和 feature 依赖装配。
- 布局、导航、标签页和主题等应用级状态。
- feature 注册清单。
- 首页、登录、无权限、异常和未找到等应用壳页面。

### 3.2 应用禁止继续拥有的内容

- 完整平台或业务页面。
- 具体功能的查询、创建、更新和删除客户端。
- 具体能力码集合和业务状态判断。
- 其他模块的业务类型、表单模型和页面状态。
- 依赖后端源码组件路径加载页面的映射逻辑。

## 4. 前端模块协议

应用通过统一的编译期模块声明装配功能。最终协议至少覆盖：

```ts
export interface AppFeatureModule {
  id: string;
  supportedApps: string[];
  dependencies: string[];
  routes: FeatureRoute[];
  capabilities: string[];
  setup?: (context: AppFeatureContext) => void | Promise<void>;
}
```

具体代码实现时使用英文标识保持工程一致性，所有新增说明、错误信息和非显然逻辑注释使用中文并采用 UTF-8。

模块注册必须满足：

1. 应用显式注册允许构建的 feature，不扫描未知业务目录。
2. feature 只依赖平台协议和其他模块公开接口，不依赖应用别名、应用 Store 或应用请求单例。
3. 应用负责注入请求、身份、导航和必要的跨模块只读 provider。
4. 模块依赖缺失、标识重复或路由 ID 冲突时在启动阶段明确失败。
5. 页面继续按路由懒加载，所有 feature 仍随管理后台统一构建和部署。

## 5. 稳定路由与菜单边界

后端只返回模块启用、菜单展示信息、稳定路由 ID、能力和必要参数，不再返回 `views/...` 等前端源码组件路径。

```text
后端 menu.routeId
  -> 前端 feature route registry
  -> 本地懒加载组件
```

迁移期间允许同时读取历史 `component` 和新 `routeId`，但必须遵守：

1. 新增菜单只能使用 `routeId`。
2. 已迁移 feature 优先使用 `routeId`，历史 component 只作为旧数据兼容输入。
3. 所有现存菜单完成映射和浏览器验收后，通过独立 Flyway 迁移停止使用源码组件路径。
4. 最终删除 `login-adapter.ts` 中针对业务页面的路径归一化和 `views/**/*.vue` 扫描。

## 6. 目标后端结构

当前继续保持一个 Spring Boot 应用和统一数据库，先使用 Java 包与 ArchUnit 建立边界，不以大量 Maven 拆分作为完成标准。

```text
bootstrap/
interfaces/adminapi/
application/
modules/
├─ identity/
├─ access/
├─ organization/
├─ platformconfiguration/
├─ platformfile/
├─ platformaudit/
├─ platformnotification/
├─ platformruntime/
├─ platformsecurity/
└─ platformdevtools/
sharedkernel/
infrastructure/
```

每个模块内部按需使用 `api`、`application`、`domain` 和 `infrastructure`。HTTP Controller 归 `interfaces/adminapi` 或模块 `api` 入口，不能继续由通用 base 工程拥有；公开 Facade 和 DTO 不暴露 Entity、DAO、Mapper 或历史 Form/VO。

`sharedkernel` 只保留稳定基础类型、当前用户抽象、分页、统一错误和极少量公共协议。数据库、Redis、文件、邮件和第三方实现归 `infrastructure` 或所属平台模块的基础设施层。

## 7. 实施批次

### F0：契约冻结与守卫

- 固定应用允许保留的目录和现有 feature 公开边界。
- 增加应用、feature、平台包之间的依赖方向测试。
- 建立后端 base Route、跨模块持久化访问和公开 DTO 泄漏基线。
- 记录全部菜单 component、前端页面、客户端和后端 owner 的一一映射。

关闭状态（2026-07-24）：F0 已建立前端遗留文件递减守卫、feature 反向依赖守卫、base HTTP Route 冻结、平台支撑跨 owner 持久化守卫和公开 Facade 模型泄漏守卫；开发库 26 个页面菜单已逐项映射目标 owner 与稳定 routeId/处置决定。测试和完整账本见 [28-f0-foundation-contract-and-ownership-freeze.md](28-f0-foundation-contract-and-ownership-freeze.md)。下一阶段进入 F1。

### F1：App Kernel 与稳定路由注册

- 建立统一 `AppFeatureModule`、注册表、依赖校验和稳定 route ID。
- 将 organization、identity-employee、access 接入真实应用注册，而不是只导出未使用的描述对象。
- 后端菜单契约增加稳定 route ID，保持历史 component 只读兼容。
- 完成管理员、受限角色、模块关闭和直达 URL 浏览器验收。

### F2：配置、文件与账号功能归位

- 抽取 identity-account、platform-configuration 和 platform-file feature。
- 迁移个人资料、密码、配置、字典和文件的页面、API、类型、能力和测试。
- 删除应用内对应实现，只保留应用装配。

### F3：审计、通知与安全功能归位

- 抽取 platform-audit、platform-notification 和 platform-security feature。
- 迁移消息、日志、短信、安全设置、登录失败和数据脱敏验证入口。
- 事务邮件继续保持内部后端能力，不虚构前端管理模块。

### F4：运行时与开发工具归位

- 抽取 platform-runtime 和 platform-devtools feature。
- 迁移任务、序列号、重载、缓存和协议验证工具。
- 开发工具与生产导航、权限和发布边界保持隔离。

### F5：后端平台模块归位与 base 收缩

- 按 owner 迁移 A4 已稳定的 Controller、Facade、ApplicationService、持久化和基础设施实现。
- `hunyuan-base` 不再拥有业务或平台 HTTP Route。
- 收缩 shared-kernel，关闭新模块对历史 Service、DAO、Mapper、Entity 和 Form/VO 的直接依赖。
- 每次只迁移一个 owner，保持数据库单一写路径，不复制表、不双写。

### F6：历史装配退役与底座验收

- Flyway 迁移现有菜单到稳定 route ID，退役源码 component 路径。
- 删除应用中的业务页面扫描、历史 API 和无消费者薄入口。
- 新建一个最小验收 feature，证明模块可以通过公开协议接入，不依赖应用内部实现。
- 完成 Codebase Memory、OpenAPI、数据库、测试、生产构建和浏览器总验收。

## 8. 每批关闭门

每一批必须同时满足：

1. 迁移前消费者、路由、菜单、权限、数据表和依赖图已经冻结。
2. 新旧实现不双写，数据只有一个 owner 和写路径。
3. 应用只装配公开 feature，不导入其内部文件。
4. 后端跨模块只调用公开 Facade，不访问其他模块持久化实现。
5. 旧消费者归零后才删除兼容入口。
6. TypeScript、Vitest、Maven、ArchUnit、Flyway、OpenAPI 和 `git diff --check` 通过。
7. 新增和修改文本为严格 UTF-8，新增非显然代码注释使用中文。
8. 至少使用管理员和一个受限角色完成真实浏览器验收。
9. Codebase Memory 使用新项目名重建，并与源码、数据库和运行态结果交叉核对。

## 9. 最终完成定义

底座结构只有同时满足以下条件才算完成：

1. `apps/hunyuan-system` 只承担启动、壳层、全局状态、守卫和 feature 装配。
2. 所有完整用户任务均归属明确 feature，应用内不再存在完整平台业务页面和客户端。
3. 应用真实消费统一模块协议，feature 到应用的反向依赖保持为 0。
4. 后端菜单不再存储或返回前端源码组件路径，所有页面通过稳定 route ID 注册。
5. `hunyuan-base` 不再拥有 HTTP Route 或无边界平台业务，shared-kernel 保持最小。
6. 新增一个 feature 时，只需实现公开协议并加入显式注册清单，不修改应用业务逻辑。
7. 前端依赖图、后端模块依赖、能力、数据所有权和运行态行为均有自动守卫。
8. 全量构建、隔离迁移、开发库升级、直接 API 和浏览器验收通过。

达到以上条件后，才能把底座报告为完成，并重新进入首个真实业务纵切的选题与实现。
