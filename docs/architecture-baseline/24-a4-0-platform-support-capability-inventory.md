# A4.0 平台支持能力总盘点与边界冻结

## 1. 状态与范围

截至 2026-07-23，A3.1、A3.2、A3.3 和 A3.4 已关闭。A4.0 不启动大范围代码迁移，
先冻结平台支持能力的 owner、消费者、数据对象、入口和处置方向，作为后续纵切实施的
唯一入口。

本账本覆盖 `hunyuan-base/module/support`、`hunyuan-admin/module/system/support`、
登录认证依赖以及 `hunyuan-design/apps/hunyuan-system/src/api/system` 中仍有前端
消费者的支持能力。A3.4 已关闭的商品、分类、OA 和工程 Demo 不重新纳入本账本。

本次盘点使用 Codebase Memory 当前索引
`E-my-project-hunyuan-pro-a4-1-account-center-20260723`，并以当前源码、Flyway、前端页面和测试交叉核对。
代码图用于发现调用簇和消费者，不单独作为数据库、权限、编译或运行态关闭证明。

## 2. 决策定义

| 决策 | 含义 |
| --- | --- |
| `KEEP` | 能力已有稳定平台价值，保持现有实现方向，补齐契约和验收 |
| `ADAPT` | 能力保留，但需要重建 owner、API、权限、前端或内部协作边界 |
| `RETIRE` | 无真实采用或无平台价值，完成消费者、数据和权限审计后退役 |
| `EVALUATE` | 证据不足，暂不扩建或删除，先完成采用度与运行责任审计 |

## 3. 能力冻结账本

| 能力 | 当前实现与数据 | 当前入口/权限 | 主要消费者 | Owner | 决策 |
| --- | --- | --- | --- | --- | --- |
| 登录认证与会话 | `system.login`、Sa-Token、登录缓存、`t_employee` 账号字段、`t_login_log` | `/login/*`；前端 `api/core/auth.ts`、登录适配器 | 前端登录、员工状态变更、权限路由装载、登录日志 | `identity.authentication` | `ADAPT` |
| 账号中心与个人自助 | 旧 `EmployeeController`；`t_employee` 的个人资料、头像、密码 | `/employee/update/center`、`/employee/update/avatar`、`/employee/update/password`、`/employee/getPasswordComplexityEnabled` | 个人中心、登录态、文件存储、密码安全策略 | `identity.account` | `ADAPT` |
| 密码安全与二次认证 | `securityprotect`、`t_password_log`、`t_login_fail`、三级保护配置 | `support:protect:*`；登录服务直接消费 | 登录、密码修改、登录失败锁定、管理员安全设置 | `platform-security` | `KEEP / HARDEN` |
| 验证码与网络防护 | `captcha`、请求保护与重复提交能力 | `/captcha`、登录验证码；前端登录组件 | 登录、敏感操作、接口防刷 | `platform-security` | `KEEP / ADAPT` |
| 文件与头像 | `file`、`t_file`、本地/云存储实现 | `/file/*`、`support:file:*`；前端 `file.ts` | 头像、附件上传、富文本图片、业务文件引用 | `platform-file` | `KEEP / ADAPT` |
| 配置中心 | `config`、`t_config` | `support:config:query/add/update`；前端 `config.ts` | 运行参数、平台开关、安全配置和业务配置 | `platform-config` | `KEEP / ADAPT` |
| 字典中心 | `dict`、`t_dict`、`t_dict_data` | `support:dict:*`、`support:dictData:*`；前端 `dict.ts` | 业务表单、状态枚举、页面选项和配置展示 | `platform-dict` | `KEEP / ADAPT` |
| 消息中心 | `message`、`t_message` | `/message/*`；前端 `message.ts` | 站内信、未读数、管理员发送和用户阅读 | `platform-message` | `KEEP / ADAPT` |
| 帮助与反馈 | `helpdoc`、`feedback`、`t_help_doc*`、`t_feedback` | `support:helpDoc:*`、`/feedback/*` | 帮助文档页面、用户反馈和管理员处理 | `platform-support` | `EVALUATE` |
| 日志审计 | `loginlog`、`operatelog`、`changelog`，对应日志表 | `support:loginLog:*`、`support:operateLog:*`、`support:changeLog:*` | 安全审计、运维排障、管理员查询 | `platform-audit` | `KEEP / ADAPT` |
| 缓存与 Redis 管理 | `cache`、`redis`、本地 Caffeine 和 Redis 实现 | `support:cache:*`；前端 `cache.ts` | 登录缓存、字典缓存、运行维护和缓存清理 | `platform-cache` | `KEEP / HARDEN` |
| 定时任务 | `job`、`t_smart_job`、`t_smart_job_log` | `support:job:*`；前端 `job.ts` | 平台任务调度、任务执行和执行日志 | `platform-job` | `ADAPT` |
| 短信 | `sms`、`t_sms_template`、`t_sms_send_log` | `support:sms:*`；前端 `sms.ts` | 短信模板、发送记录和验证码类扩展 | `platform-notification` | `EVALUATE / ADAPT` |
| 邮件 | `mail`、`t_mail_template` | 以服务调用为主，暂无稳定管理页面证据 | 登录验证码、通知和模板发送 | `platform-notification` | `EVALUATE` |
| 代码生成 | `codegenerator`、`t_code_generator_config` | `/codeGenerator/*` | 开发人员和工程验收，不属于业务运行闭环 | `platform-devtools` | `KEEP / ISOLATE` |
| 数据脱敏 | `datamasking`、脱敏演示接口和权限 | `support:protect:dataMasking:query` | A3.4 脱敏验证、开发验收和安全测试 | `platform-security` | `KEEP / ISOLATE` |
| 数据追踪 | `datatracer`、`t_data_tracer` | `/dataTracer/query` | 运维排障或审计查询，采用证据不足 | `platform-audit` | `EVALUATE` |
| 热更新与序列号 | `reload`、`serialnumber`、对应记录表 | `support:reload:*`、`support:serialNumber:*`；前端有 API 和页面 | 配置刷新、编号生成和编号使用记录 | `platform-runtime` | `ADAPT` |
| 心跳、重复提交、API 加密等横切能力 | `heartbeat`、`repeatsubmit`、`apiencrypt` | 注解、过滤器和内部服务，无独立业务菜单 | 登录、写操作、敏感接口和请求安全 | `platform-runtime-security` | `KEEP / HARDEN` |

## 4. 当前边界结论

### 4.1 可以直接保留的稳定能力

以下能力已有明确消费者或横切运行责任，不进入退役候选：

- 登录认证、账号状态、密码安全、验证码和会话失效。
- 文件存储、配置、字典、消息、登录日志和操作日志。
- 缓存、Redis、重复提交、API 加密和基础运行安全。

这些能力的下一步不是复制数据表，而是补齐公开 Facade、稳定 API、错误码、权限契约和
运行责任，继续保持单体内单一写路径。

### 4.2 必须先适配再扩展的能力

任务调度、热更新、序列号、短信和代码生成可以保留，但必须先区分平台运行能力、
开发工具和业务通知能力。它们不能继续通过 `system.support` 目录隐式拥有所有业务
语义，也不能在没有消费者登记的情况下增加新页面或新表。

### 4.3 暂不扩建的评估能力

