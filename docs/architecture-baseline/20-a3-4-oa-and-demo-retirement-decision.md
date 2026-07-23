# A3.4 OA 与 Demo 退役决策及依赖矩阵

## 1. 文档定位

本文件记录 2026-07-23 对 OA、商品、分类及各类 Demo 的产品处置决定。
它既记录产品决策，也记录各批次完成工程验收后的最终状态。

本文件与 [19-a3-4-oa-adoption-audit.md](19-a3-4-oa-adoption-audit.md) 的关系如下：

- 19 号文档记录审计事实：当前代码、数据、权限和调用关系仍然存在。
- 本文件记录产品决定：哪些能力不再作为产品建设范围，哪些工程 Demo 仍需保留。
- 审计事实与退役决定不冲突。产品可以批准退役，但工程必须先完成消费者、数据、
  权限、迁移和运行态验证，再删除实现。

## 2. 总体结论

| 范围 | 产品决定 | 当前工程状态 | 处置标记 |
| --- | --- | --- | --- |
| 商品、分类及自定义分组 | 不再作为产品能力 | P1 已完成代码、菜单、权限和表退役 | `RETIRE_CLOSED` |
| OA 企业 | 不再作为产品能力 | P2 已完成代码、数据、菜单和权限退役 | `RETIRE_CLOSED` |
| OA 企业员工关联 | 不再作为产品能力 | P2 已归档数据并删除历史实现 | `RETIRE_CLOSED` |
| OA 银行信息 | 不再作为产品能力 | P2 已完成代码、数据、菜单和权限退役 | `RETIRE_CLOSED` |
| OA 发票信息 | 不再作为产品能力 | P2 已完成代码、数据、菜单和权限退役 | `RETIRE_CLOSED` |
| OA 通知、公告和员工通知查询 | 不再作为 OA 产品能力 | P3 已完成备份、代码、数据、菜单、权限和路由退役 | `RETIRE_CLOSED` |
| `message` 消息能力 | 保留为平台支持能力 | 支持逐用户消息、未读和模板消息 | `KEEP` |
| `web-ele` 工程 Demo | 保留为私有工程验收资产 | 被默认 dev/preview 应用使用 | `RETAIN_PRIVATE` |
| 数据脱敏 Demo | 保留并改为诊断/验证能力 | 有真实页面、接口和测试消费者 | `ADAPT` |
| `backend-mock` Demo | 仅保留开发和 Mock 用途 | 无生产业务消费者 | `RETAIN_DEV_ONLY` |

P2 已按冻结边界完成企业、企业员工关联、银行和发票退役。P3 已直接退役 OA 通知，
没有迁移到平台 `message`。商品、分类和 OA 生产能力以及已无子节点的 `功能Demo`
菜单外壳均已关闭；平台 `message` 与各类私有工程 Demo 的保留决定不变。

## 3. OA 退役边界

### 3.1 企业、员工关联、银行和发票

这四个子域已经获得产品层面的退役批准，原因是当前没有确认的目标业务 owner、
当前前端没有对应稳定业务页面，且它们属于历史 OA 示例能力。

退役时必须分别处理：

1. 导出并校验开发库及必要历史快照中的业务数据。
2. 盘点表、Mapper、Service、Controller、前端 API、菜单、权限和角色授权。
3. 通过 Codebase Memory 和源码搜索确认仓库内生产消费者归零。
4. 确认没有仓库外正式集成；若系统从未生产且未开放正式外部集成，记录为
   `N/A`，不能用“没有搜到”代替事实依据。
5. 通过 Flyway 删除授权、菜单、字典、表或兼容数据，并保留可恢复备份。
6. 验证旧 API、OpenAPI、直接 URL、菜单加载和构建产物均达到关闭预期。

企业员工关联不得直接改写组织目录或员工主表。若数据仍需保留，应先确定归档
或迁移 owner，再删除 OA 专属关系表和入口。

### 3.2 通知与 message 的关系

通知默认批准退役，但不能把 `message` 描述成无损替代品。

当前 `message` 可以承接：

- 单个用户或批量用户的站内消息投递。
- 标题、正文、消息类型和业务数据 ID。
- 未读数量、已读状态、分页查询和删除。
- 模板消息发送。

当前 `message` 不具备通知公告的完整语义：

- 没有全员公告抽象。
- 没有部门及子部门可见范围。
- 没有定时发布时间和发布状态。
- 没有通知分类。
- 没有通知查看记录和浏览次数统计。

因此有两条允许的产品路径：

- 若确认系统不需要公告：直接退役 OA 通知，不迁移到 `message`。
- 若未来仍需要公告：另立平台公告能力设计，公告负责内容、范围、定时和统计，
  `message` 仅作为逐用户投递通道。

