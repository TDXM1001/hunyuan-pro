# Hunyuan BPM 后续开发基线

- 状态：持续维护的开发基线
- 初始整理日期：2026-07-10
- 适用仓库：`E:\my-project\hunyuan-pro`

> 2026-07-12 路线调整：用户已完成 BPM 数据重置，后续所有运行验收从空定义、空实例和空任务状态重新建立证据。当前唯一总体设计与 M1-M8 任务基线为 `2026-07-11-bpm-enterprise-blueprint.md`；新增定义直接使用正式流程图，不建设旧作者模型导入、双格式或兼容发布链。

## 使用方式

开始任何 BPM/流程引擎改动前，先阅读本文件，再阅读与当前切片直接相关的最新验收记录。它记录的是 Hunyuan 当前已经闭环的能力、明确未闭环的前后端差异，以及不能被当作“小页面缺失”的平台能力边界。

本文件不是对 Yudao/RuoYi 的迁移清单。前端参考线为 `E:\my-project\huanyuan-pro-jichu\yudao-ui-admin-vue3-master`，后端参考线为 `E:\my-project\huanyuan-pro-jichu\ruoyi-vue-pro-master`；只有当当前需求跨越本文件列出的能力边界时，才重新分析参考实现中可借用的具体机制。

每关闭一个 BPM 切片时：

1. 新增该切片独立的设计或验收记录。
2. 更新本文件的“已完成能力”“当前优先级”或“平台边界”。
3. 将本次真实执行过的验证写清楚；历史验证不得改写为本次验证。

## 一句话结论

Hunyuan BPM 是一个以 Flowable `7.2.0` 为内部执行引擎的 **Hunyuan 原生企业审批引擎**。当前代码已经证明线性审批、受限 `parallelAll`、逐节点数据治理、分支编译运行时，以及 SLA、延迟、登记连接器和外部等待等语义可行；这些结论是新平台的算法与测试资产，不代表旧模型、旧表或旧接口继续作为架构边界。当前主线按新 M1-M8 从空库重建。

当前已实现设计器使用 v2 递归流程树，编译器按 authored AST 组合受限片段；以下内容只记录历史运行时事实，不是新增定义的作者模型或兼容要求：

```text
单人：userTask
顺序多人：userTask_1 -> userTask_2 -> ...
parallelAll：parallelGateway split -> N 个 userTask -> parallelGateway join
排他/包容：route delegate -> gateway split -> authored branches -> gateway join
独立并行：parallelGateway split -> authored branches -> parallelGateway join
```

新 M1 使用显式节点、边、作用域和策略组成的正式流程图，并重新建设保存、校验、编译、发布与设计器链路；仍不开放任意 BPMN、脚本 EL 或未经校验的动态拓扑。

## 已完成能力

### 1. 定义与设计治理

- 流程分类、表单、模型/设计器、定义发布、版本管理、发起范围、挂起/恢复已经形成管理端能力。
- 流程表单与流程模型设计器均作为隐藏业务路由保留在 `BasicLayout` 内，并通过 `activePath` 维持“流程表单”或“流程模型”的菜单上下文；表单使用全高三栏工作区，模型使用摘要、流程设计、运行规则和发布检查工作区。
- 发布前会校验模型、发起规则、分类、表单、字段一致性与候选人预检。
- 发布读取单一模型快照，并在部署前按 `model_id + update_time` 认领；模型被并发修改时拒绝部署，避免发布旧快照。
- 设计器发布前检查内部脏状态和运行规则草稿快照，避免“修改运行规则但未保存，仍然发布”的问题。
- 定义的当前版本和可发起版本保持一致；处理版本问题时应沿真实发布、定义查询和发起列表链路验证，而不是只调用准备接口。

### 2. 设计器与候选人规则

