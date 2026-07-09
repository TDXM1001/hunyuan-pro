# BPM P2.3 回调执行器与重试闭环验收记录

## 验收范围

- 业务结果事件生成的回调记录可由统一 executor 执行。
- 手动重试和自动到期重试复用同一执行路径。
- 成功回调写入成功状态和响应摘要。
- 失败回调写入失败原因、递增失败次数、设置下次重试时间。
- 达到最大失败次数后进入需人工补偿状态。
- 管理员可标记需补偿记录为已补偿，并保存补偿人、补偿时间和补偿说明。
- 管理端回调记录列表和实例可靠性区域展示回调状态、下次重试和补偿信息。
- 员工运行端详情不展示平台级失败细节。

## 验收结果

| 门禁 | 命令 | 结果 |
| --- | --- | --- |
| 后端聚焦门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmBusinessCallbackServiceTest,BpmBusinessCallbackExecutorTest,BpmBusinessCallbackSchedulerTest,BpmBusinessIntegrationRecordServiceTest,BpmInstanceTraceServiceTest' test` | PASS，Tests run: 17, Failures: 0, Errors: 0, Skipped: 0；Finished at: 2026-07-09T11:06:15+08:00 |
| 后端模块门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test` | PASS，Tests run: 82, Failures: 0, Errors: 0, Skipped: 0；Finished at: 2026-07-09T11:06:34+08:00 |
| 前端 BPM 合同测试 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom` | PASS，Test Files: 2 passed；Tests: 36 passed；Finished at: 2026-07-09T11:06:59+08:00 |
| 前端类型检查 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck` | PASS，`vue-tsc --noEmit --skipLibCheck` exit 0；Finished at: 2026-07-09T11:07:08+08:00 |
| Flowable 边界门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test` | PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0；Finished at: 2026-07-09T11:07:41+08:00 |

## 边界说明

- 本轮只覆盖 P2.3：回调执行、失败重试、自动扫描和人工补偿。
- 未新增 MQ、通用 HTTP 节点平台、完整 BPM 事件账本或 P2.4 业务样板。
- 业务处理器接口已具备扩展点，真实业务回写样板进入 P2.4。
- Hunyuan BPM 对外合同仍使用 Hunyuan 原生字段和状态语义，没有向前端或外部 API 暴露 Flowable 原生对象。

## 运行提示

- Maven 本机配置仍提示 `F:\maven\apache-maven-3.9.11\conf\settings.xml` 第 235 行附近存在非预期文本；本轮所有 Maven 门禁均在该既有 warning 下通过。
- Git 提交时仍提示未找到 lefthook 配置和存在较多 unreachable loose objects；这是既有仓库维护提示，本轮未清理。
- `BpmFlowableCompatibilityTest` 编译阶段仍出现 `MockBean` 过时 warning；该 warning 不影响本轮边界门禁结果。

## 结论

P2.3 回调执行器与重试闭环通过验收。BPM 回调记录已经从“可查 + 手动计数”推进为可执行、可失败、可自动重试、可人工补偿的可靠性闭环；管理端实例 trace 和回调记录列表也已展示同一套可靠性状态。
