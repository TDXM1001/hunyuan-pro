# BPM 顺序多人审批组投影与异常路径闭环验收记录

- 日期：2026-07-11
- 结论：顺序多人审批组的结构化投影、异常路径闭环、前端展示与 `parallelAll` / 普通任务回归已通过自动化门禁和本地 Chrome 活体验收。

## 交付范围

- 顺序多人审批编译快照补齐 `approvalGroupKey`、`approvalGroupName`、顺序索引与总数。
- 运行时为顺序多人审批持久化/恢复结构化审批组，并在实例详情、任务详情、trace、待办列表中透出组级进度。
- 顺序审批的通过、拒绝、退回、重提、转办、委派、加签、减签按组语义闭环。
- `parallelAll` 继续保持“加签/减签隐藏且后端拒绝”的约束。
- 普通单人任务继续保持 `approvalGroup=null`，退回发起人仍进入 `WAIT_RESUBMIT`。

## 自动化门禁

### 后端聚焦门禁

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=SimpleModelBpmnCompilerTest,SimpleModelValidatorTest,BpmApprovalGroupServiceTest,BpmTaskProjectionServiceTest,BpmTaskServiceTest,BpmRuntimeCommandServiceTest,BpmTaskAdvancedActionServiceTest,BpmRuntimeDetailServiceTest,BpmInstanceTraceServiceTest' test
```

结果：`Tests run: 75, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

### BPM 模块全量门禁

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

结果：`Tests run: 207, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

### Flowable 兼容门禁

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin '-Dtest=BpmFlowableCompatibilityTest' test
```

结果：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