- 当前节点类型为 `USER_TASK/HANDLE_TASK/COPY_TASK/EXCLUSIVE_BRANCH/PARALLEL_BRANCH/INCLUSIVE_BRANCH`，分支最大深度为 3。
- 已支持以下候选人策略与组织来源：
  - `EMPLOYEE`：指定员工。
  - `DEPARTMENT_MANAGER`：部门主管。
  - `ROLE`：角色成员。
  - `POST`：岗位成员。
  - `USER_GROUP`：用户组成员。
  - `START_EMPLOYEE`：发起人本人。
  - `START_DEPARTMENT_MANAGER`：发起人部门主管。
  - `ROUTING_FACT_EMPLOYEE`：从已声明并冻结的业务路由事实解析员工。
  - `MANAGEMENT_CHAIN`：部门主管链或员工汇报链，按冻结深度与自审规则展开。
  - `EMPLOYEE_SELECT_AT_START`：发起时从表单选择审批人。
- `EMPLOYEE_SELECT_AT_START` 已经贯通设计器、`formSchemaJson`、运行时表单和发起校验；设计器字段必须来自表单 schema，运行时会校验员工存在且可用。
- 候选人预检统一使用 `READY`、`RUNTIME_REQUIRED`、`BLOCKING` 语义。角色无成员、部门无主管等确定性错误会阻断发布；必须依赖实际发起人或表单数据的规则保留为运行时校验。
- 模拟发起人与模拟表单员工也经过组织身份网关校验；禁用、删除或不存在的员工以 `EMPLOYEE_NOT_FOUND` 阻断。

### 3. 多人审批

#### 3.1 顺序多人审批

- 已支持指定员工的顺序多人审批：`approvalMode=sequential` 且候选人策略为 `EMPLOYEE`。
- 这不是 Flowable multi-instance。设计器中的一个节点会按员工顺序展开为多个连续的单处理人任务，例如 `task_finance_1`、`task_finance_2`。
- 顺序审批至少需要两名不同的正整数员工 ID；发布前逐一校验员工。
- 编译后的节点 key 需要符合 `[A-Za-z_][A-Za-z0-9_]*`，全局唯一且不超过 128 字符，以保证 `assignee_<nodeKey>` 变量表达式安全。
- 编译快照会补齐 `approvalGroupKey`、`approvalGroupName`、`sequentialIndex`、`sequentialTotal`，运行时据此为同一 authored 节点的真实任务建立稳定审批组。
- 运行时使用 `t_bpm_approval_group` 保存顺序审批组状态、进度、关闭原因和成员顺序；恢复时同时按 Hunyuan `instanceId` 与当前 `engineProcessInstanceId` 过滤，避免旧引擎遗留任务串组。
- 实例详情、任务详情、trace 和员工待办列表已透出结构化顺序审批组；未来成员只显示待激活人数，不创建占位任务行。
- 顺序审批的拒绝、退回会先取消当前 Flowable 实例，再关闭审批组并写入 `REJECTED` / `RETURNED` 语义；重提会生成新的引擎实例与新的审批组 ID，旧组保留为历史事实。
- 顺序审批成员允许转办、委派、加签、减签；转办/委派保持同一任务和同一组内索引身份，加签子任务不继承 `approvalGroupId`、不计入 authored 组进度。

#### 3.2 并行全员会签

- 已支持受限的指定员工并行全员会签：`approvalMode=parallelAll` 且候选人策略固定为 `EMPLOYEE`。
- 至少需要两名有效且不重复的员工；发布前校验员工身份和所有编译后 key。
- 编译器生成固定的分叉网关、N 个独立成员任务和汇聚网关，不使用 Flowable multi-instance。
- 运行时使用 Hunyuan `t_bpm_approval_group` 保存组状态、进度与关闭原因，成员仍是独立 `t_bpm_task`。
- 所有成员通过后组状态为 `APPROVED/ALL_APPROVED`；任一拒绝或退回会关闭整组并取消其他待办。
- 发起人撤回或取消实例时，进行中的审批组统一变为 `CANCELLED` 并记录实例级关闭原因。
- 任务、实例详情和 trace 使用结构化审批组契约；普通流程保持空审批组字段。
- 设计器只读画布可显示固定 split、成员和 join 拓扑；这不代表支持通用并行网关建模。

