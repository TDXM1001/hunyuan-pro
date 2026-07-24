# F0 底座契约与所有权冻结

## 1. 状态与范围

截至 2026-07-24，底座结构收口 F0 已完成前端遗留文件、feature 公开边界、后端 base HTTP 入口、平台支撑模块持久化访问和公开 Facade 模型边界的冻结，并建立开发库菜单到目标 owner 与稳定 routeId 的迁移账本。

F0 不移动生产文件、不修改菜单数据、不新增 routeId 字段，也不改变运行时路由行为。当前生产行为保持不变，F1 只能在本冻结账本上递减遗留项。

当前状态：`F0_CLOSED`。

## 2. Codebase Memory 证据

本次先使用项目 `E-my-project-hunyuan-pro-a4-5-closeout-20260724` 发现实施前边界；F0 实施后使用新项目 `E-my-project-hunyuan-pro-foundation-f0-closed-20260724` 完成全量索引，共 15,808 个节点和 41,714 条关系，并将持久化产物写入 `.codebase-memory/graph.db.zst`。索引排除清单明确包含 `.codex-runtime` 和 `数据库SQL脚本/mysql/backups`，本机运行产物与数据库备份没有进入代码图。

| 边界 | 结果 | F0 处置 |
| --- | --- | --- |
| 应用前端 | 709 个节点、1,189 条关系 | 冻结应用允许保留和待迁出的职责 |
| feature 前端 | 179 个节点、374 条关系 | 冻结三个现有 feature 的公开入口 |
| 应用到 feature | 19 条导入 | 作为正确装配方向保留 |
| feature 到应用 | 0 条导入 | 建立自动守卫，禁止出现反向依赖 |
| base HTTP Route | 32 个 Route 节点 | 归并为 10 个 `@RestController` 遗留类冻结 |
| admin 到 base | 297 条导入 | 作为 F5 收缩基线，不允许用新增万能 base 能力解决归属问题 |
| base 到 admin | 0 条导入 | 保持工程依赖方向 |

Codebase Memory 用于发现结构和调用关系；具体文件、菜单数据和测试结果继续由源码、开发库与构建交叉确认。

## 3. 前端冻结边界

新增 `foundation-structure-boundary.test.ts`，建立以下四项守卫：

1. `apps/hunyuan-system/src/api/system` 当前 14 个非测试文件只能减少，不能新增。
2. `apps/hunyuan-system/src/views/support` 当前 22 个生产页面或组件文件只能减少，不能新增。
3. `packages/features` 禁止使用应用别名 `#/`、应用源码路径或 `@hunyuan/system` 包。
4. 每个现有 feature 必须通过根公开入口导出模块描述，并由应用以 workspace 依赖显式声明。

现有 feature 为：

| feature | 当前标识 | 当前装配状态 | F1 目标 |
| --- | --- | --- | --- |
| organization | `organization.directory`、`organization.position.directory` | 应用薄入口装配 | 统一注册到 App Kernel |
| identity-employee | `identity.employee` | 应用薄入口装配 | 统一注册到 App Kernel |
| access | `access.management` | 应用薄入口装配 | 统一注册到 App Kernel |

删除冻结清单中的文件不需要更新基线；新增或改名为另一个应用内完整功能文件会立即失败。迁移完成后应删除对应基线项，不得把新路径重新加入遗留集合。

## 4. 后端冻结边界

`ArchitectureGuardTest` 新增三条规则：

| 规则 | 当前基线 | 关闭方向 |
| --- | ---: | --- |
| base HTTP Route 不得增长 | 10 个 `@RestController` 遗留类 | F5 归位后递减至 0 |
| support 跨 owner 持久化访问不得增长 | 0 个违规 | 始终保持 0 |
| 公开 Facade 内部模型泄漏不得增长 | 0 个违规 | 始终保持 0 |

当前冻结的 10 个 base HTTP Controller 为：

```text
CaptchaController
ChangeLogController
PlatformCodeGeneratorController
ConfigController
PlatformFileController
FileController
PlatformMessageInboxController
MessageController
SmsController
TableColumnController
```

冻结存储继续使用 `src/test/resources/archunit-store`，默认禁止自动创建和改写。F0 首次建立新规则时显式允许创建基线；提交后恢复默认只读模式，后续构建不能静默接受新增违规。

## 5. 菜单、页面与 routeId 账本

开发库 `hunyuan` 当前 Flyway 版本为 `3.78.0`。只读查询确认 `t_menu` 中存在 26 个未删除且带 component 的页面菜单。F1 使用下表建立稳定 routeId，不从 component 字符串自动生成标识。

