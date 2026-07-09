# BPM 样板费用流程定义初始化设计

## 结论

本切片采用 **受控样板流程定义 seed + 复用现有发布链路**。

当前 P2 收官活体验收阻塞在本地运行库缺少 `sample_expense_apply` 流程定义。该定义不是普通业务字典数据：真实发起依赖 Hunyuan 流程定义快照、流程节点快照和 Flowable 引擎部署同时存在。因此本轮不直接向 `t_bpm_definition` 插入 SQL，也不把源级门禁当作活体验收替代证据。

一句话目标：提供一个幂等的、Hunyuan 原生的样板定义准备能力，让 P2 样板费用申请能在真实运行环境中发起、审批、产生回调失败记录并手动重试恢复。

## 当前证据

- P2 live acceptance 记录显示，管理员登录成功后查询 `definitionKey = sample_expense_apply` 返回 `total = 0`，活体链路停止在定义查询阶段。
- `BpmSampleExpenseService` 发起样板费用申请时固定使用 `definitionKey = "sample_expense_apply"`。
- `数据库SQL脚本/mysql/sql-update-log/v3.42.0.sql` 只创建 `t_bpm_sample_expense`，没有初始化样板流程定义。
- `BpmDefinitionService.publish` 会校验模型、生成 BPMN XML、调用 `FlowableProcessDefinitionGateway.deploy`、写入 `t_bpm_definition` 和 `t_bpm_definition_node`，并历史化旧版本。
- `SimpleModelBpmnCompiler` 会把 `userTask` 编译为 Flowable user task，并使用 `assignee_{nodeKey}` 变量。
- `BpmTaskAssignmentResolver` 已支持 `candidateResolverType = EMPLOYEE` 和显式 `employeeId`，适合构造一个最小单人审批样板。

## 方案取舍

### 方案 A：SQL 直插定义表

直接插入 `t_bpm_category`、`t_bpm_form`、`t_bpm_model`、`t_bpm_definition` 和 `t_bpm_definition_node`。

优点是表面上简单；缺点是无法保证 Flowable 引擎存在对应部署，真实发起很可能在 `engineProcessDefinitionId` 处失败。该方案绕过了 Hunyuan 的发布校验、BPMN 编译和版本治理，不采用。

### 方案 B：受控 seed 复用发布链路（采用）

新增一个小的样板定义准备服务或管理接口：缺失 `sample_expense_apply` 时，创建样板分类、表单、模型草稿，并调用现有 `BpmDefinitionService.publish` 发布。

优点是同时获得 Hunyuan 定义快照和 Flowable 部署，最贴近真实业务发起路径；缺点是需要明确触发方式、幂等规则和审批人选择。

### 方案 C：只写环境准备说明，人工在页面创建模型并发布

优点是代码最少；缺点是不能解除自动活体验收阻塞，每台环境都需要手工点页面，且容易出现模型草稿、审批人和 Flowable 部署不一致。该方案仅可作为应急旁路，不作为本切片设计。

## 范围

### 本轮包含

- 新增一个最小样板定义 seed 能力，用于准备 `sample_expense_apply`。
- seed 只在显式调用时执行，不在应用启动时自动改库。
- seed 幂等：如果当前可发起定义已经存在，则直接返回现有定义，不覆盖或重新发布。
- 缺失时创建或复用样板分类、样板表单和样板模型。
- 模型草稿只包含一个审批节点，使用显式员工审批。
- 发布必须通过 `BpmDefinitionService.publish`，保证 Hunyuan 表与 Flowable 部署一致。
- 增加后端测试覆盖缺失创建、已存在不重复、发布链路参数和定义 key。
- 活体验收解除阻塞后，继续执行 P2 收官链路。

### 本轮不包含

- 不建设通用流程模板市场。
- 不做启动自动初始化或生产环境静默补数据。
- 不直接操作 Flowable 表或暴露 Flowable 原生对象。
- 不新增前端样板页面、菜单或复杂向导。
- 不扩大成费用报销业务模块。
- 不改变 `BpmSampleExpenseService` 的业务合同。

## 触发方式

seed 通过管理端受控接口触发，路径建议为：

- `POST /bpm/sample/expense/prepareDefinition`

权限沿用样板验收接口的更新权限：

- `bpm:integration:update`

选择接口触发而不是启动自动执行，是为了保持环境变更可见、可审计、可由验收脚本显式调用。后续 Playwright/API 活体验收可以先调用该接口准备定义，再执行创建样板费用申请、失败注入、发起、审批和回调重试。

## 后端设计

### Seed Service

新增 `BpmSampleExpenseDefinitionSeedService`，放在 `com.hunyuan.sa.bpm.module.sampleexpense.service`。

核心方法：

- `prepare()`：准备样板流程定义，返回当前可发起定义 ID。

职责边界：

- 查询 `BpmDefinitionDao.selectCurrentByDefinitionKey("sample_expense_apply")`。
- 当前定义存在且可发起时直接返回定义 ID。
- 确保存在样板分类、样板表单和样板模型。
- 写入模型草稿所需的 `simpleModelJson`、`startRuleJson`、标题规则和摘要规则。
- 调用 `BpmDefinitionService.publish` 完成正式发布。
- 不直接写 `t_bpm_definition` 或 Flowable 表。

### 样板元数据

固定编码：

- 分类编码：`bpm_sample`
- 分类名称：`BPM验收样板`
- 表单编码：`sample_expense_form`
- 表单名称：`样板费用申请表单`
- 模型编码：`sample_expense_apply`
- 模型名称：`样板费用申请`