帮助、反馈、邮件和数据追踪目前存在源码或 API，但采用范围、运行责任和外部消费者
证据不足。A4.0 只冻结为 `EVALUATE`，不将“存在 Controller”视为产品承诺，也不因
目录较旧而直接删除。

## 5. A4.1 首个实施纵切：认证与账号中心

A4.1 建议把登录认证、账号中心和个人自助合并为一条纵切，原因是当前四个旧自助入口
仍由旧员工 Controller 保留，源码已明确它们等待 account-center 边界迁移：

```text
/employee/update/center
/employee/update/avatar
/employee/update/password
/employee/getPasswordComplexityEnabled
```

A4.1 的实施范围：

1. 建立 `identity.authentication` 与 `identity.account` 的公开 DTO、Facade 和
   application 用例，不暴露员工 Entity、密码摘要或安全配置内部对象。
2. 让登录只通过账号公开查询获取登录名、状态、头像和最小用户摘要；禁止新增对
   `EmployeeService`、`EmployeeDao`、`EmployeeEntity` 的直接依赖。
3. 将个人资料、头像、修改密码、密码复杂度查询迁移到稳定的账号中心 API；头像只保存
   文件引用，文件内容仍由 `platform-file` 负责。
4. 保持 `t_employee` 为当前单一数据源，不建立账号镜像表，不做新旧双写。
5. 把密码日志、登录失败锁定、二次认证和会话失效作为 `platform-security` 的公开
   协作端口，不在账号中心复制安全规则。
6. 完成前端个人中心、登录适配器、请求刷新和权限路由装载的消费者审计。
7. 在旧入口退役前完成仓库内引用归零、权限迁移、直接 API 负向验收和运行态登录回归。

### 5.1 A4.1 第一批实施状态

截至 2026-07-23，A4.1 已完成第一批账号自助边界改造：

- 新增 `identity.employee.api.EmployeeAccountFacade`，统一承载个人资料、头像、修改密码和密码复杂度查询。
- 新增账号自助公开命令与应用服务，旧 Controller 不再直接依赖 `system.employee.EmployeeService`。
- 保留四个历史 URL 作为兼容适配层，暂未做路由删除或前端切换。
- 个人资料和头像继续写入 `t_employee`，没有建立账号镜像表，也没有新旧双写。
- 修改密码继续调用 `platform-security` 的复杂度、历史重复和密码日志能力，并清理登录缓存。
- 新增账号自助应用服务单测；连同员工管理和旧入口边界测试共 16 个测试全部通过。
- Codebase Memory 已按 A4.1 代码状态重新索引，索引名为
  `E-my-project-hunyuan-pro-a4-1-account-center-20260723`，最新索引节点 15,117、关系 37,148。

当前 A4.1 尚未关闭，剩余工作是前端个人中心消费者迁移、登录主流程改为账号 Facade、
旧 Controller 引用归零、权限与 OpenAPI 契约核对，以及运行态登录和密码修改回归。

### 5.2 A4.1 第二批实施状态

截至 2026-07-23，A4.1 第二批已完成主管理后台的稳定 HTTP 契约和个人中心接入：

- 新增当前登录账号接口：
  `GET/PUT /api/admin/v1/identity/account/me`、`POST /api/admin/v1/identity/account/me/password`
  以及头像和密码策略接口。
- Controller 只从 `SmartRequestUtil` 获取当前用户 ID，不接受前端传入 `employeeId`；
  新增契约测试锁定路径、方法和当前用户 ID 传递边界。
- `hunyuan-design/apps/hunyuan-system` 新增账号 API 封装，资料页已移除演示角色和假数据，
  改为真实读取与保存姓名、用户名、手机号、邮箱和备注。
- 密码页已从“仅显示成功提示”改为真实调用修改密码接口，保留确认密码校验。
- 后端账号 HTTP 契约测试与应用服务测试共 4 个通过，主管理后台前端类型检查通过，
  UTF-8 严格读取和 `git diff --check` 通过。

当前仍不关闭 A4.1，剩余边界为：

- `apps/web-ele` 已根据既有 A3.4 决策确认是独立工程验收模板，不属于当前
  `hunyuan-system` 产品消费者；本轮不迁移其模板个人中心，也不把模板旧接口视为
  A4.1 生产缺口；
- 登录主流程已完成从旧员工服务到 `EmployeeDirectoryFacade` 的消费者核对，并新增
  架构守卫和冻结测试，禁止 `LoginService` 重新依赖员工 Service、Dao 或 Entity；
- 头像上传代码闭环和密码策略页面展示已经完成；旧四个 URL 的负向 HTTP 验收、
  以及运行态登录、头像、密码修改和缓存失效回归尚未完成；
- 需要在完成上述核对后，再决定旧 `EmployeeController` 和旧自助权限是否退役。

### 5.3 A4.1 第三批收口状态

截至 2026-07-23，A4.1 已完成登录消费者和模板应用的边界冻结：

- `LoginService.login`、`LoginService.sendEmailCode` 均通过
  `EmployeeDirectoryFacade.findAuthenticationAccountByLoginName` 获取认证账号；
  登录服务没有直接依赖 `EmployeeService`、`EmployeeDao` 或 `EmployeeEntity`。
- 新增 `LoginServiceIdentityEmployeeBoundaryTest` 和架构守卫，锁定登录服务只能
  通过身份员工公开接口读取账号。
- `apps/hunyuan-system` 是当前认证与账号中心的正式前端消费者；`apps/web-ele`
  继续保留为独立 Vben 模板和工程验收应用，默认开发入口也不承担本产品登录契约。
- 后端已提供头像和密码策略 API；`hunyuan-system` 个人资料页已接入文件服务上传、
  头像文件引用保存和当前用户头像同步，头像上传已完成代码级闭环。

因此，A4.1 还不能关闭。剩余工作已经收敛为：

1. 对四个旧自助 URL 执行负向 HTTP 验收，确认兼容适配层的退役条件。
2. 在可用登录环境执行登录、头像、密码修改和缓存失效回归。

### 5.4 A4.1 第四批验证状态

截至 2026-07-23，第四批代码级验证已完成：

- 后端账号中心、旧入口边界、登录身份边界和架构守卫共 26 个聚焦测试全部通过。
- `@hunyuan/system` 前端 TypeScript 类型检查通过。
- `git diff --check` 通过；本批涉及的 Java、TypeScript 和 Markdown 文件按 UTF-8
  严格读取无异常。
- 结合 A3.1 已确认“从未生产、从未开放正式外部集成”的消费者结论，旧
  `EmployeeController`、旧 `EmployeeService` 和三个旧自助表单已经删除。
- 负向源码契约已改为锁定四个旧 URL、旧 Controller、旧 Service 和旧表单不得恢复。

运行态负向验收结果：

- 修复 `RedisCacheServiceImpl` 按父类型注入缓存管理器导致的启动阻塞后，后端使用
  最新构建包在 `1024` 端口成功启动，MySQL、Redis、配置缓存和 Web 容器初始化完成。
- `/v3/api-docs` 共返回 134 个路径；账号中心 5 个新路径全部存在，四个旧自助路径
  全部不存在。
- 按旧 HTTP 方法直接请求四个旧路径，均返回 HTTP `404` 和业务码 `30002`，确认
  运行态不再命中历史业务处理器。

当前结论仍为：A4.1 未关闭，剩余边界只包括需要有效认证账号执行的登录、头像、
密码修改和登录缓存失效回归。

### 5.5 A4.1 第五批认证态页面回归

