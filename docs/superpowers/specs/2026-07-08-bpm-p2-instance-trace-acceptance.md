# BPM P2.1 Instance Trace Acceptance

## Scope

- Added admin-only BPM instance trace endpoint: `GET /bpm/instance/trace/{instanceId}`.
- Added `instanceId` filters for callback and command integration records.
- Added frontend trace API and an admin-only reliability trace section in the existing instance detail drawer.
- Kept runtime employee-side detail loading on `/app/bpm/instance/detail/{instanceId}` without platform reliability failure details.
- Kept Flowable native objects out of public API and frontend types.

## Verification

| Gate | Command | Result |
| --- | --- | --- |
| Backend targeted | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmBusinessIntegrationRecordServiceTest,BpmInstanceTraceServiceTest' test` | PASS at 2026-07-08 21:58:22 +08:00; 6 tests, 0 failures |
| Backend module | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test` | PASS at 2026-07-08 21:54:50 +08:00; 63 tests, 0 failures |
| Frontend BPM contracts | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom` | PASS at 2026-07-08 21:55:05 +08:00; 2 files, 36 tests |
| Frontend typecheck | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck` | PASS at 2026-07-08 21:55 +08:00; `vue-tsc --noEmit --skipLibCheck` exited 0 |
| Flowable boundary | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test` | PASS at 2026-07-08 21:57:56 +08:00; 1 test, 0 failures |

## Observed Non-Blockers

- Maven reports an existing `F:\maven\apache-maven-3.9.11\conf\settings.xml` warning: `expected START_TAG or END_TAG not TEXT` around line 235. The warning appeared on Maven runs but did not fail builds.
- `BpmBusinessIntegrationRecordServiceTest` still emits an existing unchecked-operation compiler note.
- `BpmFlowableCompatibilityTest` emits existing Spring `@MockBean` deprecation warnings.

## Boundaries

- This slice aggregates existing action logs, callback records, and command records; it does not create the full BPM event ledger.
- Notification delivery records are not included in this slice.
- Callback execution and retry behavior remains the current implementation path.
- Browser proof was not required for this source-level slice because route/API/type boundaries passed and no live UI dispute was present.
