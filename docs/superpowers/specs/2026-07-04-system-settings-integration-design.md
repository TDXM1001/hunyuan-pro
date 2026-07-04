# 系统设置模块整体对接设计

## 背景

`hunyuan-system` 已经完成真实登录、后端菜单加载、组织架构相关页面接入，以及未知后端页面的 `module-bridge` 承接。当前系统管理能力已经不再缺少“菜单入口”，而是缺少“菜单入口命中真实页面”的逐模块落地。

本次任务不是随意补几个页面，而是要把“数据库菜单配置 -> 登录返回菜单 -> 动态路由命中 -> 本地真实页面 -> 页面 API -> 后端 controller/service”整条链路先梳理清楚，再按 `AGENTS.md` 的要求逐步实施。

## 本次目标

1. 梳理系统设置菜单组的真实页面路径，不自造路径。
2. 梳理每个菜单对应的前端页面、后端接口、核心数据流和缺口。
3. 给每个模块提供清晰流程图，明确第一版页面职责。
4. 在整体设计确认后，按“一次一个可验证增量”推进实现。

## 全局约束

- 严格以数据库 `t_menu` 中已有的 `path` 和 `component` 为准，不自造页面路径。
- 优先复用现有项目模式，不新增依赖。
- 列表/查询/表格页面遵循 `docs/frontend-list-table-page-standard.md`。
- 编辑/详情场景若后续出现，遵循 `docs/frontend-edit-detail-page-standard.md`。
- 页面只调用 `apps/hunyuan-system/src/api/system/*.ts`，不把请求逻辑写进共享组件。
- 实施阶段不用子代理，按单模块、单增量逐步执行。
- 全程使用 UTF-8；如代码复杂到需要说明，可使用简洁中文注释。

## 当前证据

### 1. 菜单路径来源

数据库 `t_menu` 已经为系统设置菜单组配置了固定的路由路径和组件路径：

- 菜单管理：`/menu/list` -> `/system/menu/menu-list.vue`
- 参数配置：`/config/config-list` -> `/support/config/config-list.vue`
- 数据字典：`/setting/dict` -> `/support/dict/index.vue`
- 单号管理：`/support/serial-number/serial-number-list` -> `/support/serial-number/serial-number-list.vue`
- 缓存管理：`/support/cache/cache-list` -> `/support/cache/cache-list.vue`
- Reload：`/hook` -> `/support/reload/reload-list.vue`
- 文件管理：`/support/file/file-list` -> `/support/file/file-list.vue`
- 定时任务：`/job/list` -> `/support/job/job-list.vue`
- 消息管理：`/message` -> `/support/message/message-list.vue`

### 2. 动态路由命中规则

前端登录后会通过 `/login` 或 `/login/getLoginInfo` 获取 `menuList`，再由 `login-adapter.ts` 转成动态路由：

1. 读取后端 `component`
2. 规范化成本地 `views` 路径
3. 若本地存在对应 `.vue`，则进入真实页面
4. 若本地不存在对应 `.vue`，则进入 `module-bridge`

因此，“数据库里有菜单”不等于“前端已经有真实页面”。

### 3. 当前前端实际落地状态

当前 `apps/hunyuan-system/src/views` 中，与系统设置相关的真实页面只有：

- `views/system/menu/menu-list.vue`

其余 `views/support/*` 页面目录尚未落地，因此当前仍会落入 `views/system/module-bridge/index.vue`。

## 总体架构

### 总链路图

```mermaid
flowchart TD
  A["数据库 t_menu 配置 path 与 component"] --> B["/login 或 /login/getLoginInfo 返回 menuList"]
  B --> C["login-adapter 规范化 route 与 component"]
  C --> D{"本地 views 是否存在对应页面"}
  D -- "存在" --> E["真实 Vue 页面"]
  D -- "不存在" --> F["module-bridge 兜底页"]
  E --> G["apps/hunyuan-system/src/api/system/*.ts"]
  G --> H["hunyuan-admin Controller"]
  H --> I["hunyuan-base Service / Dao / 表"]
  I --> H
  H --> G
  G --> E
```

### 页面职责边界

