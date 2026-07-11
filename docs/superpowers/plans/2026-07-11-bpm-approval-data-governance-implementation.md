# BPM Approval Data Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build one closed-loop BPM approval data governance module covering node field permissions, published authorization snapshots, authorized task edits, optimistic data versions, field-level audit, resubmission continuity, and reliable business write-back.

**Architecture:** Keep permissions inside the existing Simple Model `userTask` DSL and freeze them into definition-node snapshots. A dedicated runtime mutation service owns schema-aware patch validation, instance data versioning, and change audit; `BpmTaskService` and `BpmInstanceService` orchestrate it in their existing transactions. Employee task APIs return server-trimmed form contexts, while business modules receive the final frozen snapshot through the existing callback boundary.

**Tech Stack:** Java 17, Spring Boot, MyBatis Plus, Fastjson, Flowable 7.2.0, MySQL, Vue 3, TypeScript, Element Plus, form-create, Vitest, Maven.

## Global Constraints

- Implement the whole module as one delivery block; task boundaries below are execution and review boundaries, not new design approvals.
- Do not add dependencies.
- Keep Flowable objects and IDs behind Hunyuan BPM contracts.
- Old definitions without `fieldPermissions` default every schema field to `READONLY`.
- `parallelAll` nodes may use `READONLY` and `HIDDEN`, but publication must reject `EDITABLE`.
- Only an approve action may atomically submit task field changes in this version.
- Reject, return, transfer, delegate, add-sign, and reduce-sign never mutate form data.
- All production code, SQL, tests, docs, and acceptance records remain inside `E:/my-project/hunyuan-pro`.
- Preserve unrelated working-tree changes and do not commit browser profiles, screenshots, sessions, or request logs.

---

## File Map

### Backend additions

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmFieldPermissionEnum.java`: authoritative permission values.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmFormDataChangeSourceEnum.java`: audit source values.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmFormDataChangeEntity.java`: audit persistence model.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmFormDataChangeDao.java`: audit writes and instance queries.
- `hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmFormDataChangeMapper.xml`: ordered instance audit query.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmFieldPermissionVO.java`: public field authorization.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmTaskFormContextVO.java`: trimmed schema, data, version, permissions.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmFormDataChangeVO.java`: trace change record.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskFormContextService.java`: snapshot lookup and employee-safe trimming.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmRuntimeFormDataValidator.java`: shared field allowlist, required-value and supported basic-type validation.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmFormDataMutationService.java`: patch validation, merge, version and audit.
- `数据库SQL脚本/mysql/sql-update-log/v3.45.0.sql`: version column, audit table, sample expense result columns.

### Backend modifications

- `SimpleModelValidator.java` and `BpmSimpleModelPublishValidator.java`: permission structure and schema validation.
- `SimpleModelBpmnCompiler.java`: preserve permissions in compiled node snapshots.
- `BpmDefinitionService.java`: surface permission validation findings during publication.
- `BpmInstanceEntity.java`, `BpmInstanceMapper.xml`, `BpmInstanceService.java`: instance version lifecycle and resubmit diff.
- `BpmTaskApproveForm.java`, `BpmTaskDetailVO.java`, `BpmTaskService.java`: task form context and atomic approve patch.
- `BpmInstanceTraceVO.java`, `BpmInstanceTraceService.java`: field change history.
- `BpmBusinessResultEvent.java` and callback event construction: frozen final data payload.
- `BpmSampleExpenseEntity.java`, `BpmSampleExpenseVO.java`, `BpmSampleExpenseService.java`: approved amount and final version write-back.

### Frontend modifications/additions

- `components/bpm/adapters/types.ts`: field permission draft types.
- `components/bpm/adapters/bpm-process-designer-adapter.vue`: node field permission matrix.
- `components/bpm/adapters/bpm-designer-adapters.test.ts`: round-trip and validation contracts.
- `api/system/bpm/runtime.ts`: task form context, patch, version, audit contracts.
- `views/system/bpm/runtime/components/bpm-runtime-form-rules.ts`: per-field permission transformation.
- `views/system/bpm/runtime/components/bpm-runtime-form-renderer.vue`: field-level hidden/disabled/required rendering.
- `views/system/bpm/runtime/components/bpm-task-form-workbench.vue`: task-bound form editor and change summary.
- `views/system/bpm/runtime/my-todo-list.vue`: workbench integration and atomic approve payload.
- `views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue`: field change timeline.
- Existing BPM Vitest contract files: API, page, designer and runtime rules coverage.

---

### Task 1: Persist the Form Data Version and Change Ledger

