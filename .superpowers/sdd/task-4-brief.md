## Task 4: P3.4 Sequential Multi-Approver Approval

**Goal:** Support the lowest-risk multi-approver use case by compiling one authored sequential approval node into multiple ordered single-assignee tasks.

**Decision checkpoint before implementation:**

- [ ] Confirm P3.1 live acceptance passed.
- [ ] Confirm P3.2 safeguards passed.
- [ ] Confirm P3.3 precheck either passed or was explicitly deferred.
- [ ] Confirm the product decision is still sequential-only, not parallel countersign or ratio approval.

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

- [ ] Write compiler tests proving sequential node expansion order.
- [ ] Write validation tests for invalid `employeeIds`.
- [ ] Write runtime start tests proving each expanded node receives an `assignee_<expandedNodeKey>` variable.
- [ ] Update frontend designer to allow sequential explicit employee list only.
- [ ] Add trace/detail explanation if needed.
- [ ] Run focused backend and frontend gates.
- [ ] Run at least one API or browser live acceptance proving tasks appear one after another.
- [ ] Create `docs/superpowers/specs/2026-07-10-bpm-p3-sequential-approval-acceptance.md`.

**Done when:**

- Sequential multi-approver approval works as ordered single-assignee tasks.
- Existing single-approver strategies remain green.
- No parallel or multi-instance behavior leaks into P3.

---