- `views/support/*`：承接模块页面、局部弹窗、页面状态。
- `api/system/*.ts`：定义 DTO、表单 payload、请求函数。
- `@vben/art-hooks`：只负责展示与交互 primitive，不直接依赖业务接口。
- `hunyuan-admin`：提供系统管理入口 controller。
- `hunyuan-base`：提供基础能力 service/dao/domain。

## 方案比较

### 方案 A：先做全量梳理，再做一个首模块落地

优点：

- 最符合 `AGENTS.md` 的“一次一个可验证增量”。
- 能先统一动态路由、页面路径、接口边界认知。
- 后续每个 support 页面都能按同一模板复制推进。

缺点：

- 第一轮交付不会一次把 8 个待迁移模块全部变成真实页面。

### 方案 B：先补齐 8 个待迁移页面骨架，再逐页补接口动作

优点：

- 系统设置菜单会很快脱离 `module-bridge`。

缺点：

- 第一轮验证面大，容易出现页面都在但行为深浅不一。
- 一旦某个模块接口边界理解偏差，会放大返工成本。

### 方案 C：按后端成熟度一次性铺开多个模块

优点：

- 表面推进速度快。

缺点：

- 容易违背“单增量、可验证”原则。
- 多个模块同时联调时，typecheck 与接口问题难定位。

### 推荐方案

推荐采用 **方案 A**：

1. 先完成系统设置整体对接设计。
2. 再选择一个后端接口最完整、页面模式最稳定的模块首批落地。
3. 验证通过后，将同一模式复制到其余模块。

## 系统设置模块总览

| 模块 | 数据库组件路径 | 当前状态 | 后端入口 | 推荐批次 |
| --- | --- | --- | --- | --- |
| 菜单管理 | `/system/menu/menu-list.vue` | 已落地 | `MenuController` `/menu/*` | 基线已完成 |
| 参数配置 | `/support/config/config-list.vue` | 未落地 | `AdminConfigController` `/config/*` | 第一批 |
| 数据字典 | `/support/dict/index.vue` | 未落地 | `AdminDictController` `/dict/*` | 第一批 |
| 文件管理 | `/support/file/file-list.vue` | 未落地 | `AdminFileController` + `FileController` | 第一批 |
| 定时任务 | `/support/job/job-list.vue` | 未落地 | `AdminSmartJobController` `/job/*` | 第二批 |
| 消息管理 | `/support/message/message-list.vue` | 未落地 | `AdminMessageController` + `MessageController` | 第二批 |
| 单号管理 | `/support/serial-number/serial-number-list.vue` | 未落地 | `AdminSerialNumberController` `/serialNumber/*` | 第三批 |
| 缓存管理 | `/support/cache/cache-list.vue` | 未落地 | `AdminCacheController` `/cache/*` | 第三批 |
| Reload | `/support/reload/reload-list.vue` | 未落地 | `AdminReloadController` `/reload/*` | 第三批 |

### 系统设置现状图

```mermaid
flowchart LR
  A["系统设置"] --> B["菜单管理: 已真实页面"]
  A --> C["参数配置: module-bridge"]
  A --> D["数据字典: module-bridge"]
  A --> E["文件管理: module-bridge"]
  A --> F["定时任务: module-bridge"]
  A --> G["消息管理: module-bridge"]
  A --> H["单号管理: module-bridge"]
  A --> I["缓存管理: module-bridge"]
  A --> J["Reload: module-bridge"]
```

## 模块逐项设计

### 1. 菜单管理

职责：

- 维护菜单树、路由路径、组件路径、权限码、显示/禁用状态。
- 作为后续 `support/*` 页面命中真实路由的基线样例。

当前状态：

- 已有真实页面 `views/system/menu/menu-list.vue`
- 已有独立 API 文件 `api/system/menu.ts`

流程图：

```mermaid
flowchart TD
  A["菜单管理页面"] --> B["queryMenuTree(false)"]
  B --> C["GET /menu/tree"]
  C --> D["MenuController -> MenuService"]
  D --> E["返回菜单树"]
  E --> F["ArtTable 树表展示"]
  F --> G["新增/编辑/删除"]
  G --> H["POST /menu/add or /menu/update or GET /menu/batchDelete"]
  H --> I["刷新菜单树"]
```

