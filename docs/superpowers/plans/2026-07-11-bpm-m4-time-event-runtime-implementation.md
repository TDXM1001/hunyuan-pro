# BPM M4 时间、SLA 与事件驱动实施计划

> **历史状态：** 本计划对应旧模块编号和旧作者模型，不再作为当前执行计划。其 timer、外部等待、连接器安全、幂等与恢复断言作为新 M5 的证据资产；当前设计以 `docs/superpowers/specs/2026-07-11-bpm-module-05-advanced-runtime-design.md` 为准。

> **执行要求：** 当前分支内联实施，按任务逐项执行 RED-GREEN-REFACTOR；不使用子代理，不新增依赖。

**目标：** 在保持旧流程定义和历史实例兼容的前提下，完整交付任务 SLA、自动终态、延迟节点、登记连接器、外部等待、幂等回调、运营处置和真实事件驱动验收。

**架构：** 受控模型升级为 schema v3，仍由 Hunyuan AST 和片段编译器生成 BPMN；Flowable 负责计时与等待信号，Hunyuan 负责策略快照、命令协调、幂等、审计和运营投影。所有外部调用只引用登记连接器，不允许模型保存 URL、凭据或表达式。

**技术栈：** Java 17、Spring Boot 3、Flowable 7.2、MyBatis-Plus、MySQL 8、Vue 3、TypeScript、Vitest。

**当前状态：** 实现、仓库门禁、数据库核对、真实启动、管理 API、Flowable timer job 和 Chrome 页面已通过；受控模拟连接器、真实成功回调、自动终态竞态和重启恢复仍待活体验收。证据见 `docs/superpowers/specs/2026-07-11-bpm-m4-time-event-runtime-acceptance.md`。

## 全局约束

- 所有文本文件使用 UTF-8，生产代码中的复杂逻辑使用简洁中文注释。
- 在用户当前 `main` 分支完成，保留并避开现有未提交修改。
- 不新增 Maven 或 npm 依赖，HTTP 调用复用 Spring Framework 客户端能力。
- 字典新增或调整必须同时进入独立版本的增量 SQL，并使用可重复执行写法。
- 所有 timer、callback 和人工动作通过统一状态锁与业务幂等键保证单次有效终态。
- 旧 schema v1/v2 定义行为不变，历史定义和实例不做数据重写。

---

### 任务 1：模型契约与发布校验

**文件：**
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/ast/ProcessNode.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/ast/ProcessNodeType.java`
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/ast/DelayNode.java`
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/ast/ExternalTriggerNode.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/ProcessAstParser.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/ProcessAstValidator.java`
- 测试：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/ProcessAstParserTest.java`
- 测试：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/ProcessAstValidatorTest.java`

**产出接口：** schema v3 支持 `DELAY` 与 `EXTERNAL_TRIGGER`；人工任务配置接受结构化 `taskSlaPolicy`。时间只允许 ISO-8601 duration、ISO offset datetime 或已发布表单日期字段。

- [ ] 先写 schema v3、非法时间、任意 URL/EL 被拒绝的失败测试并确认 RED。
- [ ] 增加 AST 类型、解析和校验，确认聚焦测试 GREEN。
- [ ] 回归 v1/v2 解析测试，证明旧定义兼容。

### 任务 2：BPMN 编译和 Flowable 信号网关

**文件：**
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompiler.java`
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/FlowableTimeEventGateway.java`
- 测试：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompilerTest.java`
- 新增测试：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/FlowableTimeEventGatewayTest.java`

**产出接口：** 延迟节点编译为 timer catch event；任务 SLA 编译为受控 boundary timer；外部等待编译为 Hunyuan delegate 与 receive task，运行网关只暴露定位和恢复所需方法。

- [ ] 写编译 XML 与重复恢复失败测试并确认 RED。
- [ ] 实现受控 BPMN 片段和 Flowable 网关，确认 GREEN。
- [ ] 运行现有 M1 编译器与兼容性测试。

### 任务 3：时间事件、SLA 与外部等待持久化

**文件：**
- 新增：`数据库SQL脚本/mysql/sql-update-log/v3.49.0.sql`
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmTimeEventStatusEnum.java`
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmExternalWaitStatusEnum.java`
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmTimeEventEntity.java`
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmExternalWaitEntity.java`
- 新增：对应 DAO、Mapper XML、VO 和查询表单。
- 修改：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/schema/BpmSchemaSourceTest.java`

