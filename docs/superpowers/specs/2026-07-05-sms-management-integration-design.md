# 短信管理模块对接设计

## 背景

当前 `hunyuan-system` 已完成系统设置下多类支撑模块页面的真实对接，包括参数配置、数据字典、文件管理、消息管理、定时任务、缓存管理、Reload 和单号管理。后端 `SMS` 能力也已经具备模板管理、发送日志、权限点和最小可用发送闭环，但前端系统设置中还没有对应的真实页面入口，菜单树也未形成完整的短信管理闭环。

这次任务不是扩展真实短信供应商，也不是重做后端短信能力，而是把现有后端短信管理能力接入 `hunyuan-system`，形成“菜单可进入、页面可命中、接口可调用、权限可归属、SQL 可增量”的前后端对接闭环。

## 目标

本次增量只完成短信管理的系统设置接入：

1. 在 `hunyuan-system` 中增加短信模板页和发送日志页。
2. 新增菜单增量 SQL，把短信管理接入系统设置菜单树。
3. 将现有短信按钮权限点归位到新页面菜单下，形成菜单与权限闭环。
4. 图标统一使用 Element Plus 图标集对应的 Iconify 名称。
5. 全程使用 UTF-8 和中文文案。

## 非目标

本次不做以下事项：

- 不新增真实短信供应商适配器。
- 不新增供应商回执、自动重试、失败补发。
- 不新增短信发送前端操作台。
- 不改造短信底层服务接口。
- 不引入新依赖或新图标库。
- 不为了形式完整强行新增无业务必要的字典表或字典数据。

## 全局约束

- 遵循 `AGENTS.md`：一次只推进一个可验证增量。
- 遵循 `AGENTS.md`：编辑前先说明为什么需要改动。
- 遵循 `AGENTS.md`：优先复用现有项目模式，不新增依赖。
- 所有前端页面遵循 `docs/frontend-list-table-page-standard.md`。
- 菜单路径和组件路径必须与后端菜单配置一致，不自造额外路径。
- 所有新增或编辑文本文件使用 UTF-8。
- 菜单图标使用 Element Plus 图标集的 Iconify 名称，如 `ep:chat-dot-round`。
- 若字段没有真实字典依赖，则本次不新增字典 SQL。

## 当前证据

### 后端能力已存在

后端已具备以下短信管理接口：

- `POST /sms/template/query`
- `POST /sms/template/add`
- `POST /sms/template/update`
- `GET /sms/template/updateDisabled/{templateCode}/{disableFlag}`
- `POST /sms/sendLog/query`

对应控制器位于：

- `hunyuan-backend/hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/system/support/AdminSmsController.java`

对应领域对象位于：

- `SmsTemplateQueryForm`
- `SmsTemplateVO`
- `SmsSendLogQueryForm`
- `SmsSendLogVO`
- `SmsSendStatusEnum`

### SQL 与权限点已具备基础

现有 `v3.31.0.sql` 已创建：

- `t_sms_template`
- `t_sms_send_log`

并已插入按钮级权限点：

- `support:sms:template:query`
- `support:sms:template:add`
- `support:sms:template:update`
- `support:sms:sendLog:query`

但这些权限点目前直接挂在系统设置父菜单下，还没有归属于一个真实的短信页面菜单。

### 前端页面尚未落地

当前 `hunyuan-system` 下未发现以下真实页面路径：

- `/support/sms/template-list.vue`
- `/support/sms/send-log-list.vue`

因此短信管理仍然不属于已对接完成的系统设置模块。

## 方案比较

### 方案 A：一页双 Tab

结构：

- 一个“短信管理”页面
- 页内分为“短信模板”和“发送日志”两个 Tab

优点：

- 菜单数量最少。
- 页面文件数量最少。

缺点：

- 模板管理和发送日志本质上是两个并列管理面，不是一个主从关系。
- 查询条件、按钮权限、页面职责会混在一起。
- 后续若扩展日志筛选或模板治理，页面会快速变重。

### 方案 B：两页分开加父菜单

结构：

- 父菜单：短信管理
- 子菜单：短信模板
- 子菜单：发送日志

优点：

- 符合当前系统设置模块的组织方式。
- 模板 CRUD 与发送日志审计职责清晰。
- 菜单、按钮权限、测试、页面路径都更容易闭环。

缺点：

- 菜单和 SQL 增量略多。

### 方案 C：模板页加日志抽屉

结构：

- 主页面为模板管理
- 日志作为模板行级抽屉查看

优点：

- 页面数少，表面上紧凑。

缺点：