### 4. 员工运行时闭环

- 发起、草稿、重提、我的流程、取消、我的待办、我的已办、我的抄送已经可用。
- 基础任务动作已经完成前端/API/UI 闭环：通过、拒绝、退回发起人、转办。
- 审批动作支持填写意见，并可在通过、拒绝、退回时选择抄送员工。
- 实例详情、任务详情、trace、当前任务投影和动作记录已经可用；管理端与员工端复用统一详情抽屉语义。
- 员工任务详情使用 `/app/bpm/task/detail/{taskId}` 并校验当前员工归属；管理端详情权限保持独立。
- 手动抄送已闭环，包含抄送列表和已读状态。
- 通知记录、回调记录、命令记录、回调重试和人工补偿已经存在于流程追踪与可靠性边界内。

### 5. 高级任务动作

员工端前端/API/UI 已接入以下动作：

- `delegate`：当前以重新分配处理人的 Hunyuan 语义实现，不等同于暴露 Flowable 原生委派模型。
- `addSign`：创建 Hunyuan 任务投影。
- `reduceSign`：撤销可撤销的加签投影。
- `recall`：将实例转为 `WAIT_RESUBMIT`，而不是任意跳转到历史节点。

顺序审批成员可转办、委派、加签、减签和实例级撤回；`parallelAll` 成员可转办、委派和实例级撤回，但加签、减签被后端硬拒绝且前端不展示。普通任务继续按原有规则使用加签、减签。

### 6. 审批数据治理

- 节点支持字段权限 `READONLY`、`EDITABLE`、`HIDDEN`，发布时冻结到定义节点编译快照；未显式配置的字段按只读处理。
- 员工任务详情返回统一 `formContext`，其中 `formSchemaJson`、`formDataJson` 和 `fieldPermissions` 都经过服务端权限过滤；`HIDDEN` 字段不会下发到员工端。
- 审批通过支持携带 `formDataVersion` 与 `formDataPatchJson`，后端只允许当前任务授权为 `EDITABLE` 的字段被修改。
- 表单数据使用乐观版本控制；过期版本在审批和重提时都会返回 `FORM_DATA_VERSION_CONFLICT`。
- 字段变更账本记录实例发起、审批修改和退回重提等来源，并在 trace 中返回 `formDataChanges`。
- 退回发起人后的重提沿用同一 Hunyuan 实例版本链，不把旧版本数据静默覆盖为新实例事实。
- 最终回调使用冻结的 `finalFormDataJson` 与 `finalFormDataVersion`；样板费用单已回写最终核定金额和最终表单版本。
- `parallelAll` 并行全员会签节点不允许配置 `EDITABLE` 字段，避免多个成员并发修改同一审批数据。
- 2026-07-11 的验收记录见 `docs/superpowers/specs/2026-07-11-bpm-approval-data-governance-acceptance.md`。

## 当前优先级

顺序多人审批组、`parallelAll`、员工高级动作、审批数据治理、分支编译和时间事件运行时均作为可复用语义与验收资产。企业级后续方向已由以下文档固定，不再在每个小切片结束后重新排序：

- 当前总体设计与 M1-M8 任务基线：`docs/superpowers/specs/2026-07-11-bpm-enterprise-blueprint.md`
- 当前差距基线：`docs/superpowers/specs/2026-07-11-bpm-enterprise-gap-baseline.md`
- 模块设计：`2026-07-11-bpm-module-01-modeling-compiler-design.md`、`02-assignment-approval-strategy-design.md`、`03-approval-data-governance-design.md`、`04-core-runtime-workbench-design.md`、`05-advanced-runtime-design.md`、`06-configurable-business-integration-design.md`、`07-operations-governance-design.md`、`08-definition-evolution-migration-design.md`

