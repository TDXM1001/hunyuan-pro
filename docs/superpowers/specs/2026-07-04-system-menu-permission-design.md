# 系统管理菜单与权限闭环设计

## 背景

`hunyuan-system` 已经完成真实登录、后端菜单加载、组织架构相关页面接入，以及未知后端页面的 `module-bridge` 承接。当前前端基础不应继续只停留在组织架构切片；下一步应回到系统管理主链路，把菜单、路由、组件路径、按钮权限码和角色授权闭合。

本设计遵循 `AGENTS.md`：一次只推进一个可验证增量，优先沿用已有项目模式，不新增依赖，不把多个系统模块一次性铺开。

## 目标

第一阶段只实现“菜单管理 + 权限闭环”：

- 管理菜单树、路由路径、组件路径、外链、显示/禁用状态、排序。
- 管理按钮权限码，包括前端权限字符串 `webPerms` 和后端权限字符串 `apiPerms`。
- 让角色页已有的菜单授权结果能够继续影响前端可见性。
- 保留 `module-bridge` 作为未迁移页面的缓冲，而不是一次性补完全部系统页面。

## 非目标

- 不在第一阶段实现字典管理、参数配置、登录日志、操作日志页面。
- 不新增路由/权限框架或第三方依赖。
- 不重做登录、菜单加载、角色页面整体结构。
- 不把 `ArtSearchPanel`、`ArtTable`、`ArtTableHeader` 等共享组件绑定到后端接口。
- 不把菜单管理做成通用低代码生成器。

## 当前证据

后端菜单接口已经具备：

- `GET /menu/query`
- `GET /menu/tree?onlyMenu=...`
- `GET /menu/detail/{menuId}`
- `POST /menu/add`
- `POST /menu/update`
- `GET /menu/batchDelete`
- `GET /menu/auth/url`

菜单字段来自后端 `MenuBaseForm` / `MenuVO`：

- `menuId`
- `menuName`
- `menuType`
- `parentId`
- `sort`
- `path`
- `component`
- `frameFlag`
- `frameUrl`
- `cacheFlag`
- `visibleFlag`
- `disabledFlag`
- `permsType`
- `webPerms`
- `apiPerms`
- `icon`
- `contextMenuId`

前端现状：

- `apps/hunyuan-system/src/router/access.ts` 已通过 `getAllMenusApi()` 接入后端菜单。
- `apps/hunyuan-system/src/views/system/module-bridge/index.vue` 已承接未迁移后端页面。
- `apps/hunyuan-system/src/views/system/role/index.vue` 已接入角色菜单授权。
- `apps/hunyuan-system/src/api/system/organization.ts` 暂时承载了组织、角色、权限相关 API，菜单管理应拆出独立 API 文件，避免继续膨胀。

## 方案比较

### 方案 A：先做菜单管理与权限闭环

优点：

- 直接补齐系统管理主链路。
- 可以验证“后端菜单变更 -> 角色授权 -> 前端菜单可见性”的核心能力。
- 能为字典、配置、日志等后续页面提供稳定菜单入口。

缺点：

- 比单纯做一个查询列表复杂，需要处理树表、表单和权限字段。

### 方案 B：先做字典/配置

优点：

- 页面形态更简单，主要是普通列表和表单。
- 能快速增加可见页面数量。

缺点：

- 不能解决系统管理主链路问题。
- 菜单可见性、按钮权限码、页面迁移路径仍然不清楚。

### 方案 C：一次性铺开菜单、字典、配置、日志

优点：

- 表面进度快。

缺点：

- 违反“一次一个可验证增量”。
- 容易复制大量页面代码，造成 API、表格、权限处理风格漂移。
- 难以定位 typecheck 或接口联调失败来源。

推荐采用方案 A。

## 页面设计

菜单管理是普通后台管理页面，不渲染额外解释性标题块。布局遵循 `docs/frontend-list-table-page-standard.md`：

- 外层使用 `Page`。
- 搜索区域使用 `ArtSearchPanel`。
- 表格区域使用 `ArtTablePanel`、`ArtTableHeader`、`ArtTable`。
- 搜索如果只有一行自然字段，禁用 collapse。
- 行操作保持紧凑 link button。

页面主体采用树表：

- 主列：菜单名称，显示图标、类型和层级。
- 辅助列：路由路径、组件路径、权限码、排序、状态。
- 操作列：新增下级、编辑、删除。

菜单表单第一阶段使用已有页面中更常见的 `ElDialog`，减少新结构引入。表单字段按菜单类型动态显示：

- 目录/菜单：显示路由路径、组件路径、图标、缓存、显示、禁用。
- 按钮/功能点：显示 `contextMenuId`、`webPerms`、`apiPerms`、`permsType`。
- 外链：显示 `frameFlag`、`frameUrl`。

## API 设计

新增：

- `apps/hunyuan-system/src/api/system/menu.ts`

职责：

- 定义菜单 DTO、表单 DTO、请求函数和 payload builder。
- 不承载页面状态。
- 不合并进 `organization.ts`。

实现函数：

- `queryMenus()`
- `queryMenuTree(onlyMenu: boolean)`
- `getMenuDetail(menuId: number)`
- `addMenu(form)`
- `updateMenu(form)`
- `batchDeleteMenus(menuIdList: number[])`
- `listAuthUrls()`

payload builder 需要处理：

- 字符串 trim。
- `parentId` 缺省为 `0`。
- 布尔字段保持显式值，避免后端校验失败。
- 按钮菜单不伪造组件路径。

## 权限闭环

菜单管理完成后，权限闭环按三个层面验证：

1. 菜单管理页面可以维护菜单和权限码。
2. 角色页面继续使用已接入的 `getRoleSelectedMenu` / `updateRoleMenu` 授权菜单。
3. 重新登录或刷新权限状态后，前端菜单来自后端菜单结果；未迁移页面仍进入 `module-bridge`，已迁移页面进入真实 Vue 页面。

第一阶段不要求实现无刷新权限热更新。菜单/角色变更后的确认路径可以是重新登录或清理权限状态后刷新。

## 错误处理

- 请求失败使用现有 request client 和页面级 loading 状态。
- 删除菜单前使用确认框。
- 后端校验错误直接展示统一错误提示。
- 查询空结果展示空态，不增加解释性页面文案。
- 批量删除仅在有选中项时启用。

## 测试与验证

最小验证：

```bash
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

建议增加源码级 Vitest 约束，类似现有 `organization-modules.test.ts`：

- 菜单页面存在并使用 `ArtTable`。
- 菜单页面没有额外 hero/title/desc 文案块。
- 菜单 API 文件包含 `/menu/query`、`/menu/tree`、`/menu/add`、`/menu/update`、`/menu/batchDelete`、`/menu/auth/url`。
- 菜单页面包含 `webPerms`、`apiPerms`、`component`、`path` 字段。

如果改动共享组件，再额外验证 `@vben/web-ele` typecheck；第一阶段目标是不改共享组件。

## 后续阶段

菜单管理验收后，再按顺序推进：

1. 字典管理。
2. 参数配置。
3. 登录日志。
4. 操作日志。

这些后续页面复用菜单管理中沉淀的系统管理页面节奏，但每次仍保持单模块增量。

## 成功标准

- 菜单管理页面替代对应后端菜单的 `module-bridge`。
- 菜单树、路由路径、组件路径、按钮权限码可查询和维护。
- 角色授权页仍能选择和保存菜单权限。
- 后端菜单可见性继续驱动前端菜单结果。
- `@hunyuan/system` typecheck 通过。
