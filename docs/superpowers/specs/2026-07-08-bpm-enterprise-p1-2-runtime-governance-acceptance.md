# BPM P1.2 Runtime Governance Acceptance

## Scope

- 管理员可通过 Hunyuan BPM 合同取消运行中或待重提流程实例。
- 管理员取消会终止运行中的 Flowable 实例、关闭 Hunyuan 待办投影、写入 `ADMIN_INSTANCE_CANCELLED` 动作日志。
- 管理员可转交流程待办，任务投影更新为目标员工快照，动作日志写入 `ADMIN_TRANSFERRED`。
- 管理员可触发实例任务投影重同步，入口复用 `BpmTaskProjectionService.syncActiveTasksForInstance`。
- 管理端入口已接入：
  - `POST /bpm/instance/adminCancel`
  - `POST /bpm/instance/resyncProjection/{instanceId}`
  - `POST /bpm/task/adminTransfer`

## Verification

- Command: `mvn -f E:/my-project/hunyuan-pro-worktrees/bpm-enterprise-p1/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmAdminInterventionServiceTest test`
  - RED result: first run failed in `testCompile` because `BpmAdminInstanceCancelForm` and `BpmAdminTaskTransferForm` did not exist.
  - GREEN result: passed after implementation, 3 tests, 0 failures, 0 errors.
- Command: `mvn -f E:/my-project/hunyuan-pro-worktrees/bpm-enterprise-p1/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmAdminInterventionServiceTest,BpmRuntimeCommandServiceTest,BpmTaskProjectionServiceTest' test`
  - Result: passed, 15 tests, 0 failures, 0 errors.
- Command: `mvn -f E:/my-project/hunyuan-pro-worktrees/bpm-enterprise-p1/hunyuan-backend/pom.xml -pl hunyuan-bpm test`
  - Result: passed, 46 tests, 0 failures, 0 errors.

## Boundaries

- This increment is backend runtime governance only; no frontend page change is included.
- Flowable remains behind Hunyuan BPM internal gateways.
- Business integration reliability, callback records, command records, and retry semantics remain P1.3.
- Delegate, add sign, reduce sign, and recall remain P1.4.
- Maven still prints the existing local `F:\maven\apache-maven-3.9.11\conf\settings.xml` line 235 warning; it did not block the tests.
