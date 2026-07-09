# BPM P3.1b 发起时自选审批人验收记录

## 结论

BPM P3.1b 发起时自选审批人通过源级验收。

本轮新增 `EMPLOYEE_SELECT_AT_START`，使用户任务审批人可以在发起表单 `formDataJson` 中通过指定字段选择。实现保持 Hunyuan 原生边界：不新增 SQL、不新增依赖、不暴露 Flowable 原生对象、不迁移参考项目接口。

## 验收范围

- 后端候选人枚举、校验器、解析器支持 `EMPLOYEE_SELECT_AT_START`。
- 启动和重提链路把当前提交的 `formDataJson` 传入候选解析上下文。
- 解析器只接受单个员工 ID，拒绝数组和逗号字符串。
- 前端 BPM 设计器能选择 `发起时自选审批人` 并保存 `employeeSelectFieldKey`。

## 验收结果

| 门禁 | 命令 | 结果 |
| --- | --- | --- |
| 后端聚焦门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,SimpleModelValidatorTest,BpmRuntimeStartAssignmentTest' test` | PASS；Tests run: 23, Failures: 0, Errors: 0, Skipped: 0；Total time: 3.601 s；Finished at: 2026-07-09T23:44:25+08:00 |
| BPM 模块门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test` | PASS；Tests run: 118, Failures: 0, Errors: 0, Skipped: 0；Total time: 4.999 s；Finished at: 2026-07-09T23:44:40+08:00 |
| 前端 BPM 合同测试 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom` | PASS；Test Files: 3 passed；Tests: 42 passed；Duration: 400ms；Start at: 23:44:53 |
| 前端类型检查 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck` | PASS；退出码 0；执行 `vue-tsc --noEmit --skipLibCheck` |
| Flowable 边界门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin '-Dtest=BpmFlowableCompatibilityTest' test` | PASS；Tests run: 1, Failures: 0, Errors: 0, Skipped: 0；Test time elapsed: 11.19 s；Total time: 15.380 s；Finished at: 2026-07-09T23:45:41+08:00 |

## 边界说明

- 本轮只支持单个员工 ID。
- 本轮不支持多人审批、用户组、岗位、表达式、表单字段选择器或多级主管。
- 本轮不校验员工是否存在，只保证启动前解析为单个员工 ID。
- 本轮不涉及 SQL 变更。
- 本轮不新增依赖。

## 已知提示

- Maven 仍提示本机 `F:\maven\apache-maven-3.9.11\conf\settings.xml` 第 235 行附近存在非预期文本，该提示属于既有本机配置问题，本轮所有 Maven 门禁均通过。
- Maven 编译阶段仍提示 `compilerVersion` 参数已废弃，该提示属于既有 Maven 编译配置提示，本轮不处理。
- `BpmFlowableCompatibilityTest` 编译阶段仍提示 `MockBean` 过时，该提示属于既有测试技术债，本轮不处理。
- 部分 Maven 中文日志在当前终端输出中显示为乱码，但测试结果、测试数和退出码均可读且通过。