- 使用本地开发账号登录主管理后台成功，登录后账号、角色和菜单加载完成。
- 右上角个人中心菜单已接入固定 `/profile` 核心路由；页面可读取当前员工姓名、用户名、
  手机号和邮箱，用户名保持只读。
- 修改密码页已读取后端密码策略，页面显示“密码复杂度校验已启用”。
- 同时补充个人中心核心路由契约测试，避免账号菜单依赖后端动态菜单生成。
- 当时为保护开发账号而未提交头像替换或密码最终修改；该缺口已在 5.7 中使用专用
  `hunyuan` 验收账号完成，不再作为当前阻塞项。

### 5.6 A4.1 非破坏性账号安全回归

- 账号应用服务测试已补齐头像更新后清理登录缓存、密码安全校验通过后更新密码并清理
  登录缓存、旧密码错误时禁止写入，以及密码复杂度配置委托。
- 账号 HTTP 契约测试已补齐当前用户资料读取和密码策略读取，确认两项能力只使用
  `SmartRequestUtil` 提供的当前登录员工 ID。
- 账号中心、旧入口退役、登录身份边界和架构守卫共 31 个聚焦测试全部通过。
- 因此缓存失效已经具备自动化验证证据；真实账号的头像替换和密码最终修改仍保持
  非破坏性边界，不在未确认的情况下自动执行。

### 5.7 A4.1 专用管理员账号运行态验收

截至 2026-07-23，已按确认范围创建专用验收账号 `hunyuan`，并完成认证态运行回归：

- `hunyuan` 已设置为活动管理员，`administrator_flag`、禁用标记和删除标记均符合
  管理员账号要求，角色集合与 `admin` 一致。
- 使用 `hunyuan` 真实登录成功，并通过当前账号资料、密码策略和管理员受保护接口验证；
  验收过程中未输出、记录或持久化明文密码与登录 Token。
- 上传专用 PNG 测试头像后，头像保存接口成功；重新读取当前资料返回解析后的头像 URL，
  `t_employee.avatar` 保存上传接口返回的 `fileKey`，证明文件引用、缓存清理和展示解析闭环有效。
- 修改密码请求按当前启用的 SM4 `ApiDecrypt` 协议加密提交，接口成功更新密码并清理登录缓存；
  旧密码再次登录失败，新密码重新登录成功。
- 新密码登录后的新会话仍可访问管理员受保护接口，最终数据库复核确认账号启用、未删除、
  管理员标记有效，且角色集合继续与 `admin` 一致。

至此，5.4 中记录的最后一项运行态缺口已完成；A4.1 的账号中心实现、旧入口退役、
认证态页面和专用账号运行回归均已具备验收证据。

### 5.8 A4.2 第一批文件平台化

截至 2026-07-23，A4.2 先完成文件能力的稳定边界，不扩张到配置和字典：

- 新增 `PlatformFileFacade`、`PlatformFileApplicationService` 和稳定上传结果 DTO，
  将历史 `FileService` 与本地/云存储实现隐藏在平台文件边界之后。
- 新增稳定 HTTP 路由：`POST /api/admin/v1/platform/files` 和
  `GET /api/admin/v1/platform/files/url`；旧 `/support/file/*` 保留为兼容入口，暂不删除。
- 账号头像上传已切换到稳定文件上传路由；员工目录和登录头像解析改为依赖
  `PlatformFileFacade`，架构守卫禁止它们重新依赖 `IFileStorageService`。
- 文件上传结果映射、历史失败码透传、路由参数显式契约、原有落库失败补偿逻辑均已覆盖测试。
- 本批后端聚焦测试 `32` 个通过，其中基础文件能力 `7` 个、管理员侧目录/登录 `5` 个、
  架构守卫 `20` 个；前端文件路由契约和类型检查通过。

A4.2 尚未关闭，下一批继续处理配置中心和字典中心的公开边界；文件下载、文件管理查询和
  历史权限命名空间的迁移仍需独立验收，不与本批上传/头像链路混合。

### 5.9 A4.2 第二批字典平台读取边界

截至 2026-07-23，字典中心已完成稳定读取契约和主管理后台读取消费者迁移：

- 新增 `PlatformDictionaryFacade`、`PlatformDictionaryApplicationService` 以及字典、
  字典项和分页查询公开 DTO；稳定边界不暴露历史 `DictVO`、`DictDataVO`、Entity 或 DAO。
- 新增稳定读取路由：`POST /api/admin/v1/platform/dictionaries/query`、
  `GET /api/admin/v1/platform/dictionaries`、`GET /api/admin/v1/platform/dictionaries/items`、
  `GET /api/admin/v1/platform/dictionaries/{dictionaryId}/items`。
- `AdminDictController` 的历史读取路由继续保留原响应对象，但已转经
  `PlatformDictionaryFacade` 兼容适配；历史增删改、启停与缓存失效写路径保持不变。
- `hunyuan-system` 字典分页、单字典项和全量字典项读取已切换至稳定平台路由；写入操作继续
  使用历史路径，避免在本批改变权限、缓存失效和管理写入语义。
- 后端 DTO 映射、稳定路由契约和既有架构守卫共 26 个聚焦测试通过；前端字典路由契约测试与
  `@hunyuan/system` TypeScript 类型检查通过。

A4.2 尚未关闭。字典写入契约、旧 `/support/dict/*` 读取路由退役条件，以及配置中心的
稳定公开边界仍需分批完成；不得把本批读取迁移视为全部字典管理能力的替换。

### 5.10 A4.2 第三批配置管理读取边界

截至 2026-07-23，配置中心已完成受权限保护的管理读取稳定边界：

- 新增 `PlatformConfigurationFacade`、`PlatformConfigurationApplicationService` 以及配置
  分页查询、配置摘要公开 DTO；稳定契约不暴露历史 `ConfigVO`、Entity 或 DAO。
- 新增稳定读取路由：`POST /api/admin/v1/platform/configurations/query`，继续要求
  `support:config:query` 权限，避免配置值在无授权场景暴露。
- `AdminConfigController` 的历史 `/support/config/query` 路由继续保留原响应对象，但已转经
  `PlatformConfigurationFacade` 兼容适配；新增、更新、缓存刷新和运行时按 key 读取保持不变。
- `hunyuan-system` 配置分页读取已切换稳定平台路由；新增与更新仍保留历史写入路径，避免本批
  改变配置缓存、运行时重载和安全策略的写入语义。
- 配置 DTO 映射、历史失败码透传、稳定路由契约、既有字典契约和架构守卫共 26 个后端聚焦测试
  通过；前端配置契约测试与 `@hunyuan/system` TypeScript 类型检查通过。

A4.2 尚未关闭。配置写入公开契约、历史读取路由退役条件，以及字典写入契约仍需独立评估；
运行时安全策略和缓存读取不是本批迁移对象，不能直接替换为管理 API。

### 5.11 A4.2 第四批配置管理写入边界

截至 2026-07-23，配置管理已完成稳定查询、创建和更新契约：

- 稳定配置管理路由完整覆盖 `POST /api/admin/v1/platform/configurations/query`、
  `POST /api/admin/v1/platform/configurations` 和
  `PUT /api/admin/v1/platform/configurations/{configurationId}`；三条路由沿用原有
  `support:config:query`、`support:config:add`、`support:config:update` 权限。
