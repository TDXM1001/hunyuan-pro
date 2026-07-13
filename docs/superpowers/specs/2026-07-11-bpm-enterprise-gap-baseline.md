# Hunyuan 企业级流程引擎差距与借鉴基线

- 日期：2026-07-11
- 状态：当前持续维护的差距基线
- 总体蓝图：`docs/superpowers/specs/2026-07-11-bpm-enterprise-blueprint.md`

> 2026-07-12 重建：BPM 数据已清空，当前按总体蓝图重新规划 M1-M8。旧作者模型、旧表和旧发布链不进入新架构；`KEEP` 只表示语义、算法或验收断言可以吸收，不能据此保留旧数据契约或兼容入口。

## 1. 使用方式

本文件把 Hunyuan 当前事实与 Yudao/RuoYi 参考能力映射为明确决策。它不是参考项目功能清单，也不表示所有 `ADAPT` 项都立即实施。模块关闭后必须把对应项更新为实际状态。

证据优先级：当前 Hunyuan 代码与本次验收记录，高于旧计划；参考源码实际实现，高于页面文案和枚举名称。

## 2. 源码证据入口

### Hunyuan

以下路径用于核对当前实现和可吸收的算法/测试证据，不代表新架构继续使用这些类名、JSON 或模块边界。

- 当前能力基线：`docs/superpowers/specs/2026-07-10-bpm-development-baseline.md`
- 受限编译器：`hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompiler.java`
- 发布校验：`.../engine/compiler/SimpleModelValidator.java`、`BpmSimpleModelPublishValidator.java`
- 运行时：`.../module/runtime/service/BpmInstanceService.java`、`BpmTaskService.java`
- 审批组：`.../module/runtime/service/BpmApprovalGroupService.java`
- 可靠集成：`.../module/integration/service/`
- 前端设计适配：`hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/`
- 前端 BPM 页面：`hunyuan-design/apps/hunyuan-system/src/views/system/bpm/`

### Yudao 前端

- 流程节点与配置：`src/components/SimpleProcessDesignerV2/src/`
- 节点类型和策略：`src/components/SimpleProcessDesignerV2/src/consts.ts`
- 设计器页面：`src/views/bpm/simple/SimpleModelDesign.vue`
- 定义和打印：`src/views/bpm/model/`
- 运行详情：`src/views/bpm/processInstance/detail/`
- 用户组、表达式、监听器：`src/views/bpm/group/`、`processExpression/`、`processListener/`
- 运行报表：`src/views/bpm/processInstance/report/`

### RuoYi 后端

- 节点类型：`yudao-module-bpm/.../enums/definition/BpmSimpleModelNodeTypeEnum.java`
- 简单模型：`.../controller/admin/definition/vo/model/simple/BpmSimpleModelNodeVO.java`
- 编译工具：`.../framework/flowable/core/util/SimpleModelUtils.java`
- BPMN 解析：`.../framework/flowable/core/util/BpmnModelUtils.java`
- 审批方式：`.../enums/definition/BpmUserTaskApproveMethodEnum.java`
- 超时处理：`.../enums/definition/BpmUserTaskTimeoutHandlerTypeEnum.java`
- 触发器：`.../service/task/trigger/`
- 子流程运行：`.../service/task/BpmProcessInstanceServiceImpl.java`
- 用户组、表达式、监听器：`.../controller/admin/definition/`

## 3. 能力差距矩阵

