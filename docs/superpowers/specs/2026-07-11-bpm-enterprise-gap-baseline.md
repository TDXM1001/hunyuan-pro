# Hunyuan 企业级流程引擎差距与借鉴基线

- 日期：2026-07-11
- 状态：持续维护的差距基线
- 总体蓝图：`docs/superpowers/specs/2026-07-11-bpm-enterprise-blueprint.md`

## 1. 使用方式

本文件把 Hunyuan 当前事实与 Yudao/RuoYi 参考能力映射为明确决策。它不是参考项目功能清单，也不表示所有 `ADAPT` 项都立即实施。模块关闭后必须把对应项更新为实际状态。

证据优先级：当前 Hunyuan 代码与本次验收记录，高于旧计划；参考源码实际实现，高于页面文案和枚举名称。

## 2. 源码证据入口

### Hunyuan

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
| 简单模型结构 | v1 兼容，v2 为最大深度 3 的受控递归 AST，已完成多路径实流 | 树形节点及多种分支节点 | `KEEP`，M1 已关闭 | M1 |
| 编译器结构 | 已按 typed fragment 组合任务、抄送和三类固定网关，JUEL 缺失变量安全 | 多类节点和网关编译 | `KEEP`，M1 已关闭 | M1 |
| 模型 JSON 导入导出 | 已形成 `.hunyuan-process.json` v2 资产契约并校验版本、类型、key 和深度 | 前端支持 simple model JSON 导入导出 | `KEEP` | M1 |
| 完整 BPMN 模式 | 当前只维护 Hunyuan 简单模型 | 参考项目同时保留 BPMN 模型入口 | `DEFER`，不作为当前企业级门槛 | M1 边界 |
| 设计时抄送节点 | 已实现非阻塞 `COPY_TASK`、收件人解析和 copy fact 幂等，实流生成 copy `2` | 参考有 COPY_NODE | `KEEP`；运营展示继续归 M6 | M1/M6 |
| 办理节点 | 已实现 `HANDLE_TASK`、办理结果和独立可用动作，实流 task `124/127/130/131/132` 完成 | 参考有 TRANSACTOR_NODE 和办理按钮 | `KEEP`；运营体验继续归 M6 | M1/M6 |
| 排他条件分支 | 已实现类型化条件、默认分支、路由 delegate 和账本，实流 routeDecision `3/5/7/9` | 条件/路由分支 | `KEEP`，M1 已关闭 | M1 |
| 并行分支 | 已实现 authored 独立并行 split/join，旧 `parallelAll` 保持兼容；实流双任务汇合通过 | 独立并行分支 | `KEEP`，M1 已关闭 | M1 |
| 包容分支 | 已实现单命中、多命中、默认分支和动态汇合 BPMN，实流 routeDecision `4/6/8/10` | 包容网关 | `KEEP`，M1 已关闭 | M1 |
| 任意循环和自由连线 | 未支持 | 完整 BPMN 可表达更多结构 | `DEFER` | 蓝图边界 |
| 任意条件表达式 | 未支持 | 支持规则和表达式 | `REJECT` 默认；登记表达式可 `ADAPT` | M1 |
| 单人审批 | 已支持 | 已支持 | `KEEP` | M2 |
| 顺序多人 | 编译为连续任务并投影审批组 | 参考使用 multi-instance | `KEEP` | M2 |
| 并行全员会签 | 固定 gateway + 独立任务 + 审批组 | 参考使用 multi-instance | `KEEP` | M2 |
| 或签和比例会签 | 未支持 | ANY、RATIO | `ADAPT` | M2 |
| 随机一人 | 未支持 | RANDOM | `DEFER`，先验证业务价值 | M2 |
| 自动通过/自动拒绝节点 | 未形成节点策略 | 参考审批类型支持自动终态 | `ADAPT`，按高风险系统动作治理 | M2/M4 |
| 指定历史节点退回 | 当前稳定语义是退回发起人重提 | 参考支持驳回到指定任务节点 | `ADAPT`，仅允许已发布祖先 user task 且建立新执行代 | M1/M2 |
| 重复审批人自动通过 | 未支持 | 参考有全部/连续节点自动通过 | `DEFER`，需要先明确审计和自审语义 | M2 |
| Flowable multi-instance | 未作为公共模型 | 参考广泛使用 | `REJECT` 直接外露；内部按场景评估 | M2/M5 |
| 部门、角色、发起人策略 | 已覆盖常用六类 | 参考覆盖更多组织来源 | `KEEP+HARDEN` | M2 |
| 岗位、用户组、多级主管 | 未支持 | 前后端均有配置 | `ADAPT` | M2 |
| 任意候选表达式 | 未支持 | 参考提供表达式策略 | `REJECT` 任意 EL；登记解析器可 `ADAPT` | M2 |
| 无审批人处理 | 以校验阻断为主 | 自动通过/拒绝/指定人/管理员 | `ADAPT`，默认失败关闭 | M2 |
| 审批人与发起人相同 | 无统一策略 | 自审/跳过/部门主管 | `ADAPT` | M2 |
| 节点字段权限 | 仓库验收已通过 READONLY/EDITABLE/HIDDEN | 参考已有字段权限 | `KEEP+HARDEN` | M3 |
| 数据版本和字段审计 | 仓库验收已通过显式版本/变更账本 | 参考主要使用流程变量和表单字段 | `KEEP` | M3 |
| 任务超时 | 未支持 | 提醒、通过、拒绝 | `ADAPT`，自动终态需高风险控制 | M4 |
| 延迟节点 | 未支持 | 固定时长/日期 | `ADAPT` | M4 |
| HTTP 请求节点 | 未支持 | 设计器可配置 URL/参数 | `ADAPT` 为登记连接器，不复制任意 URL | M4 |
| HTTP 回调等待 | 只有流程结果回调 | 参考有 receive task 等待 | `ADAPT` 并复用现有幂等补偿 | M4 |
| 表单修改触发器 | 审批数据服务可修改 | 参考有更新/删除表单触发器 | `HARDEN` 为受控数据命令，不允许任意删除 | M3/M4 |
| 子流程 | 未支持 | callActivity、变量映射、超时、多实例 | `ADAPT` | M5 |
| 主子流程取消/拒绝传播 | 未支持 | 参考已有传播逻辑 | `ADAPT` 为显式策略 | M5 |
| 任务、实例、抄送 | 已闭环 | 参考已闭环 | `KEEP` | M6 |
| 流程管理员 | 依赖系统管理权限和管理员动作 | 参考定义可配置流程管理员 | `ADAPT` 为定义级运营责任人 | M6 |
| 任务/执行监听器 | 已有监听器目录和节点 listeners 快照，运行契约有限 | 参考可登记 execution/task listener | `HARDEN` 为登记事件订阅，不开放 class/EL | M4/M6 |
| 评论 | 审批意见属于动作日志 | 参考有独立评论域 | `ADAPT`，区分动作意见和协作评论 | M6 |
| 附件、签名图片 | BPM 未形成结构化证据域 | 参考运行页已展示 | `ADAPT`，复用 Hunyuan 文件服务 | M6 |
| 打印模板 | 未形成 BPM 打印资产 | 参考有打印模板和打印页 | `ADAPT` | M6 |
| 运行流程图 | 受限设计预览和结构化 trace | 参考有 BPMN/simple viewer | `ADAPT`，数据来自结构化运行事实 | M6 |
| 流程报表 | 缺少流程业务报表 | 参考有实例报表 | `ADAPT`，避免直接查询 Flowable 表 | M6 |
| 命令、通知、回调记录 | Hunyuan 已有并可人工补偿 | 参考也有消息和触发器 | `KEEP+HARDEN` | M4/M6/M7 |
| 业务结果事件 | 已有 Hunyuan 业务事件和样板费用 | 参考有 OA 请假和监听器接入 | `KEEP+HARDEN` | M7 |
| 自定义业务表单 | 当前以表单 schema 和样板业务为主 | 参考支持自定义创建/详情路径 | `ADAPT` 为业务页面注册，不存任意路由字符串 | M7 |