- 新增创建、更新公开命令 DTO；更新 ID 只从稳定 URL 路径接收，命令体不再接受 ID，避免路径
  与请求体的标识歧义。
- `PlatformConfigurationApplicationService` 仍委托 `ConfigService.add` 和
  `ConfigService.updateConfig`，因此配置重复校验、失败码、缓存刷新与运行时读取语义不变。
- `AdminConfigController` 的三个历史管理路由全部转经 `PlatformConfigurationFacade`；
  `hunyuan-system` 的配置查询、创建、更新均已切换稳定平台路由，历史路由继续保留兼容。
- 后端创建/更新映射、失败语义、路径契约和架构守卫共 25 个聚焦测试通过；前端配置测试和
  `@hunyuan/system` TypeScript 类型检查通过。

配置管理稳定接口已具备完整的管理端契约，但 A4.2 尚未关闭：旧 `/support/config/*` 路由的
退役条件仍需在版本兼容窗口后独立验收；内部按 key 读取、缓存刷新和安全策略调用保持为
运行时能力，不纳入 HTTP 接口替换范围。

### 5.12 A4.2 第五批字典本体创建与更新边界

截至 2026-07-23，字典本体已完成稳定查询、创建和更新契约：

- 新增 `POST /api/admin/v1/platform/dictionaries` 与
  `PUT /api/admin/v1/platform/dictionaries/{dictionaryId}`；分别沿用原
  `support:dict:add` 和 `support:dict:update` 权限。
- 新增字典创建、更新公开命令 DTO；更新 ID 仅由稳定 URL 路径提供，防止请求体与路径标识
  不一致。
- 稳定 Facade 继续委托 `DictService.add`、`DictService.update`，保留字典编码唯一性校验、
  历史失败码和缓存失效语义；历史 Controller 的创建和更新入口已转经 Facade。
- `hunyuan-system` 的字典分页、创建、更新和读取已使用稳定路由；字典启停、删除以及全部
  字典项写入仍保留历史路径，作为后续独立子批，不能据此判定字典管理完全迁移。
- 字典创建、更新对象映射、路径契约和架构守卫共 25 个后端聚焦测试通过；前端字典测试与
  `@hunyuan/system` TypeScript 类型检查通过。

### 5.13 A4.2 第六批字典本体完整管理边界

截至 2026-07-23，字典本体管理已完成稳定查询、创建、更新、启停、单删和批删契约：

- 新增稳定动作路由：`POST /api/admin/v1/platform/dictionaries/{dictionaryId}/toggle-disabled`、
  `POST /api/admin/v1/platform/dictionaries/batch-delete`、
  `DELETE /api/admin/v1/platform/dictionaries/{dictionaryId}`，保持原有权限码。
- 所有字典本体操作继续委托 `DictService`，保留启停失败码、空批量删除处理和历史缓存失效语义；
  `AdminDictController` 的字典本体六类历史接口均已经由 `PlatformDictionaryFacade` 适配。
- `hunyuan-system` 的字典本体全部管理操作已使用稳定平台路由；字典项的创建、更新、启停和删除
  仍保留历史路径，必须作为单独批次处理。
- 字典本体状态和删除委托、稳定路由契约和架构守卫共 26 个后端聚焦测试通过；前端字典测试与
  `@hunyuan/system` TypeScript 类型检查通过。

### 5.14 A4.2 第七批字典项完整管理边界

截至 2026-07-24，字典项管理已完成稳定查询、创建、更新、启停、单删和批删契约：

- 新增稳定字典项管理路由：
  `POST /api/admin/v1/platform/dictionaries/{dictionaryId}/items`、
  `PUT /api/admin/v1/platform/dictionaries/{dictionaryId}/items/{dictionaryItemId}`、
  `POST /api/admin/v1/platform/dictionaries/items/{dictionaryItemId}/toggle-disabled`、
  `POST /api/admin/v1/platform/dictionaries/items/batch-delete`、
  `DELETE /api/admin/v1/platform/dictionaries/items/{dictionaryItemId}`。
- 新增 UTF-8 中文注释的字典项创建、更新公开命令 DTO；更新命令保留 `dictCode`，用于维持
  历史 `dictCode + dataValue` 缓存失效键，不擅自改变缓存语义。
- 所有字典项操作继续委托 `DictService`，保留所属字典校验、值唯一性校验、失败码和删除前缓存
  清理；`AdminDictController` 已完全退出对 `DictService` 的直接依赖。
- `hunyuan-system` 字典本体和字典项全部管理操作均已切换稳定平台路由；历史
  `/support/dict/*` 路由仍保留兼容，退役需另行完成版本窗口和运行态负向验收。
- 字典 Facade 映射、路径标识、缓存字段传递、稳定路由契约和架构守卫共 27 个后端聚焦测试
  通过；前端字典测试与 `@hunyuan/system` TypeScript 类型检查通过。

### 5.15 A4.2 第八批文件查询与下载边界

截至 2026-07-24，文件平台已补齐管理查询和下载能力，A4.2 的文件、配置和字典稳定接口
实施范围完成：

- 新增文件分页查询、文件摘要和下载结果公开 DTO；稳定边界不向管理端暴露历史
  `FileQueryForm`、`FileVO`、`FileDownloadVO` 或具体存储实现。
- 新增稳定管理路由：`POST /api/admin/v1/platform/files/query` 和
  `GET /api/admin/v1/platform/files/download`，两条路由继续映射原
  `support:file:query` 权限，避免兼容期内改变现有角色授权结果。
- `PlatformFileApplicationService` 统一完成分页对象和下载元数据转换；历史
  `AdminFileController`、`FileController` 已转经 `PlatformFileFacade`，旧
  `/support/file/*` 路由继续返回原响应对象，不再直接依赖 `FileService`。
- `hunyuan-system` 的文件分页、访问地址解析和下载链接均已切换稳定平台路由；文件管理
  前端不再引用旧 `/support/file/*` 地址。
- 文件 Facade 映射、稳定路由契约和历史管理适配共 8 个后端聚焦测试通过；文件 API 与系统设置模块共
  37 个前端测试通过，`@hunyuan/system` TypeScript 类型检查和管理端 Maven 打包通过。
- 运行态 OpenAPI 已确认两条新路径存在；未认证请求命中稳定路由后由权限边界拒绝，未返回
  `404`；本轮临时 Java 进程已停止，`1024` 端口释放。
- Codebase Memory 已刷新为
  `E-my-project-hunyuan-pro-a4-2-file-management-20260724`，索引包含 13,263 个节点和
  35,636 条关系；调用图确认 `AdminFileController.queryPage` 已指向
  `PlatformFileFacade.queryPage`，不再保留旧索引中的 `FileService` 直连。

至此，A4.2 的文件、配置和字典公开 Facade、稳定管理 API、主管理后台消费者迁移以及历史
路由兼容适配均已完成。旧 `/support/file/*`、`/support/config/*`、`/support/dict/*`
路由和 `support:*` 权限命名空间仍处于兼容窗口，后续退役必须单独完成外部消费者确认、
权限迁移和运行态负向验收，不与 A4.2 实施关闭混为一项。

### 5.16 A4.3 第一批消息管理稳定边界

截至 2026-07-24，A4.3 先完成管理端消息查询、发送和删除纵切，不扩张到当前用户个人消息箱：