| 能力 | Hunyuan 当前事实 | 参考项目事实 | 决策 | 目标模块 |
| --- | --- | --- | --- | --- |
| 分类、表单、模型、定义 | 已有管理和发布闭环 | 参考项目同样具备 | `KEEP` | M1/M3 |
| 发布快照与并发认领 | 已阻止并发发布旧快照 | 参考项目主要依赖模型更新部署 | `KEEP` | M1 |
| 候选人发布预检 | 已有 READY/RUNTIME_REQUIRED/BLOCKING | 参考项目候选策略更广 | `KEEP+HARDEN` | M2 |
| 旧作者模型结构 | v1/v2 递归 AST 已证明若干分支语义，但不满足正式流程图和多层设计器要求 | 树形节点及多种分支节点 | `REPLACE`，新库只保存 `HunyuanProcessDefinitionGraph`，不建设旧模型导入 | M1 |
| 编译器结构 | typed fragment、路由 delegate 和固定网关已证明部分算法可行 | 多类节点和网关编译 | `REFACTOR`，抽取可复用算法，唯一入口改为已验证 Graph/Structured IR | M1 |
| 流程资产导入导出 | 旧 `.hunyuan-process.json` 是树形资产格式 | 参考前端支持 simple model JSON 导入导出 | `REPLACE` 为 Graph 资产、schema 版本、canonical hash 和错误定位契约 | M1 |
| 完整 BPMN 模式 | 当前只维护 Hunyuan 简单模型 | 参考项目同时保留 BPMN 模型入口 | `DEFER`，不作为当前企业级门槛 | M1 边界 |
| 设计时抄送节点 | 已实现非阻塞抄送与 copy fact 幂等 | 参考有 COPY_NODE | `KEEP` 业务语义，在新 Graph、发布快照和 M4 运行事实中重新实现 | M1/M4 |
| 办理节点 | 已实现办理结果和独立可用动作 | 参考有 TRANSACTOR_NODE 和办理按钮 | `KEEP` 业务语义，在新任务命令和运行投影中重新实现 | M1/M4 |
| 排他条件分支 | 已实现类型化条件、默认分支、路由 delegate 和账本 | 条件/路由分支 | `KEEP+REFACTOR`，Graph 显式配置节点、边和条件，M4 记录路由事实 | M1/M4 |
| 并行分支 | 已实现固定 split/join 与汇合运行证据 | 独立并行分支 | `KEEP+REFACTOR`，正式图校验成对汇合，M4 证明并发推进 | M1/M4 |
| 包容分支 | 已实现单命中、多命中、默认分支和动态汇合 | 包容网关 | `KEEP+REFACTOR`，正式图负责结构，M4 负责动态路由与汇合事实 | M1/M4 |
| 任意循环和自由连线 | 未支持 | 完整 BPMN 可表达更多结构 | `DEFER` | 蓝图边界 |
| 任意条件表达式 | 未支持 | 支持规则和表达式 | `REJECT` 默认；登记表达式可 `ADAPT` | M1 |
| 单人审批 | 已支持 | 已支持 | `KEEP` | M2 |
| 顺序多人 | 编译为连续任务并投影审批组 | 参考使用 multi-instance | `KEEP` | M2 |
| 并行全员会签 | 固定 gateway + 独立任务 + 审批组 | 参考使用 multi-instance | `KEEP` | M2 |
| 或签和比例会签 | 未支持 | ANY、RATIO | `ADAPT` | M2 |
| 随机一人 | 未支持 | RANDOM | `DEFER`，先验证业务价值 | M2 |
| 自动通过/自动拒绝节点 | 未形成节点策略 | 参考审批类型支持自动终态 | `ADAPT`，按高风险系统动作治理 | M2/M4 |
| 指定历史节点退回 | 当前稳定语义是退回发起人重提 | 参考支持驳回到指定任务节点 | `ADAPT`，仅允许已发布祖先审批节点且建立新执行代 | M1/M4 |
| 重复审批人自动通过 | 未支持 | 参考有全部/连续节点自动通过 | `DEFER`，需要先明确审计和自审语义 | M2 |
| Flowable multi-instance | 未作为公共模型 | 参考广泛使用 | `REJECT` 直接外露；内部按场景评估 | M2/M5 |
| 部门、角色、发起人策略 | 已覆盖常用六类 | 参考覆盖更多组织来源 | `KEEP+HARDEN` | M2 |
| 岗位、用户组、多级主管 | 未支持 | 前后端均有配置 | `ADAPT` | M2 |
| 任意候选表达式 | 未支持 | 参考提供表达式策略 | `REJECT` 任意 EL；登记解析器可 `ADAPT` | M2 |
| 无审批人处理 | 以校验阻断为主 | 自动通过/拒绝/指定人/管理员 | `ADAPT`，默认失败关闭 | M2 |
| 审批人与发起人相同 | 无统一策略 | 自审/跳过/部门主管 | `ADAPT` | M2 |
| 节点字段权限 | M3 已按 Graph 节点冻结 READONLY/EDITABLE/HIDDEN 与敏感级别，并由服务端裁剪详情和写入权限 | 参考已有字段权限 | `KEEP+HARDEN` 已落地 | M3 |
| 数据版本和字段审计 | M3 已通过显式版本锁、追加工作数据和动作前后值证据闭环 | 参考主要使用流程变量和表单字段 | `KEEP` 已落地 | M3 |
| 任务超时 | M5 已纳入结构化时间事实和 M4 单次任务终态边界 | 提醒、通过、拒绝 | `REBUILD` 已落地并通过竞态断言 | M5 |
| 延迟节点 | M5 已形成 Graph 冻结配置、结构化事实和真实 Flowable timer | 固定时长/日期 | `REBUILD` 已落地 | M5 |
| HTTP 请求节点 | 已有登记连接器、SSRF 策略和幂等重试资产 | 参考可配置 URL/参数 | `REBUILD` 为登记连接器，继续拒绝任意 URL | M5/M6 |
| HTTP 回调等待 | M5 已关闭 receive task、持久边界 timer、token 摘要、HMAC、回调/超时单次认领和重启恢复 | 参考有 receive task 等待 | `REBUILD` 已落地 | M5 |
| 表单修改触发器 | 审批数据服务已有版本化修改语义 | 参考有更新/删除表单触发器 | `HARDEN` 为受控数据命令，不允许任意删除 | M3/M5 |
| 子流程 | M5 已形成冻结版本、独立 Hunyuan 子实例、callActivity、输入输出和父子事实闭环 | callActivity、变量映射、超时、多实例 | `ADAPT` 已落地；多实例不在 M5 范围 | M5 |
| 主子流程取消/拒绝传播 | M5 已按冻结策略实现完成、拒绝、暂停、人工处置和父取消传播 | 参考已有传播逻辑 | `ADAPT` 已落地 | M5 |
| 任务、实例、抄送 | M4 已按 Graph authored 映射、冻结动作、任务版本、执行代和结构化运行图完成重建 | 参考已闭环 | `KEEP+HARDEN` 已落地 | M4 |
| 流程管理员 | 依赖系统管理权限和管理员动作 | 参考定义可配置流程管理员 | `ADAPT` 为定义级运营责任人和授权范围 | M7 |
| 任务/执行监听器 | 已有目录和节点快照资产，运行契约有限 | 参考可登记 execution/task listener | `HARDEN` 为登记事件订阅，不开放 class/EL | M5/M6 |
| 评论 | 审批意见属于动作日志 | 参考有独立评论域 | `ADAPT`，区分动作证据和协作评论 | M4/M7 |
| 附件、签名图片 | BPM 未形成完整结构化证据域 | 参考运行页已展示 | `ADAPT`，复用文件服务并进入动作证据 | M3/M4 |
| 打印模板 | 未形成 BPM 打印资产 | 参考有打印模板和打印页 | `ADAPT` | M7 |
| 运行流程图 | 有受限设计预览和结构化 trace 资产 | 参考有 BPMN/simple viewer | `REBUILD` 为正式 Graph 运行投影 | M4/M7 |
| 流程报表 | 缺少流程业务报表 | 参考有实例报表 | `ADAPT`，只读运行投影和审计事实 | M7 |
| 命令、通知、回调记录 | 已有幂等与人工补偿资产 | 参考也有消息和触发器 | `KEEP+REFACTOR` | M4/M5/M6/M7 |
| 业务结果事件 | 已有业务事件和样板费用资产 | 参考有 OA 请假和监听器接入 | `KEEP+REFACTOR` 为固定协议与 Outbox/Inbox | M6 |
| 业务接入与审批详情 | M3 已落地业务契约、审批对象快照、通用申请和统一详情；来源系统登记与外部可靠接入仍归 M6 | 参考支持自定义创建/详情路径 | `REFACTOR` 的 M3 范围已落地，M6 继续外部接入 | M3/M6 |
| 内外任务入口 | 当前仅 Hunyuan 页面与内部 API 为主 | 成熟平台支持嵌入和开放任务 API | `ADAPT`，统一任务命令、员工映射和服务端字段权限 | M4/M6 |
| 定义演进与实例迁移 | 定义版本和实例快照已具备，未提供受限迁移 | 成熟引擎提供迁移工具 | `ADAPT`，仅支持预演后满足安全条件的实例，使用补偿而非任意回滚 | M8 |

