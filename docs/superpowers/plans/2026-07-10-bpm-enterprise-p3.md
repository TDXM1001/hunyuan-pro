# BPM Enterprise P3 Overall Task Plan

> **For agentic workers:** Execute this as a stage-gate plan. Each P3 slice should be completed, verified, and recorded before moving to the next slice. Use the detailed slice plan pattern already established by `2026-07-09-bpm-p3-candidate-strategy.md`, `2026-07-09-bpm-p3-employee-select-at-start.md`, and `2026-07-10-bpm-p3-employee-select-usability.md`.

**Goal:** Deliver BPM P3 as a fast, evidence-backed upgrade from source-level candidate strategy support to real business-usable approval assignment, publish/start safeguards, candidate explainability, and a narrow sequential multi-approver capability.

**Design Source:** `docs/superpowers/specs/2026-07-10-bpm-enterprise-p3-design.md`

**Architecture:** Keep Flowable hidden behind Hunyuan BPM boundaries. Preserve the current single-assignee runtime core for P3.1-P3.3. If P3.4 is implemented, model sequential multi-approver approval by compiling one authored node into multiple ordered single-assignee user tasks, not by introducing Flowable multi-instance or parallel countersign semantics.

**Tech Stack:** Java 17, Spring Boot, Flowable 7.2.0, MyBatis-Plus, JUnit 5, Mockito, AssertJ, Vue 3, TypeScript, Element Plus, form-create, Vitest, Maven, pnpm, Playwright MCP for live evidence when useful.

## Global Constraints

- All production code, tests, contracts, docs, and verification records stay in `E:/my-project/hunyuan-pro`.
- Yudao frontend and RuoYi backend remain reference lines only; borrow mechanisms, not code shape, route names, enums, dependency assumptions, or public contracts.
- Do not add dependencies.
- Do not add SQL unless a slice explicitly proves schema, menu, permission, or seed data is required. P3 is expected to require no SQL.
- Do not expose Flowable native objects, native IDs, or Flowable implementation semantics through public Hunyuan contracts.
- Keep Chinese docs, test names, user-facing messages, and UI copy UTF-8.
- Keep P3 focused on candidate strategy, assignment safety, explainability, and sequential multi-approver approval only.
- Do not implement parallel countersign, or-sign, approval ratio, Flowable multi-instance, gateways, subprocesses, scripts, expression approvers, posts, user groups, dynamic department members, or a generic approval-rule DSL in P3.
- Runtime evidence, Playwright screenshots, browser profiles, network logs, and temporary output must stay under `G:/code-mcp/playwright-mcp-temp/runtime` or `G:/code-mcp/playwright-mcp-temp/cache`; do not commit them unless explicitly requested.

## Current Baseline

- P2 live acceptance is closed: sample expense creation, BPM start, approval, callback failure visibility, manual retry recovery, business status writeback, and trace reliability data all have live proof.
- P2.3 callback executor/retry is closed.
- P2.4 sample business integration is closed at source level and has live P2 proof.
- P3.1a source work already supports `START_EMPLOYEE` and `START_DEPARTMENT_MANAGER`.
- P3.1b source work already supports `EMPLOYEE_SELECT_AT_START`.
- P3.1c source work already makes `EMPLOYEE_SELECT_AT_START` usable from form schema selection and runtime employee single-select fields.
- The next fastest meaningful task is P3.1 live acceptance, not another broad feature.

## Stage Gates

- [x] **Gate A: P3.1 live acceptance completed**
  - Acceptance record exists under `docs/superpowers/specs/`.
  - Real local services were checked before live proof.
  - `EMPLOYEE_SELECT_AT_START` is proven in a real start flow.
- [x] **Gate B: P3.2 assignment safeguards completed**
  - Backend rejects invalid or nonexistent selected employees with clear messages.
  - Start and resubmit use the same assignment safety path.
  - Focused backend and frontend contract gates pass.
- [x] **Gate C: P3.3 candidate precheck/explainability completed**
  - Admin can understand candidate source per node before publish/start surprises.
  - Precheck stays Hunyuan-native and does not talk to Flowable directly.
- [x] **Gate D: P3.4 sequential multi-approver decision completed**
  - Either implemented with live proof, or explicitly deferred with rationale.
  - No parallel countersign or multi-instance semantics are introduced.
- [ ] **Gate E: P3 final acceptance completed**
  - Full targeted backend/frontend gates pass.
  - P3 acceptance record summarizes passed scope and remaining P4 boundaries.

---

## Task 1: P3.1 Candidate Strategy Live Acceptance

**Goal:** Prove existing P3.1a/b/c candidate strategy work in a real local flow, especially `EMPLOYEE_SELECT_AT_START`.

**Why first:** This reuses already completed code and gives the fastest confidence increase. It prevents building P3.2/P3.3 on a strategy that only passed source-level tests.

