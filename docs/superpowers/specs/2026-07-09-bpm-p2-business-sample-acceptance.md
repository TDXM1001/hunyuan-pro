# BPM P2.4 业务样板闭环验收记录

## 验收范围

- 新增 `t_bpm_sample_expense` 样板费用申请表，用于验证业务单据与 BPM 实例的绑定关系。
- 新增 `sampleexpense` 后端边界：实体、DAO、创建表单、详情 VO、业务 service 和 callback handler。
- 新增最小管理端 API：创建样板单据、发起流程、查询详情、设置下一次回调失败。
- 真实审批终态发布 `BpmBusinessResultEvent`，让有 `businessType/businessId` 的实例进入 P2.3 回调记录链路。
- 样板 handler 覆盖通过、拒绝、失败注入、同事件幂等、终态重复和冲突结果。
- 前端只新增样板 API 合同，可靠性可视化复用既有实例 trace 和回调记录区域。
- 回调执行器测试接入真实 `BpmSampleExpenseCallbackHandler`，验证 `businessType = "sample_expense"` 可以被发现并执行。

## 验收结果

| 门禁 | 命令 | 结果 |
| --- | --- | --- |
| 后端聚焦门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmRuntimeCommandServiceTest,BpmSampleExpenseServiceTest,BpmSampleExpenseCallbackHandlerTest,BpmBusinessCallbackExecutorTest,BpmBusinessProcessApiTest' test` | PASS at 2026-07-09 13:01:54 +08:00；36 tests，0 failures |
| BPM 模块门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test` | PASS at 2026-07-09 13:02:11 +08:00；96 tests，0 failures |
| 前端 BPM 合同测试 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom` | PASS at 2026-07-09 13:02 +08:00；2 files，37 tests |
| 前端类型检查 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck` | PASS at 2026-07-09 13:02 +08:00；`vue-tsc --noEmit --skipLibCheck` exited 0 |
| Flowable 边界门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test` | PASS at 2026-07-09 13:02:47 +08:00；1 test，0 failures |

## 已知非阻塞项

- Maven 运行时仍可能报告本机 `F:\maven\apache-maven-3.9.11\conf\settings.xml` 解析警告。该警告来自本机 Maven 配置，本轮相关 Maven 门禁均通过。
- `BpmFlowableCompatibilityTest` 编译阶段仍可能出现 Spring 测试注解过期提示，这是既有测试技术债，本轮未改动。
- 本轮未做浏览器级创建/审批演示，因为 P2.4 的代码闭环由后端聚焦测试、模块测试、前端 API 合同和类型检查覆盖；后续如需业务演示，可按 `AGENTS.md` 使用持久 Playwright MCP 会话补充。

## 边界说明

- 本轮只交付 P2.4 业务样板闭环，不建设完整费用报销、采购、合同或资产模块。
- 未新增样板菜单或复杂业务页面。
- 未暴露 Flowable 原生对象、原生 ID 或外部流程引擎细节。
- 未新增 MQ、通用 HTTP 回调平台、事件总线或外部调度。
- 样板费用申请位于 `hunyuan-bpm/module/sampleexpense`，仅作为 BPM 接入验收样板，不作为生产业务模块模板。
- 可靠性展示继续复用 P2.1/P2.2/P2.3 已有实例 trace、通知记录和回调记录区域。

## 结论

P2.4 业务样板闭环通过源级验收。当前实现已经证明 Hunyuan 原生业务单据可以通过 `BpmBusinessProcessApi.start` 发起 BPM，真实审批终态可以发布业务结果事件，P2.3 回调执行器可以发现 `sample_expense` handler，并将审批结果回写到样板业务状态。