第一版边界：

- 保持现状，不扩 scope。
- 仅作为其余模块的页面结构与 API 分层参考。

### 2. 参数配置

职责：

- 作为标准单表 CRUD 列表页。
- 查询并维护 `configKey/configName/configValue/remark`。

核心接口：

- `POST /config/query`
- `POST /config/add`
- `POST /config/update`

核心字段：

- `ConfigVO.configId`
- `ConfigVO.configKey`
- `ConfigVO.configName`
- `ConfigVO.configValue`
- `ConfigVO.remark`

流程图：

```mermaid
flowchart TD
  A["参数配置页 /support/config/config-list.vue"] --> B["输入 configKey 查询条件"]
  B --> C["POST /config/query"]
  C --> D["AdminConfigController -> ConfigService.queryConfigPage"]
  D --> E["返回 ConfigVO 分页"]
  E --> F["ArtTable 展示 key/name/value/remark/time"]
  F --> G["新增或编辑配置"]
  G --> H["POST /config/add or /config/update"]
  H --> I["ConfigService 刷新 CONFIG_CACHE"]
  I --> J["页面重新查询列表"]
```

第一版边界：

- 页面形态保持 `ArtSearchPanel + ArtTable + Dialog Form`。
- `configValue` 第一版使用普通多行输入，不额外做 JSON 可视化编辑器。

### 3. 数据字典

职责：

- 维护字典主表与字典项子表。
- 支持字典启停、字典项启停、增删改。

核心接口：

- `POST /dict/queryPage`
- `POST /dict/add`
- `POST /dict/update`
- `GET /dict/updateDisabled/{dictId}`
- `GET /dict/delete/{dictId}`
- `GET /dict/dictData/queryDictData/{dictId}`
- `POST /dict/dictData/add`
- `POST /dict/dictData/update`
- `GET /dict/dictData/updateDisabled/{dictDataId}`
- `GET /dict/dictData/delete/{dictDataId}`

核心字段：

- 主表：`dictId/dictName/dictCode/remark/disabledFlag`
- 子表：`dictDataId/dataValue/dataLabel/dataStyle/sortOrder/disabledFlag`

流程图：

```mermaid
flowchart TD
  A["数据字典页 /support/dict/index.vue"] --> B["POST /dict/queryPage"]
  B --> C["AdminDictController -> DictService.queryPage"]
  C --> D["返回 DictVO 分页"]
  D --> E["选中某个 dictId"]
  E --> F["GET /dict/dictData/queryDictData/{dictId}"]
  F --> G["返回 DictDataVO 列表"]
  G --> H["页面展示字典项"]
  H --> I["新增/编辑/启停/删除 字典或字典项"]
  I --> J["/dict/add /dict/update /dict/updateDisabled /dict/delete"]
  I --> K["/dict/dictData/add /update /updateDisabled /delete"]
  J --> L["刷新字典与字典项"]
  K --> L
```

第一版边界：

- 先完成字典主表 + 字典项列表/表单闭环。
- 不做拖拽排序、批量导入、复杂样式编辑器。

### 4. 文件管理

职责：

- 展示后端已上传文件记录。
- 支持查询、查看 URL、下载。
- 若后续需要上传入口，也只做最小挂接，不重做上传协议。

核心接口：

- `POST /file/queryPage`
- `POST /file/upload`
- `GET /file/getFileUrl`
- `GET /file/downLoad`

核心字段：

- `fileId/folderType/fileName/fileType/fileSize/fileKey/creatorName/fileUrl/createTime`

流程图：

```mermaid
flowchart TD
  A["文件管理页 /support/file/file-list.vue"] --> B["POST /file/queryPage"]
  B --> C["AdminFileController -> FileService.queryPage"]
  C --> D["返回 FileVO 分页"]
  D --> E["ArtTable 展示 fileName/fileType/fileSize/creator/time"]
  E --> F["查看文件 URL"]
  F --> G["GET /file/getFileUrl?fileKey=..."]
  E --> H["下载文件"]
  H --> I["GET /file/downLoad?fileKey=..."]
  E --> J["需要时进入上传入口"]
  J --> K["POST /file/upload"]
  K --> L["FileService 校验并入库"]
  L --> M["页面重新查询"]
```

