# BPM P3.1a Candidate Strategy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add real Hunyuan-native `START_EMPLOYEE` and `START_DEPARTMENT_MANAGER` BPM candidate strategies across backend runtime assignment, publish validation, and frontend designer snapshots.

**Architecture:** Keep Flowable hidden behind the current Hunyuan BPM assignment boundary. Preserve the existing `BpmTaskAssignmentResolver.resolve(definitionNodes, startEmployeeSnapshot)` signature for P3.1a, add two resolver branches that use the existing start employee snapshot and organization gateway, and wire the same strategy names through the frontend simple model adapter.

**Tech Stack:** Java, Spring, JUnit 5, Mockito, AssertJ, Vue 3, TypeScript, Vitest, Element Plus, Maven, pnpm.

## Global Constraints

- Production code, contracts, tests, routes, permissions, menus, docs, and verification artifacts must stay in `E:/my-project/hunyuan-pro`.
- Yudao frontend and RuoYi backend are reference lines only; borrow mechanisms, not names, routes, page shells, or module boundaries.
- P3.1a implements only `START_EMPLOYEE` and `START_DEPARTMENT_MANAGER`.
- `EMPLOYEE_SELECT_AT_START` is deferred to P3.1b and must not be implemented in this plan.
- Do not add dependencies.
- Keep Flowable and third-party BPM concepts behind Hunyuan BPM boundaries.
- All Chinese docs, test names, error messages, SQL comments, and frontend copy must stay UTF-8.
- P3.1a is expected to require no SQL.
- If SQL becomes necessary, create the next file under `数据库SQL脚本/mysql/sql-update-log/`; do not edit `数据库SQL脚本/mysql/hunyuan.sql` or any historical `v3.xx.0.sql`.
- New behavior must follow TDD: write the failing test, run it and observe the expected failure, then implement the smallest passing code.

---

## File Structure

- Modify `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskAssignmentResolverTest.java`
  - Owns direct resolver behavior for the two new candidate strategies and failure messages.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentResolver.java`
  - Owns runtime conversion from published node snapshots to Flowable `assignee_<nodeKey>` variables.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmCandidateResolverTypeEnum.java`
  - Owns the supported Hunyuan BPM candidate resolver type list.
- Create `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidatorTest.java`
  - Owns focused publish-draft validation for new strategy names and updated validation copy.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java`
  - Owns publish-draft validation and user-facing unsupported strategy message.
- Modify `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeStartAssignmentTest.java`
  - Owns proof that resolved variables from the new strategy reach `FlowableProcessInstanceGateway.start`.
- Modify `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts`
  - Owns frontend simple model candidate type typing.
- Modify `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue`
  - Owns the candidate strategy options shown in the Hunyuan BPM designer.
- Modify `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts`
  - Owns simple model parse/stringify contract coverage for new strategy names.

---

### Task 1: Backend Runtime Resolver Semantics

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskAssignmentResolverTest.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentResolver.java`

**Interfaces:**
- Consumes: `BpmTaskAssignmentResolver.resolve(List<BpmDefinitionNodeEntity>, BpmEmployeeSnapshot)`
- Produces: Existing Flowable variable contract `Map<String, Object>` with entries named `assignee_<nodeKey>` and string employee IDs.

- [ ] **Step 1: Write failing resolver tests**

Add this static import to `BpmTaskAssignmentResolverTest.java`:

```java
import static org.assertj.core.api.Assertions.assertThatThrownBy;
```

Add these tests before `buildNode`:

```java
    @Test
    void resolveShouldUseStartEmployeeWhenNodeUsesStartEmployeeStrategy() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        Map<String, Object> variables = resolver.resolve(
                List.of(buildNode(
                        "task_self",
                        "{\"nodeKey\":\"task_self\",\"name\":\"发起人自审\",\"type\":\"userTask\",\"candidateResolverType\":\"START_EMPLOYEE\"}"
                )),
                startEmployee
        );

        assertThat(variables).containsEntry("assignee_task_self", "100");
    }

    @Test
    void resolveShouldRejectStartEmployeeStrategyWhenStartEmployeeMissing() {
        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_self",
                        "{\"nodeKey\":\"task_self\",\"name\":\"发起人自审\",\"type\":\"userTask\",\"candidateResolverType\":\"START_EMPLOYEE\"}"
                )),
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【发起人自审】未找到发起人");
    }

    @Test
    void resolveShouldUseStartEmployeeDepartmentManagerWhenNodeUsesStartDepartmentManagerStrategy() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);
        when(identityGateway.resolveDepartmentManagerEmployeeId(7L)).thenReturn(200L);

        Map<String, Object> variables = resolver.resolve(
                List.of(buildNode(
                        "task_start_manager",
                        "{\"nodeKey\":\"task_start_manager\",\"name\":\"发起人部门主管审批\",\"type\":\"userTask\",\"candidateResolverType\":\"START_DEPARTMENT_MANAGER\"}"
                )),
                startEmployee
        );

        assertThat(variables).containsEntry("assignee_task_start_manager", "200");
    }

    @Test
    void resolveShouldRejectStartDepartmentManagerStrategyWhenStartDepartmentMissing() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", null, null, null, null);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_start_manager",
                        "{\"nodeKey\":\"task_start_manager\",\"name\":\"发起人部门主管审批\",\"type\":\"userTask\",\"candidateResolverType\":\"START_DEPARTMENT_MANAGER\"}"
                )),
                startEmployee
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【发起人部门主管审批】未找到发起人部门");
    }

    @Test
    void resolveShouldRejectStartDepartmentManagerStrategyWhenManagerMissing() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);
        when(identityGateway.resolveDepartmentManagerEmployeeId(7L)).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_start_manager",
                        "{\"nodeKey\":\"task_start_manager\",\"name\":\"发起人部门主管审批\",\"type\":\"userTask\",\"candidateResolverType\":\"START_DEPARTMENT_MANAGER\"}"
                )),
                startEmployee
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【发起人部门主管审批】未找到发起人部门主管");
    }
```

- [ ] **Step 2: Run the resolver tests and verify RED**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest' test
```

Expected result:

```text
FAILURE
resolveShouldUseStartEmployeeWhenNodeUsesStartEmployeeStrategy
审批节点【发起人自审】未配置指定员工
```

The exact Maven summary can vary, but the failure must prove the resolver does not yet support `START_EMPLOYEE` or `START_DEPARTMENT_MANAGER`.

- [ ] **Step 3: Implement the minimal resolver branches**

In `BpmTaskAssignmentResolver.resolveNodeAssignee`, add these branches after `nodeName` is computed and before the existing `DEPARTMENT_MANAGER` branch:

```java
        if ("START_EMPLOYEE".equalsIgnoreCase(resolverType)) {
            Long employeeId = startEmployeeSnapshot == null ? null : startEmployeeSnapshot.employeeId();
            if (employeeId == null) {
                throw new IllegalArgumentException("审批节点【" + nodeName + "】未找到发起人");
            }
            return employeeId;
        }

        if ("START_DEPARTMENT_MANAGER".equalsIgnoreCase(resolverType)) {
            Long departmentId = startEmployeeSnapshot == null ? null : startEmployeeSnapshot.departmentId();
            if (departmentId == null) {
                throw new IllegalArgumentException("审批节点【" + nodeName + "】未找到发起人部门");
            }
            Long managerEmployeeId = bpmOrgIdentityGateway.resolveDepartmentManagerEmployeeId(departmentId);
            if (managerEmployeeId == null) {
                throw new IllegalArgumentException("审批节点【" + nodeName + "】未找到发起人部门主管");
            }
            return managerEmployeeId;
        }
```

- [ ] **Step 4: Run resolver tests and verify GREEN**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest' test
```

Expected result:

```text
BUILD SUCCESS
```

- [ ] **Step 5: Check SQL boundary before commit**

Run:

```powershell
git diff --name-only -- "数据库SQL脚本"
```

Expected result:

```text

```

- [ ] **Step 6: Commit backend resolver behavior**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskAssignmentResolverTest.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentResolver.java
git commit -m "feat: 支持 BPM 发起人候选解析"
```

---

### Task 2: Backend Validation And Start-Path Contract

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmCandidateResolverTypeEnum.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidatorTest.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeStartAssignmentTest.java`

**Interfaces:**
- Consumes: `SimpleModelValidator.validate(String simpleModelJson, String startRuleJson)`
- Consumes: `BpmInstanceService.startInstance(BpmInstanceStartForm form)`
- Produces: New enum values available through `BpmCandidateResolverTypeEnum.values()`.
- Produces: Validation copy that names all five supported P3.1a strategies.