- 新增 `PlatformMessageFacade`、`PlatformMessageApplicationService` 以及消息分页查询、消息摘要、
  发送命令公开 DTO，稳定边界不暴露历史 `MessageQueryForm`、`MessageSendForm` 或 `MessageVO`。
- 新增稳定管理路由：`POST /api/admin/v1/platform/messages/query`、
  `POST /api/admin/v1/platform/messages` 和 `DELETE /api/admin/v1/platform/messages/{messageId}`；
  继续沿用 `system:message:query`、`system:message:send`、`system:message:delete` 权限。
- 历史 `AdminMessageController` 已转经 `PlatformMessageFacade`，旧 `/message/query`、
  `/message/sendMessages` 和 `/message/delete/{messageId}` 在兼容期继续保留原请求与响应结构。
- `hunyuan-system` 消息管理前端已切换稳定路由，并将历史 GET 删除动作改为 DELETE 语义。
- `/message/queryMyMessage`、`/message/getUnreadCount`、`/message/read/{messageId}` 属于当前登录用户
  个人消息箱，必须单独处理用户所有权和已读状态边界，不纳入本批管理员权限模型。
- 消息应用服务、稳定路由、历史入口适配和架构守卫共 28 个后端聚焦测试通过；消息 API 与
  系统设置模块共 37 个前端测试通过，`@hunyuan/system` TypeScript 检查和管理端 Maven
  打包通过。
- 运行态 OpenAPI 已确认三条稳定消息管理路径存在；未认证查询返回业务码 `30007` 且未返回
  `404`，证明请求命中权限边界；本轮临时 Java 进程已停止，独立验收端口已释放。
- Codebase Memory 已刷新为 `E-my-project-hunyuan-pro-a4-3-message-management-20260724`，索引包含
  13,368 个节点和 36,035 条关系，持久化产物已写入 `.codebase-memory/graph.db.zst`；新调用图
  确认新旧管理 Controller 均只依赖 `PlatformMessageFacade`，历史 `MessageService` 收口在
  `PlatformMessageApplicationService` 内部。

A4.3 尚未关闭。个人消息箱稳定接口已在 5.17 完成；后续继续盘点操作日志、登录日志、通知发送与
外部通道之间的边界。旧消息管理路由的退役仍需等待兼容窗口和运行态负向验收。

### 5.17 A4.3 第二批当前用户消息箱稳定边界

截至 2026-07-24，当前用户消息查询、未读数和标记已读已形成独立于管理员消息管理的稳定边界：

- 新增 `PlatformMessageInboxFacade`、`PlatformMessageInboxApplicationService` 和个人消息箱
  分页查询 DTO；公开查询模型不包含接收人 ID 或类型，应用服务无条件根据当前登录态注入
  `receiverUserId` 和 `receiverUserType`，客户端不能覆盖消息所有权范围。
- 新增稳定路由：`POST /api/admin/v1/platform/message-inbox/query`、
  `GET /api/admin/v1/platform/message-inbox/unread-count` 和
  `PUT /api/admin/v1/platform/message-inbox/{messageId}/read`；标记已读继续将当前用户类型和 ID
  下沉到 DAO 条件，不能修改其他用户消息。
- 历史 `MessageController` 已退出对 `MessageService` 的直接依赖，旧
  `/support/message/queryMyMessage`、`/support/message/getUnreadCount` 和
  `/support/message/read/{messageId}` 继续通过消息箱 Facade 保持兼容。
- `hunyuan-system` 消息 API 已提供稳定个人消息箱查询、未读数和标记已读客户端；当前仓库没有
  个人消息箱页面消费者，因此本批不虚构 UI 接入，后续真实入口出现时直接使用稳定客户端。
- 管理消息、个人消息箱和架构守卫合计 36 个后端聚焦测试通过；消息 API 与系统设置模块共
  38 个前端测试通过，`@hunyuan/system` TypeScript 检查和管理端 Maven 打包通过。
- 运行态 OpenAPI 已确认三条稳定路由和三条带 `/support` 继承前缀的兼容路由全部存在；未认证
  查询返回业务码 `30007` 且未返回 `404`。本轮 Java 进程已停止，独立验收端口已释放。
- Codebase Memory 已刷新为 `E-my-project-hunyuan-pro-a4-3-message-inbox-20260724`，索引包含
  13,432 个节点和 36,459 条关系；调用图确认新旧消息箱 Controller 均只调用
  `PlatformMessageInboxFacade`，历史 `MessageService` 仅由消息箱应用服务适配。

A4.3 仍未关闭。下一批进入操作日志与登录日志边界，先区分审计事件所有权、管理员查询权限和
运行时写入链路，再决定通知发送、短信和邮件通道是否属于同一平台能力。

### 5.18 A4.3 第三批操作日志与登录日志稳定边界

截至 2026-07-24，操作日志与登录日志的管理员查询能力已形成稳定审计边界，运行时写入链保持不变：

- 新增 `PlatformAuditLogFacade`、`PlatformAuditLogApplicationService`，以及操作日志、登录日志
  分页查询和公开摘要 DTO；稳定契约不暴露历史 QueryForm、VO、Entity 或 DAO。
- 新增稳定管理路由：`POST /api/admin/v1/platform/audit/operation-logs/query`、
  `GET /api/admin/v1/platform/audit/operation-logs/{operateLogId}` 和
  `POST /api/admin/v1/platform/audit/login-logs/query`，继续映射原有
  `support:operateLog:query`、`support:operateLog:detail`、`support:loginLog:query` 权限。
- `AdminOperateLogController`、`AdminLoginLogController` 已退出对历史日志 Service 的直接依赖；
  受权限保护的管理入口和当前用户查询兼容入口均转经 `PlatformAuditLogFacade`，旧响应对象不变。
- `hunyuan-system` 操作日志分页、详情和登录日志分页已切换稳定路由；日志页面不再引用旧
  `/support/operateLog/*`、`/support/loginLog/page/query` 管理地址。
- 操作日志切面异步写入、登录认证日志写入、上次登录信息读取均继续使用历史内部服务，不属于
  HTTP 管理查询迁移范围，避免稳定查询接口反向侵入认证和运行时审计链。
- 历史 `/support/operateLog/page/query/login` 和 `/support/loginLog/page/query/login` 仍保留
  当前用户兼容入口，虽然依赖登录态限制用户范围，但没有独立权限码；其消费者与退役条件需要
  后续单独确认，不能与受权限保护的管理员查询混为同一授权模型。
- 审计应用服务、路由、兼容适配和架构守卫共 30 个后端聚焦测试通过；操作日志、登录日志和
  网络安全页面共 11 个前端测试通过，`@hunyuan/system` TypeScript 检查和 Maven 打包通过。
- 运行态 OpenAPI 已确认 3 条稳定路由和 5 条兼容路由全部存在；未认证稳定查询返回业务码
  `30007` 且未返回 `404`。本轮 Java 进程已停止，独立验收端口已释放。
- Codebase Memory 已刷新为 `E-my-project-hunyuan-pro-a4-3-audit-logs-20260724`，索引包含
  13,585 个节点和 36,960 条关系；调用图确认新旧查询 Controller 均经
  `PlatformAuditLogFacade`，历史查询 Service 收口在应用服务内部，而登录日志写入仍保持
  `LoginService.saveLoginLog -> LoginLogService.log` 原链路。

A4.3 仍未关闭。下一批应盘点通知发送、短信、邮件和站内消息之间的通道边界，明确模板、发送、
发送记录及外部提供商配置的所有权，再决定统一通知平台的最小稳定接口。

