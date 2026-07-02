# Hunyuan System Frontend Plan

## 目标

建立一个独立的 `hunyuan-system` 前端应用，用于对接真实后端的系统管理能力；保留 `web-ele` 作为无登录 demo、组件样板和交互验证应用。

这次拆分不是为了创建第二套组件，而是为了分清两个运行场景：

- `web-ele`: 展示组件、验证样式、沉淀交互规范。
- `hunyuan-system`: 对接真实登录、权限、菜单、系统管理接口。
- `@vben/art-hooks`: 继续作为共享组件和页面 primitives 的沉淀位置。

## Karpathy-style 推进原则

1. 先理解边界，再写代码。
2. 每次只推进一条可验证竖切片。
3. 不新增依赖，不复制组件库。
4. 通用组件只负责传参、样式和交互，不直接绑定后端接口。
5. 页面和 API 模块负责把后端 DTO 转成组件需要的 props 和事件。

## 应用边界

```text
hunyuan-design/apps/web-ele
  demo app
  mock/no-login friendly
  component examples

hunyuan-design/apps/hunyuan-system
  real system app
  backend login/session
  system management pages
  platform capability pages

hunyuan-design/packages/@vben/art-hooks
  shared UI primitives
  table/search/action/status/edit/detail components
```

## 第一阶段：应用壳

成功标准：

- `apps/hunyuan-system` 被 pnpm workspace 识别。
- `@hunyuan/system` 可以单独运行 typecheck。
- 暂不接真实登录，先建立独立应用命名空间和 API base 配置。

验证命令：

```bash
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

## 第一阶段补充：完整壳迁移

当前 `hunyuan-system` 只完成了最小应用壳，还没有迁移 `web-ele` 的完整非 demo 应用能力。下一步要把它升级为可承接真实后端系统管理的完整壳。

### 迁移目标

`hunyuan-system` 应该具备和 `web-ele` 同级的应用基础设施：

- 应用初始化：`main.ts`、`bootstrap.ts`、`app.vue`、`preferences.ts`。
- 组件适配：`adapter/component`、`adapter/form`，必要时保留 `adapter/vxe-table`。
- 请求基础：`api/request.ts`、`api/core/*`。
- 权限状态：`store/auth.ts`、`store/index.ts`。
- 路由体系：`router/index.ts`、`router/guard.ts`、`router/access.ts`、`router/routes/core.ts`、`router/routes/index.ts`。
- 布局：`layouts/basic.vue`、`layouts/auth.vue`、`layouts/index.ts`。
- 核心页面：`views/_core/authentication`、`views/_core/fallback`、`views/_core/profile`、`views/_core/about`。
- 本地化和类型：`locales/*`、`types/*`。

### 不迁移内容

- 不迁移 `views/demos/*`。
- 不迁移 `router/routes/modules/demos.ts`。
- 不迁移纯组件演示用页面。
- `views/dashboard/*` 暂缓迁移，因为当前 dashboard 包含 demo 链接，需要先改造成系统首页后再进入 `hunyuan-system`。
- `router/routes/modules/hunyuan.ts` 暂缓迁移，等真实系统管理菜单结构明确后再设计。

### 迁移方式

第一轮使用“复制再收敛”，不提前抽成新的 app-core 包。

原因：

- 目前只有 `web-ele` 和 `hunyuan-system` 两个应用，过早抽包会增加理解成本。
- `web-ele` 仍要承担 demo 作用，不能被真实后端登录、菜单、权限污染。
- `hunyuan-system` 需要先跑通真实系统管理闭环，再判断哪些壳能力值得反向抽取。

### 迁移后的立即调整

迁移完成后立刻做以下隔离：

- `.env` 使用 `Hunyuan System` 标题和 `hunyuan-system` namespace。
- `vite.config.ts` 代理默认指向后端服务，不指向 mock 服务。
- `api/core/auth.ts` 保留文件位置，但接口路径准备从 `/auth/*` 改为后端真实 `/login/*`。
- `router/routes/index.ts` 不引入 demo routes。
- `locales` 删除或不引用 demo 文案。
- 首页暂时使用轻量系统首页，不复用 demo dashboard。

### 验证门槛

每次迁移后必须通过：

```bash
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
pnpm --dir hunyuan-design -F @vben/web-ele run typecheck
```

成功标准：

- `hunyuan-system` 能独立 typecheck。
- `web-ele` demo 不被破坏。
- 没有新增外部依赖。
- demo routes 不进入 `hunyuan-system`。
- 权限、菜单、请求逻辑仍在应用层，不下沉到通用组件。

### 推荐实施顺序

1. 迁移完整非 demo 应用壳。
2. 清理 `hunyuan-system` 中的 demo route 引用。
3. 保留登录页，但先不改真实接口。
4. typecheck 双应用。
5. 再进入真实登录闭环：`/login`、`/login/getLoginInfo`、`/login/logout`、`/login/getCaptcha`。

## 第二阶段：真实登录闭环

目标接口优先级：

1. `POST /login`
2. `GET /login/getLoginInfo`
3. `GET /login/logout`
4. `GET /login/getCaptcha`

成功标准：

- 使用真实后端 token。
- 能获取当前登录用户。
- 登录失败和登录过期有明确提示。
- 不污染 `web-ele` 的 demo 登录。

## 第三阶段：菜单和权限闭环

目标：

- 对接真实菜单、角色、权限码。
- 明确前端动态路由来源。
- 保留静态核心路由，如登录、错误页、个人中心。

成功标准：

- 登录后系统菜单来自后端或后端映射结果。
- 页面按钮级权限可以落到 `ArtActionGroup` 的 actions 配置上。
- 权限逻辑不写进通用组件。

## 第四阶段：系统管理页面

推荐顺序：

1. 用户管理
2. 角色管理
3. 部门/组织管理
4. 菜单管理
5. 字典管理
6. 参数/配置管理

每个页面都按同一条数据链路推进：

```text
backend controller
  -> apps/hunyuan-system/src/api/system/*.ts
  -> page state/composable
  -> ArtSearchPanel / useTable / ArtActionGroup / ArtStatusTag
```

## 第五阶段：平台能力中心

系统管理稳定后，再接入以下平台能力：

1. 文件管理
2. SMS 模板和发送日志
3. 站内消息
4. 邮件
5. 操作日志和登录日志

这些能力属于 foundation/platform，不属于单个业务模块。业务模块只消费能力，不拥有上传协议、短信通道或消息发送机制。

## 不做的事

- 不把真实后端接口写进 `web-ele` demo 页面。
- 不让 `ArtSearchPanel`、`ArtActionGroup`、`ArtStatusTag` 直接调用接口。
- 不在第一阶段重做完整登录、菜单和系统管理页面。
- 不新增依赖。
- 不把编辑/详情页面抽象成大而全生成器。
