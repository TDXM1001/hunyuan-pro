# BPM P1.3 Business Integration Acceptance

## Scope

- Business start/status/result contract exists under Hunyuan BPM API.
- Command records and callback records are persisted through `t_bpm_command_record` and `t_bpm_callback_record`.
- Business start uses command idempotency key `START:{businessType}:{businessId}:{definitionKey}`.
- Callback retry is available through `POST /bpm/integration/callback/retry/{callbackRecordId}`.
- Admin monitoring endpoints expose callback and command records:
  - `POST /bpm/integration/callback/query`
  - `POST /bpm/integration/command/query`
- Frontend monitoring pages expose callback and command record lists.

## Verification

- Command: `mvn -f E:/my-project/hunyuan-pro-worktrees/bpm-enterprise-p1/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmBusinessProcessApiTest,BpmBusinessCallbackServiceTest,BpmBusinessIntegrationRecordServiceTest,BpmApiIsolationTest' test`
  - Result: passed, 10 tests, 0 failures, 0 errors.
- Command: `mvn -f E:/my-project/hunyuan-pro-worktrees/bpm-enterprise-p1/hunyuan-backend/pom.xml -pl hunyuan-bpm test`
  - Result: passed, 55 tests, 0 failures, 0 errors.
- Command: `pnpm --dir E:/my-project/hunyuan-pro-worktrees/bpm-enterprise-p1/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom`
  - Result: passed, 2 files, 35 tests.
- Command: `pnpm --dir E:/my-project/hunyuan-pro-worktrees/bpm-enterprise-p1/hunyuan-design -F @hunyuan/system run typecheck`
  - Result: passed, `vue-tsc --noEmit --skipLibCheck` exited 0.

## Boundaries

- No production business module is forced to depend on the sample.
- No Flowable object appears in business API signatures.
- Callback retry currently records retry intent by incrementing `retryCount`; durable async delivery scheduling remains a later hardening step.
- Browser proof was not run for this increment because the change is covered by API/module/type contracts and backend unit tests; live route/menu wiring remains a separate deployment/menu task.
- P1.4 advanced approval actions are not included.
