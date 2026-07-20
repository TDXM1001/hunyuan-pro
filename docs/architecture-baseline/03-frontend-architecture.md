# 前端架构

## 1. 目标

本期建设一个管理后台应用，同时保证未来可以增加 H5、小程序和桌面端，而不要求重写后端业务能力。

前端采用 Monorepo、编译期子包注册、路由级懒加载和运行时权限过滤。

## 2. 目标目录结构

以下结构表达职责，不要求在没有真实业务时提前创建空子包：

```text
frontend/
├─ apps/
│  └─ admin-web/
├─ packages/
│  ├─ app-kernel/
│  ├─ admin-shell/
│  ├─ platform-auth/
│  ├─ platform-http/
│  ├─ platform-ui/
│  ├─ platform-contracts/
│  └─ features/
│     ├─ feature-a/
│     └─ feature-b/
└─ tooling/
```

该目录树是逻辑职责示意，现有前端工程可以在保持当前目录命名的前提下落实相同依赖边界。是否调整现有目录应由单独的现状审计和迁移设计决定。

各层职责：

| 区域 | 职责 |
| --- | --- |
| `apps/admin-web` | 管理后台组合入口和构建配置 |
| `app-kernel` | 应用生命周期、模块协议和模块注册 |
| `admin-shell` | 布局、导航、标签页和管理后台外壳 |
| `platform-*` | 身份、请求、UI 和公共协议等平台能力 |
| `features/*` | 按真实业务能力划分的子包 |

## 3. 依赖方向

```text
应用 -> 主包与业务子包 -> 平台包
```

约束如下：

- 主包不能包含具体业务状态判断或业务接口调用。
- 业务子包不能直接访问其他子包内部页面、Store 和 API 实现。
- 业务子包通过公开类型、导航协议或明确服务接口协作。
- 平台包不能反向依赖具体业务子包。
- 所有跨包依赖应由 lint、构建或依赖图测试检查。

## 4. 子包注册方式

业务子包通过稳定入口向主应用暴露模块声明。下面只是协议示意，不代表已经确定具体字段：

```ts
export interface AppModule {
  id: string;
  supportedApps: string[];
  dependencies?: string[];
  routes: ModuleRoute[];
  navigation?: NavigationItem[];
  capabilities: string[];
  setup?: (context: AppContext) => void | Promise<void>;
}
```

主应用在构建期明确装配模块：

```ts
createAdminApplication({
  modules: [moduleA, moduleB],
});
```

模块代码随应用统一构建，页面使用动态 `import()` 实现路由级懒加载。当前不从远程地址动态下载业务代码。

## 5. 路由、菜单和权限

推荐职责分配：

```text
前端：定义路由、页面组件和默认导航
后端：返回启用模块和当前用户能力
系统配置：可覆盖菜单名称、顺序和可见性
```

后端不应返回前端源码组件路径，例如 `views/a/list.vue`。前端使用稳定路由 ID 将导航配置映射到本地组件。

应用启动顺序：

```text
加载环境配置
  -> 初始化请求客户端
  -> 恢复身份状态
  -> 获取当前用户、启用模块和能力
  -> 校验模块依赖
  -> 注册允许访问的路由
  -> 生成导航
  -> 打开目标页面
```

权限尚未加载完成时，应显示明确初始化状态，避免先展示全部菜单再突然移除。

## 6. 状态分类

前端状态分为：

| 类型 | 示例 | 建议归属 |
| --- | --- | --- |
| 应用状态 | 当前用户、主题、语言、标签页 | 应用级 Store |
| 服务端状态 | 列表、详情、分页、缓存 | API 查询层或专用查询工具 |
| 模块状态 | 跨模块页面保留的业务状态 | 模块内部 Store |
| 页面状态 | 表单输入、弹窗、临时筛选 | 页面或组件本地状态 |

不要把所有接口数据放入单一全局 Store，也不要为了形式统一给每个页面创建长期状态。

## 7. 模块间通信

优先顺序：

```text
公开接口 > 稳定路由协议 > 类型化小范围事件 > 直接访问对方 Store
```

跨模块导航使用稳定路由 ID。确实需要事件时，应定义事件名称和载荷类型，禁止无约束的全局 `eventBus.emit("somethingChanged")`。

核心业务联动必须由后端完成，不能依赖用户浏览器保持打开或收到某个前端事件。

## 8. 多终端策略

未来可以增加：

```text
apps/
├─ admin-web/
├─ mobile-h5/
├─ mini-program/
└─ desktop/
```

跨端优先共享：

- TypeScript 数据类型。
- 接口契约和客户端基础设施。
- 权限和能力编码。
- 与 UI 无关且含义一致的规则。

通常不直接共享：

- 管理后台表格和复杂表单。
- 页面布局和导航。
- 路由定义。
- 强依赖浏览器或小程序运行时的组件。

桌面端若主要复用管理后台，可在实际立项时评估 Tauri 或 Electron。小程序框架应在小程序需求明确时选择，不在本期预设。

## 9. 当前技术基线

```text
Vue 3
TypeScript
Vite
Vue Router
Pinia
Element Plus
pnpm workspace
Vitest
Playwright
```

该基线支持当前管理后台，不代表未来所有终端必须使用同一 UI 框架。