在未确认公告需求前，不新增 OA 通知功能，也不新增 `message` 的公告字段。

## 4. Demo 分类决策

### 4.1 工程验收 Demo：保留但隔离

以下 `web-ele` 页面用于验证共享组件、表格、详情、编辑、表单和 Element Plus
适配能力：

```text
hunyuan-design/apps/web-ele/src/router/routes/modules/demos.ts
hunyuan-design/apps/web-ele/src/views/demos/
hunyuan-design/apps/web-ele/src/locales/langs/zh-CN/demos.json
hunyuan-design/apps/web-ele/src/locales/langs/en-US/demos.json
```

这些页面不属于产品业务，但 `hunyuan-design/package.json` 的默认 `dev` 和 `preview`
仍指向 `@vben/web-ele`。因此本批不删除 `web-ele`，也不删除上述工程验收页。

后续治理要求：

- 标记为 `RETAIN_PRIVATE`，禁止出现在生产业务菜单和正式产品导航。
- 保持在独立 Demo 应用或开发入口中，不让业务应用依赖 Demo 路由。
- 在发布构建中明确排除，或通过开发环境开关隔离。
- 页面名称、测试名称和说明中区分“工程验收”与“业务示例”。

### 4.2 数据脱敏 Demo：适配为诊断能力

以下代码虽然含有 Demo 命名，但存在真实页面、接口和测试消费者：

```text
hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/system/support/AdminDataMaskingController.java
hunyuan-design/apps/hunyuan-system/src/views/support/level3protect/data-masking-list.vue
hunyuan-design/apps/hunyuan-system/src/api/system/data-masking.ts
```

该能力不应直接删除。后续应将命名和入口调整为“数据脱敏验证”或“数据脱敏诊断”，
限制为开发环境或管理员诊断权限，并补充直接 URL 和 API 的权限验收。

### 4.3 backend-mock：仅保留开发资产

以下文件属于 Mock 数据和框架开发辅助，不是生产业务能力：

```text
hunyuan-design/apps/backend-mock/api/demo/bigint.ts
hunyuan-design/apps/backend-mock/utils/mock-data.ts
```

标记为 `RETAIN_DEV_ONLY`。它们可以继续支持本地开发和组件测试，但不得进入生产
业务菜单、生产 API 清单或正式产品验收范围。

## 5. 依赖矩阵

| 对象 | 主要源码/入口 | 数据与权限 | 当前消费者 | 替代能力 | 处置批次 |
| --- | --- | --- | --- | --- | --- |
| 商品 | `module.business.goods`（已删除） | `t_goods`、商品菜单和权限已由 `V3.74.0` 处理 | 无生产消费者 | 无 | P1，已关闭 |
| 分类 | `module.business.category`（已删除） | `t_category`、分类菜单和权限已由 `V3.74.0` 处理 | 商品历史链路已归零 | 无 | P1，已关闭 |
| OA 企业 | `module.business.oa.enterprise`（已删除） | `t_oa_enterprise`、企业菜单和权限已由 `V3.75.0` 处理 | 无生产消费者 | 无 | P2，已关闭 |
| OA 企业员工关联 | 历史企业员工关联实现（已删除） | `t_oa_enterprise_employee` 已由 `V3.75.0` 处理 | 员工和组织公开 Facade 不再被旧模块消费 | 归档备份 | P2，已关闭 |
| OA 银行 | `module.business.oa.bank`（已删除） | `t_oa_bank`、银行菜单和权限已由 `V3.75.0` 处理 | 无生产消费者 | 无 | P2，已关闭 |
| OA 发票 | `module.business.oa.invoice`（已删除） | `t_oa_invoice`、发票菜单和权限已由 `V3.75.0` 处理 | 无生产消费者 | 无 | P2，已关闭 |
| OA 通知 | 历史 `module.business.oa.notice`（已删除） | 四张通知表、菜单和权限已由 `V3.76.0`、`V3.76.1` 处理 | 仓库内外正式消费者均为 0 或 `N/A` | 未迁移；未来有需求时独立建设公告 | P3，已关闭 |
| 平台 message | `module.support.message` | 消息表、消息权限 | 管理端消息 API 和前端客户端 | 平台站内消息 | 保留 |
| `web-ele` Demo | `apps/web-ele/src/views/demos` | Demo 路由和本地化资源 | `web-ele` 默认 dev/preview | 独立开发验收入口 | P4 |
| 数据脱敏验证 | `AdminDataMaskingController` 及系统页面 | 数据脱敏权限和接口 | 真实页面、API、测试 | 数据脱敏诊断 | P4 |
| backend-mock | `apps/backend-mock/api/demo` | Mock 环境配置 | 本地 Mock 和开发测试 | 无 | P4 |