**Files:**
- Create: `数据库SQL脚本/mysql/sql-update-log/v3.45.0.sql`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmFormDataChangeSourceEnum.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmFormDataChangeEntity.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmFormDataChangeDao.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmFormDataChangeMapper.xml`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmInstanceEntity.java`

**Interfaces:**
- Produces: `BpmFormDataChangeDao.queryByInstanceId(Long)` ordered by `change_id`.
- Produces: `BpmInstanceEntity.formDataVersion: Long`.
- Produces: source values `INSTANCE_STARTED`, `TASK_APPROVED`, `INSTANCE_RESUBMITTED`.

- [ ] **Step 1: Reserve migration version and write the failing schema contract**

Add a focused test or extend the BPM SQL contract in `bpm-modules.test.ts` to assert that `v3.45.0.sql` contains `form_data_version`, `t_bpm_form_data_change`, `approved_amount`, and `final_form_data_version`.

- [ ] **Step 2: Run the SQL contract and verify RED**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: FAIL because `v3.45.0.sql` or the required columns do not exist.

- [ ] **Step 3: Add the migration and persistence types**

The migration must use this shape:

```sql
ALTER TABLE t_bpm_instance
    ADD COLUMN form_data_version BIGINT NOT NULL DEFAULT 1 COMMENT '当前表单数据版本' AFTER current_form_data_snapshot_json;

CREATE TABLE t_bpm_form_data_change (
    change_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '变更ID',
    instance_id BIGINT NOT NULL COMMENT 'Hunyuan流程实例ID',
    task_id BIGINT NULL COMMENT 'Hunyuan流程任务ID',
    definition_node_id BIGINT NULL COMMENT '定义节点快照ID',
    node_key_snapshot VARCHAR(128) NULL COMMENT '节点标识快照',
    change_source VARCHAR(32) NOT NULL COMMENT 'INSTANCE_STARTED/TASK_APPROVED/INSTANCE_RESUBMITTED',
    actor_employee_id BIGINT NOT NULL COMMENT '操作员工ID',
    actor_name_snapshot VARCHAR(100) NOT NULL COMMENT '操作员工姓名快照',
    before_version BIGINT NOT NULL COMMENT '修改前版本，首次发起为0',
    after_version BIGINT NOT NULL COMMENT '修改后版本',
    changed_fields_json JSON NOT NULL COMMENT '实际变化字段key数组',
    before_values_json JSON NOT NULL COMMENT '变化字段修改前值',
    after_values_json JSON NOT NULL COMMENT '变化字段修改后值',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (change_id),
    KEY idx_bpm_form_change_instance (instance_id, change_id),
    KEY idx_bpm_form_change_task (task_id)
) COMMENT='BPM表单数据变更记录';

ALTER TABLE t_bpm_sample_expense
    ADD COLUMN approved_amount DECIMAL(18, 2) NULL COMMENT '审批核定金额',
    ADD COLUMN final_form_data_version BIGINT NULL COMMENT '最终流程表单数据版本';
```

If the repository's existing MySQL version or JSON conventions differ, use `LONGTEXT` for the three JSON columns while keeping the same Java contract.

- [ ] **Step 4: Implement the entity, enum, DAO and mapper**

`BpmFormDataChangeSourceEnum` exposes stable string codes. The DAO method signature is:

```java
List<BpmFormDataChangeEntity> queryByInstanceId(Long instanceId);
```

- [ ] **Step 5: Run the schema contract and compile**

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -DskipTests compile
```

Expected: PASS and `BUILD SUCCESS`.

- [ ] **Step 6: Commit the persistence foundation**

```powershell
git add -- '数据库SQL脚本/mysql/sql-update-log/v3.45.0.sql' 'hunyuan-backend/hunyuan-bpm/src/main'
git commit -m "feat(bpm): 建立审批数据版本与变更账本"
```

### Task 2: Validate and Freeze Node Field Permissions

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmFieldPermissionEnum.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/BpmSimpleModelPublishValidator.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompiler.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/BpmDefinitionService.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/BpmSimpleModelPublishValidatorTest.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelBpmnCompilerTest.java`

**Interfaces:**
- Consumes: `simpleModelJson` node property `fieldPermissions`.
- Produces: compiled node snapshots preserving identical permission objects for single and expanded sequential tasks.
- Produces: blocking finding codes listed in the design.

- [ ] **Step 1: Write failing publication tests**

Cover valid readonly/editable/hidden rules, missing schema field, duplicate field, required non-editable, no form, and editable `parallelAll`. Add a compiler assertion that every expanded sequential node preserves the authored permissions.