- 发送日志不是模板的从属明细，而是全局审计数据。
- 按手机号、状态、时间查询日志时体验会很别扭。

### 推荐方案

推荐采用 **方案 B：两页分开加父菜单**。

原因：

- 最符合当前仓库系统设置页的结构和节奏。
- 最适合做“菜单、页面、权限、SQL”一体化增量。
- 能把短信模板管理与发送日志审计清晰拆开，避免一开始就把页面做杂。

## 推荐设计

### 信息架构

本次新增以下菜单结构，统一挂到系统设置父菜单 `parent_id = 50` 下：

- 父菜单：`短信管理`
- 子菜单：`短信模板`
- 子菜单：`发送日志`

父菜单只承担分组职责，不直接命中业务页面。

## 菜单路径与组件路径

建议采用以下菜单定义：

### 父菜单：短信管理

- 菜单名：`短信管理`
- 路径：`/support/sms`
- 组件：空
- 图标：`ep:chat-dot-round`

### 子菜单：短信模板

- 菜单名：`短信模板`
- 路径：`/support/sms/template-list`
- 组件：`/support/sms/template-list.vue`
- 图标：`ep:tickets`

### 子菜单：发送日志

- 菜单名：`发送日志`
- 路径：`/support/sms/send-log-list`
- 组件：`/support/sms/send-log-list.vue`
- 图标：`ep:list`

以上组件路径直接对应本地真实页面文件，确保登录后可以命中本地视图，而不是落入 `module-bridge`。

## 图标策略

本次菜单图标统一使用 Element Plus 图标集对应的 Iconify 名称，而不是旧的 `MailOutlined` 之类名称。

原因：

- 当前 `hunyuan-system` 菜单图标字段最终以字符串形式进入图标解析链。
- 现有路由中已经存在 `lucide:*` 和 `ep:*` 风格图标字符串。
- `ep:*` 可以直接表达 Element Plus 图标集来源，满足这次“图标采用 element plus 的图标”约束。

## 前端文件结构

本次新增前端文件如下：

- `hunyuan-design/apps/hunyuan-system/src/api/system/sms.ts`
- `hunyuan-design/apps/hunyuan-system/src/api/system/sms.test.ts`
- `hunyuan-design/apps/hunyuan-system/src/views/support/sms/template-list.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/support/sms/send-log-list.vue`

并修改：

- `hunyuan-design/apps/hunyuan-system/src/views/support/system-settings-modules.test.ts`

## 页面一：短信模板

### 职责

短信模板页负责：

- 查询模板
- 新增模板
- 编辑模板
- 启用或禁用模板

本页不负责：

- 删除模板
- 发送短信
- 查看发送日志明细

### 查询条件

- `templateCode`
- `templateName`
- `disableFlag`

### 表格字段

- `templateCode`
- `templateName`
- `templateContent`
- `disableFlag`
- `remark`
- `updateTime`
- `createTime`

### 页面结构

采用标准密集列表页结构：

- 外层：`Page`
- 搜索区：`ArtSearchPanel`
- 表格区：`ArtTablePanel + ArtTableHeader + ArtTable`
- 表单：`ElDialog`

交互要求：

- 搜索天然单行，`ArtSearchPanel` 显式 `:collapsible="false"`
- 顶部主按钮：`新增模板`
- 行操作：`编辑`、`启用/禁用`
- 页面不增加额外 hero、标题介绍块

## 页面二：发送日志

### 职责

发送日志页负责：

- 查询短信发送日志
- 作为审计和排障查看页

本页不负责：

- 重发短信
- 删除日志
- 人工补发

### 查询条件

- `phone`
- `templateCode`
- `sendStatus`
- `startDate`
- `endDate`

### 表格字段

- `provider`
- `requestId`
- `phone`
- `templateCode`
- `sendContent`
- `sendStatus`
- `failReason`
- `sendTime`
- `createTime`

### 页面结构

采用标准密集列表页结构：

- 外层：`Page`
- 搜索区：`ArtSearchPanel`
- 表格区：`ArtTablePanel + ArtTableHeader + ArtTable`

交互要求：

- 不设置主操作按钮
- `sendStatus` 在前端本地映射中文：
  - `0 -> 待发送`
  - `1 -> 发送成功`
  - `2 -> 发送失败`
- 搜索项较多，保留 `ArtSearchPanel` 默认折叠能力，不强制关闭

## API 设计

新增 API 模块：

- `apps/hunyuan-system/src/api/system/sms.ts`

### 模板相关函数

