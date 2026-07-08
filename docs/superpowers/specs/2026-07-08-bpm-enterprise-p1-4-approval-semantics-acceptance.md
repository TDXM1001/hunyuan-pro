# BPM P1.4 Approval Semantics Acceptance

## Scope

- User task delegate is available through `POST /app/bpm/task/delegate`.
- User add sign is available through `POST /app/bpm/task/addSign`.
- User reduce sign is available through `POST /app/bpm/task/reduceSign`.
- User recall is available through `POST /app/bpm/task/recall`.
- Admin delegate is available through `POST /bpm/task/adminDelegate`.
- Action logs are written with Hunyuan action types:
  - `DELEGATED`
  - `ADD_SIGNED`
  - `REDUCE_SIGNED`
  - `RECALLED`

## Verification

- Command: `mvn -f E:/my-project/hunyuan-pro-worktrees/bpm-enterprise-p1/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmTaskAdvancedActionServiceTest test`
  - RED result: first run failed in `testCompile` because advanced action forms did not exist.
  - GREEN result: passed after implementation, 4 tests, 0 failures, 0 errors.
- Command: `mvn -f E:/my-project/hunyuan-pro-worktrees/bpm-enterprise-p1/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAdvancedActionServiceTest,BpmRuntimeCommandServiceTest' test`
  - Result: passed, 15 tests, 0 failures, 0 errors.
- Command: `mvn -f E:/my-project/hunyuan-pro-worktrees/bpm-enterprise-p1/hunyuan-backend/pom.xml -pl hunyuan-bpm test`
  - Result: passed, 59 tests, 0 failures, 0 errors.

## Boundaries

- Delegate updates the Flowable task assignee through `FlowableTaskGateway.transfer`; no Flowable table is updated directly.
- Add sign creates a Hunyuan pending task projection for controlled协同 review; it does not perform arbitrary node jumps.
- Reduce sign only cancels pending Hunyuan add-sign task projections.
- Recall is limited to the start employee of a running instance and moves the Hunyuan instance to `WAIT_RESUBMIT`.
- Full visual/browser acceptance was not run for this backend semantic increment.