M1“流程定义中心”、M2“身份组织与审批策略”和 M3“审批对象与数据治理”均已关闭，分别见 `docs/superpowers/specs/2026-07-11-bpm-m1-modeling-compiler-acceptance.md`、`docs/superpowers/specs/2026-07-13-bpm-m2-assignment-approval-strategy-acceptance.md` 和 `docs/superpowers/specs/2026-07-13-bpm-m3-approval-data-governance-acceptance.md`。下一交付块必须以总体蓝图和最新模块验收为准，不按模块编号机械推断；M1-M3 的正式版本目录与真实运行主链已经闭合，M4 仍是首个可交付产品基线的下一前置能力面。

M2“身份组织与审批策略”已关闭：策略目录、五种审批模式、自动终态、完整组织来源、高风险独立确认、受控转办、`INELIGIBLE` 恢复、幂等并发、重启恢复、跨节点连续审批、退回发起人和浏览器详情均形成真实证据，当前状态为 `RELEASABLE`，详见 `docs/superpowers/specs/2026-07-13-bpm-m2-assignment-approval-strategy-acceptance.md`。

## 平台能力边界

以下能力不是“参考仓库中有一个页面，所以补一个页面”就能完成的事项。实施时必须遵循总体蓝图和对应模块设计，保证 Graph、发布、运行投影、详情契约、测试和活体验收同时演进，不再为已确认范围新增第二套设计审批。

- 或签。
- 比例审批。
- Flowable multi-instance。
- 未经结构校验的任意 BPMN 网关、自由脚本和不可解释的动态拓扑。
- 绕过 M5/M6 登记连接器、签名和幂等边界的任意外部触发器。
- 子流程。
- 运行中动态增加、删除或替换会签成员。
- 表达式或脚本候选人。
- 未登记解析器的动态部门成员等候选人来源。
- 绕过 Hunyuan 结构化运行投影、直接解析 BPMN XML 或 Flowable ID 的动态图形状态。

判断原则：如果需求改变了“下一步由谁处理”“是否同时生成多个任务”“何时结束一个节点”或“可以走向哪个节点”，它至少涉及模型 DSL、编译器和运行时三个边界，不能只在前端补操作按钮。

## 参考仓库的使用规则

### Yudao 前端

- 可借用：运行时操作按钮分组、确认交互、表单展示和详情页的信息组织。
- 典型参考锚点：`src/views/bpm/processInstance/detail/ProcessInstanceOperationButton.vue`。
- 不可直接迁移：页面壳、路由组织、接口路径、权限码、依赖假设和业务命名。

### RuoYi 后端

- 可借用：节点类型、候选策略、字段权限等机制的建模和验证思路。
- 典型参考锚点：`BpmSimpleModelNodeTypeEnum`、`BpmTaskCandidateStrategyEnum`、`BpmFieldPermissionEnum`。
- 本地 checkout 中 BPM 模块默认未启用，`/admin-api/bpm/**` 也处于禁用边界；它只能作为源码机制参考，不能作为本项目已验证的运行能力证据。

### Hunyuan 不可突破的契约

- 所有生产代码、接口、菜单、权限、测试、文档和验收材料都在 `E:\my-project\hunyuan-pro` 内完成。
- 外部调用方看到的是 Hunyuan 的流程、实例、员工和组织契约，不是 Flowable 原生对象或 ID。
- 不整体迁移 Yudao/RuoYi 的类名、接口、数据库结构、模块边界或依赖。
- 每次借用参考实现前，先写出“借用的机制是什么”，然后实现最小 Hunyuan 原生版本。

## 源码导航

以下路径只表示当前实现和可复用断言的证据入口，不是新 M1-M8 的目标类名、数据模型或兼容清单。

