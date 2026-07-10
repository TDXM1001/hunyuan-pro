# BPM Enterprise P3.4 顺序多人审批验收

日期：2026-07-10

## 结论

P3.4 Sequential Multi-Approver Approval 已通过源码测试与真实 API 活体验收。

一个配置为 `approvalMode=sequential` 的指定员工节点，会按员工选择顺序编译为多个单处理人任务。实现复用现有 `assignee_<nodeKey>` 变量和单人任务运行时，不引入 Flowable multi-instance、并行会签或比例审批语义。

## 实现规则

- 顺序审批只支持 `candidateResolverType=EMPLOYEE`。
- `employeeIds` 至少包含两名不同的正数员工 ID。
- 发布预检逐一校验员工存在。
- 编译后的节点 key 为 `<authoredNodeKey>_<序号>`。
- 编译前检查展开后的 key 全局唯一，并限制在数据库 `varchar(128)` 长度内。
- 任务名称追加 `（序号/总数）`，例如 `财务复核（1/2）`。
- 编译快照保留 authored node key、名称、顺序和总数，运行时仍按展开节点解析单一审批人。
- 待办和任务详情公共投影补齐 `taskKey`，避免真实接口返回 `null`。

## 真实活体验收

运行环境：

- 最新源码后端：`http://127.0.0.1:1025`
- 运行证据：`G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-p3-sequential-live-acceptance-20260710-120102.json`
- 证据只保存在 Playwright runtime 目录，不进入仓库。

关键对象：

- 模型：`modelId=4`，`modelKey=p3_seq_20260710-114811`
- 定义：`definitionId=6`
- 实例：`instanceId=56`，`instanceNo=DK20260710NO01015`
- 第一任务：`taskId=59`，`taskKey=task_finance_1`，`taskName=财务复核（1/2）`，审批人 `admin / 管理员 / employeeId=1`
- 第二任务：`taskId=60`，`taskKey=task_finance_2`，`taskName=财务复核（2/2）`，审批人 `huke / 胡克 / employeeId=2`
- 最终状态：`runState=3`，`resultState=1`

链路断言：

1. 实例启动后只出现第一审批人的 `task_finance_1` 待办。
2. 第一任务审批通过后才生成第二审批人的 `task_finance_2` 待办。
3. 两个任务分别由配置顺序中的员工完成。
4. 实例详情和 trace 均包含两条 `APPROVED` 动作记录。
5. 第二任务完成后实例正常结束且结果为通过。

## 验证

后端聚焦门禁：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmCandidatePrecheckServiceTest,BpmDefinitionGovernanceServiceTest,SimpleModelValidatorTest,SimpleModelBpmnCompilerTest,BpmTaskAssignmentResolverTest,BpmRuntimeStartAssignmentTest,BpmTaskListProjectionContractTest' test
```

结果：`Tests run: 53, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

前端聚焦门禁：

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

结果：`Test Files 3 passed (3)`，`Tests 47 passed (47)`。

## 解释边界

当前公共任务语义通过展开后的 `taskKey` 和任务名称中的 `（1/2）` 表达。编译快照已经保留 authored node 元数据，但面向前端的结构化“顺序审批组”对象、组级进度和聚合展示仍属于 P4，不在 P3 扩大公共契约。

## 不在 P3

- 并行会签。
- 或签。
- 比例审批。
- Flowable multi-instance。
- 动态增加或删除后续顺序审批人。
- 角色自动展开为多人顺序审批。

本阶段未新增依赖，未新增 SQL。
