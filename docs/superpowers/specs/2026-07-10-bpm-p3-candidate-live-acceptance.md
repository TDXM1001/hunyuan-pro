# BPM P3.1 候选策略活体验收记录

日期：2026-07-10

## 结论

P3.1 Candidate Strategy Live Acceptance 已通过。

本次验收用真实本地服务证明：`EMPLOYEE_SELECT_AT_START` 可以在发起流程时通过表单字段 `approverEmployeeId` 选择审批人，并且运行时待办确实落到被选择的员工 `huke / 胡克 / employeeId=2`，不是回落到发起人 `admin / 管理员 / employeeId=1`。

## 验收范围

- 新建 P3.1 活体验收流程分类、表单、模型和定义。
- 表单 schema 包含单选员工字段 `approverEmployeeId`。
- 流程节点使用 `candidateResolverType=EMPLOYEE_SELECT_AT_START` 与 `employeeSelectFieldKey=approverEmployeeId`。
- `admin` 发起流程，并在 `formDataJson` 中选择 `employeeId=2`。
- `huke` 查询到自己的待办并完成审批。
- `admin` 侧实例详情和 trace 可加载，trace 包含 `APPROVED` 动作日志。

## 运行环境

- 后端：`http://127.0.0.1:1024`，端口监听 PID `13292`
- 前端：`http://127.0.0.1:5788`，端口监听 PID `32788`
- 运行证据：`G:\code-mcp\playwright-mcp-temp\runtime\hunyuan-p3-candidate-live-acceptance-20260710-095549.json`
- 运行证据只保存在 runtime 目录，不进入仓库提交范围。

## 关键对象

- 分类：`categoryCode=p3live_20260710-095549`，`categoryId=4`
- 表单：`formKey=p3_form_20260710-095549`，`formId=3`
- 模型：`modelKey=p3_model_20260710-095549`，`modelId=3`
- 定义：`definitionId=5`，`definitionKey=p3_model_20260710-095549`
- 实例：`instanceId=55`，`instanceNo=DK20260710NO01008`
- 任务：`taskId=58`，`taskName=P3自选审批`

## 活体链路

1. `POST /login` 登录 `admin` 与 `huke`，确认 `admin.employeeId=1`、`huke.employeeId=2`。
2. `POST /bpm/category/add`、`POST /bpm/form/add`、`POST /bpm/model/add` 创建 P3.1 验收对象。
3. `POST /bpm/designer/save` 保存单节点模型，节点候选策略为 `EMPLOYEE_SELECT_AT_START`。
4. `POST /bpm/definition/publish` 发布定义，返回 `definitionId=5`。
5. `GET /app/bpm/start-draft/5` 返回发起草稿，`formNameSnapshot=P3发起时自选审批人表单-20260710-095549`，表单快照包含 `employeeSelect` 和 `approverEmployeeId`。
6. `GET /app/bpm/startable` 包含 `definitionId=5`。
7. `POST /app/bpm/start` 由 `admin` 发起实例，`formDataJson` 包含 `{"approverEmployeeId":2,"amount":100.5}`。
8. `POST /app/bpm/my-todo` 使用 `huke` 查询，返回 `taskId=58`，`assigneeNameSnapshot=胡克`。
9. `POST /app/bpm/task/approve` 使用 `huke` 审批通过。
10. `GET /app/bpm/instance/detail/55` 显示 `runState=3`、`resultState=1`、当前待办数为 `0`。
11. `GET /bpm/instance/trace/55` 显示 `actionTypes=["APPROVED"]`，并返回 notification trace 数据。

## 断言

- 发起人与被选择审批人不同：`admin.employeeId=1`，`huke.employeeId=2`。
- 待办归属等于被选择审批人：`assigneeNameSnapshot=胡克`。
- trace 包含审批动作：`APPROVED`。
- 实例审批通过：`runState=3`，`resultState=1`。
- 发起草稿携带员工选择字段：`employeeSelect` 与 `approverEmployeeId` 均存在。

## 源码门禁

前端门禁：

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

结果：`Test Files 3 passed (3)`，`Tests 39 passed (39)`，开始时间 `2026-07-10 10:00:10`。

后端门禁：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,SimpleModelValidatorTest,BpmRuntimeStartAssignmentTest' test
```

结果：`Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`，完成时间 `2026-07-10T10:00:14+08:00`。

## 边界

- 本次没有修改生产代码。
- 本次没有新增 SQL。
- 本次没有新增依赖。
- 本次没有引入 Flowable 原生对象、原生 ID 或多实例语义到 Hunyuan 公共契约。
- 本次验收以 API 活体证据为主；前端运行时员工单选字段由 Vitest 合同测试覆盖，未额外保存浏览器截图。

## 下一步

进入 P3.2 Assignment Safety And Publish Consistency：补齐后端对不存在员工、非法发起时选择值、重提重新解析表单数据的防线，并继续保持无 SQL、无依赖、Hunyuan-native 边界。
