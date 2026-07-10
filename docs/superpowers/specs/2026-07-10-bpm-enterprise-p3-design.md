# BPM P3 快速推进总设计

## 结论

P3 不做“更大的流程引擎目录”，而做 **候选人与审批语义产品化 + 活体证明 + 最小运行护栏**。

按 Karpathy methodology，这一阶段的核心不是堆功能，而是确认我们真的理解当前系统：P2 已经证明 Hunyuan BPM 能发起、审批、记录通知、记录回调、失败重试和业务回写；P3 要把“谁来审批、发起时怎么选择、出错时怎么解释”做成真实业务可用的能力。

一句话路线：

1. 先把 P3.1 已有候选策略跑成真实活体闭环。
2. 再补发起/发布前的最小一致性校验。
3. 再做设计器可解释性和候选预检。
4. 最后只做一类低风险多人语义：顺序多人审批。

复杂会签、或签、比例通过、并行多实例、表达式审批、脚本审批、子流程和网关分支不进入 P3。

## 当前证据

- P2 收官活体验收已通过，覆盖样板费用申请创建、BPM 实例发起、待办审批、回调失败可见、手动重试恢复、业务状态回写，以及 trace 中回调、命令、动作、通知数据。
- P2.3 回调执行器已经从“可查 + 手动计数”推进为可执行、可失败、可自动重试、可人工补偿。
- P2.4 样板费用申请已经证明业务单据可以通过 `BpmBusinessProcessApi.start` 发起流程，并由回调执行器回写业务状态。
- P3.1a/b/c 已经在源级落地：
  - `START_EMPLOYEE`
  - `START_DEPARTMENT_MANAGER`
  - `EMPLOYEE_SELECT_AT_START`
  - 表单 schema 字段选择
  - 运行时员工单选字段归一化
- 当前 `SimpleModelValidator` 仍只接受 `single` / `singleOnly` 审批模式，并明确返回 `P0 只支持单人审批`。
- 当前 `SimpleModelBpmnCompiler` 仍把 simple model 编译成顺序 user task，使用 `flowable:assignee="${assignee_<nodeKey>}"`，这说明 P3 要尊重“单任务单处理人”的现有内核边界。
- 当前 `BpmTaskAssignmentResolver` 已经通过 `BpmTaskAssignmentContext` 同时拿到发起人快照和 `formDataJson`，这是 P3 继续做候选策略的正确接入点。

## P3 总目标

P3 完成后，Hunyuan BPM 应能支撑常见企业审批配置中的“单人审批主线”：

- 发起人本人审批。
- 发起人部门主管审批。
- 发起时由发起人选择一个真实员工审批。
- 指定员工、部门主管、角色成员继续可用。
- 发布前能发现明显错误配置。
- 发起时能发现员工字段缺失、员工不存在或解析无效。
- 管理员能通过现有 trace / 集成监控 / 实例详情理解审批人来源。
- 至少一条真实浏览器/API 活体链路证明 P3 策略可用。

P3 不追求“万能工作流”，追求“当前平台上能稳定解释、稳定验收、稳定扩展”。

## 设计原则

### 理解优先

不从参考项目照搬候选枚举、会签模型或 Flowable 多实例配置。每个能力必须回答：

- 在 Hunyuan simple model 中怎么表达？
- 发布校验怎么失败？
- Flowable 启动变量怎么生成？
- Hunyuan `t_bpm_task` 投影如何保持一致？
- 管理员和员工端看到的语义是什么？
- 哪个测试或活体验收能证明它成立？

### 快进但不跳层

P3 的速度来自切片小，不来自跳过证据。每个切片必须有最少一个源级门禁；触及真实页面或业务流时，补活体验收记录。

### 保持单人内核

P3 默认仍以“一个 Flowable user task 对应一个 Hunyuan 当前处理人”为内核。多人语义如果进入 P3，只允许做“顺序多人审批”，即在编译阶段展开成多个连续单人节点；不做并行会签或比例通过。

## P3 切片

### P3.1 候选策略活体闭环

状态：源级已完成，缺一条活体闭环验收。