### 前端合同与类型门禁

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts --dom
```

结果：`Test Files: 4 passed`，`Tests: 57 passed`。

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

结果：退出码 `0`，无类型错误。

## 真实验收对象

- 活体验收窗口：`2026-07-11T06:28:57.623Z` 至 `2026-07-11T06:29:01.677Z`
- 活体验收总证据：
  - `G:/code-mcp/playwright-mcp-temp/runtime/bpm-sequential-approval-group-acceptance-20260711.json`
- Chrome 页面证据：
  - 顺序详情抽屉：`G:/code-mcp/playwright-mcp-temp/runtime/bpm-sequential-detail-20260711.png`
  - 顺序高级菜单：`G:/code-mcp/playwright-mcp-temp/runtime/bpm-sequential-menu-visible-20260711.png`
  - 并行待办页：`G:/code-mcp/playwright-mcp-temp/runtime/bpm-parallel-page-20260711.png`
  - 并行高级菜单：`G:/code-mcp/playwright-mcp-temp/runtime/bpm-parallel-menu-visible-20260711.png`
- Chrome 会话可见结论：
  - 顺序审批详情抽屉显示 `顺序审批` 与 `后续 2 人待激活`
  - 顺序高级菜单显示 `转办 / 委派 / 加签 / 减签 / 撤回`
  - 并行高级菜单仅显示 `转办 / 委派 / 撤回`

## S1-S11 运行态证据

### S1-S4：顺序三人审批依次推进并完成

- 顺序定义：`definitionId=12`
- `S1`：
  - `instanceId=72`
  - `expenseId=12`
  - `instanceNo=DK20260711NO01070`
  - `taskId=97`
  - `approvalGroupId=17`
  - `engineProcessInstanceId=cc0f67b9-7cf1-11f1-a212-a8e2913e212c`
  - 组状态：`PENDING`
  - 进度：`0/3`
  - 当前待办数：管理员 `1`，胡克 `0`，卓大 `0`
- `S2`：
  - 管理员通过 `taskId=97`
  - 第二成员任务 `taskId=98`
  - 当前处理人切换为胡克
  - 组进度：`1/3`
- `S3`：
  - 胡克通过 `taskId=98`
  - 第三成员任务 `taskId=99`
  - 当前处理人切换为卓大
  - 组进度：`2/3`
- `S4`：
  - 卓大通过 `taskId=99`
  - 组状态：`APPROVED / ALL_APPROVED`
  - 进度：`3/3`
  - 实例状态：`runState=3`，`resultState=1`
  - 当前任务数：`0`
  - 三名审批人已办数：管理员 `1`，胡克 `1`，卓大 `1`

### S5：第二成员拒绝关闭整组

- `instanceId=73`
- `instanceNo=DK20260711NO01080`
- `approvalGroupId=18`
- `engineProcessInstanceId=cc7f1895-7cf1-11f1-a212-a8e2913e212c`
- 第一成员管理员先通过 `taskId=100`
- 第二成员胡克拒绝 `taskId=101`
- 组状态：`REJECTED / MEMBER_REJECTED`
- 组进度：`processed=2`，`approved=1`，`rejected=1`
- 实例状态：`runState=3`，`resultState=2`
- 当前任务数：`0`
- 数据库中该引擎实例待处理任务数：`0`

### S6：退回发起人会先取消旧引擎，再进入 WAIT_RESUBMIT

- `S6-1 第一成员退回`
  - `instanceId=74`
  - `instanceNo=DK20260711NO01085`
  - `taskId=102`
  - `approvalGroupId=19`
  - `engineProcessInstanceId=ccc633ca-7cf1-11f1-a212-a8e2913e212c`
  - 组状态：`RETURNED / MEMBER_RETURNED`
  - 组进度：`1/3`
  - 实例状态：`runState=2`，`resultState=null`
  - 当前任务数：`0`
  - 数据库中该引擎实例待处理任务数：`0`
- `S6-2 第二成员退回`
  - `instanceId=75`
  - `instanceNo=DK20260711NO01087`
  - 第一成员管理员先通过 `taskId=103`
  - 第二成员胡克退回 `taskId=104`
  - 旧组 `approvalGroupId=20`
  - 旧引擎 `engineProcessInstanceId=ccfab15a-7cf1-11f1-a212-a8e2913e212c`
  - 组状态：`RETURNED / MEMBER_RETURNED`
  - 组进度：`processed=2`，`approved=1`
  - 实例状态：`runState=2`，`resultState=null`
  - 当前任务数：`0`
  - 数据库中旧引擎待处理任务数：`0`

### S7：重提生成新引擎与新审批组，旧组保留历史事实

- 同一 `instanceId=75` 执行重提
- 旧组：`approvalGroupId=20`
- 新组：`approvalGroupId=21`
- 新引擎：`engineProcessInstanceId=cd3ac7af-7cf1-11f1-a212-a8e2913e212c`
- 旧引擎：`engineProcessInstanceId=ccfab15a-7cf1-11f1-a212-a8e2913e212c`
- 旧组保持 `RETURNED / MEMBER_RETURNED`
- 新组状态：`PENDING`
- 新待办：管理员 `taskId=105`
- 重提后实例状态：`runState=1`，`resultState=null`
- 数据库中新引擎待处理任务数：`1`

### S8：转办与委派保持同一任务与审批组身份

- `instanceId=76`
- `instanceNo=DK20260711NO01097`
- `approvalGroupId=22`
- `engineProcessInstanceId=cd54914f-7cf1-11f1-a212-a8e2913e212c`
- 原任务 `taskId=106`
- 管理员将 `taskId=106` 转办给琴酒，再由琴酒委派给善逸
- 转办后任务仍为 `taskId=106`
- 委派后任务仍为 `taskId=106`
- `taskKey=finance_review_1`
- 组模式：`sequential`
- 组状态：`PENDING`
- 成员索引与总数保持 `1/3`
- 当前处理人快照变为善逸

### S9：顺序审批允许加签/减签，且不计入 authored 组进度

- `instanceId=77`
- `instanceNo=DK20260711NO01099`
- `approvalGroupId=23`
- `engineProcessInstanceId=cd8fecb5-7cf1-11f1-a212-a8e2913e212c`
- 原任务 `taskId=107`
- 加签子任务 `taskId=108`
- 加签后：
  - 当前任务数：`2`
  - 组状态仍为 `PENDING`
  - 组进度仍为 `0/3`
  - 子任务 `approvalGroup=null`
  - 子任务 `runtimeAssignmentSnapshotJson={"addSign":true,"sourceTaskId":107,"assigneeEmployeeId":48}`
- 减签对 `taskId=108` 执行后：
  - 当前任务数回落为 `1`
  - 组状态仍为 `PENDING`
  - 组进度仍为 `0/3`
  - 琴酒对该实例的待办数回落为 `0`

### S10：parallelAll 继续隐藏并拒绝加签/减签，并正常收敛

- 并行定义：`definitionId=13`
- `instanceId=78`
- `instanceNo=DK20260711NO01102`
- `approvalGroupId=24`
- `engineProcessInstanceId=cdcfdbf8-7cf1-11f1-a212-a8e2913e212c`
- 初始成员任务：
  - 管理员 `taskId=109`
  - 胡克 `taskId=110`
  - 卓大 `taskId=111`
- 初始组状态：`PENDING`
- 初始进度：`0/3`
- 并行成员调用加签/减签均返回：
  - `code=30001`
  - `msg=并行全员会签成员不支持加签或减签`
- 三名成员依次通过后：
  - 组状态：`APPROVED / ALL_APPROVED`
  - 进度：`3/3`
  - 实例状态：`runState=3`，`resultState=1`
  - 当前任务数：`0`

### S11：普通单人任务保持 approvalGroup=null 与既有退回语义

- 单人定义：`definitionId=14`
- `instanceId=79`
- `instanceNo=DK20260711NO01105`
- `taskId=112`
- `engineProcessInstanceId=ce3c80a1-7cf1-11f1-a212-a8e2913e212c`
- 任务详情：
  - `taskKey=sample_approve`
  - `taskName=样板审批`
  - `approvalGroup=null`
- 退回发起人后：
  - 实例状态：`runState=2`，`resultState=null`
  - `approvalGroups=[]`
  - 当前任务数：`0`
  - 管理员待办数：`0`

## 普通任务与 parallelAll 回归

- 普通单人任务没有被错误包装成审批组，任务详情与实例详情都保持 `approvalGroup=null / approvalGroups=[]`。
- `parallelAll` 的拒绝与通过收敛语义保持不变，加签/减签依然由 UI 隐藏、后端拒绝双保险控制。
- 顺序审批组的转办、委派、加签、减签均不会改写 authored 成员总数，也不会把加签子任务误计入组进度。

## 已知边界

- 顺序审批未来成员只显示数量，不创建占位任务或成员计划表；Chrome 详情抽屉显示 `后续 2 人待激活`，不展示虚拟成员行。
- 本次没有批量回填历史已结束顺序实例；恢复逻辑只覆盖当前运行中的实例和当前引擎实例范围。
- 并行高级菜单的 Chrome 截图在当前窗口尺寸下未稳定渲染出下拉层，但同一会话的可见 DOM 已明确只暴露 `转办 / 委派 / 撤回` 三项，且不包含 `加签 / 减签`。

## 结论

- 顺序多人审批组现在已经具备稳定组标识、结构化进度、异常路径闭环、重提新旧组隔离，以及与高级动作共存的运行时能力。
- `parallelAll` 与普通任务的既有语义在同一轮验收中保持稳定，没有被顺序审批组改造破坏。
- 本轮交付已经满足“结构化 authored 审批组、组级进度与聚合展示”这一切片的闭环标准；后续 BPM 独立候选项只剩业务单据集成深化。