**Files:**
- Create: `docs/superpowers/specs/2026-07-10-bpm-p3-candidate-live-acceptance.md`
- Optional runtime evidence only under: `G:/code-mcp/playwright-mcp-temp/runtime`

**Steps:**

- [x] Confirm frontend `http://127.0.0.1:5788` and backend `http://127.0.0.1:1024` are available.
- [x] Re-run the P3.1 source gates:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,SimpleModelValidatorTest,BpmRuntimeStartAssignmentTest' test
```

- [x] Prepare or reuse a BPM form with one `employeeSelect` field such as `approverEmployeeId`.
- [x] Prepare or publish a single-node process whose node uses `candidateResolverType = EMPLOYEE_SELECT_AT_START` and `employeeSelectFieldKey = approverEmployeeId`.
- [x] Start the process from the runtime start form and choose a real employee.
- [x] Verify the created todo belongs to the selected employee, not the starter by accident.
- [x] Complete the todo and verify instance detail, action log, and trace still load.
- [x] Record exact IDs, endpoints, selected employee, task owner, and evidence file names in the acceptance record.
- [x] Review the acceptance record and confirm it contains no unfinished template text before closing the slice.

**Done when:**

- `EMPLOYEE_SELECT_AT_START` has real local runtime proof.
- Source gates pass.
- Acceptance record is UTF-8 readable and contains no placeholder text.
- No runtime evidence files are committed.

---

## Task 2: P3.2 Assignment Safety And Publish Consistency

**Goal:** Move from “frontend tries to provide valid employee fields” to “backend gives clear failures for invalid assignment input.”

**Scope:**

- Validate selected employee existence for `EMPLOYEE_SELECT_AT_START` after parsing a single employee ID.
- Preserve existing invalid-shape rejections for arrays, comma strings, blank values, and non-numeric values.
- Ensure start and resubmit both use the same assignment context and validation path.
- Add a small publish-time consistency layer if current publish detail can access form schema safely.

**Primary backend files to inspect first:**

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentResolver.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentContext.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/BpmDefinitionService.java`

**Tests to extend or create:**

- `BpmTaskAssignmentResolverTest`
- `BpmRuntimeStartAssignmentTest`
- Resubmit-focused test if an existing test covers `resubmitMyInstance`
- `SimpleModelValidatorTest` or a new publish-consistency validator test

**Steps:**