第一版边界：

- 以“查询和使用现有文件”为主。
- 不扩展成素材中心或复杂预览中心。

### 5. 定时任务

职责：

- 查询任务、查看详情、启停、手动执行、编辑、删除、查看执行日志。

核心接口：

- `POST /job/query`
- `GET /job/{jobId}`
- `POST /job/add`
- `POST /job/update`
- `POST /job/update/enabled`
- `POST /job/execute`
- `GET /job/delete`
- `POST /job/log/query`

核心字段：

- 主表：`jobId/jobName/jobClass/triggerType/triggerValue/enabledFlag/remark/sort`
- 日志：`logId/jobId/successFlag/executeStartTime/executeEndTime/executeResult`

流程图：

```mermaid
flowchart TD
  A["定时任务页 /support/job/job-list.vue"] --> B["POST /job/query"]
  B --> C["AdminSmartJobController -> SmartJobService.queryJob"]
  C --> D["返回 SmartJobVO 分页"]
  D --> E["展示 jobName/jobClass/trigger/enabled/lastLog"]
  E --> F["新增/编辑/启停/删除/立即执行"]
  F --> G["POST /job/add /job/update /job/update/enabled /job/execute"]
  F --> H["GET /job/delete?jobId=..."]
  E --> I["查看执行日志"]
  I --> J["POST /job/log/query"]
  J --> K["返回 SmartJobLogVO 分页"]
```

第一版边界：

- 先完成列表与日志查看闭环。
- `nextJobExecuteTimeList` 作为详情补充信息，不要求第一版复杂呈现。

### 6. 消息管理

职责：

- 面向后管的消息列表、发送消息、删除消息。
- 与“我的消息”区分开，菜单页优先对接 admin `/message/query`。

核心接口：

- `POST /message/query`
- `POST /message/sendMessages`
- `GET /message/delete/{messageId}`

辅助基础接口：

- `POST /message/queryMyMessage`
- `GET /message/getUnreadCount`
- `GET /message/read/{messageId}`

核心字段：

- `messageId/messageType/receiverUserType/receiverUserId/title/content/readFlag/createTime`

流程图：

```mermaid
flowchart TD
  A["消息管理页 /support/message/message-list.vue"] --> B["POST /message/query"]
  B --> C["AdminMessageController -> MessageService.query"]
  C --> D["返回 MessageVO 分页"]
  D --> E["展示 type/title/content/receiver/read/time"]
  E --> F["发送消息"]
  F --> G["POST /message/sendMessages"]
  G --> H["MessageService.sendMessage"]
  H --> I["写入消息表"]
  E --> J["删除消息"]
  J --> K["GET /message/delete/{messageId}"]
  K --> L["页面刷新列表"]
```

第一版边界：

- 仅做 admin 消息管理，不混入“我的消息”工作台逻辑。
- 发送消息先做最小表单，不做模板消息编排器。

### 7. 单号管理

职责：

- 查看单号定义。
- 查看生成记录。
- 支持手动生成测试单号。

核心接口：

- `GET /serialNumber/all`
- `POST /serialNumber/queryRecord`
- `POST /serialNumber/generate`

核心字段：

- 定义：`serialNumberId/businessName/format/ruleType/initNumber/lastNumber/lastTime`
- 记录：`serialNumberId/recordDate/lastNumber/count/lastTime`

流程图：

```mermaid
flowchart TD
  A["单号管理页 /support/serial-number/serial-number-list.vue"] --> B["GET /serialNumber/all"]
  B --> C["返回 SerialNumberEntity 列表"]
  C --> D["展示 businessName/format/ruleType/lastNumber/lastTime"]
  D --> E["选择某个 serialNumberId"]
  E --> F["POST /serialNumber/queryRecord"]
  F --> G["返回 SerialNumberRecordEntity 分页"]
  D --> H["手动生成"]
  H --> I["POST /serialNumber/generate"]
  I --> J["返回生成结果"]
  J --> K["刷新定义与记录"]
```