## 4. 必须保留的 Hunyuan 优势

1. 发布使用单一模型快照并认领更新版本，避免部署陈旧模型。
2. 候选预检明确区分可确定错误与运行时依赖。
3. 审批组是 Hunyuan 结构化领域投影，不要求前端解析引擎多实例。
4. 任务、实例、trace 和通知使用 Hunyuan ID、状态和员工语义。
5. 命令、回调、通知均有持久化记录和人工补偿方向。
6. 审批数据治理显式处理字段权限、版本冲突和字段审计。
7. 管理端完整视图与员工授权视图保持独立。

## 5. 需要重构的当前结构

### 5.1 定义与编译主链

当前作者模型、原始 JSON 校验和编译入口相互耦合。新 M1 不在旧入口上继续增加分支，而是重建唯一主链：

```text
HunyuanProcessDefinitionGraph
-> GraphValidator
-> Structured IR
-> NodeCompiler / FragmentComposer
-> CompiledDefinitionArtifact + authored/compiled mappings
```

### 5.2 运行时命令入口

当前实例和任务服务直接承担较多 Flowable 推进、投影和动作日志职责。新 M4 必须先形成统一命令协调边界，明确锁定、校验、驱动引擎、写运行事实、投影和结果 Outbox；M5 再在此边界上增加异步事件与子流程。

