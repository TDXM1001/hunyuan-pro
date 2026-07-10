# Task 2 报告：P3.2 分配安全与发布一致性

## 状态

已完成。

## RED

命令：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,BpmRuntimeStartAssignmentTest,BpmRuntimeCommandServiceTest,BpmSimpleModelPublishValidatorTest,BpmDefinitionPublishServiceTest' clean test
```

关键结果：

- `BUILD FAILURE`
- testCompile 失败，原因是新增测试引用的 `BpmSimpleModelPublishValidator` 尚未实现。
- 这是预期 RED，证明发布一致性的新生产能力尚不存在。

## GREEN

命令：

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,BpmRuntimeStartAssignmentTest,BpmRuntimeCommandServiceTest,BpmSimpleModelPublishValidatorTest,BpmDefinitionPublishServiceTest' clean test
```

关键结果：

- `BUILD SUCCESS`
- `Tests run: 43, Failures: 0, Errors: 0, Skipped: 0`

## 文件列表

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentResolver.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/BpmSimpleModelPublishValidator.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/BpmDefinitionService.java`
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskAssignmentResolverTest.java`
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeStartAssignmentTest.java`
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java`
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/BpmSimpleModelPublishValidatorTest.java`
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/definition/BpmDefinitionPublishServiceTest.java`
- `docs/superpowers/specs/2026-07-10-bpm-p3-assignment-safety-acceptance.md`
- `.superpowers/sdd/task-2-report.md`

## 自审

- TDD：已先改测试并通过 `clean test` 观察 RED，再做生产改动并 GREEN。
- 分配安全：`EMPLOYEE_SELECT_AT_START` 只在合法解析出正数 employeeId 后调用 `requireEmployee`；空值、数组、逗号字符串、非数字仍走原有中文错误。
- 发起安全：不存在员工时在 Flowable `start` 前失败，并断言不插入实例。
- 重提一致性：测试使用真实 `BpmTaskAssignmentResolver`，旧表单 301、新表单 302，断言启动变量为 `assignee_task_selected=302`。
- 发布一致性：小型 Hunyuan 原生校验器覆盖缺字段、类型错误、嵌套合法字段、schema JSON 非法；失败在 compiler/deploy 前返回中文参数错误。
- 边界：未改公共 API，未改前端，未引入 SQL 或新依赖；未处理 P3.3/P3.4 文件。
