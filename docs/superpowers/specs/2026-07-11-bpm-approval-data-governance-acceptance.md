# BPM 审批数据治理验收记录

- 验收日期：2026-07-11
- 验收范围：`docs/superpowers/specs/2026-07-11-bpm-approval-data-governance-design.md`
- 实施计划：`docs/superpowers/plans/2026-07-11-bpm-approval-data-governance-implementation.md`
- 验收状态：通过

## 1. 验收结论

BPM 审批数据治理交付块已经形成闭环：定义发布会冻结节点字段权限，运行时任务详情会按权限返回过滤后的表单上下文，审批通过只允许授权字段补丁并校验数据版本，退回重提沿用同一实例版本链，追踪页可回看字段变更，最终回调使用冻结的最终表单数据，并且 `parallelAll` 并行全员会签拒绝配置可编辑字段。

本次 SQL 迁移由用户提前运行完成，本验收未重复执行迁移脚本。

## 2. 真实运行环境

- 后端：`http://127.0.0.1:1024`
- 前端：`http://127.0.0.1:5788`
- 浏览器：Chrome 插件会话，任务名 `BPM 验收`
- 数据库：本地 MySQL `hunyuan`
- 运行证据：Chrome 页面、后端 API、数据库状态和 trace 返回共同校验

## 3. 主链路验收

样板费用定义通过 `POST /bpm/sample/expense/prepareDefinition` 发布为 `definitionId=18`、`definitionVersion=13`。定义节点快照中：

- `sample_finance_review.approvedAmount = EDITABLE, required=true`
- `sample_archive_review.approvedAmount = READONLY, required=false`

费用单 `expenseId=22` 发起为实例 `instanceId=82` 后，财务任务 `taskId=117` 返回 `formContext.dataVersion=1`，其中 `approvedAmount` 为可编辑字段，`expenseId` 与 `requestedAmount` 为只读字段。

关键验收点：

- 使用过期 `formDataVersion=0` 审批被拒绝，返回 `FORM_DATA_VERSION_CONFLICT：审批数据已变化，请刷新后重新确认`。
- 使用当前 `formDataVersion=1` 将 `approvedAmount` 修改为 `301.11` 后审批成功。
- 后续归档任务 `taskId=118` 返回 `formContext.dataVersion=2`，`approvedAmount=301.11` 且权限为只读。
- 实例 trace 中记录 `INSTANCE_STARTED` 与 `TASK_APPROVED` 两条字段变更，`TASK_APPROVED` 明确记录 `approvedAmount: 88.66 -> 301.11`。
- 最终归档通过后，样板费用单回写 `approvedAmount=301.11`、`approvalStatus=2`、`finalFormDataVersion=2`。

## 4. 回调冻结与重试验收

费用单 `expenseId=22` 在归档前执行 `markNextCallbackFailed`，第一次人工触发回调执行时按预期失败，回调记录 `callbackRecordId=9` 进入 `FAILED`，失败原因是样板费用申请模拟回调失败。

第二次执行 `POST /bpm/integration/callback/retry/9` 后，同一回调记录变为 `SUCCEEDED`，样板费用单仍回写同一最终审批结果：

- `callbackEventId=RESULT:82:1`
- `approvedAmount=301.11`
- `finalFormDataVersion=2`
- `callbackFailFlag=false`

这证明最终回调使用冻结的最终表单数据和版本，而不是重新拼装一个可能漂移的运行时状态。

## 5. 退回重提版本连续性验收

费用单 `expenseId=23` 发起为实例 `instanceId=83` 后，财务任务 `taskId=119` 退回发起人。重提草稿返回 `formDataVersion=1` 与初始表单数据。

关键验收点：

- 使用过期 `formDataVersion=0` 重提被拒绝，返回 `FORM_DATA_VERSION_CONFLICT：审批数据已变化，请刷新后重新确认`。
- 使用当前版本重提后仍复用同一 Hunyuan 实例 `instanceId=83`。
- 重提后的新财务任务 `taskId=120` 返回 `formContext.dataVersion=2`。
- trace 中记录 `INSTANCE_RESUBMITTED` 字段变更，`requestedAmount` 与 `approvedAmount` 均从 `66.66` 变为 `99.99`。

## 6. 隐藏字段与 Chrome 验收

本次新增隐藏字段验收模型 `definitionId=19`，发起实例 `instanceId=84`、任务 `taskId=121`，表单字段包含 `requestAmount`、`internalCode`、`visibleNote`，其中 `internalCode` 配置为 `HIDDEN`。

API 任务详情返回：

- `formSchemaJson` 只包含 `requestAmount` 与 `visibleNote`
- `formDataJson` 只包含 `requestAmount=55.5` 与 `visibleNote=VISIBLE-1783758805`
- `fieldPermissions` 只包含 `requestAmount READONLY` 与 `visibleNote READONLY`
- 隐藏字段 `internalCode` 与隐藏值 `SECRET-1783758805` 均未出现在员工端响应中

Chrome 插件打开员工待办列表后，任务 `GOV_HIDDEN_1783758805` 的审批弹窗仅显示禁用的 `55.5` 和 `VISIBLE-1783758805`，没有显示隐藏字段名或隐藏值。该证据覆盖了真实页面渲染层，而不仅是 API 层过滤。

## 7. 并行全员会签权限边界验收

发布 `parallelAll` 节点并配置 `approvedAmount=EDITABLE` 时，后端拒绝发布：

```text
审批节点【并行会签校验】并行全员会签字段【approvedAmount】不允许配置为可编辑
```

该约束避免多个并行成员同时修改同一审批数据，当前交付块只允许并行全员会签节点配置 `READONLY` 或 `HIDDEN` 字段权限。

## 8. 自动化门禁

本次关闭前重新执行以下门禁：

- `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test`：通过，`224` 个测试全部成功。
- `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin '-Dtest=BpmFlowableCompatibilityTest' test`：通过，`1` 个 Flowable 兼容测试成功。
- `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom`：通过，`4` 个测试文件、`62` 个测试全部成功。
- `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck`：通过。
- `git diff --check`：通过，仅输出工作区 LF/CRLF 提示。

已知环境提示：Maven settings 第 `235` 行存在既有 XML 空白警告；`BpmFlowableCompatibilityTest` 中 `MockBean` 存在既有弃用警告。本次门禁未出现失败项。

## 9. 已知边界

- 本交付块不引入通用网关、条件路由、或签、比例审批、子流程或 Flowable multi-instance。
- `parallelAll` 的可编辑字段仍然被禁止；需要多人共同填写同一表单字段时，应先设计冲突合并语义。
- 本次验收会在本地数据库留下 `GOV_ACC_*`、`GOV_RESUBMIT_*`、`GOV_HIDDEN_*`、`GOV_PARALLEL_*` 等验收数据，不作为生产种子数据。