## 4. 必须保留的 Hunyuan 优势

1. 发布使用单一模型快照并认领更新版本，避免部署陈旧模型。
2. 候选预检明确区分可确定错误与运行时依赖。
3. 审批组是 Hunyuan 结构化领域投影，不要求前端解析引擎多实例。
4. 任务、实例、trace 和通知使用 Hunyuan ID、状态和员工语义。
5. 命令、回调、通知均有持久化记录和人工补偿方向。
6. 审批数据治理显式处理字段权限、版本冲突和字段审计。
7. 管理端完整视图与员工授权视图保持独立。

## 5. 需要重构的当前结构

### 5.1 `SimpleModelBpmnCompiler`

当前通过顺序循环和 `instanceof` 处理 `UserTaskFragment`、`ParallelAllFragment`。增加分支、时间、触发器和子流程后会快速膨胀。M1 必须将其重构为：

```text
AST node
-> NodeCompiler
-> BpmnFragment(entryRefs, exitRefs, generatedElements, snapshots)
-> FragmentComposer
-> CompiledDefinitionArtifact
```

### 5.2 运行时命令入口

当前实例和任务服务直接承担较多 Flowable 推进、投影和动作日志职责。M4/M5 引入异步事件和子流程前，需要形成统一命令协调边界，明确：锁定、校验、驱动引擎、写运行事实、投影和异步后续。

### 5.3 审批组完成语义

当前顺序与 parallelAll 的逻辑已经可用，但新增 ANY、RATIO 后不应继续增加大量模式分支。M2 应抽取成员解析、完成策略、终止策略和允许动作策略，同时保留现有表和公共 VO 兼容。

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
BASELINE  M3 审批数据治理已通过仓库验收，继续接受用户回归反馈
P0  M4 时间、SLA 与事件驱动
P1  M2 审批人与多人审批策略
P1  M5 子流程与流程组合
P1  M6 运行时工作台与运营治理
P1  M7 业务接入与开放平台
```

M6、M7 的基础部分随每个前置模块同步接入，但其总体完成在核心运行能力稳定之后验收。

## 8. 更新规则

- 模块设计完成不改变本表状态。
- 生产实现和自动化验证完成后，状态可从缺失更新为“已实现待活体验收”。
- 真实业务流验收和基线回写完成后，才更新为 `KEEP` 或已完成事实。
- 参考项目新增功能不会自动成为 Hunyuan 路线图；必须重新经过决策分类。
