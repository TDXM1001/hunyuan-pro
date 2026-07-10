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

- [ ] Write failing backend tests for nonexistent selected employee.
- [ ] Write failing backend tests proving resubmit re-parses the latest `formDataJson`.
- [ ] Decide whether publish-time field existence can be validated without broad service coupling.
- [ ] Implement the smallest backend safety change, preferably by reusing `BpmOrgIdentityGateway.requireEmployee`.
- [ ] Preserve existing user-facing messages for blank, array, comma string, and invalid numeric values.
- [ ] Run focused backend gates:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,BpmRuntimeStartAssignmentTest,SimpleModelValidatorTest' test
```

- [ ] Run frontend P3.1c contract gates if any type or API contract changes:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

- [ ] Create `docs/superpowers/specs/2026-07-10-bpm-p3-assignment-safety-acceptance.md`.

**Done when:**

- Invalid or nonexistent selected employees fail before Flowable start.
- Start and resubmit behavior are both covered.
- No SQL or dependency change is introduced.
- Acceptance record explains why any publish-time schema validation was implemented or deferred.

---