- [ ] **Step 1: Write failing validator tests**

Create `SimpleModelValidatorTest.java`:

```java
package com.hunyuan.sa.bpm.engine.compiler;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleModelValidatorTest {

    private final SimpleModelValidator validator = new SimpleModelValidator();

    @Test
    void validateShouldAcceptStartEmployeeCandidateStrategies() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":["
                        + "{\"id\":\"task_self\",\"nodeKey\":\"task_self\",\"name\":\"发起人自审\",\"type\":\"userTask\",\"candidateResolverType\":\"START_EMPLOYEE\"},"
                        + "{\"id\":\"task_start_manager\",\"nodeKey\":\"task_start_manager\",\"name\":\"发起人部门主管审批\",\"type\":\"userTask\",\"candidateResolverType\":\"START_DEPARTMENT_MANAGER\"}"
                        + "]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isTrue();
    }

    @Test
    void validateShouldExplainSupportedCandidateStrategiesWhenResolverTypeUnsupported() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"id\":\"task_unknown\",\"nodeKey\":\"task_unknown\",\"name\":\"未知审批\",\"type\":\"userTask\",\"candidateResolverType\":\"USER_GROUP\"}]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg())
                .contains("EMPLOYEE")
                .contains("DEPARTMENT_MANAGER")
                .contains("ROLE")
                .contains("START_EMPLOYEE")
                .contains("START_DEPARTMENT_MANAGER");
    }
}
```

- [ ] **Step 2: Run validator tests and verify RED**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=SimpleModelValidatorTest' test
```

Expected result:

```text
FAILURE
validateShouldAcceptStartEmployeeCandidateStrategies
```

The failure must come from unsupported candidate resolver type, not from JSON parsing or test setup.

- [ ] **Step 3: Add enum values**

Update `BpmCandidateResolverTypeEnum.java` enum constants to this exact list:

```java
    EMPLOYEE("EMPLOYEE", "指定员工"),
    DEPARTMENT_MANAGER("DEPARTMENT_MANAGER", "部门主管"),
    ROLE("ROLE", "角色成员"),
    START_EMPLOYEE("START_EMPLOYEE", "发起人本人"),
    START_DEPARTMENT_MANAGER("START_DEPARTMENT_MANAGER", "发起人部门主管");
```

- [ ] **Step 4: Update unsupported strategy copy**

In `SimpleModelValidator.java`, replace the old unsupported resolver message with:

```java
                return ResponseDTO.userErrorParam("当前只支持 EMPLOYEE、DEPARTMENT_MANAGER、ROLE、START_EMPLOYEE、START_DEPARTMENT_MANAGER 五类候选人解析类型");
```

- [ ] **Step 5: Run validator tests and verify GREEN**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=SimpleModelValidatorTest' test
```

Expected result:

```text
BUILD SUCCESS
```

- [ ] **Step 6: Write failing start-path test**

Add this test to `BpmRuntimeStartAssignmentTest.java` before helper methods:

