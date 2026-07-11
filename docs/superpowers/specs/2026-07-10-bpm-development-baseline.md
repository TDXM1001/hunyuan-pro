# Hunyuan BPM 后续开发基线

- 状态：持续维护的开发基线
- 初始整理日期：2026-07-10
- 适用仓库：`E:\my-project\hunyuan-pro`

## 使用方式

开始任何 BPM/流程引擎改动前，先阅读本文件，再阅读与当前切片直接相关的最新验收记录。它记录的是 Hunyuan 当前已经闭环的能力、明确未闭环的前后端差异，以及不能被当作“小页面缺失”的平台能力边界。

本文件不是对 Yudao/RuoYi 的迁移清单。前端参考线为 `E:\my-project\huanyuan-pro-jichu\yudao-ui-admin-vue3-master`，后端参考线为 `E:\my-project\huanyuan-pro-jichu\ruoyi-vue-pro-master`；只有当当前需求跨越本文件列出的能力边界时，才重新分析参考实现中可借用的具体机制。

每关闭一个 BPM 切片时：

1. 新增该切片独立的设计或验收记录。
2. 更新本文件的“已完成能力”“当前优先级”或“平台边界”。
3. 将本次真实执行过的验证写清楚；历史验证不得改写为本次验证。

## 一句话结论

Hunyuan BPM 是一个以 Flowable `7.2.0` 为内部执行引擎的 **Hunyuan 原生企业审批引擎**。它已经覆盖线性审批和受限的 `parallelAll` 并行全员会签，以及流程定义治理、发起、待办审批、追踪、抄送、通知和回调等基础闭环；但它不是通用 BPMN 编排平台，也不支持通用网关 DSL、或签、比例审批、子流程、定时器或 Flowable multi-instance。

当前设计器仍只建模 `userTask`。编译器按 authored 节点顺序生成受限片段：

```text
单人：userTask
顺序多人：userTask_1 -> userTask_2 -> ...
parallelAll：parallelGateway split -> N 个 userTask -> parallelGateway join
```

只有 `parallelAll` 内部允许固定分叉和汇聚；条件路由、任意并行分支等需求仍然不是只改一个页面或一条接口的工作。

## 已完成能力

### 1. 定义与设计治理

- 流程分类、表单、模型/设计器、定义发布、版本管理、发起范围、挂起/恢复已经形成管理端能力。
- 流程表单与流程模型设计器均作为隐藏业务路由保留在 `BasicLayout` 内，并通过 `activePath` 维持“流程表单”或“流程模型”的菜单上下文；表单使用全高三栏工作区，模型使用摘要、流程设计、运行规则和发布检查工作区。
- 发布前会校验模型、发起规则、分类、表单、字段一致性与候选人预检。
- 发布读取单一模型快照，并在部署前按 `model_id + update_time` 认领；模型被并发修改时拒绝部署，避免发布旧快照。
- 设计器发布前检查内部脏状态和运行规则草稿快照，避免“修改运行规则但未保存，仍然发布”的问题。
- 定义的当前版本和可发起版本保持一致；处理版本问题时应沿真实发布、定义查询和发起列表链路验证，而不是只调用准备接口。

### 2. 设计器与候选人规则

- 当前节点类型只有 `userTask`。
- 已支持六类候选人策略：
  - `EMPLOYEE`：指定员工。
  - `DEPARTMENT_MANAGER`：部门主管。
  - `ROLE`：角色成员。
  - `START_EMPLOYEE`：发起人本人。
  - `START_DEPARTMENT_MANAGER`：发起人部门主管。
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
- 编译快照保留原始节点及顺序元数据；当前公共任务契约通过展开后的 `taskKey` 和任务名中的 `（序号/总数）` 表达顺序，而不是返回结构化审批组。

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

会签成员可转办、委派和实例级撤回；加签、减签被后端硬拒绝且前端不展示。普通任务继续按原有规则使用加签、减签。

## 当前优先级

`parallelAll` 和员工高级动作已经闭环。下一切片必须重新从当前仓库事实和真实业务需求排序，不自动延伸为通用网关平台。

仍可独立评估的工作：

- 对顺序多人审批提供面向前端的结构化 authored 审批组、组级进度和聚合展示。
- 根据真实业务流程补充更丰富的表单与业务单据集成；继续保持业务单据在 Hunyuan 业务模块中拥有生命周期，BPM 只管理审批过程。

## 平台能力边界

以下能力不是“参考仓库中有一个页面，所以补一个页面”就能完成的事项。若要进入其中任一项，先写设计记录，明确 DSL、编译器、运行时投影、详情契约、测试和活体验收如何同时演进。

- 或签。
- 比例审批。
- Flowable multi-instance。
- 通用并行网关、条件网关、包容网关和任意分支路由。
- 定时器、触发器和超时流转。
- 子流程。
- 运行中动态增加、删除或替换会签成员。
- 表达式或脚本候选人。
- 岗位、用户组、动态部门成员等候选人来源。
- 逐节点表单字段权限。
- 图形化运行态高亮和复杂节点进度展示。

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

| 关注点 | Hunyuan 源码锚点 |
| --- | --- |
| 设计器节点 DSL | `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts` |
| 模型合法性与多人审批约束 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java` |
| 受限片段 BPMN 编译 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompiler.java` |
| 审批组投影与收敛 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmApprovalGroupService.java` |
| 员工端任务接口 | `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmTaskController.java` |
| 前端 BPM 运行时 API | `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts` |
| 我的待办及操作入口 | `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-todo-list.vue` |
| 实例详情、trace、通知与回调展示 | `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue` |
| P3 总验收 | `docs/superpowers/specs/2026-07-10-bpm-p3-acceptance.md` |
| 顺序审批验收 | `docs/superpowers/specs/2026-07-10-bpm-p3-sequential-approval-acceptance.md` |
| 并行全员会签验收 | `docs/superpowers/specs/2026-07-10-bpm-collaborative-approval-acceptance.md` |

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
