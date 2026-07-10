# BPM Enterprise P3.3 候选策略预检验收

日期：2026-07-10

## 结论

P3.3 Candidate Precheck And Explanation 已通过源码验收。

流程设计器现在通过现有 `validateForPublish` 接口展示逐节点候选策略摘要。预检保持 Hunyuan 原生边界，不调用 Flowable；阻断项会进入发布校验 findings，`validateForPublish.pass=true` 表示当前模型满足真实发布门禁。

## 六类候选策略

| 策略 | 预检规则 |
| --- | --- |
| `EMPLOYEE` | 校验固定员工存在；顺序模式校验至少两名不同员工，并逐一校验员工存在。 |
| `ROLE` | 校验角色配置，并通过组织身份网关解析当前角色成员；角色无可用员工时返回 `ROLE_EMPLOYEE_EMPTY`。 |
| `DEPARTMENT_MANAGER` | 指定部门时立即解析主管；未指定部门且没有模拟发起上下文时标记为运行时提供；部门无主管时返回 `DEPARTMENT_MANAGER_EMPTY`。 |
| `START_EMPLOYEE` | 有模拟发起人时通过 `requireEmployee` 校验员工有效性；没有模拟上下文时标记为运行时提供。 |
| `START_DEPARTMENT_MANAGER` | 有模拟发起部门时实际解析主管；没有模拟上下文时标记为运行时提供；部门无主管时返回 `DEPARTMENT_MANAGER_EMPTY`。 |
| `EMPLOYEE_SELECT_AT_START` | 校验表单字段存在且为 `employee` 或 `employeeSelect`；有模拟表单值时解析单一员工 ID 并通过 `requireEmployee` 校验员工有效性；没有模拟表单值时标记为运行时提供，非法字段或非法模拟值会阻断。 |

## 状态语义

- `READY`：当前信息足以解析候选人，且已完成可执行的组织身份检查。
- `RUNTIME_REQUIRED`：静态发布信息合法，但必须等发起人、发起部门或发起表单值进入运行时后才能解析；该状态本身不阻断发布。
- `BLOCKING`：配置缺失、员工不存在、角色无成员、部门无主管、表单字段不一致或其他确定性错误；该状态阻断发布。

模拟上下文中的发起员工和发起表单员工不会只做 ID 形状检查。员工被禁用、删除或不存在时，预检统一返回 `EMPLOYEE_NOT_FOUND`，避免把无效员工误报为 `READY`。

## 发布门禁

`validateForPublish` 现在统一组合以下规则：

- simple model 与发起规则校验；
- 流程分类存在性校验；
- 流程表单存在性校验；
- 发起时自选审批字段与表单 schema 一致性校验；
- 六类候选策略预检。

报告中的 `pass` 由阻断项数量统一计算。前端发布动作先刷新报告，只有 `pass=true` 才进入发布确认；后端 `publish` 同时保留关键校验，作为发布期间数据变化的竞态保护。

## 前端展示

- 流程设计器编辑页新增紧凑的“候选策略预检”区。
- 每个审批节点展示节点、策略、依赖配置、当前能否解析、是否依赖表单、状态和消息。
- 保存草稿、加载详情和手工刷新都会获取最新发布前校验报告。
- 普通 `input` 不再因字段名包含 `employeeId` 或 `approver` 被误识别为员工单选字段。

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

## 边界

- 未新增依赖。
- 未新增 SQL。
- 未向公共契约暴露 Flowable 原生对象或原生 ID。
- P3 只完成当前六类策略，不扩展岗位、用户组、动态部门成员、表达式或脚本审批人。