目标：证明 `EMPLOYEE_SELECT_AT_START` 在真实前后端中可用。

范围：

- 准备一个包含 `employeeSelect` 字段的 BPM 表单。
- 设计并发布一个单节点流程，节点候选策略为 `EMPLOYEE_SELECT_AT_START`。
- 员工端发起时选择真实审批员工。
- 验证待办落到被选择员工。
- 审批后验证实例详情、动作日志、trace 不破坏。
- 写入 `docs/superpowers/specs/2026-07-10-bpm-p3-candidate-live-acceptance.md`。

不包含：

- 不新增生产业务模块。
- 不新增菜单。
- 不做多人审批。

建议门禁：

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,SimpleModelValidatorTest,BpmRuntimeStartAssignmentTest' test
```

活体证据仍写到：

```text
G:/code-mcp/playwright-mcp-temp/runtime
```

### P3.2 发起时员工存在性与配置一致性护栏

目标：把 P3.1c 的边界从“前端尽量保证字段正确”推进到“后端能给出明确失败”。

范围：

- `EMPLOYEE_SELECT_AT_START` 解析到员工 ID 后，调用 `BpmOrgIdentityGateway.requireEmployee(employeeId)` 确认员工存在。
- 发起和重提都使用同一套校验。
- 对不存在员工、空字段、数组、逗号字符串保留明确错误。
- 发布前校验 `employeeSelectFieldKey` 至少配置且字段名合法；如果当前发布链路能拿到表单 schema，则校验字段存在且是员工选择字段。
- 前端设计器继续以 schema 字段选择为主，不退回手输。

可选增强：

- 新增 `BpmSimpleModelPublishValidator`，在 `BpmDefinitionService.publish` 里组合 simple model、start rule、form schema 做发布期一致性校验。
- 保持 `SimpleModelValidator` 兼容旧签名，避免一次性改动过大。

不包含：

- 不校验员工是否有发起人可见权限。
- 不建设表单字段权限系统。
- 不新增组织接口，优先复用 `BpmOrgIdentityGateway.requireEmployee`。

完成定义：

- 后端测试覆盖不存在员工被拒绝。
- 后端测试覆盖重提时重新按新表单数据解析审批人。
- 前端合同测试保持通过。
- 不新增 SQL，不新增依赖。

### P3.3 候选策略预检与解释

目标：管理员发布前能看懂“这个节点最终会找谁”，减少运行时才失败。

范围：

- 增加一个后端预检服务，输入 simple model、表单 schema、可选模拟发起人、可选模拟 formData。
- 输出每个节点的候选策略摘要：
  - 节点名
  - 策略类型
  - 需要的字段或配置
  - 可解析 / 需运行时提供 / 当前不可解析
  - 失败原因
- 前端设计器在节点属性区域或发布前确认里展示轻量预检结果。
- 预检只走 Hunyuan 网关，不直接访问 Flowable。

不包含：

- 不做可视化大屏。
- 不做复杂调试器。
- 不在 P3 做 BPMN 图高亮。

完成定义：

- `EMPLOYEE`、`ROLE`、`DEPARTMENT_MANAGER`、`START_EMPLOYEE`、`START_DEPARTMENT_MANAGER`、`EMPLOYEE_SELECT_AT_START` 都有预检摘要。
- 预检失败不一定阻止保存草稿，但发布必须按 P3.2 规则阻止明显错误。
- 前端合同测试锁定预检 API 和展示入口。

### P3.4 顺序多人审批

目标：只解决最常见、风险最低的多人场景：一个节点配置多个审批人，按顺序逐个审批。

设计取舍：

- 采用编译期展开，而不是 Flowable 多实例。
- 一个 simple model 节点：

```json
{
  "nodeKey": "finance_review",
  "type": "userTask",
  "approvalMode": "sequential",
  "employeeIds": [101, 102, 103]
}
```

- 编译后展开为连续 user task：
  - `finance_review_1`
  - `finance_review_2`
  - `finance_review_3`
- 每个展开节点仍走现有 `assignee_<nodeKey>` 变量和现有任务投影。
- 实例详情中保留原始节点名，并展示顺序序号。

为什么这样最快：

- 不需要引入 Flowable multi-instance。
- 不改变当前 `t_bpm_task` 单处理人投影模型。
- 不改变审批、驳回、退回、转办等现有动作的主语义。
- 可以用现有顺序 BPMN 编译器扩展完成。

边界：

- 不支持并行会签。
- 不支持比例通过。
- 不支持任意一个人通过即通过。
- 不支持动态增减审批人改变后续展开节点。
- 不支持角色多人自动展开；P3.4 只做显式员工列表。

完成定义：

- 发布校验接受 `approvalMode = sequential` 且 `employeeIds` 至少 2 人。
- 编译器把顺序多人节点展开成多个连续 user task。
- 运行时任务逐个出现。
- 实例详情能看懂这是一组顺序审批。
- 后端模块门禁、前端合同、至少一条 API 活体验收通过。

### P3.5 P3 收官验收

目标：把 P3 作为一个可交付版本收口。

范围：

- 源级门禁复跑：
  - BPM 模块测试。
  - Flowable 边界门禁。
  - 前端 BPM 合同测试。
  - 前端类型检查。
- 活体验收至少覆盖：
  - `EMPLOYEE_SELECT_AT_START` 单人选择。
  - `START_DEPARTMENT_MANAGER` 或 `START_EMPLOYEE` 真实待办。
  - 如果 P3.4 实施，则覆盖顺序多人审批。
- 写入 `docs/superpowers/specs/2026-07-xx-bpm-p3-acceptance.md`。

## 快速推进顺序

建议按这个顺序推进，最快也最稳：

1. **P3.1 live acceptance**：不写大功能，先证明已有 P3.1 能跑。
2. **P3.2 后端护栏**：补员工存在性与发布期一致性，减少未来活体失败。
3. **P3.3 预检解释**：让设计器变得可用，不靠猜。
4. **P3.4 顺序多人审批**：只在前三步稳定后做。
5. **P3.5 收官验收**：整理 P3 证据，作为下一阶段入口。

如果目标是“进度快一些”，不要从 P3.4 开始。多人审批看起来更像大功能，但真正最快的路径是先把 P3.1 的真实证据补齐，再给它加护栏。

## P3 之后再考虑的能力

这些能力不进入 P3，避免拖慢当前阶段：

- 并行会签。
- 或签。
- 比例审批。
- Flowable multi-instance。
- 条件网关和分支建模。
- 子流程。
- 脚本表达式审批人。
- 岗位、用户组、动态部门成员。
- 表单字段权限系统。
- 通用审批规则 DSL。

这些可以作为 P4 或 P5，因为它们会改变模型、编译器、运行投影、前端交互和验收方式，不适合混进 P3。

## 验收矩阵

| 切片 | 后端门禁 | 前端门禁 | 活体验收 | SQL | 依赖 |
| --- | --- | --- | --- | --- | --- |
| P3.1 | 候选解析/启动变量测试 | 设计器/运行表单合同 | 必须 | 否 | 否 |
| P3.2 | 员工存在性/发布一致性测试 | 设计器合同 | 可选 | 否 | 否 |
| P3.3 | 预检服务测试 | 预检展示合同 | 可选 | 否 | 否 |
| P3.4 | 编译器/启动/任务投影测试 | 设计器合同 | 必须 | 否 | 否 |
| P3.5 | 全量目标门禁 | 全量目标门禁 | 必须 | 否 | 否 |

## 人工审阅重点

- 是否接受 P3 不做完整会签，只做顺序多人审批。
- 是否接受 P3.1 先补活体验收，而不是继续写新功能。
- 是否接受 P3.2 开始做后端字段/员工存在性护栏。
- 是否接受 P3.3 做轻量预检解释，而不是复杂流程调试器。
- 是否接受 P3 所有切片默认不新增 SQL、不新增依赖。

## 最短下一步

下一张卡应是：

```text
BPM P3.1 live acceptance: EMPLOYEE_SELECT_AT_START 真实发起闭环
```

完成它后再做 P3.2。这样推进速度最快，因为它直接复用已经实现的 P3.1a/b/c，不先开启新的复杂实现面。
