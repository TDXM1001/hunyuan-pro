# BPM M1-M8 平台总验收与发布基线

- 基线标识：`BPM-M1-M8-R1`
- 封版日期：2026-07-14
- 功能基线提交：`3293f469d987a465a8b868b290fcc037b57e3c17`
- 验收分支：`main`
- 发布状态：`RELEASABLE`
- 结论：M1-M8 均已完成模块关闭，平台级自动化、Flowable/MySQL 和认证浏览器门禁通过，本基线封版。

## 1. 发布范围

本基线包含企业 BPM 蓝图定义的八个模块：

| 模块 | 关闭状态 | 主要发布能力 | 模块验收记录 |
| --- | --- | --- | --- |
| M1 流程定义中心 | `RELEASABLE` | 正式 Graph、草稿、校验、编译、发布、冻结依赖与元素映射 | `2026-07-11-bpm-m1-modeling-compiler-acceptance.md` |
| M2 身份组织与审批策略 | `RELEASABLE` | 版本化策略、冻结候选、多人审批、自动终态、治理恢复与幂等命令 | `2026-07-13-bpm-m2-assignment-approval-strategy-acceptance.md` |
| M3 审批对象与数据治理 | `RELEASABLE` | 业务契约、对象快照、路由事实、工作数据、字段权限与动作证据 | `2026-07-13-bpm-m3-approval-data-governance-acceptance.md` |
| M4 核心审批运行与工作台 | `RELEASABLE` | Graph 发起、路由、任务授权、退回重提、实例详情与统一工作台 | `2026-07-13-bpm-m4-core-runtime-workbench-acceptance.md` |
| M5 高级流程运行 | `RELEASABLE` | 时间/SLA、外部等待、持久连接器、子流程与重启恢复 | `2026-07-13-bpm-m5-advanced-runtime-acceptance.md` |
| M6 配置化业务接入 | `RELEASABLE` | 开放认证、员工映射、流程绑定、事件订阅与可靠回调 | `2026-07-13-bpm-m6-configurable-business-integration-acceptance.md` |
| M7 运营与治理 | `RELEASABLE` | 异常投影、受控处置、审计、指标、导出与保留策略 | `2026-07-14-bpm-m7-operations-governance-acceptance.md` |
| M8 迁移与演进 | `RELEASABLE` | 定义 Diff、影响预演、受限迁移、租约 fencing 与逐项审计 | `2026-07-14-bpm-m8-definition-evolution-migration-acceptance.md` |

本基线不扩展各模块已声明的非目标，不提供任意 BPMN/脚本、Flowable multi-instance、任意 execution jump、直接修改 Flowable 表、未登记外部副作用重放或已消费副作用的物理回滚。

## 2. 本次平台级自动化证据

以下命令均在功能基线提交 `3293f469`、当前工作树无业务改动的状态下执行。Maven 命令工作目录为 `hunyuan-backend`，pnpm 命令工作目录为 `hunyuan-design`：

| 门禁 | 本次结果 |
| --- | --- |
| `mvn -pl hunyuan-bpm -am test` | 552 个测试，0 失败，0 错误，0 跳过；reactor `BUILD SUCCESS` |
| `pnpm test:unit -- apps/hunyuan-system/src/api/system/bpm apps/hunyuan-system/src/components/bpm apps/hunyuan-system/src/views/system/bpm apps/hunyuan-system/src/router/bpm-designer-layout.test.ts` | 17 个测试文件、42 个测试通过 |
| `pnpm --filter @hunyuan/system typecheck` | 退出码 0 |
| `mvn --% -pl hunyuan-admin -am -Dtest=BpmFlowableCompatibilityTest -Dsurefire.failIfNoSpecifiedTests=false test` | 14 个测试通过；真实 MySQL/Flowable 建库、部署、路由与恢复兼容门禁通过 |
| `mvn --% -pl hunyuan-admin -am -Dtest=BpmM8LiveMigrationAcceptanceTest -Dm8.live=true -Dsurefire.failIfNoSpecifiedTests=false test` | 3 个测试通过，0 跳过；真实迁移、唯一键竞争和租约接管通过 |
| `mvn -pl hunyuan-admin -am -DskipTests package` | 从 `3293f469` 打包成功，reactor `BUILD SUCCESS` |
| `git diff --cached --check` | 三份封版文档暂存后退出码 0，包含新增发布基线文件 |

第一次组合运行未携带 `-Dm8.live=true`，因此 M8 实库验收类的 3 个测试被 JUnit 条件跳过；该结果未计入通过证据。随后使用显式开关独立重跑，3 个测试全部执行并通过。