### 5.19 A4.3 第四批短信管理稳定边界

截至 2026-07-24，短信模板和发送记录管理已形成稳定平台边界，业务短信发送链保持不变：

- 新增 `PlatformSmsFacade`、`PlatformSmsApplicationService`，以及短信模板查询、创建、更新、
  启停和发送记录查询公开 DTO；稳定契约不暴露历史 Form、VO、Entity、DAO 或 Provider。
- 新增稳定管理路由：
  `POST /api/admin/v1/platform/notifications/sms/templates/query`、
  `POST /api/admin/v1/platform/notifications/sms/templates`、
  `PUT /api/admin/v1/platform/notifications/sms/templates/{templateCode}`、
  `PUT /api/admin/v1/platform/notifications/sms/templates/{templateCode}/disabled` 和
  `POST /api/admin/v1/platform/notifications/sms/send-logs/query`。
- 稳定路由继续映射原有 `support:sms:template:query`、`support:sms:template:add`、
  `support:sms:template:update`、`support:sms:sendLog:query` 权限；模板更新以路径编码为唯一标识，
  不接受请求体覆盖模板主键。
- 历史 `AdminSmsController` 已退出对 `SmsService` 的直接依赖，旧 `/support/sms/*` 管理路由继续
  通过 `PlatformSmsFacade` 保持原请求和响应结构；应用服务仍委托历史服务执行数据库校验和写入。
- `hunyuan-system` 的短信模板查询、创建、更新、启停和发送记录查询均已切换稳定平台路由；
  更新和启停请求使用 PUT 语义，模板编码统一进行 URL 编码。
- 本批不修改 `SmsService.send -> SmsProvider.send` 真实发送链，也不迁移 Redis 幂等、频率限制、
  发送记录落库或外部提供商实现；邮件当前只有内部发送服务和登录验证码调用，继续保持
  `EVALUATE`，不虚构管理端平台接口。
- 短信应用服务、稳定路由、历史入口适配和架构守卫共 31 个后端聚焦测试通过；短信 API 共
  5 个前端测试通过，`@hunyuan/system` TypeScript 检查和管理端 Maven 打包通过。
- 运行态 OpenAPI 已确认 5 条稳定短信路由存在；未认证模板查询返回业务码 `30007` 且未返回
  `404`。本轮 Java 进程已停止，独立验收端口 `11024` 已释放。
- Codebase Memory 已刷新为 `E-my-project-hunyuan-pro-a4-3-sms-notification-20260724`，索引包含
  13,718 个节点和 37,465 条关系，持久化产物已写入 `.codebase-memory/graph.db.zst`；调用图确认
  新旧短信管理 Controller 均只调用 `PlatformSmsFacade`，`SmsService` 收口在应用服务内部，
  `SmsService.send -> SmsProvider.send` 原发送链保持不变。

A4.3 仍未关闭。下一批应继续处理通知通道配置和邮件能力的产品化判定；旧短信管理路由的退役
仍需等待兼容窗口、外部消费者确认和运行态负向验收。

### 5.20 A4.3 第五批事务邮件内部稳定边界

截至 2026-07-24，登录双因子验证码邮件已通过平台公开边界发送，邮件模板管理继续保持评估状态：

- 新增 `PlatformMailFacade`、`PlatformMailApplicationService`、稳定邮件模板编码和不可变模板邮件
  命令；公开边界不暴露 `JavaMailSender`、SMTP 配置、历史模板实体、DAO 或内部模板枚举。
- `LoginService` 已退出对 `MailService` 和 `MailTemplateCodeEnum` 的直接依赖，登录验证码通过
  `PlatformMailFacade` 发送；验证码生成、Redis 五分钟有效期和一分钟重复发送限制保持原语义。
- 应用服务显式映射稳定模板编码到历史模板枚举，模板读取、禁用校验、字符串或 Freemarker 渲染、
  非生产环境主题标记、附件处理和 SMTP 发送仍由原 `MailService` 承担。
- 当前数据库只有 `login_verification_code` 一条邮件模板，仓库不存在邮件模板管理 Controller、
  权限码、管理菜单或前端消费者，因此本批不新增邮件管理 HTTP 和页面，不将 `EVALUATE` 误判为
  已产品化的管理能力。
- `spring.mail` 和短信 Provider 模式均属于启动期部署配置，包含外部通道凭据或实现选择；本批不把
  这些配置迁入业务数据库，也不通过平台配置查询接口暴露。
- 邮件应用服务、登录调用和架构守卫共 30 个聚焦测试通过；邮件应用服务与现有登录模块共 7 个
  扩展回归测试通过。
- 管理端 Maven 打包和运行态 Spring 装配通过；OpenAPI 确认原
  `/login/sendEmailCode/{loginName}` 路由仍存在。本轮未实际触发邮件发送，避免对真实 SMTP
  通道产生副作用；验收 Java 进程已停止，独立端口 `11024` 已释放。
- Codebase Memory 已刷新为 `E-my-project-hunyuan-pro-a4-3-platform-mail-20260724`，索引包含
  13,764 个节点和 37,647 条关系，持久化产物已写入 `.codebase-memory/graph.db.zst`；调用图确认
  `LoginService.sendEmailCode` 只调用 `PlatformMailFacade.sendTemplateMail`，底层
  `MailService.sendMail` 仅由邮件应用服务适配。

A4.3 的消息、消息箱、审计日志、短信管理和事务邮件调用边界已经完成。剩余工作转为旧路由兼容
窗口、外部消费者与通道运维责任确认；没有新证据前，不继续虚构通知通道管理产品。

### 5.21 A4.3 整体收口审计

截至 2026-07-24，A4.3 的仓内实施范围可以关闭，历史路由退役作为独立兼容治理事项保留：

- 仓内生产消费者扫描覆盖消息管理、个人消息箱、操作日志、登录日志和短信 API；`hunyuan-system`
  已全部使用 `/admin/v1/platform/*` 客户端路径，未发现生产代码继续调用历史消息、日志或短信路由。
- 收口审计发现短信客户端曾把后端完整 `/api/admin/v1/*` 路径直接传给已有 `/api` base URL，存在
  重复 `/api` 前缀风险；现已统一改为 `/admin/v1/platform/notifications/sms/*`，与配置、字典、
  文件、消息和审计客户端约定一致。
- 系统设置模块原短信契约测试仍断言 `/support/sms/*`，现已更新为稳定平台路由；新增 A4.3
  前端聚合边界守卫，同时禁止消息、消息箱、审计日志和短信客户端回退到历史兼容地址。
- 当前保留 16 条历史兼容入口：消息管理 3 条、个人消息箱 3 条、操作与登录日志 5 条、短信管理
  5 条。它们均已转经平台 Facade，不再直接访问历史服务；保留目的仅是兼容未知外部消费者。
- 仓库内无法证明仓外脚本、第三方集成或旧版本客户端已经全部停止调用历史路由，因此本次不删除
  兼容 Controller。退役必须另行具备访问日志观测期、外部消费者确认、版本公告和运行态负向验收。
- A4.3 后端整体契约簇共 56 项测试通过；前端消息、审计、短信和系统设置聚合测试共 52 项通过，
  `@hunyuan/system` TypeScript 类型检查和生产构建通过。