**产出接口：** `t_bpm_time_event` 保存 SLA/延迟事实，`t_bpm_external_wait` 保存等待与不可猜测 token 摘要；增量 SQL 同步写入 M4 状态、动作、节点、超时策略和等待模式字典。

- [ ] 先扩展 schema 契约测试，确认缺表、索引、字典时 RED。
- [ ] 编写 UTF-8、幂等的 v3.49.0 增量 SQL 和持久化模型。
- [ ] 运行 schema 测试并确认 GREEN。

### 任务 4：SLA 调度、提醒与自动终态

**文件：**
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTimeEventService.java`
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmRuntimeCommandCoordinator.java`
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/HunyuanTimeEventDelegate.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskProjectionService.java`
- 修改：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`
- 新增测试：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTimeEventServiceTest.java`
- 新增测试：`hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandCoordinatorTest.java`

**产出接口：** 新任务按冻结策略创建 SLA 事实；提醒复用可靠通知；自动通过、拒绝和转管理员使用系统 actor、固定意见、同一任务锁和先到终态胜出规则。

- [ ] 写正常提醒、重复触发、人工竞态和风险分类失败测试并确认 RED。
- [ ] 实现最小命令协调与时间事件状态机，确认 GREEN。
- [ ] 回归任务动作、审批组、通知和实例 trace 测试。

### 任务 5：延迟、连接器与外部等待恢复

**文件：**
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/entity/BpmConnectorDefinitionEntity.java`
- 新增：对应 DAO、Mapper、表单、VO 与 `BpmConnectorRegistryService.java`
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmConnectorInvocationService.java`
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/HunyuanExternalTriggerDelegate.java`
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmExternalWaitService.java`
- 新增测试：连接器安全、NO_WAIT、WAIT_CALLBACK、重复/伪造回调、超时和取消测试。

**产出接口：** 连接器目录按 key/version 冻结；仅允许 HTTPS 登记端点并阻断回环、链路本地与元数据地址；回调使用 token 摘要、签名、相关键和状态锁恢复一次。

- [ ] 写 SSRF、安全引用、重试限制和回调幂等失败测试并确认 RED。
- [ ] 实现登记、调用、等待和恢复服务，确认 GREEN。
- [ ] 使用本地受控模拟服务完成成功、超时和重复回调集成测试。

### 任务 6：管理 API、前端设计器与运营页面

**文件：**
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmTimeEventController.java`
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmConnectorController.java`
- 新增：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmExternalCallbackController.java`
- 修改：`hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts`
- 修改：`hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-tree-editor.vue`
- 修改：`hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue`
- 新增：M4 API、失败时间事件、外部等待和连接器目录页面及路由。
- 修改：`hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

**产出接口：** 设计器提供 SLA、延迟和连接器结构化控件；详情显示截止、提醒、延迟和等待；管理员可查询并重试失败事件，普通用户不能配置高风险自动终态。

- [ ] 先写 API 契约、页面模块和设计器序列化失败测试并确认 RED。
- [ ] 实现 API、控件、页面、权限与路由，确认 GREEN。
- [ ] 运行 BPM Vitest、Vue typecheck 和后端控制器/服务测试。

### 任务 7：真实验收与基线回写

**文件：**
- 新增：`docs/superpowers/specs/2026-07-11-bpm-m4-time-event-runtime-acceptance.md`
- 修改：`docs/superpowers/specs/2026-07-11-bpm-enterprise-blueprint.md`
- 修改：`docs/superpowers/specs/2026-07-11-bpm-enterprise-gap-baseline.md`
- 修改：`docs/superpowers/specs/2026-07-10-bpm-development-baseline.md`

**验收证据：** 自动化门禁、数据库升级、实际 timer job、提醒、人工竞态、延迟到期、NO_WAIT、回调恢复、重复/伪造回调、取消、重启恢复、管理处置和 Chrome 页面证据。

- [ ] 在测试库执行 v3.49.0 并核对表、索引、字典和字符集。
- [ ] 启动真实后端与前端，跑通事件驱动样板费用流程并记录真实 ID。
- [ ] 完成 Chrome 验收、全量门禁和 UTF-8 扫描。
- [ ] 回写验收记录与三份基线，只记录本次实际证据。