- [x] Write failing backend tests for nonexistent selected employee.
- [x] Write failing backend tests proving resubmit re-parses the latest `formDataJson`.
- [x] Decide whether publish-time field existence can be validated without broad service coupling.
- [x] Implement the smallest backend safety change, preferably by reusing `BpmOrgIdentityGateway.requireEmployee`.
- [x] Preserve existing user-facing messages for blank, array, comma string, and invalid numeric values.
- [x] Run focused backend gates:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,BpmRuntimeStartAssignmentTest,SimpleModelValidatorTest' test
```

- [x] Run frontend P3.1c contract gates if any type or API contract changes:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

- [x] Create `docs/superpowers/specs/2026-07-10-bpm-p3-assignment-safety-acceptance.md`.

**Done when:**

- Invalid or nonexistent selected employees fail before Flowable start.
- Start and resubmit behavior are both covered.
- No SQL or dependency change is introduced.
- Acceptance record explains why any publish-time schema validation was implemented or deferred.

---

## Task 3: P3.3 Candidate Precheck And Explanation

**Goal:** Let admins understand candidate assignment before publishing or starting a broken process.

**Scope:**

- Add a Hunyuan-native candidate precheck service.
- Input should include simple model, optional form schema, optional simulated starter, and optional simulated form data.
- Output one summary row per user task:
  - node key
  - node name
  - candidate resolver type
  - required configuration
  - whether it can be resolved now
  - whether it requires runtime form data
  - failure or warning message
- The service must not call Flowable.
- Frontend can show this in a quiet designer-side panel or publish confirmation surface.

**Candidate strategies to cover:**

- `EMPLOYEE`
- `ROLE`
- `DEPARTMENT_MANAGER`
- `START_EMPLOYEE`
- `START_DEPARTMENT_MANAGER`
- `EMPLOYEE_SELECT_AT_START`

**Likely backend files:**

- New service under `hunyuan-bpm/module/definition` or `hunyuan-bpm/engine/compiler`
- Admin controller endpoint under existing BPM model/definition surface if needed
- New VO for precheck summaries

**Likely frontend files:**

- `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/*.ts`
- `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

**Steps:**

- [x] Write backend tests for all six candidate resolver types.
- [x] Implement precheck summary service without touching Flowable.
- [x] Reuse the existing definition validation API for the frontend live precheck.
- [x] Wire frontend display in a dense, operational UI surface, not a marketing-style explanation block.
- [x] Run focused backend tests.
- [x] Run BPM frontend API/module tests and typecheck.
- [x] Create `docs/superpowers/specs/2026-07-10-bpm-p3-candidate-precheck-acceptance.md`.

**Done when:**

- Admins can see candidate source and missing configuration before runtime failure.
- Precheck is source-tested for all current candidate strategies.
- Flowable remains hidden.

---

## Task 4: P3.4 Sequential Multi-Approver Approval

**Goal:** Support the lowest-risk multi-approver use case by compiling one authored sequential approval node into multiple ordered single-assignee tasks.

**Decision checkpoint before implementation:**

- [x] Confirm P3.1 live acceptance passed.
- [x] Confirm P3.2 safeguards passed.
- [x] Confirm P3.3 precheck passed.
- [x] Confirm the product decision is still sequential-only, not parallel countersign or ratio approval.

**Scope:**

- Allow `approvalMode = sequential` for explicit `employeeIds`.
- Require at least two employee IDs.
- Reject empty, duplicate, invalid, or nonexistent employee IDs.
- Compile one authored node into ordered internal user tasks:
  - `originalNodeKey_1`
  - `originalNodeKey_2`
  - `originalNodeKey_3`
- Preserve enough authored metadata so instance detail can explain these tasks as one sequential approval group.
- Reuse the existing single-assignee `assignee_<nodeKey>` variable contract.

**Do not implement:**

- Parallel countersign.
- Or-sign.
- Approval ratio.
- Flowable multi-instance.
- Dynamic add/remove of future sequential approvers.
- Role-to-many automatic expansion.

**Likely backend files:**

- `SimpleModelValidator.java`
- `SimpleModelBpmnCompiler.java`
- `BpmTaskAssignmentResolver.java`
- `BpmInstanceTraceService.java`
- Related tests for compiler, resolver, runtime start, and trace/detail projection

**Likely frontend files:**

- `types.ts`
- `simple-model-bridge.ts`
- `bpm-process-designer-adapter.vue`
- `bpm-designer-adapters.test.ts`
- Instance detail drawer only if group explanation needs UI support

**Steps:**

- [x] Write compiler tests proving sequential node expansion order.
- [x] Write validation tests for invalid `employeeIds`.
- [x] Write runtime start tests proving each expanded node receives an `assignee_<expandedNodeKey>` variable.
- [x] Update frontend designer to allow sequential explicit employee list only.
- [x] Add task key and ordered task-name explanation to the current detail surfaces.
- [x] Run focused backend and frontend gates.
- [x] Run at least one API live acceptance proving tasks appear one after another.
- [x] Create `docs/superpowers/specs/2026-07-10-bpm-p3-sequential-approval-acceptance.md`.

**Done when:**

- Sequential multi-approver approval works as ordered single-assignee tasks.
- Existing single-approver strategies remain green.
- No parallel or multi-instance behavior leaks into P3.

---

## Task 5: P3 Final Acceptance

**Goal:** Close P3 as a coherent delivery stage and make P4 boundaries explicit.

**Files:**

- Create: `docs/superpowers/specs/2026-07-10-bpm-p3-acceptance.md`

**Verification bundle:**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin '-Dtest=BpmFlowableCompatibilityTest' test
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

**Acceptance record must include:**

- P3 scope that passed.
- P3 scope intentionally deferred.
- Live acceptance evidence file names.
- Backend/frontend gate commands and results.
- Explicit statement that P3 did not add dependencies.
- Explicit statement whether P3 added SQL; expected answer is no unless a prior task proved otherwise.
- P4 boundary list:
  - parallel countersign
  - or-sign
  - approval ratio
  - Flowable multi-instance
  - gateways and branching
  - subprocesses
  - expression/script approvers
  - posts, user groups, dynamic department members
  - form field permission system

**Done when:**

- P3 has one final acceptance note in `docs/superpowers/specs/`.
- No runtime evidence is accidentally committed.
- `git status --short` is understood before any commit/stage action.

---

## Suggested Commit Batches

- `docs: 增加 BPM P3 总体任务计划`
- `test: 补齐 BPM P3 候选策略活体验收`
- `feat: 强化 BPM 发起时审批人校验`
- `feat: 增加 BPM 候选策略预检`
- `feat: 支持 BPM 顺序多人审批`
- `docs: 记录 BPM P3 收官验收`

Keep implementation commits scoped by slice. Do not mix runtime evidence, generated logs, browser profiles, or unrelated backend-foundation work into P3 commits.

## Fastest Next Action

Start with Task 1:

```text
P3.1 Candidate Strategy Live Acceptance
```

This is the fastest path because it turns already implemented P3.1a/b/c source behavior into live proof before opening new code surfaces.