- 运行态 OpenAPI 总验收确认 14 条稳定平台路由和 16 条历史兼容路由全部注册；验收 Java 进程
  已停止，独立端口 `11024` 已释放。稳定路由负责新消费者，兼容路由只承担迁移窗口。
- Codebase Memory 已刷新为 `E-my-project-hunyuan-pro-a4-3-closeout-20260724`，索引包含
  13,767 个节点和 37,662 条关系，持久化产物已写入 `.codebase-memory/graph.db.zst`；新图确认
  短信客户端 5 个调用点均使用 `/admin/v1/platform/notifications/sms/*`，不存在 `/support/sms/*`
  或重复 `/api/admin/v1/*` 调用。

结论：A4.3 的新能力边界、主管理后台迁移、历史入口适配、架构守卫和运行态验收均已完成，实施
范围关闭。历史路由退役不再阻塞 A4.3 关闭，作为跨版本兼容治理清单继续跟踪。下一实施阶段进入
A4.4 任务、运行时与开发工具隔离。

### 5.22 A4.4 第一批序列号运行时稳定边界

截至 2026-07-24，序列号定义、生成记录和受控生成功能已从通用支撑目录收口为平台运行时能力：

- 新增 `PlatformSerialNumberFacade`、`PlatformSerialNumberApplicationService`，以及序列号定义、
  生成记录、分页查询和生成命令公开 DTO；稳定边界不暴露历史 Entity、DAO 或具体生成器实现。
- 新增稳定路由：`GET /api/admin/v1/platform/runtime/serial-numbers`、
  `POST /api/admin/v1/platform/runtime/serial-numbers/records/query` 和
  `POST /api/admin/v1/platform/runtime/serial-numbers/generate`。
- 记录查询和生成继续映射原有 `support:serialNumber:record`、
  `support:serialNumber:generate` 权限；定义列表保持历史无独立权限码语义，但仍受统一登录边界保护。
- 稳定生成命令将单次生成数量限制为 1 至 50，与现有管理页面控件一致；未知序列号定义在进入
  生成器前返回参数错误，避免无效标识触发底层运行异常。
- 历史 `AdminSerialNumberController` 已退出对 `SerialNumberDao`、`SerialNumberService` 和
  `SerialNumberRecordService` 的直接依赖，旧 `/support/serialNumber/*` 路由继续通过 Facade
  保持原响应对象和生成语义。
- `hunyuan-system` 序列号定义、记录查询和生成功能已切换
  `/admin/v1/platform/runtime/serial-numbers/*` 客户端路径，并增加防止回退历史路由的契约测试。
- 三种序列号生成器、事务边界、并发控制、格式规则和记录写入链均保持不变；本批不把运行时编号
  能力与代码生成器等开发工具混为同一 owner。
- 序列号应用服务、稳定路由、兼容适配和架构守卫共 34 个后端聚焦测试通过；前端序列号和系统
  设置模块共 38 个测试通过，`@hunyuan/system` TypeScript 检查和管理端 Maven 打包通过。
- Codebase Memory 已刷新为 `E-my-project-hunyuan-pro-a4-4-serial-runtime-20260724`，索引包含
  13,868 个节点和 38,045 条关系。调用链复核确认稳定列表入口仅经
  `PlatformSerialNumberFacade.listDefinitions` 访问应用边界，生成入口继续调用
  `SerialNumberService.generate`，未旁路既有生成器、事务和记录写入链。
- 运行态 OpenAPI 已确认 3 条稳定路由和 3 条兼容路由全部存在；未认证定义查询返回业务码
  `30007`。本轮 Java 进程已停止，独立验收端口 `11024` 已释放。

A4.4 尚未关闭。下一批应处理定时任务管理边界；代码生成器继续归属 `platform-devtools`，必须与
生产运行能力隔离，不因存在 Controller 就扩建为业务平台能力。

### 5.23 A4.4 第二批定时任务管理稳定边界

截至 2026-07-24，定时任务配置、执行控制和执行日志已从通用支撑入口收口为平台运行时能力：

- 新增 `PlatformJobFacade`、`PlatformJobApplicationService`，以及任务视图、执行日志视图、
  分页查询和操作命令公开 DTO；稳定边界不暴露历史 Form、VO、Entity、DAO 或调度器实现。
- 新增 `/api/admin/v1/platform/runtime/jobs` 稳定路由组，共承载任务详情、任务分页、日志分页、
  创建、更新、启停、立即执行和删除 8 个操作；权限继续映射原有 `support:job:*` 能力码。
- 创建、更新、启停、立即执行和删除的操作人统一从服务端当前登录会话注入，公开请求体不接受
  `updateName`，避免客户端伪造审计操作人。
- 历史 `AdminSmartJobController` 已退出对 `SmartJobService` 的直接依赖，旧
  `/support/job/*` 路由继续通过 Facade 保持原响应对象、HTTP 方法和权限语义；架构守卫禁止
  旧 Controller 回退到任务内部服务。
- `SmartJobService` 仅增加按操作人名称删除的内部重载，原 `RequestUser` 签名继续委托该重载；
  任务校验、数据库写入、启停、物理删除标记、消息发布、调度线程和执行日志链均保持不变。
- `hunyuan-system` 任务页面已切换 `/admin/v1/platform/runtime/jobs` 客户端路径，新增任务 API
  防回退测试，并同步更新系统设置聚合契约。
- 平台任务应用服务、稳定路由、兼容适配、权限和架构守卫共 35 个后端聚焦测试通过；前端任务
  API 与系统设置共 40 个测试通过，`@hunyuan/system` TypeScript 检查和生产构建、管理端
  Maven 打包均通过。
- 运行态 OpenAPI 已确认 6 条稳定路径包含 8 个操作，8 条历史兼容路径包含 8 个操作；未认证
  任务分页请求返回业务码 `30007`。本轮 Java 进程已停止，独立验收端口 `11024` 已释放。
- Codebase Memory 已刷新为 `E-my-project-hunyuan-pro-a4-4-job-runtime-20260724`，索引包含
  16,266 个节点和 42,260 条关系。调用链复核确认稳定 Controller 只经 `PlatformJobFacade`
  进入应用边界，立即执行仍沿 `SmartJobService.execute -> SmartJobClientManager.publishToClient`
  进入既有执行端，历史 Controller 同样只经 Facade 兼容适配。

A4.4 尚未关闭。下一批应建立 `platform-devtools` 的代码生成器隔离边界，再核对心跳、重复提交、
API 加密和运行时重载等内部机制是否需要公开端口；内部机制不得因目录迁移而扩建为业务 API。

### 5.24 A4.4 第三批代码生成器开发工具隔离边界

截至 2026-07-24，代码生成器已从通用支撑入口隔离为平台开发工具能力：

- 新增 `PlatformCodeGeneratorFacade`、`PlatformCodeGeneratorApplicationService`、集中映射器，
  以及数据库表、列、生成配置和预览命令公开 DTO；稳定边界不暴露历史 Form、VO、Entity、
  DAO 或模板服务实现。
- 新增 `/api/admin/v1/platform/devtools/code-generator` 稳定路由组，共承载表字段、表分页、
  配置读取、配置保存、代码预览和压缩包下载 6 个操作；该路由组明确归属开发工具，不进入
  `platform-runtime` 或业务模块 owner。
- 稳定生成配置完整复制原基础命名、字段、新增修改、删除、查询和列表配置结构及校验语义；
  映射器保留“尚未配置”时的 `null` 字段，避免历史接口把未配置状态误报为空配置。