表单 schema 保持最小：

```json
{
  "fields": [
    { "field": "expenseId", "label": "样板费用申请ID", "type": "number" },
    { "field": "amount", "label": "申请金额", "type": "number" }
  ]
}
```

流程草稿保持单节点：

```json
{
  "nodes": [
    {
      "nodeKey": "sample_approve",
      "type": "userTask",
      "name": "样板审批",
      "approvalMode": "single",
      "candidateResolverType": "EMPLOYEE",
      "employeeId": 1
    }
  ]
}
```

`employeeId = 1` 来自当前活体验收中已确认的管理员员工 ID。后续如需要多账号审批，可把审批员工 ID 提升为接口参数；本切片先保持固定值，避免把 seed 扩展成配置中心。

### Controller

在 `AdminBpmSampleExpenseController` 增加：

- `POST /bpm/sample/expense/prepareDefinition`

返回 `ResponseDTO<Long>`，`data` 为当前可发起定义 ID。

该接口只用于验收样板和本地/测试环境准备，不作为通用模型发布入口。

## 幂等与错误处理

- 如果当前定义存在、生命周期为当前版本且发起状态可发起，直接返回。
- 如果模型已存在但没有当前可发起定义，更新样板草稿后发布一个新版本。
- 如果分类或表单已存在，则复用并修正必要的名称、禁用标记和删除标记。
- 如果发布校验失败，返回明确错误，不吞掉 `BpmDefinitionService.publish` 的失败信息。
- 如果发布人员快照无法解析，保留现有发布链路错误，避免 seed 伪造发布人。
- seed 不删除历史定义，不修改非样板编码的数据。

## 数据流

1. 验收脚本或管理员调用 `POST /bpm/sample/expense/prepareDefinition`。
2. seed 查询 `sample_expense_apply` 当前可发起定义。
3. 如果存在，返回现有定义 ID。
4. 如果不存在，准备分类、表单和模型草稿。
5. seed 调用 `BpmDefinitionService.publish`。
6. 发布服务编译 BPMN、部署 Flowable、写定义快照和节点快照。
7. seed 返回新定义 ID。
8. P2 live acceptance 继续调用样板费用申请 API 发起真实实例。

## 测试策略

后端新增或扩展测试：

- `BpmSampleExpenseDefinitionSeedServiceTest`
  - 当前定义存在时直接返回，不创建模型，不调用发布。
  - 定义缺失时创建或复用分类、表单、模型，并调用发布。
  - 生成的模型 key 等于 `sample_expense_apply`。
  - 生成的 simple model 包含 `candidateResolverType = EMPLOYEE` 和 `employeeId = 1`。
  - 发布失败时返回失败响应，不伪造 definitionId。
- `AdminBpmSampleExpenseControllerTest` 如仓库已有 controller 单测模式，则覆盖接口委托；没有模式时由 service 测试和 API 合同测试兜底。
- `bpm-api.test.ts` 增加 `prepareBpmSampleExpenseDefinition` 路径合同。

建议门禁：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmSampleExpenseDefinitionSeedServiceTest,BpmSampleExpenseServiceTest,BpmSampleExpenseCallbackHandlerTest,BpmBusinessCallbackExecutorTest' test
```

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test
```

## 活体验收调整

解除 `BLOCKED_DEFINITION_MISSING` 后，P2 收官活体验收前置步骤增加：

1. 登录管理员。
2. 调用 `POST /bpm/sample/expense/prepareDefinition`。
3. 查询 `POST /bpm/definition/query`，确认 `definitionKey = sample_expense_apply` 返回当前可发起定义。
4. 继续执行样板费用申请创建、失败注入、发起、审批、失败回调查询、手动重试、样板详情查询和可靠性区域检查。

runtime 证据继续写入 `G:\code-mcp\playwright-mcp-temp\runtime`，不提交到仓库。

## 完成定义

- `sample_expense_apply` 可以通过显式 seed 准备出来。
- seed 准备出的定义能通过现有定义查询接口查到。
- 真实发起时 Flowable 引擎有对应部署，不依赖 SQL 伪造定义。
- 已存在当前可发起定义时重复 seed 不产生新版本。
- 后端和前端合同测试通过。
- P2 live acceptance 能越过定义缺失阶段并继续收集真实实例、回调和可靠性页面证据。

## 理解校验

关键假设：

- 当前活体验收使用管理员 `employeeId = 1`，单节点审批人固定为 1 能形成最小可跑闭环。
- `BpmDefinitionService.publish` 是当前唯一可信的 Hunyuan 定义发布入口。
- seed 应该显式触发，避免启动时自动改库造成环境不可解释。

已验证依据：

- P2 live acceptance 记录明确阻塞于 `sample_expense_apply` 缺失。
- 发布服务源码显示发布链路同时负责 BPMN 编译、Flowable 部署、定义快照和节点快照。
- 分配器测试与源码显示显式员工审批可解析为 Flowable assignee 变量。

仍需实施时验证：

- service 测试需要证明重复调用不重新发布。
- 活体环境中管理员员工 ID 是否仍为 1；若不是，seed 需要在验收前失败并给出明确错误，而不是发布不可审批流程。
- 真实审批完成后，P2.3 回调失败和手动重试仍按既有可靠性区域展示。

人工必须重点审阅：

- 固定 `employeeId = 1` 是否满足当前验收环境；如希望支持其他审批人，应在下一轮把审批人做成接口参数。
- seed 接口是否只应保留在样板 controller；若后续进入生产发布，应移到更明确的运维/初始化边界。