## 6. 执行批次与关闭门禁

### P2：OA 主数据历史子域退役

范围：企业、企业员工关联、银行、发票。

2026-07-23 已完成备份恢复、退役迁移、生产实现删除、数据库和授权对账、完整构建、
23 条旧路由与 OpenAPI 负向验收以及 Codebase Memory 刷新，状态为 `P2_CLOSED`。
执行与关闭台账见
[21-a3-4-oa-master-data-retirement-freeze.md](21-a3-4-oa-master-data-retirement-freeze.md)。

已通过的关闭条件：

- 每个子域完成数据备份、恢复抽样和 owner 记录。
- OA 角色授权、菜单、前端入口和 API 消费者完成分类。
- 仓库内生产消费者归零，仓库外正式消费者完成审计或有事实依据的 `N/A`。
- Flyway 在空库和当前开发库通过。
- 旧路由、OpenAPI、权限、菜单和构建产物达到预期归零。
- Codebase Memory 刷新后目标 Controller、Service 和前端入口达到预期归零。

### P3：OA 通知直接退役

已确认当前产品不需要公告能力，因此采用直接退役路径，不迁移到 `message`。
2026-07-23 已完成通知数据备份和恢复验证、生产实现删除、Flyway 迁移、数据库与
授权对账、12 条旧路由负向验收、OpenAPI 和构建产物检查，状态为 `P3_CLOSED`。
完整证据见
[22-a3-4-oa-notice-retirement-closeout.md](22-a3-4-oa-notice-retirement-closeout.md)。

### P4：Demo 生产隔离与命名治理

范围：`web-ele` 工程验收页、数据脱敏验证页和 backend-mock。

2026-07-23 已完成生产隔离、命名治理、权限迁移和运行态验收，状态为
`P4_CLOSED`。完整证据见
[23-a3-4-demo-production-isolation-closeout.md](23-a3-4-demo-production-isolation-closeout.md)。

已通过的关闭条件：

- 工程 Demo 不再出现在生产业务菜单和正式导航。
- 数据脱敏能力保留明确管理员/开发权限和直接 API 防护。
- backend-mock 仅在开发或测试配置中启用。
- `web-ele` 的 dev/preview 默认入口仍可用，不能因清理 Demo 破坏工程验收。
- 前端测试、类型检查、生产构建和严格 UTF-8 校验通过。

### P5：最终验收

2026-07-23 已完成最终验收，状态为 `P5_CLOSED`：

- 后端完整 `clean verify` 通过：`hunyuan-base` 12 项、`hunyuan-admin` 155 项，
  0 失败、0 错误，3 项外部环境测试按配置跳过，ArchUnit 18 项通过。
- 前端数据脱敏 API 契约测试 1 项通过，`@hunyuan/system` 类型检查和生产构建通过。
- 隔离运行态使用随机数据库和随机管理员完成全量 Flyway、登录与权限验收；未登录请求
  返回业务码 `30007`，授权请求返回 11 条数据且手机号、密码字段均已脱敏。
- OpenAPI 保留兼容路由 `/support/dataMasking/demo/query`，控制器直接权限注解为
  `support:protect:dataMasking:query`。
- Codebase Memory 全量索引
  `E-my-project-hunyuan-pro-a3-4-p5-closed-20260723` 状态为 `indexed`，包含
  15,059 个节点和 37,284 条关系；新 `AdminDataMaskingController` 存在，旧
  `AdminDataMaskingDemoController` 节点为 0。

## 7. 当前状态与下一步

截至 2026-07-23：

- 商品与分类：`RETIRE_CLOSED`。
- OA 企业、企业员工关联、银行、发票：`RETIRE_CLOSED`。
- OA 通知：`RETIRE_CLOSED`，直接退役且未迁移到 `message`。
- message：`KEEP`，不承担无损公告替代。
- 工程 Demo：`RETAIN_PRIVATE`、`ADAPT` 或 `RETAIN_DEV_ONLY`，生产隔离已关闭。
- OA 整体：`RETIRE_CLOSED`。
- A3.4 业务示例与 OA 退役范围：`RETIRE_CLOSED`。
- A3.4：`CLOSED`。

P1-P5 均已关闭。后续如治理工程 Demo，只能按本文件定义的私有工程资产边界单独实施，
不能恢复已经退役的商品、分类或 OA 业务能力。新的业务建设必须先明确业务 owner、
目标用户、流程和验收标准，不从现存 Demo 代码反推业务需求。
