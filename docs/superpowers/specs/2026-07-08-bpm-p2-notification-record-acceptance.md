# BPM P2.2 通知投递记录验收记录

## 验收范围

- 新增 BPM 自有通知投递记录，用于回答“哪个流程实例、哪个任务、向谁、通过什么渠道发送通知、结果如何、失败原因是什么”。
- 新待办任务投影成功后，在存在通知监听配置且存在处理人时触发通知记录与投递。
- 通知投递按渠道记录 `PENDING`、`SUCCESS`、`FAIL`，单个渠道失败不回滚任务投影，也不阻断其它渠道尝试。
- 管理员实例 trace 返回 `notificationRecords`，并在现有实例详情抽屉的“可靠性追踪”区域展示通知记录。
- 员工运行端详情仍通过 `/app/bpm/instance/detail/{instanceId}` 加载，不展示平台通知失败细节。
- 新增 SQL 增量 `v3.40.0.sql` 创建 `t_bpm_notification_record`。

## 验收结果

| 门禁 | 命令 | 结果 |
| --- | --- | --- |
| 后端聚焦门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmNotificationRecordServiceTest,BpmNotificationListenerServiceTest,BpmTaskProjectionServiceTest,BpmInstanceTraceServiceTest' test` | PASS at 2026-07-08 23:36:54 +08:00; 13 tests, 0 failures |
| 后端模块门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test` | PASS at 2026-07-08 23:38:27 +08:00; 73 tests, 0 failures |
| 前端 BPM 合同测试 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom` | PASS at 2026-07-08 23:39:36 +08:00; 2 files, 36 tests |
| 前端类型检查 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck` | PASS; `vue-tsc --noEmit --skipLibCheck` exited 0 |
| Flowable 边界门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test` | PASS at 2026-07-08 23:42:38 +08:00; 1 test, 0 failures |

## 已知非阻塞项

- Maven 运行时仍报告本机 `F:\maven\apache-maven-3.9.11\conf\settings.xml` 警告：`expected START_TAG or END_TAG not TEXT`，位置约为 line 235。该警告来自本机 Maven 配置，本轮所有 Maven 门禁均通过。
- `BpmFlowableCompatibilityTest` 编译阶段仍有 Spring `@MockBean` 过期警告，这是既有测试技术债，本轮未改动。
- `BpmBusinessIntegrationRecordServiceTest` 在模块门禁中仍有既有 unchecked-operation 编译提示，不影响本轮通知记录闭环。

## 边界说明

- 本轮只覆盖 P2.2：通知投递记录、首个新待办通知触发、记录接入实例 trace、管理员前端可靠性区域展示。
- 独立的通知记录监控列表页暂不实现，后续可在 trace 支持稳定后单独做。
- 不新增消息队列、通知重试执行器、完整 BPM 事件账本、通用消息中心、SMS/mail 平台重构。
- P2.3 回调执行器与重试闭环仍未开始；自动重试、人工补偿状态、统一回调执行路径属于下一张开发卡。
- 本轮没有执行浏览器验收，因为该切片的后端行为、前端合同、类型检查和 Flowable 边界均已由源级门禁覆盖，且没有新的 live UI 争议。

## 结论

P2.2 通知投递记录闭环通过。当前实现已经能在新待办通知触发时生成 BPM 自有通知记录，并把通知记录纳入管理员实例可靠性追踪；员工运行端详情保持业务视角，不暴露平台投递失败细节。