- [ ] **Step 2: Verify RED**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmSimpleModelPublishValidatorTest,SimpleModelBpmnCompilerTest' test
```

Expected: FAIL on missing permission validation and snapshot preservation.

- [ ] **Step 3: Implement strict permission parsing**

Use the structured Fastjson tree already parsed by `BpmSimpleModelPublishValidator`. The validator must reject invalid values, never normalize an unknown value to readonly, and default absent fields only at runtime.

The permission enum contract is:

```java
public enum BpmFieldPermissionEnum {
    READONLY,
    EDITABLE,
    HIDDEN
}
```

- [ ] **Step 4: Preserve permissions in compiled snapshots**

Copy `fieldPermissions` into every compiled task snapshot. Sequential expansion copies the same authored list; `parallelAll` member snapshots copy only already-validated readonly/hidden rules.

- [ ] **Step 5: Run publication and compiler tests**

Run the command from Step 2. Expected: PASS.

- [ ] **Step 6: Commit publication governance**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main hunyuan-backend/hunyuan-bpm/src/test
git commit -m "feat(bpm): 冻结节点字段权限发布契约"
```

### Task 3: Build Authorized Task Form Context and Atomic Mutation

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmFieldPermissionVO.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmTaskFormContextVO.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskFormContextService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmRuntimeFormDataValidator.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmFormDataMutationService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskApproveForm.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmTaskDetailVO.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskFormContextServiceTest.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeFormDataValidatorTest.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmFormDataMutationServiceTest.java`
- Test: create `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmInstanceServiceTest.java` if it does not exist.
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskServiceTest.java`

**Interfaces:**
- Produces: `BpmTaskFormContextService.buildForEmployeeTask(BpmTaskEntity, BpmInstanceEntity): BpmTaskFormContextVO`.
- Produces: `BpmFormDataMutationService.applyTaskApprovePatch(...)` returning `MutationResult(changed, afterVersion, mergedFormDataJson)`.
- Consumes: `BpmTaskApproveForm.formDataVersion` and `formDataPatchJson`.

- [ ] **Step 1: Write failing security and transaction tests**

Cover hidden-field removal from schema and data, readonly default for old definitions, corrupt permission snapshot fail-closed behavior, editable patch success, readonly/hidden/unknown patch rejection, required and supported basic-type validation, stale version rejection, no-change patch, initial start version/audit, and audit-write failure rollback through `BpmTaskService`.

- [ ] **Step 2: Verify RED**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskFormContextServiceTest,BpmRuntimeFormDataValidatorTest,BpmFormDataMutationServiceTest,BpmInstanceServiceTest,BpmTaskServiceTest' test
```

Expected: FAIL because services and approve fields do not exist.

- [ ] **Step 3: Implement employee-safe form context**

`buildForEmployeeTask` must:

1. Read the frozen definition and node snapshot associated with the task; if the permission snapshot is missing or corrupt, return no employee form context and reject mutation.
2. Build an explicit permission map for every schema field, defaulting missing entries to `READONLY`.
3. Deep-filter nested schema `fields`/`children` structures.
4. Remove hidden and schema-unknown keys from returned data.
5. Return the instance's current version.

- [ ] **Step 4: Implement mutation service**

Use a typed result:

```java
public record MutationResult(boolean changed, Long afterVersion, String mergedFormDataJson) {}
```

Reject stale versions with the stable message/code `FORM_DATA_VERSION_CONFLICT`. Compare JSON values structurally, not by raw string order. Insert `TASK_APPROVED` audit only when at least one value changed.

- [ ] **Step 5: Establish the start-data baseline**

Use `BpmRuntimeFormDataValidator` in the existing start path. On successful instance creation set `formDataVersion=1` and insert one `INSTANCE_STARTED` record with `beforeVersion=0`, `afterVersion=1`, and the accepted initial fields. Invalid JSON objects, schema-unknown fields, missing required values, invalid employee selections, and supported basic-type mismatches must fail before Flowable starts.

- [ ] **Step 6: Integrate the existing approve transaction**

Call the mutation service after task/instance authorization and locks, but before completing the Flowable task. Keep the current group action behavior, copies and action logs intact. Requests without a patch follow the old path.

- [ ] **Step 7: Run focused tests and full BPM tests**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskFormContextServiceTest,BpmRuntimeFormDataValidatorTest,BpmFormDataMutationServiceTest,BpmInstanceServiceTest,BpmTaskServiceTest,BpmApprovalGroupServiceTest' test
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

Expected: PASS and `BUILD SUCCESS`.

- [ ] **Step 8: Commit runtime authorization**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main hunyuan-backend/hunyuan-bpm/src/test
git commit -m "feat(bpm): 原子提交授权审批数据"
```