第一版边界：

- 以“查看定义 + 查看记录 + 手动生成验证”为主。
- 不开放前端编辑单号规则。

### 8. 缓存管理

职责：

- 查看缓存名称。
- 查看某个缓存下的 key。
- 删除某个缓存。

核心接口：

- `GET /cache/names`
- `GET /cache/keys/{cacheName}`
- `GET /cache/remove/{cacheName}`

流程图：

```mermaid
flowchart TD
  A["缓存管理页 /support/cache/cache-list.vue"] --> B["GET /cache/names"]
  B --> C["返回缓存名称列表"]
  C --> D["选择 cacheName"]
  D --> E["GET /cache/keys/{cacheName}"]
  E --> F["返回 key 列表"]
  F --> G["执行删除缓存"]
  G --> H["GET /cache/remove/{cacheName}"]
  H --> I["刷新缓存名称与 key 列表"]
```

第一版边界：

- 只做安全、可确认的运维工具页。
- 不做“清空全部缓存”这类高风险快捷动作。

### 9. Reload

职责：

- 查看 reload tag 列表。
- 修改某个 tag 的 `identification/args`。
- 查看执行结果历史。

核心接口：

- `GET /reload/query`
- `POST /reload/update`
- `GET /reload/result/{tag}`

核心字段：

- 列表：`tag/args/identification/updateTime`
- 结果：`tag/args/result/exception/createTime`

流程图：

```mermaid
flowchart TD
  A["Reload 页 /support/reload/reload-list.vue"] --> B["GET /reload/query"]
  B --> C["返回 ReloadItemVO 列表"]
  C --> D["展示 tag/identification/args/updateTime"]
  D --> E["修改 identification 或 args"]
  E --> F["POST /reload/update"]
  F --> G["ReloadService.updateByTag"]
  D --> H["查看结果历史"]
  H --> I["GET /reload/result/{tag}"]
  I --> J["返回 ReloadResultVO 列表"]
```

第一版边界：

- 定位为运维工具页，不做过多视觉包装。
- 更新动作必须有明确确认提示。

## 推荐实施顺序

### 第一批

1. 参数配置
2. 数据字典
3. 文件管理

原因：

- 后端接口面最完整。
- 页面形态最接近标准管理页，最适合先沉淀 `support` 页模板。

### 第二批

1. 定时任务
2. 消息管理

原因：

- 列表页可稳定落地，但动作和状态比第一批更复杂。

### 第三批

1. 单号管理
2. 缓存管理
3. Reload

原因：

- 更偏工具/运维页面，与普通 CRUD 页节奏不同。

### 批次图

```mermaid
flowchart LR
  A["第一批: 参数配置"] --> B["第一批: 数据字典"]
  B --> C["第一批: 文件管理"]
  C --> D["第二批: 定时任务"]
  D --> E["第二批: 消息管理"]
  E --> F["第三批: 单号管理"]
  F --> G["第三批: 缓存管理"]
  G --> H["第三批: Reload"]
```

## 验证策略

### 路由验证

- 登录后，这些菜单需要从 `module-bridge` 逐个切换为真实页面命中。
- 不修改数据库路径约定，只补本地真实 `views` 页面。

### 页面规范验证

- 所有列表页遵循 `docs/frontend-list-table-page-standard.md`。
- 不添加多余 hero/title/desc 页面介绍块。
- 单行搜索条件默认关闭 collapse。

### 分层验证

- `views/support/*` 只承接页面层逻辑。
- `api/system/*.ts` 统一承接 DTO 和接口请求。
- 共享组件不直接依赖业务接口。

### 命令验证

第一优先验证命令：

```bash
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

若改到共享层，再补：

```bash
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
```

## 成功标准

- 系统设置菜单组的真实路径、真实页面状态、后端接口面全部梳理清楚。
- 设计文档可以直接指导后续单模块实现。
- 第一批模块具备明确的页面职责、流程图和验证路径。
- 后续实现时无需再重新发明路径、接口边界和页面结构。