- 历史 `CodeGeneratorController` 已退出对 `CodeGeneratorService` 和代码生成 DAO 的直接依赖，
  旧 `/support/codeGenerator/*` 路由继续通过 Facade 保持响应对象、下载文件名和错误响应语义；
  架构守卫禁止旧 Controller 回退到内部生成服务和持久化实现。
- 当前 `hunyuan-system` 源码没有代码生成器页面或 API 消费者；历史 SQL 菜单已处于删除状态。
  本批不新增业务菜单、前端页面或业务权限码，6 个稳定操作继续受统一登录边界保护。
- 模板选择、变量注入、Velocity 渲染、ZIP 组装、表结构校验和 `t_code_generator_config` 单一写路径
  均保持不变；本批仅建立开发工具公开边界，不修改生成结果。
- 稳定路由、应用映射、历史兼容和架构守卫共 36 个后端聚焦测试通过，管理端 Maven 打包通过。
- 运行态 OpenAPI 已确认 6 个稳定开发工具操作和 6 个历史兼容操作全部存在；未认证表分页请求
  返回业务码 `30007`。本轮 Java 进程已停止，独立验收端口 `11024` 已释放。
- Codebase Memory 已刷新为 `E-my-project-hunyuan-pro-a4-4-devtools-codegen-20260724`，索引包含
  16,482 个节点和 42,971 条关系。调用链复核确认稳定 Controller 只经
  `PlatformCodeGeneratorFacade` 进入应用边界，代码预览继续沿
  `CodeGeneratorService.preview -> CodeGeneratorTemplateService.generate` 使用既有模板链，
  历史 Controller 同样只经 Facade 兼容适配。

A4.4 尚未关闭。下一批应审计运行时重载、心跳、重复提交和 API 加密等内部机制，明确哪些能力
只保留内部端口、哪些管理入口需要稳定化；不得为了目录整齐而复制线程、缓存或安全状态。

### 5.25 A4.4 第四批内部运行机制边界收口

截至 2026-07-24，运行时重载、心跳、重复提交和 API 加密已按真实管理职责完成边界分类与收口：

- 运行时重载具有独立管理页面、`t_reload_item` 与 `t_reload_result` 数据表、更新权限和执行结果查询，
  因此建立 `PlatformRuntimeReloadFacade`、公开 DTO 与应用服务；稳定路由组
  `/api/admin/v1/platform/runtime/reloads` 承载重载项列表、结果列表和重载项更新 3 个操作。
- 历史 `AdminReloadController` 已退出对 `ReloadService` 的直接依赖，原 `/support/reload/*` 入口
  继续通过 Facade 兼容原响应模型、HTTP 方法和 `support:reload:*` 权限；管理前端已切换稳定路由，
  并以契约测试禁止回退旧路径。
- 心跳能力同时包含内部定时上报与持久化记录查询。仅查询侧建立 `PlatformHeartbeatFacade`、公开分页
  DTO 和 `POST /api/admin/v1/platform/runtime/heartbeats/query` 只读端口；`HeartBeatManager`、线程、
  记录处理器和写入 DAO 保持原单一路径，没有复制为新的运行时服务。
- 历史 `AdminHeartBeatController` 已退出对 `HeartBeatService` 的直接依赖，原
  `POST /support/heartBeat/query` 仅作为 Facade 兼容入口保留。当前仓库内无心跳管理前端消费者，
  本批不新增页面、菜单、写端口或权限码。
- 重复提交保持为 `@RepeatSubmit`、切面和内存/Redis 票据机制，不建立管理 Controller、状态查询或
  人工清理端口；稳定写操作可以使用该注解，但公开边界不拥有票据和缓存状态。
- API 加密继续由 `@ApiDecrypt`、`@ApiEncrypt`、请求响应处理器和加密服务承担协议职责，登录密码
  解密链保持不变。历史 `/support/apiEncrypt/test*` 页面和端点只作为协议演示入口保留，不迁入
  `platform-runtime`，也不扩建为业务加解密 API。
- 新增架构守卫禁止历史重载、心跳 Controller 回退到内部 Service；新增源码契约确保重复提交包不
  暴露 HTTP Controller，API 加密实现不进入两个稳定运行时 Controller。
- 重载和心跳应用映射、稳定路由、历史兼容、内部机制守卫及架构规则共 38 项后端测试通过；重载
  API 与系统设置聚合契约共 38 项前端测试通过，`@hunyuan/system` TypeScript 检查、生产构建和
  管理端 Maven 打包均通过。
- 运行态 OpenAPI 已确认 4 个稳定操作和 4 个历史兼容操作全部注册；未认证重载列表和心跳分页
  请求均返回业务码 `30007`。本轮 Java 进程 PID `9804` 已停止，独立端口 `11024` 已释放。
- Codebase Memory 已刷新为 `E-my-project-hunyuan-pro-a4-4-runtime-mechanisms-20260724`，索引包含
  14,407 个节点和 40,112 条关系，持久化产物已写入 `.codebase-memory/graph.db.zst`。新图确认
  前端历史重载路径引用为 0；重载更新继续委托 `ReloadService.updateByTag`，心跳查询继续沿
  `HeartBeatService.pageQuery -> HeartBeatRecordDao.pageQuery` 使用既有内部链。

结论：A4.4 的序列号、定时任务、代码生成器、运行时重载、心跳、重复提交和 API 加密均已获得
明确 owner 与公开/内部边界，源码、契约、构建、运行态和 Codebase Memory 验收全部通过，A4.4
实施范围关闭。下一阶段进入 A4.5 评估能力关闭或产品化。

## 6. A4.0 关闭条件

A4.0 只有同时满足以下条件才算完成：

1. 每项纳入范围的能力均已有 owner、数据表、API、权限、前端消费者和处置决定。
2. `KEEP / ADAPT / RETIRE / EVALUATE` 的判定都有源码或运行证据，不以目录名称推断。
3. A4.1 的认证与账号消费者清单固定，四个旧自助入口都有迁移或退役条件。
4. 平台支持能力与 A3 已关闭的业务示例、OA 和工程 Demo 边界明确，不发生连带删除。
5. Codebase Memory 当前索引与源码搜索交叉核对完成；旧索引不作为当前状态依据。
6. 文档通过严格 UTF-8、Markdown 结构和 `git diff --check` 校验。

A4.0 关闭后，实施顺序为：

```text
A4.1 认证与账号中心
  -> A4.2 文件、配置与字典平台化
  -> A4.3 消息、日志与通知边界
  -> A4.4 任务、运行时与开发工具隔离
  -> A4.5 评估能力关闭或产品化
```

## 7. 风险与依赖

- 认证与账号中心同时触及登录、员工状态、密码安全、文件和前端请求刷新，必须保持
  单一写路径并优先做契约测试。
- `support:*` 权限仍是历史命名空间。A4.1 不应一次性重命名全部权限，应先建立稳定
  能力码与旧权限映射，再按纵切退役。
- 部分路由是旧 Controller 直接暴露的历史路径，Codebase Memory 中还可能存在无源码
  文件的历史 Route 节点；任何删除前必须用源码、OpenAPI 和 HTTP 结果三方确认。
- 帮助、反馈、邮件和数据追踪的 `EVALUATE` 结论不等于退役，必须先补齐菜单、访问、
  数据量、调用方和运行责任证据。