### 5.3 审批组完成语义

当前顺序与 `parallelAll` 的逻辑已经证明部分完成语义可行，但新增 ANY、RATIO 后不应继续增加模式分支。M2 应以新的候选快照、审批组、完成策略、终止策略和授权决策重建，不保留旧表与公共 VO 兼容要求。

### 5.4 诊断快照与公共契约

内部 JSON 快照可以保留用于诊断，但后续路由、时间、事件和子流程必须拥有结构化投影和 VO，不得继续让页面从快照推断业务状态。

## 6. 不应照搬的参考机制

| 参考机制 | 不直接照搬的原因 | Hunyuan 优化方向 |
| --- | --- | --- |
| 任意条件表达式 | 注入、不可预测、难以静态校验 | 类型化规则 + 登记表达式键 |
| 设计器直接配置任意 HTTP URL | SSRF、凭据泄露、环境漂移 | 登记连接器 + 凭据引用 + 出站策略 |
| 所有多人审批都用 multi-instance | 引擎语义泄露，历史解释和投影复杂 | Hunyuan 审批组 + 可插拔完成策略 |
| 直接用 Flowable 状态解释页面 | 业务语义受引擎升级影响 | Hunyuan 结构化运行投影 |
| 自动通过/拒绝作为普通选项 | 可能直接改变业务终态 | 权限、风险级别、发布警告和审计 |
| 任意表单字段删除触发器 | 破坏审计和数据连续性 | 受控 patch、版本和变更审计 |
| 自定义页面路径自由字符串 | 路由注入和部署漂移 | 业务页面注册键 |

## 7. 优先级

```text
EVIDENCE  旧 M1/M3/M4 实现只作为算法、状态语义和测试资产
P0  M1 流程定义中心
P1  M2 身份组织与审批策略 + M3 审批对象与数据治理
DONE  M4 核心审批运行与工作台；M1-M4 首个产品基线已关闭
DONE  M5 高级流程运行；时间、等待、持久命令和子流程已关闭
P2  M6 配置化业务接入（在 M5 可靠事件语义稳定后关闭）
P3  M7 运营治理
P4  M8 定义演进与受限迁移
```

严格依赖为 `M1 -> (M2 + M3) -> M4 -> M5 -> M6 -> M7 -> M8`。M1-M4 共同构成首个可用产品基线，不能把前置能力面的单独完成误报为整个平台完成。

## 8. 更新规则

- 模块设计完成不改变本表状态。
- 生产实现和自动化验证完成后，状态可从缺失更新为“已实现待活体验收”。
- 真实业务流验收和基线回写完成后，才更新为 `KEEP` 或已完成事实。
- 参考项目新增功能不会自动成为 Hunyuan 路线图；必须重新经过决策分类。
