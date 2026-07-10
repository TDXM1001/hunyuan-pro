# BPM Enterprise P3.2 分配安全与发布一致性验收

## 范围

- `EMPLOYEE_SELECT_AT_START` 在表单值合法解析为正数员工 ID 后，必须通过 `BpmOrgIdentityGateway.requireEmployee(employeeId)` 校验员工存在。
- 保持空值、数组、逗号字符串、非数字等既有中文错误语义不变。
- 发起与重提共用 `BpmTaskAssignmentResolver` 的表单上下文解析路径。
- 发布前校验 simple model 中发起自选审批人的字段与表单 schema 一致，失败时停在 compiler/deploy 之前。

## 已通过行为

- 发起时选择 301/302 会校验员工存在，成功变量仍为 `assignee_task_selected=301/302`。
- 选择不存在员工时，发起服务在调用 Flowable `start` 前返回中文参数错误，不写入实例。
- 重提使用真实 `BpmTaskAssignmentResolver` 重新读取最新表单；旧表单为 301，新重提表单为 302，最终变量为 `assignee_task_selected=302`。
- 发布一致性支持递归读取 `fields`/`children` 容器，识别 `employee`、`employeeSelect`，并兼容 `props.type`/`props.component`。
- 发布一致性覆盖缺字段、字段类型错误、嵌套合法字段、schema JSON 非法；校验失败不会调用 BPMN compiler 或 Flowable deploy。

## 验证

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,BpmRuntimeStartAssignmentTest,BpmRuntimeCommandServiceTest,BpmSimpleModelPublishValidatorTest,BpmDefinitionPublishServiceTest' clean test
```

结果：`Tests run: 43, Failures: 0, Errors: 0, Skipped: 0`。

## 边界

- 未引入 SQL、Flowable 公共 API、前端改动或新依赖。
- 发布校验器保持 Hunyuan 原生小范围规则，不抽象成通用表单引擎。