## 3. 认证浏览器总验收

本机服务为从 `3293f469` 重新打包的后端可执行包 `http://127.0.0.1:1024` 和前端开发服务器 `http://127.0.0.1:5173`。后端 JAR SHA-256 为 `a192cb8c3153d59d66ac8d036afdab6501394ee30f2859eb01eac173bef6a11b`，最终烟测进程 PID 为 `37480`。管理员登录后按正式动态路由逐项检查：

| 模块 | 页面证据 | 本次结果 |
| --- | --- | --- |
| M1 | `/system/bpm/model` | 模型列表、草稿状态及编辑/设计/发布动作可见 |
| M2 | `/system/bpm/policy/policy-catalog` | 策略目录加载真实候选与审批策略版本 |
| M3 | `/system/bpm/business-contract/business-contract-catalog` | 业务契约目录加载 `m3-live-generic@1` |
| M4 | `/system/bpm/runtime/startable-list` | Graph 可发起列表加载 M2 验收定义及发起入口 |
| M5 | `/system/bpm/time-event/time-event-list` | 时间事件筛选、表格与空状态正常 |
| M6 | `/system/bpm/integration/configuration-workbench` | 来源系统、应用、员工映射、流程绑定和事件订阅工作台正常 |
| M7 | `/system/bpm/operations/workbench` | 查询、指标、导出和异常工单表格正常 |
| M8 | `/system/bpm/evolution/workbench` | 版本影响分析、迁移预演入口和审计批次入口正常 |

浏览器首次连接时记录到一条 `access guard bootstrap failed`，刷新后动态路由恢复。随后通过页面正常退出并重新登录，错误日志数量没有增加，M8 动态路由无需刷新即可打开，因此该现象未形成可重复发布阻断。若后续环境再次出现同类错误并可稳定复现，应重新将基线标记为 `NOT_RELEASABLE`，按登录信息、菜单映射和动态路由链路排查。

重新打包并重启后，`/login/getCaptcha` 返回 HTTP 200；M4 可发起列表继续读取真实 Graph 定义，M8 迁移工作台正常加载，浏览器没有新增 error 日志。

## 4. 数据库、兼容与恢复边界

- M1-M8 增量链覆盖 `v3.50.0.sql` 至 `v3.60.0.sql`；各模块验收记录保存了实库执行、重放或结构查询证据。
- 本次总验收没有重新删除并从空库顺序执行全部增量脚本，避免破坏当前验收数据；以 552 个后端测试中的 schema source 门禁、当前已迁移开发库和本次真实 M8 MySQL/Flowable 测试作为总验收证据。
- 发布前必须备份 Hunyuan BPM 配置、运行事实和 Flowable 运行表，并停止会影响迁移、补偿或外部回调的一致性写流量。
- 数据库对象回退只适用于确认没有对应模块运行事实和外部副作用的环境。已完成审批、已投递回调、已触发连接器、已启动子流程和已迁移实例不得通过删表或直接改 Flowable 表回滚。
- 运行补偿统一遵循 M7 已登记处置能力；定义迁移遵循 M8 预演、确认、租约和逐项审计边界。

## 5. 封版结论

M1-M8 的实现完成、模块关闭与平台发布状态已经分别形成证据。本次同一提交上的全量回归、真实 Flowable/MySQL 门禁、认证登录和八模块页面矩阵均通过，没有未关闭的 Critical 或 Important 发布阻断。

`BPM-M1-M8-R1` 状态为 `RELEASABLE`，允许作为当前企业 BPM 发布基线。后续功能、契约、迁移、安全边界或模块语义变化必须建立新的版本化基线，不得覆盖本记录。

## 6. 2026-07-15 补充说明：可视化配置中心闭环

2026-07-15 在当前 `main` 本地工作树上补充完成了“可视化审批规则与业务对象配置中心”闭环验收，详见：

- `docs/superpowers/specs/2026-07-14-bpm-visual-policy-business-object-configuration-acceptance.md`

该补充验收证明：

- 审批规则、业务对象、Graph 引用、通用申请、审批与管理端对账已形成真实闭环；
- 技术协议权限仍在服务端关闭；
- `v3.62.0.sql` 连续执行两次成功，历史 v1 行未被改写；
- 管理端实例/任务详情已收敛为业务可读展示。

本节是对 `BPM-M1-M8-R1` 之后本地增强验收的补充说明，不覆盖 2026-07-14 的历史封版提交号与封版日期。