| ID | 菜单 | 当前 component | 当前前端 | 目标 owner | 目标 routeId / 决策 |
| ---: | --- | --- | --- | --- | --- |
| 151 | 代码生成 | `/support/code-generator/code-generator-list.vue` | 本地页面缺失 | platform-devtools | `platform.devtools.code-generator`，F4 重新对账 |
| 219 | 部门目录 | `/organization/directory/index.vue` | organization feature 薄入口 | organization | `organization.department.directory` |
| 228 | 岗位目录 | `/system/position/position-list.vue` | organization feature 薄入口 | organization | `organization.position.directory` |
| 46 | 员工管理 | `/system/employee/index.vue` | identity-employee feature 薄入口 | identity | `identity.employee.management` |
| 76 | 角色管理 | `/system/role/index.vue` | access feature 薄入口 | access | `access.role.management` |
| 130 | 单号管理 | `/support/serial-number/serial-number-list.vue` | 应用完整页面 | platform-runtime | `platform.runtime.serial-number` |
| 26 | 菜单管理 | `/system/menu/menu-list.vue` | access feature 薄入口 | access | `access.menu.management` |
| 133 | 缓存管理 | `/support/cache/cache-list.vue` | 应用完整页面 | platform-runtime | `platform.runtime.cache` |
| 117 | Reload | `/support/reload/reload-list.vue` | 应用完整页面 | platform-runtime | `platform.runtime.reload` |
| 109 | 参数配置 | `/support/config/config-list.vue` | 应用完整页面 | platform-configuration | `platform.configuration.parameters` |
| 193 | 文件管理 | `/support/file/file-list.vue` | 应用完整页面 | platform-file | `platform.file.management` |
| 221 | 定时任务 | `/support/job/job-list.vue` | 应用完整页面 | platform-runtime | `platform.runtime.job` |
| 110 | 数据字典 | `/support/dict/index.vue` | 应用完整页面 | platform-configuration | `platform.configuration.dictionary` |
| 300 | 消息管理 | `/support/message/message-list.vue` | 应用完整页面 | platform-notification | `platform.notification.message` |
| 85 | 组件演示 | `/support/demonstration/index.vue` | 本地页面缺失 | platform-devtools | `RETIRE_CANDIDATE`，不得为悬空菜单新造生产页面 |
| 206 | 心跳监控 | `/support/heart-beat/heart-beat-list.vue` | 本地页面缺失 | platform-runtime | `EVALUATE`，当前只保留后端只读能力 |
| 215 | 接口加解密 | `/support/api-encrypt/api-encrypt-index.vue` | 应用验证页面 | platform-devtools | `platform.devtools.api-encrypt` |
| 251 | 敏感数据脱敏 | `/support/level3protect/data-masking-list.vue` | 应用验证页面 | platform-security | `platform.security.data-masking-validation` |
| 152 | 更新日志 | `/support/change-log/change-log-list.vue` | 本地页面缺失 | platform-audit | `EVALUATE`，F3 完成采用审计 |
| 234 | swagger文档 | 外部 URL | 外部页面 | platform-devtools | `platform.devtools.openapi-docs` |
| 306 | 短信模板 | `/support/sms/template-list.vue` | 应用完整页面 | platform-notification | `platform.notification.sms-template` |
| 307 | 发送日志 | `/support/sms/send-log-list.vue` | 应用完整页面 | platform-notification | `platform.notification.sms-send-log` |
| 250 | 安全基线设置 | `/support/level3protect/level3-protect-config-index.vue` | 应用完整页面 | platform-security | `platform.security.baseline-settings` |
| 214 | 登录失败锁定 | `/support/login-fail/login-fail-list.vue` | 应用完整页面 | platform-security | `platform.security.login-failure` |
| 143 | 登录登出记录 | `/support/login-log/login-log-list.vue` | 应用完整页面 | platform-audit | `platform.audit.login-log` |
| 81 | 用户操作记录 | `/support/operate-log/operate-log-list.vue` | 应用完整页面 | platform-audit | `platform.audit.operation-log` |

`RETIRE_CANDIDATE` 和 `EVALUATE` 不是最终删除决定。F1-F4 必须先核对菜单授权、直接 URL、前端消费者、后端能力、开发库数据和真实运行责任，再通过独立 Flyway 作出保留或退役决定。

## 6. F0 验证结果

- 前端底座结构守卫：1 个测试文件、4 项测试通过。
- 后端 ArchitectureGuardTest：34 项测试通过。
- support 跨 owner 持久化访问违规：0。
- 公开 Facade 内部模型泄漏：0。
- base HTTP Controller 冻结基线：10 个类，只允许递减。
- 开发库菜单账本：26 个页面菜单全部建立 owner 和 routeId/决策映射。
- 新增和修改文本通过严格 UTF-8 与 `git diff --check` 校验后方可提交。

F0 关闭后，下一阶段进入 F1：App Kernel、统一 feature 注册协议和稳定 routeId 双读迁移。