### Task 4: Add Designer Permissions and the Employee Approval Workbench

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-renderer.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-task-form-workbench.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-todo-list.vue`
- Test: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts`
- Test: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

**Interfaces:**
- Consumes: backend `BpmTaskFormContext` and `BpmFieldPermission` contracts.
- Produces: approve payload `{ taskId, commentText, copyEmployeeIds, formDataVersion, formDataPatchJson }`.
- Produces: designer node `fieldPermissions` round-trip.

- [ ] **Step 1: Write failing TypeScript contracts**

Add exact types:

```ts
export type BpmFieldPermissionMode = 'EDITABLE' | 'HIDDEN' | 'READONLY';

export interface BpmFieldPermission {
  fieldKey: string;
  permission: BpmFieldPermissionMode;
  required: boolean;
}
```

Test round-trip serialization, parallel editable rejection, hidden nested rule removal, readonly disabling, required editable rules, and approve payload forwarding.

- [ ] **Step 2: Verify frontend RED**

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: FAIL on missing field permission and task form contracts.

- [ ] **Step 3: Implement the node permission matrix**

Derive field options from the existing `formSchemaJson` passed into the designer adapter. Persist only explicit rows. When mode changes to `parallelAll`, retain readonly/hidden rows and surface editable rows as validation errors instead of silently rewriting authored intent.

- [ ] **Step 4: Extend runtime rule normalization**

Add a permission map argument. Recursively remove hidden rules, set `props.disabled=true` for readonly rules, and merge `required` validation only for editable fields. Do not mutate the published raw schema object.

- [ ] **Step 5: Implement the task workbench**

The component accepts `formContext`, keeps the loaded data/version immutable as its baseline, emits a JSON object patch containing only changed editable fields, and exposes `submitPatch()` for the approve flow. Reject/return dialogs do not call it.

- [ ] **Step 6: Integrate approve without changing other actions**

When `actionForm.type === 'approve'`, collect the workbench patch and version. Preserve existing comment and copy behavior. On `FORM_DATA_VERSION_CONFLICT`, reload detail and keep a visible conflict message; do not auto-merge.

- [ ] **Step 7: Run frontend gates**

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: all tests PASS and typecheck exits `0`.

- [ ] **Step 8: Commit the designer and workbench**

```powershell
git add hunyuan-design/apps/hunyuan-system/src
git commit -m "feat(bpm): 增加节点字段权限与审批工作台"
```

### Task 5: Preserve Data History Through Resubmission and Trace

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmFormDataChangeVO.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmInstanceResubmitForm.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmRuntimeStartDraftVO.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceTraceVO.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceTraceService.java`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/start-form.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmInstanceTraceServiceTest.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmInstanceServiceTest.java`

**Interfaces:**
- Consumes: resubmit `formDataVersion`.
- Produces: draft `formDataVersion` and trace `formDataChanges`.

- [ ] **Step 1: Write failing resubmit and trace tests**

Cover latest-current-snapshot draft, stale resubmit version rejection, changed resubmit version increment, unchanged resubmit version retention, `INSTANCE_RESUBMITTED` audit, and ordered trace changes.

- [ ] **Step 2: Verify RED**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmInstanceServiceTest,BpmInstanceTraceServiceTest' test
```

Expected: FAIL on missing version and audit behavior.

- [ ] **Step 3: Implement resubmit continuity**

Load and lock the instance, compare the submitted version, structurally diff full resubmit data against `currentFormDataSnapshotJson`, update only on valid schema data, insert audit, then start the replacement Flowable instance. If engine start fails, data and audit roll back.

- [ ] **Step 4: Add trace contracts and labels**

Map audit entities to `BpmFormDataChangeVO`. Include frozen field labels where available; frontend falls back to field key. The trace must not expose hidden values through the employee task endpoint; instance trace remains behind its existing admin/runtime authorization.

- [ ] **Step 5: Update resubmit UI**

Forward the loaded version with the full resubmit form. On conflict reload the latest draft and require explicit resubmission.

- [ ] **Step 6: Run backend and frontend gates**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmInstanceServiceTest,BpmInstanceTraceServiceTest' test
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS.

- [ ] **Step 7: Commit history and trace**

```powershell
git add hunyuan-backend/hunyuan-bpm hunyuan-design/apps/hunyuan-system/src
git commit -m "feat(bpm): 贯通审批数据版本与变更追踪"
```

### Task 6: Close Reliable Final Data Write-Back and Total Acceptance

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/business/domain/BpmBusinessResultEvent.java`
- Modify: callback event construction under `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/domain/entity/BpmSampleExpenseEntity.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/domain/vo/BpmSampleExpenseVO.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/service/BpmSampleExpenseService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/service/BpmSampleExpenseDefinitionSeedService.java`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/sample-expense.ts`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessCallbackExecutorTest.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/sampleexpense/BpmSampleExpenseServiceTest.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/sampleexpense/BpmSampleExpenseDefinitionSeedServiceTest.java`
- Create after verification: `docs/superpowers/specs/2026-07-11-bpm-approval-data-governance-acceptance.md`
- Modify after verification: `docs/superpowers/specs/2026-07-10-bpm-development-baseline.md`