- `buildSmsTemplateQueryPayload`
- `buildSmsTemplateMutationPayload`
- `buildSmsTemplateDisabledPath`
- `querySmsTemplatePage`
- `addSmsTemplate`
- `updateSmsTemplate`
- `updateSmsTemplateDisabled`

### 日志相关函数

- `buildSmsSendLogQueryPayload`
- `querySmsSendLogPage`

### 对应后端接口

- `POST /sms/template/query`
- `POST /sms/template/add`
- `POST /sms/template/update`
- `GET /sms/template/updateDisabled/{templateCode}/{disableFlag}`
- `POST /sms/sendLog/query`

### 处理原则

- 字符串统一 `trim`
- `disableFlag`、`sendStatus` 等布尔值和枚举值显式透传
- 查询 payload 仅传真实有值字段
- 发送日志日期范围字段固定使用 `startDate`、`endDate`，与后端 `SmsSendLogQueryForm` 对齐

## 增量 SQL 设计

### SQL 文件

新增：

- `数据库SQL脚本/mysql/sql-update-log/v3.32.0.sql`

### 菜单 ID 规划

当前未发现 `305`、`306`、`307` 被占用，建议使用：

- `305`：短信管理
- `306`：短信模板
- `307`：发送日志

### 菜单新增内容

在 `v3.32.0.sql` 中新增三条菜单记录：

1. 父菜单：短信管理
2. 子菜单：短信模板
3. 子菜单：发送日志

### 现有权限点归位

现有 `v3.31.0.sql` 中的按钮权限点本次不重建，改为在 `v3.32.0.sql` 中通过增量 `UPDATE` 归位：

- `301/302/303` 归到 `306` 短信模板菜单下
- `304` 归到 `307` 发送日志菜单下

归位字段建议同步处理：

- `parent_id`
- `context_menu_id`
- `update_time`

这样可以保证：

- 页面菜单和按钮权限归属一致
- 角色授权树结构更清晰
- 后续扩展发送日志能力时不必重新整理权限关系

## 字典 SQL 策略

本轮 **默认不新增字典 SQL**。

原因如下：

1. 短信模板页字段为 `templateCode/templateName/templateContent/disableFlag/remark`，没有真实字典依赖。
2. 发送日志页筛选中的 `sendStatus` 已由后端 `SmsSendStatusEnum` 提供，属于枚举语义，不是当前必需的字典型配置。
3. 如果为了形式统一强行新增短信状态字典，会把当前范围扩大成“枚举 + 字典 + 前端展示双轨维护”，超出本次最小增量目标。

因此，本次对“涉及到的字典也要增量 sql”的落实方式是：

- 先核查是否存在真实字典依赖
- 当前结论为：**本增量无真实字典依赖**
- 在设计和实现中明确记录“不新增字典 SQL”这一决定

## 测试设计

### 源码契约测试

在 `system-settings-modules.test.ts` 中补充短信模块断言：

- `template-list.vue` 存在
- `send-log-list.vue` 存在
- `sms.ts` 存在
- 页面使用共享的 `ArtSearchPanel / ArtTablePanel / ArtTableHeader / ArtTable`
- API 文件包含正确短信接口路径

### API builder 单测

新增：

- `apps/hunyuan-system/src/api/system/sms.test.ts`

覆盖：

- 模板查询 payload
- 模板新增/更新 payload
- 启停 URL builder
- 日志查询 payload

### 前端类型校验

必须运行：

```bash
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

并按列表页标准补跑共享前端合同校验：

```bash
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
```

### 支撑模块合同回归

必须运行：

```bash
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/support/system-settings-modules.test.ts --dom
```

## 验收标准

本轮完成后，应满足以下标准：

1. 后端菜单能将“短信模板”和“发送日志”命中到本地真实页面，而不是 `module-bridge`。
2. 短信模板页可完成查询、新增、编辑、启停。
3. 发送日志页可按手机号、模板编码、发送状态、日期范围查询。
4. 增量 SQL 已补齐短信管理菜单，并完成既有权限点归位。
5. 菜单图标统一采用 Element Plus 图标集的 Iconify 名称。
6. 本轮不引入新依赖，不新增伪字典。
7. `@hunyuan/system` typecheck 通过，并按列表页标准完成 `@vben/web-ele` typecheck 校验。
8. 系统设置模块源码契约测试通过。

## 实施顺序建议

推荐按以下顺序执行：

1. 先补短信模块设计契约测试
2. 再落 `sms.ts` API 模块和对应单测
3. 先实现短信模板页
4. 再实现发送日志页
5. 最后补 `v3.32.0.sql`
6. 统一跑源码契约测试与 typecheck 验收

这样可以保证每一步都有清晰的失败点和验证出口。