| 关注点 | Hunyuan 源码锚点 |
| --- | --- |
| 旧设计器节点 DSL 证据 | `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts` |
| 旧模型校验与多人约束证据 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java` |
| 旧受限片段编译证据 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompiler.java` |
| 审批组投影与收敛 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmApprovalGroupService.java` |
| 员工端任务接口 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmTaskController.java` |
| 前端 BPM 运行时 API | `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts` |
| 我的待办及操作入口 | `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-todo-list.vue` |
| 实例详情、trace、通知与回调展示 | `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue` |
| P3 总验收 | `docs/superpowers/specs/2026-07-10-bpm-p3-acceptance.md` |
| 顺序审批验收 | `docs/superpowers/specs/2026-07-10-bpm-p3-sequential-approval-acceptance.md` |
| 并行全员会签验收 | `docs/superpowers/specs/2026-07-10-bpm-collaborative-approval-acceptance.md` |
| 顺序审批组闭环验收 | `docs/superpowers/specs/2026-07-11-bpm-sequential-approval-group-acceptance.md` |
| 审批数据治理验收 | `docs/superpowers/specs/2026-07-11-bpm-approval-data-governance-acceptance.md` |

## 验证基线

优先从当前改动相关的最小门禁开始，再扩大到模块级验证。

| 改动范围 | 首选验证 |
| --- | --- |
| 模型、候选人、编译、运行时后端 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=...' test` |
| Flowable 兼容层 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin '-Dtest=BpmFlowableCompatibilityTest' test` |
| 前端 BPM API/设计器/菜单契约 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run <相关测试文件> --dom` |
| 前端运行时规则或页面 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck` |
| 真实用户流转 | 服务可用时，复用持久 Playwright MCP 会话完成最小浏览器/API 验收 |

历史证据说明：

- 2026-07-10 的 P3 总验收记录了 `hunyuan-bpm` 全量 `171` 个测试、前端四文件 `51` 个契约测试、`@hunyuan/system` 类型检查，以及发起时自选审批人和顺序多人审批的活体验收。
- 2026-07-10 的并行全员会签验收记录了后端 `74` 个聚焦测试、Flowable 兼容测试、前端四文件 `57` 个契约测试、类型检查，以及 L1-L8 API/浏览器运行态证据。
- 2026-07-11 的顺序审批组闭环验收记录了后端 `75` 个聚焦测试、`hunyuan-bpm` 全量 `207` 个测试、Flowable 兼容测试、前端四文件 `57` 个契约测试、`@hunyuan/system` 类型检查，以及 S1-S11 的 API/Chrome/数据库三层运行态证据。
- 2026-07-11 的审批数据治理验收记录了节点字段权限冻结、隐藏字段过滤、可编辑补丁、版本冲突、退回重提版本连续性、字段变更账本、最终回调冻结和 `parallelAll + EDITABLE` 发布拒绝的 API/Chrome/数据库三层运行态证据。
- 这些结果是可追溯的历史证据，不代表后续任意工作树或任意提交仍然通过。每个新切片必须运行自己的相关门禁。

## 理解检查

开始实现前，负责人至少应能用自己的话回答：

1. 当前 `parallelAll` 为什么只是固定片段，而不是通用并行网关能力？
2. 顺序多人和并行全员会签为什么都不是 multi-instance？它们分别编译为什么拓扑？
3. 审批组、成员任务和 Flowable 引擎对象之间的边界是什么？
4. 委派、加签、减签、撤回在普通任务与会签成员上分别允许什么？
5. 当前需求是前端闭环、运行时机制扩展，还是流程建模平台扩展？
6. 如果要参考 Yudao/RuoYi，准备借用的是哪一项机制，Hunyuan 的最小原生契约是什么？

无法回答时，先从“源码导航”中的对应锚点和最近验收记录重新建立理解，再开始改动。