```java
    @Test
    void startInstanceShouldPassStartEmployeeAssignmentToFlowable() {
        BpmDefinitionEntity definitionEntity = new BpmDefinitionEntity();
        definitionEntity.setDefinitionId(1L);
        definitionEntity.setEngineProcessDefinitionId("leave:1:1000");
        definitionEntity.setDefinitionKey("leave");
        definitionEntity.setDefinitionVersion(1);
        definitionEntity.setCategoryIdSnapshot(7L);
        definitionEntity.setCategoryNameSnapshot("人事流程");
        definitionEntity.setInstanceNoRuleIdSnapshot(1);
        definitionEntity.setLifecycleState(1);
        definitionEntity.setStartState(1);

        BpmDefinitionNodeEntity nodeEntity = new BpmDefinitionNodeEntity();
        nodeEntity.setNodeKey("task_self");
        nodeEntity.setNodeType("userTask");
        nodeEntity.setNodeNameSnapshot("发起人自审");
        nodeEntity.setAuthoredRuleSnapshotJson(
                "{\"nodeKey\":\"task_self\",\"name\":\"发起人自审\",\"type\":\"userTask\",\"candidateResolverType\":\"START_EMPLOYEE\"}"
        );

        when(definitionDao.selectById(1L)).thenReturn(definitionEntity);
        when(definitionNodeDao.selectList(any())).thenReturn(List.of(nodeEntity));
        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(identityGateway().requireEmployee(100L)).thenReturn(new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null));
        when(serialNumberService().generate(any())).thenReturn("SN-2026-0003");
        when(processInstanceGateway.start("leave:1:1000", 100L, "{\"amount\":100}", Map.of("assignee_task_self", "100")))
                .thenReturn("process-1002");
        when(instanceDao.insert(any(BpmInstanceEntity.class))).thenAnswer(invocation -> {
            BpmInstanceEntity entity = invocation.getArgument(0);
            entity.setInstanceId(10L);
            return 1;
        });

        BpmInstanceStartForm form = new BpmInstanceStartForm();
        form.setDefinitionId(1L);
        form.setFormDataJson("{\"amount\":100}");
        form.setTitle("请假申请");

        ResponseDTO<Long> response = service.startInstance(form);

        assertThat(response.getOk()).isTrue();

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(processInstanceGateway).start(
                Mockito.eq("leave:1:1000"),
                Mockito.eq(100L),
                Mockito.eq("{\"amount\":100}"),
                variablesCaptor.capture()
        );
        assertThat(variablesCaptor.getValue()).containsEntry("assignee_task_self", "100");
    }
```

- [ ] **Step 7: Run start-path tests and verify GREEN**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmRuntimeStartAssignmentTest,SimpleModelValidatorTest' test
```

Expected result:

```text
BUILD SUCCESS
```

- [ ] **Step 8: Check UTF-8 for changed backend Chinese files**

Run:

```powershell
$paths = @(
  'hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java',
  'hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidatorTest.java',
  'hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeStartAssignmentTest.java'
)
$utf8 = [System.Text.UTF8Encoding]::new($false, $true)
foreach ($path in $paths) {
  [void]$utf8.GetString([System.IO.File]::ReadAllBytes($path))
  "$path UTF8_STRICT_OK"
}
```

Expected result:

```text
hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java UTF8_STRICT_OK
hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidatorTest.java UTF8_STRICT_OK
hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeStartAssignmentTest.java UTF8_STRICT_OK
```

- [ ] **Step 9: Check SQL boundary before commit**

Run:

```powershell
git diff --name-only -- "数据库SQL脚本"
```

Expected result:

```text

```

- [ ] **Step 10: Commit backend validation and start-path contract**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmCandidateResolverTypeEnum.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidatorTest.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeStartAssignmentTest.java
git commit -m "feat: 校验 BPM 发起人候选策略"
```

---

### Task 3: Frontend Designer Strategy Contract

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue`

**Interfaces:**
- Consumes: `BpmProcessNodeDraft.candidateResolverType`
- Consumes: `parseSimpleModelDraft(jsonText: string)`
- Consumes: `stringifySimpleModelDraft(nodes: BpmProcessNodeDraft[])`
- Produces: Frontend simple model snapshots that preserve `START_EMPLOYEE` and `START_DEPARTMENT_MANAGER`.

- [ ] **Step 1: Write failing frontend adapter test**

Add this type import to `bpm-designer-adapters.test.ts`:

```typescript
import type { BpmProcessNodeDraft } from './types';
```

Add this test to `bpm-designer-adapters.test.ts` before the BPMN XML test:

```typescript
  it('保留发起人相关候选策略的 simpleModelJson 合同', () => {
    const parsedNodes = parseSimpleModelDraft(
      JSON.stringify({
        nodes: [
          {
            approvalMode: 'single',
            candidateResolverType: 'START_EMPLOYEE',
            id: 'task_self',
            listeners: [],
            name: '发起人自审',
            type: 'userTask',
          },
          {
            approvalMode: 'single',
            candidateResolverType: 'START_DEPARTMENT_MANAGER',
            id: 'task_start_manager',
            listeners: [],
            name: '发起人部门主管审批',
            type: 'userTask',
          },
        ],
      }),
    );

    expect(parsedNodes.map((item) => item.candidateResolverType)).toEqual([
      'START_EMPLOYEE',
      'START_DEPARTMENT_MANAGER',
    ]);

    const startDepartmentManagerNode: BpmProcessNodeDraft = {
      approvalMode: 'single',
      candidateResolverType: 'START_DEPARTMENT_MANAGER',
      id: 'task_start_manager',
      listeners: [],
      name: '发起人部门主管审批',
      nodeKey: 'task_start_manager',
      type: 'userTask',
    };

    expect(stringifySimpleModelDraft([startDepartmentManagerNode])).toContain(
      '"candidateResolverType":"START_DEPARTMENT_MANAGER"',
    );
  });
