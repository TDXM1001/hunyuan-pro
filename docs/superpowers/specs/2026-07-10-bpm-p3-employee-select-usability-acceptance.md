# BPM P3.1c 发起时自选审批人可用性补强验收记录

## 结论

BPM P3.1c 发起时自选审批人可用性补强通过源级验收。

本轮将 P3.1b 的 `EMPLOYEE_SELECT_AT_START` 从手输字段 key 推进为表单 schema 字段选择和运行时员工单选。后端仍保持单员工 ID 解析边界，不新增 SQL、不新增依赖、不扩展会签或多人审批。

## 验收范围

- 流程设计器从模型关联表单 `formSchemaJson` 提取员工字段候选项。
- 模型编辑页把 `formSchemaJson` 传给流程设计器。
- 运行时表单把 `employeeSelect` 字段归一化为单员工下拉。
- 后端 P3.1b 单 ID 解析边界保持通过。

## 验收结果

| 门禁 | 命令 | 结果 |
| --- | --- | --- |
| 前端 P3.1c 合同测试 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom` | PASS；Test Files: 3 passed；Tests: 39 passed；Duration: 388ms；Start at: 00:25:17 |
| 前端类型检查 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck` | PASS；退出码 0；执行 `vue-tsc --noEmit --skipLibCheck` |
| 后端 P3.1b 边界测试 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,SimpleModelValidatorTest,BpmRuntimeStartAssignmentTest' test` | PASS；Tests run: 23, Failures: 0, Errors: 0, Skipped: 0；Total time: 3.492 s；Finished at: 2026-07-10T00:26:18+08:00 |

## 边界说明

- 本轮不新增后端字段存在性校验，字段存在性先由前端设计器保证。
- 本轮不支持多人审批、会签、或签、岗位、角色组、表达式或多实例。
- 本轮不新增 SQL。
- 本轮不新增依赖。
- 本轮不修改后端 `SimpleModelValidator` 签名。

## 已知提示

- Maven 仍提示本机 `F:\maven\apache-maven-3.9.11\conf\settings.xml` 第 235 行附近存在非预期文本，该提示属于既有本机配置问题，本轮 Maven 门禁通过。
- Maven 编译阶段仍提示 `compilerVersion` 参数已废弃，该提示属于既有 Maven 编译配置提示，本轮不处理。
