# BPM M4 核心审批运行与工作台验收记录

- 日期：2026-07-13
- 分支：`main`
- 状态：`RELEASABLE`；实现、数据库迁移、自动化门禁、真实启动、Flowable 路由运行和浏览器工作台均已关闭
- 设计：`docs/superpowers/specs/2026-07-11-bpm-module-04-core-runtime-workbench-design.md`
- 前置模块：M1、M2、M3 均已关闭

## 1. 当前能力事实

- Graph 条件网关通过固定 execution listener 请求 Hunyuan 路由服务，使用冻结 Graph 边条件和 M3 路由事实形成不可变路由决定；Flowable 只消费生成的布尔变量。
- Graph 办理任务通过发布期 authored/compiled 映射恢复 authored 节点 ID、名称和类型，不从 Flowable task key 猜测业务语义。
- 任务详情和列表返回服务端计算的 `availableActions`；M2 成员动作读取冻结审批策略，办理任务只开放办理动作。
- 实例保存 `currentGeneration`，任务保存 `taskVersion`。工作台提交审批、拒绝、退回和办理时携带任务版本，过期版本返回 `TASK_VERSION_CONFLICT`。
- 实例详情从冻结 Graph 快照、任务、审批阶段、路由决定和动作事实装配 authored 运行图。结果 Outbox 继续复用同库事务内的 `t_bpm_callback_record` 和既有重试调度器，通知继续复用 `t_bpm_notification_record`。

## 2. 自动化证据

| 门禁 | 本次结果 |
| --- | --- |
| M4 RED/GREEN 聚焦测试 | 43 个测试通过 |
| `mvn -pl hunyuan-bpm -am test` | 476 个测试，0 失败，0 错误 |
| `BpmFlowableCompatibilityTest` | 7 个测试，0 失败，0 错误 |
| 前端 `vitest` | 31 个测试文件、144 个测试通过 |
| `pnpm --filter @hunyuan/system typecheck` | 退出码 0 |
| `git diff --check` | 无 whitespace error |

Flowable 新增运行断言实际部署 Graph 条件流程、启动实例、触发 `hunyuanGraphRouteDecisionListener`、写入 `large` 分支变量，并确认活动任务为 authored 办理节点对应的 `graph_node_large_handle`。

## 3. 数据库与启动证据

- 已执行 `数据库SQL脚本/mysql/sql-update-log/v3.56.0.sql`。
- `t_bpm_instance.current_generation` 为非空、默认 1；`t_bpm_task.task_version` 为非空、默认 1。
- `t_bpm_route_decision` 已增加 `graph_definition_version_id`，旧 `definition_id` 和 `definition_node_id` 对 Graph 路由允许为空。
- 新运行包构建成功，后端由 PID `28864` 监听 `1024`；验证码接口 HTTP 200，日志包含 `Started AdminApplication`。
- 启动后恢复扫描实际读取待投递 callback、活动审批成员和未完成引擎副作用，未出现 schema 或 Bean 初始化错误。

## 4. 浏览器工作台证据

- 登录后“我的待办”页面可正常加载服务端动作契约，当前管理员账号无待办时显示稳定空态。
- “我的申请”实际读取 24 条实例，包含运行中、待重提、已通过和已取消状态。
- 实例 `DK20260713NO01162` 详情显示审批对象摘要、3/3 多人审批阶段、三名成员动作、动作轨迹和 authored `Start -> Review -> End` 运行图，节点状态均与运行事实一致。
- 页面刷新后仍保持认证并重新读取列表；没有新增 error 级控制台日志。首次登录切换产生的一条历史 access-guard 日志未在刷新时复现。

## 5. 关键异常与恢复边界

- 重复 M2 `requestId`、异指纹冲突、多人终态竞争、阶段副作用恢复和重启恢复继续由 M2 已关闭矩阵覆盖。
- 本次新增过期 `taskVersion` 失败关闭，Graph 路由决定按实例代际与 authored 节点幂等复用。
- M3 工作数据版本冲突、隐藏字段裁剪、动作证据和最终冻结版本继续由 M3 已关闭矩阵覆盖。
- 业务结果与通知写入失败不回滚已形成终态，由现有 Outbox/通知记录和调度恢复；M7 后续只增加运营处置，不改变 M4 终态事实。

## 6. 当前结论

M4 已把 M1 的正式 Graph、M2 的冻结审批阶段和 M3 的审批对象转化为可发起、可路由、可处理、可退回重提、可审计和可恢复的核心运行时，并通过待办、已办、抄送、我的申请和实例详情提供统一工作台。当前未发现阻止首个产品基线发布的实现级缺口，M4 状态为 `RELEASABLE`，模块可以关闭；M1-M4 首个产品基线同时关闭。