```

- [ ] **Step 2: Run frontend typecheck and verify RED**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected result:

```text
FAIL
Type '"START_DEPARTMENT_MANAGER"' is not assignable
```

The exact TypeScript diagnostic can vary, but the failure must come from `BpmProcessNodeDraft.candidateResolverType` not accepting the new strategy names.

- [ ] **Step 3: Extend frontend type union**

In `types.ts`, change `candidateResolverType` to:

```typescript
  candidateResolverType?:
    | 'DEPARTMENT_MANAGER'
    | 'EMPLOYEE'
    | 'ROLE'
    | 'START_DEPARTMENT_MANAGER'
    | 'START_EMPLOYEE';
```

- [ ] **Step 4: Add designer select options**

In `bpm-process-designer-adapter.vue`, add these options inside the candidate resolver select after the existing `部门负责人` option:

```vue
              <ElOption label="发起人本人" value="START_EMPLOYEE" />
              <ElOption
                label="发起人部门主管"
                value="START_DEPARTMENT_MANAGER"
              />
```

- [ ] **Step 5: Run frontend typecheck and verify GREEN**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected result:

```text
Done
```

- [ ] **Step 6: Run frontend adapter test and verify GREEN**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts
```

Expected result:

```text
Test Files  1 passed
```

- [ ] **Step 7: Check UTF-8 for changed frontend files**

Run:

```powershell
$paths = @(
  'hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts',
  'hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts',
  'hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue'
)
$utf8 = [System.Text.UTF8Encoding]::new($false, $true)
foreach ($path in $paths) {
  [void]$utf8.GetString([System.IO.File]::ReadAllBytes($path))
  "$path UTF8_STRICT_OK"
}
```

Expected result:

```text
hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts UTF8_STRICT_OK
hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts UTF8_STRICT_OK
hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue UTF8_STRICT_OK
```

- [ ] **Step 8: Check SQL boundary before commit**

Run:

```powershell
git diff --name-only -- "数据库SQL脚本"
```

Expected result:

```text

```

- [ ] **Step 9: Commit frontend designer contract**

```powershell
git add hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue
git commit -m "feat: 前端支持 BPM 发起人候选策略"
```

---

### Task 4: Final Verification And SQL Boundary Proof

**Files:**
- Modify: none.
- Test: backend Maven focused tests, frontend Vitest, frontend typecheck, UTF-8 strict reads, SQL diff check.

**Interfaces:**
- Consumes: all changes from Tasks 1-3.
- Produces: final verification evidence that P3.1a is real, UTF-8 safe, and does not require SQL.

- [ ] **Step 1: Run backend focused verification**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,BpmRuntimeStartAssignmentTest,SimpleModelValidatorTest' test
```

Expected result:

```text
BUILD SUCCESS
```

- [ ] **Step 2: Run frontend contract verification**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts
```

Expected result:

```text
Test Files  1 passed
```

- [ ] **Step 3: Run frontend typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected result:

```text
Done
```

- [ ] **Step 4: Prove no SQL changed**

Run:

```powershell
git diff --name-only HEAD~3..HEAD -- "数据库SQL脚本"
```

Expected result:

```text

```

- [ ] **Step 5: Inspect final status**

Run:

```powershell
git status --short
```

Expected result:

```text

```

- [ ] **Step 6: Summarize final evidence**

Final implementation response must include:

```text
后端验证: mvn ... BpmTaskAssignmentResolverTest,BpmRuntimeStartAssignmentTest,SimpleModelValidatorTest => BUILD SUCCESS
前端验证: vitest bpm-designer-adapters.test.ts => passed
类型检查: pnpm -F @hunyuan/system run typecheck => passed
SQL: 未修改 数据库SQL脚本；P3.1a 不涉及增量 SQL
UTF-8: changed Chinese files strict UTF-8 read OK
```
