# F1 App Kernel 与稳定路由交付记录

## 1. 交付结论

截至 2026-07-24，F1 的代码、数据库迁移和管理员运行时链路已经完成。前端不再依靠未消费的 feature 描述对象表达模块边界，而是由应用唯一注册表显式装配 organization、identity-employee 和 access；后端菜单新增稳定 `routeId`，前端按 `routeId` 优先解析本地懒加载组件，历史 `component` 仅保留兼容读取。

F1 当前状态为“实现完成、验收未完全关闭”。管理员、五个迁移页面、直达 URL 和未注册模块已经完成真实浏览器验收；受限角色仍缺独立测试账号的真实浏览器复验，不将自动化契约测试替代描述为人工浏览器通过。

## 2. App Kernel 协议

统一协议位于 `hunyuan-design/packages/app-kernel`，注册阶段校验以下不变量：

1. 模块 ID 不重复。
2. 启用模块的依赖全部存在且启用。
3. `routeId` 在应用内唯一。
4. 每个声明路由都有且只有一个懒加载器。
5. 关闭模块不向应用页面映射暴露路由。

应用唯一注册表位于 `apps/hunyuan-system/src/app-kernel/feature-registry.ts`。应用负责组合请求客户端与页面薄入口，feature 包只声明模块 ID、依赖、能力和稳定路由，不依赖应用别名或应用请求单例。

## 3. 首批稳定路由

| routeId | 后端菜单 ID | 应用路径 | feature owner |
| --- | ---: | --- | --- |
| `organization.department.directory` | 219 | `/organization/directory` | organization |
| `organization.position.directory` | 228 | `/organization/position` | organization |
| `identity.employee.management` | 46 | `/organization/employee` | identity-employee |
| `access.role.management` | 76 | `/organization/role` | access |
| `access.menu.management` | 26 | `/menu/list` | access |

运行时解析顺序固定为：

```text
后端 menu.routeId
  -> 应用 App Feature Registry
  -> 本地懒加载薄入口
  -> feature 页面
```

当菜单没有 `routeId` 时继续读取历史 `component`；当菜单携带未知 `routeId` 时进入模块桥接页，不回退到可能属于其他模块的旧组件路径。

## 4. 后端与数据库约束

`V3_79_0__f1_stable_menu_route_id.sql` 为 `t_menu` 增加可空唯一字段 `route_id`，并只回填上述五个已迁移页面。多个空值用于承载尚未迁移的历史菜单，非空值由唯一索引阻断冲突。

菜单管理契约同步增加 `routeId`。新建非外链页面菜单必须填写稳定 `routeId`；编辑历史菜单仍允许暂时保留空值，避免在 F1 一次性破坏剩余旧页面。服务层同时检查 routeId 重复，数据库唯一索引提供最终一致性保护。

本机开发库验证结果：

- Flyway 当前版本：`3.79.0`
- 失败迁移：0
- 五个目标菜单 routeId：全部匹配
- 重复非空 routeId：0
- 后端运行地址：`http://127.0.0.1:1024`

## 5. 验证证据

自动化门禁：

- App Kernel、登录菜单双读、feature 契约和 F0 结构守卫：26 项通过。
- `@hunyuan/system` TypeScript 检查通过。
- 菜单目录和授权查询 Java 测试：22 项通过。
- Maven `hunyuan-admin -am` 打包通过。
- `git diff --check` 通过。

全量前端测试仍有一个既有测试使用与其他结构测试冲突的 `process.cwd()` 假设：从 workspace 根目录运行时，`account.test.ts` 会错误查找 `hunyuan-design/src/api/core/account.ts`；改到应用目录运行又会使其他结构测试重复拼接 `apps/hunyuan-system`。该问题不影响上述 F1 定向测试、类型检查、生产构建或浏览器验收，留待后续统一测试根目录时修复。

浏览器验收：

- 管理员登录成功，菜单包含部门、岗位、员工和角色入口。
- 部门、岗位、员工、角色和菜单管理五个页面均能直达并渲染真实业务内容。
- 菜单管理页面展示“稳定路由标识”列。
- 未注册模块直达 URL 展示 404，不装配历史业务组件。

尚未关闭的验收项：

- 使用独立受限角色账号登录，确认只生成后端授权返回的菜单，并验证无权限直达 URL 的最终行为。当前已有后端授权查询和前端子集映射测试，但仍需真实账号浏览器证据。

## 6. 后续边界

F2 可以继续迁移平台配置 feature，但不能删除 `component` 双读。只有当 F0 账本中的剩余历史页面全部拥有稳定 routeId、完成浏览器验收并经过独立 Flyway 迁移后，才能删除应用内 `views/**/*.vue` 兼容扫描和后端 `component` 输出。