**Interfaces:**
- Produces: result event `finalFormDataVersion`, `finalFormDataJson`, `formDataLastModifiedAt`.
- Consumes: sample expense `approvedAmount` from the final frozen form data.

- [ ] **Step 1: Write failing final-event and idempotency tests**

Verify the callback payload freezes final data, retry reuses the same payload, sample expense stores `approvedAmount` and version, duplicate same event is idempotent, and conflicting terminal data is rejected.

- [ ] **Step 2: Verify RED**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmBusinessCallbackExecutorTest,BpmSampleExpenseServiceTest,BpmSampleExpenseDefinitionSeedServiceTest' test
```

Expected: FAIL on missing final data fields.

- [ ] **Step 3: Freeze final data in the result event**

Build the event from the locked/finished instance and persist that serialized event in the existing callback record. Retry must execute the stored request payload, not regenerate it from the instance.

- [ ] **Step 4: Upgrade the sample definition and callback**

Seed a form containing `requestedAmount` and editable `approvedAmount`. Configure the finance node to edit and require `approvedAmount`, then use a later readonly node to prove propagation. The callback writes the final approved amount and data version.

- [ ] **Step 5: Run complete automated gates**

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin '-Dtest=BpmFlowableCompatibilityTest' test
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-runtime-form-rules.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
git diff --check
```

Expected: all commands PASS; `git diff --check` has no output.

- [ ] **Step 6: Run real API/browser acceptance**

Using the persistent Playwright MCP environment from `docs/playwright-mcp-business-flow-verification.md`, prove in one live flow:

1. Configure field permissions and publish the sample definition.
2. Start an expense with requested amount.
3. Confirm hidden values are absent from employee task network responses.
4. Modify approved amount and approve with the current version.
5. Confirm the next sequential task sees the new value.
6. Submit a stale-version probe and receive the expected business error.
7. Complete the process and inspect field changes in trace.
8. Force callback failure, retry it, and confirm the expense stores the same approved amount and final version.
9. Run a return/resubmit flow and confirm continuous version history.
10. Confirm a `parallelAll` definition with editable permission cannot publish.

Store screenshots and request logs under `G:/code-mcp/playwright-mcp-temp/runtime`, not in the repository.

- [ ] **Step 7: Write one total acceptance record and update the baseline**

The acceptance record must separate current facts, commands/results from this run, live evidence, and remaining non-goals. Update the baseline only after every completion criterion passes.

- [ ] **Step 8: Commit the final integration and closure docs**

```powershell
git add hunyuan-backend/hunyuan-bpm hunyuan-design/apps/hunyuan-system/src docs/superpowers/specs/2026-07-11-bpm-approval-data-governance-acceptance.md docs/superpowers/specs/2026-07-10-bpm-development-baseline.md
git commit -m "feat(bpm): 闭环审批数据治理模块"
```

---

## Plan Self-Review

- Spec coverage: design, publication, runtime authorization, mutation, audit, resubmission, trace, business callback, compatibility, and live acceptance each map to a task.
- Scope: all tasks contribute to one module; no conditional routing, collaborative parallel editing, or generic low-code data source work is included.
- Type consistency: backend and frontend both use `READONLY | EDITABLE | HIDDEN`, `formDataVersion`, `formDataPatchJson`, and `formDataChanges`.
- Safety: hidden fields are removed server-side; patch validation is whitelist-based; stale writes fail before Flowable completion.
- Verification: every implementation task has a focused RED/GREEN command, and Task 6 owns the full automated and live acceptance gates.

## Execution Handoff

Execute this plan continuously on the current branch. Use test-driven development at each task boundary, preserve unrelated dirty-tree files, and do not report the module complete until Task 6's total acceptance record and baseline update are backed by fresh command and runtime evidence.
