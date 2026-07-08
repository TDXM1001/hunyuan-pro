# BPM P1.1 Definition Governance Acceptance

## Scope

- Publish validation report endpoint and frontend contract.
- Publish diff preview endpoint and frontend contract.
- Definition start scope persistence and runtime filtering.
- Definition start enable/suspend controls.

## Verification

- Command: `mvn -f E:/my-project/hunyuan-pro-worktrees/bpm-enterprise-p1/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmDefinitionGovernanceServiceTest,BpmDefinitionPublishServiceTest,BpmRuntimeStartAssignmentTest' test`
- Result: PASS, 9 tests passed with 0 failures and 0 errors.
- Evidence: targeted backend tests cover validation report, publish diff, start scope save, start enable/suspend, startable list filtering, and start API range rejection.

- Command: `pnpm --dir E:/my-project/hunyuan-pro-worktrees/bpm-enterprise-p1/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom`
- Result: PASS, 2 files and 33 tests passed.
- Evidence: API contract tests include validation, diff, start scope, suspend, and enable endpoints; BPM module tests include model publish precheck/diff and definition list governance actions.

- Command: `pnpm --dir E:/my-project/hunyuan-pro-worktrees/bpm-enterprise-p1/hunyuan-design -F @hunyuan/system run typecheck`
- Result: PASS, `vue-tsc --noEmit --skipLibCheck` exited 0.
- Evidence: frontend TypeScript contracts compile in the Hunyuan system package.

## Boundaries

- No Flowable type is exposed in public API or frontend types.
- No Yudao/RuoYi route or contract is migrated.
- No runtime admin intervention is implemented in P1.1.
- No advanced approval action is implemented in P1.1.
- Start scope supports Hunyuan-native `ALL`, `EMPLOYEE`, `DEPARTMENT`, and `ROLE` JSON semantics; invalid or unknown scope JSON is not allowed to start.
