# BPM P3.1b Employee Select At Start Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Hunyuan-native `EMPLOYEE_SELECT_AT_START` so a BPM user task assignee can be selected from the submitted start form data before Flowable creates the task.

**Architecture:** Extend the existing assignment resolver with a small runtime context carrying the start employee snapshot and `formDataJson`. Keep the old resolver method as a compatibility delegate, wire start/resubmit to the new context, and let the frontend save one field key on the node draft. This stays inside Hunyuan BPM contracts and continues to produce `assignee_<nodeKey>` Flowable variables.

**Tech Stack:** Java 17, Spring Boot, Flowable 7.2.0, MyBatis-Plus, JUnit 5, Mockito, AssertJ, Vue 3, TypeScript, Element Plus, Vitest.

## Global Constraints

- All production code stays in `E:/my-project/hunyuan-pro`.
- Yudao/RuoYi are reference lines only; do not migrate their enums, APIs, routes, page shells, or module boundaries.
- Do not add dependencies.
- Do not add SQL; this slice has no schema, menu, permission, or seed data changes.
- Keep all Chinese docs, errors, labels, and test names UTF-8.
- Support only one employee ID, not arrays, comma strings, users groups, roles, posts, expressions, or multi-instance.
- Do not expose Flowable native objects or native IDs through Hunyuan public contracts.
- Preserve existing `resolve(definitionNodes, startEmployeeSnapshot)` behavior by delegating to the new context method.

---

## File Structure

- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentContext.java`
  - Holds the start employee snapshot and start form JSON for assignment resolution.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmCandidateResolverTypeEnum.java`
  - Adds `EMPLOYEE_SELECT_AT_START`.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentResolver.java`
  - Adds context-based resolve overload and field-based employee parsing.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java`
  - Allows the new resolver type and requires a field key for it.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
  - Passes `formDataJson` to the resolver for start and resubmit.
- Modify backend tests:
  - `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskAssignmentResolverTest.java`
  - `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeStartAssignmentTest.java`
  - `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidatorTest.java`
- Modify frontend files:
  - `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts`
  - `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/simple-model-bridge.ts`
  - `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue`
  - `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts`

---

### Task 1: Backend Assignment Context And Resolver

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentContext.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmCandidateResolverTypeEnum.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentResolver.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskAssignmentResolverTest.java`

**Interfaces:**
- Consumes: existing `BpmEmployeeSnapshot`, `BpmDefinitionNodeEntity`, and old `resolve(List<BpmDefinitionNodeEntity>, BpmEmployeeSnapshot)`.
- Produces:
  - `public record BpmTaskAssignmentContext(BpmEmployeeSnapshot startEmployeeSnapshot, String formDataJson)`
  - `public Map<String, Object> resolve(List<BpmDefinitionNodeEntity> definitionNodes, BpmTaskAssignmentContext context)`
  - enum value `EMPLOYEE_SELECT_AT_START("EMPLOYEE_SELECT_AT_START", "发起时自选审批人")`

- [ ] **Step 1: Write failing resolver tests**

Add these imports to `BpmTaskAssignmentResolverTest.java`:

```java
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskAssignmentContext;
```

Add these tests before `buildNode`:

```java
    @Test
    void resolveShouldUseEmployeeSelectedFromStartFormData() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        Map<String, Object> variables = resolver.resolve(
                List.of(buildNode(
                        "task_selected",
                        "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
                )),
                new BpmTaskAssignmentContext(startEmployee, "{\"approverEmployeeId\":301}")
        );

        assertThat(variables).containsEntry("assignee_task_selected", "301");
    }

    @Test
    void resolveShouldUseStringEmployeeSelectedFromStartFormData() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        Map<String, Object> variables = resolver.resolve(
                List.of(buildNode(
                        "task_selected",
                        "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
                )),
                new BpmTaskAssignmentContext(startEmployee, "{\"approverEmployeeId\":\"302\"}")
        );

        assertThat(variables).containsEntry("assignee_task_selected", "302");
    }

    @Test
    void resolveShouldRejectEmployeeSelectAtStartWhenFieldKeyMissing() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_selected",
                        "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\"}"
                )),
                new BpmTaskAssignmentContext(startEmployee, "{\"approverEmployeeId\":301}")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【发起时选择审批】未配置发起时自选审批人字段");
    }

    @Test
    void resolveShouldRejectEmployeeSelectAtStartWhenFormFieldMissing() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_selected",
                        "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
                )),
                new BpmTaskAssignmentContext(startEmployee, "{\"amount\":100}")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【发起时选择审批】未找到发起时自选审批人");
    }

    @Test
    void resolveShouldRejectEmployeeSelectAtStartWhenValueIsArray() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_selected",
                        "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
                )),
                new BpmTaskAssignmentContext(startEmployee, "{\"approverEmployeeId\":[301,302]}")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【发起时选择审批】发起时自选审批人无效");
    }

    @Test
    void resolveShouldRejectEmployeeSelectAtStartWhenValueIsCommaString() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_selected",
                        "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
                )),
                new BpmTaskAssignmentContext(startEmployee, "{\"approverEmployeeId\":\"301,302\"}")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【发起时选择审批】发起时自选审批人无效");
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest' test
```

Expected: compile failure because `BpmTaskAssignmentContext` does not exist and `resolve(..., BpmTaskAssignmentContext)` is not defined.

- [ ] **Step 3: Add context record**

Create `BpmTaskAssignmentContext.java`:

```java
package com.hunyuan.sa.bpm.module.runtime.service;

import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;

/**
 * BPM 任务候选人解析上下文。
 */
public record BpmTaskAssignmentContext(
        BpmEmployeeSnapshot startEmployeeSnapshot,
        String formDataJson
) {
}
```

- [ ] **Step 4: Add enum value**

In `BpmCandidateResolverTypeEnum.java`, add the value after `START_DEPARTMENT_MANAGER`:

```java
    START_DEPARTMENT_MANAGER("START_DEPARTMENT_MANAGER", "发起人部门主管"),
    EMPLOYEE_SELECT_AT_START("EMPLOYEE_SELECT_AT_START", "发起时自选审批人");
```

If the file currently ends the previous value with `;`, change that semicolon to a comma and put the semicolon after the new value.

- [ ] **Step 5: Implement resolver overload and parsing**

In `BpmTaskAssignmentResolver.java`, change the old method to delegate:

```java
    public Map<String, Object> resolve(List<BpmDefinitionNodeEntity> definitionNodes, BpmEmployeeSnapshot startEmployeeSnapshot) {
        return resolve(definitionNodes, new BpmTaskAssignmentContext(startEmployeeSnapshot, "{}"));
    }

    public Map<String, Object> resolve(List<BpmDefinitionNodeEntity> definitionNodes, BpmTaskAssignmentContext context) {
        Map<String, Object> variables = new HashMap<>();
        if (definitionNodes == null || definitionNodes.isEmpty()) {
            return variables;
        }

        BpmTaskAssignmentContext safeContext = context == null
                ? new BpmTaskAssignmentContext(null, "{}")
                : context;
        definitionNodes.stream()
                .filter(node -> "userTask".equals(node.getNodeType()))
                .sorted(Comparator.comparing(BpmDefinitionNodeEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .forEach(node -> {
                    Long assigneeEmployeeId = resolveNodeAssignee(node, safeContext);
                    variables.put("assignee_" + node.getNodeKey(), String.valueOf(assigneeEmployeeId));
                });
        return variables;
    }
```

Change `resolveNodeAssignee` signature and snapshot reads:

```java
    private Long resolveNodeAssignee(BpmDefinitionNodeEntity node, BpmTaskAssignmentContext context) {
        JSONObject nodeObject = parseNodeObject(node);
        BpmEmployeeSnapshot startEmployeeSnapshot = context.startEmployeeSnapshot();
        String resolverType = firstNonBlank(
                nodeObject.getString("candidateResolverType"),
                nodeObject.getString("resolverType")
        );
        String nodeName = firstNonBlank(nodeObject.getString("name"), node.getNodeNameSnapshot(), node.getNodeKey());
```

Add this branch after the `START_DEPARTMENT_MANAGER` branch and before `DEPARTMENT_MANAGER`:

```java
        if ("EMPLOYEE_SELECT_AT_START".equalsIgnoreCase(resolverType)) {
            String fieldKey = firstNonBlank(
                    nodeObject.getString("employeeSelectFieldKey"),
                    nodeObject.getString("candidateFieldKey"),
                    nodeObject.getString("assigneeFieldKey")
            );
            if (StringUtils.isBlank(fieldKey)) {
                throw new IllegalArgumentException("审批节点【" + nodeName + "】未配置发起时自选审批人字段");
            }
            Long employeeId = readEmployeeIdFromFormData(context.formDataJson(), fieldKey);
            if (employeeId == null) {
                throw new IllegalArgumentException("审批节点【" + nodeName + "】未找到发起时自选审批人");
            }
            if (employeeId <= 0) {
                throw new IllegalArgumentException("审批节点【" + nodeName + "】发起时自选审批人无效");
            }
            return employeeId;
        }
```

Add helper methods near the other private helpers:

```java
    private Long readEmployeeIdFromFormData(String formDataJson, String fieldKey) {
        JSONObject formDataObject = parseFormDataObject(formDataJson);
        Object rawValue = formDataObject.get(fieldKey);
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Number numberValue) {
            return numberValue.longValue();
        }
        if (rawValue instanceof JSONArray || rawValue instanceof Iterable<?>) {
            return -1L;
        }
        String text = String.valueOf(rawValue).trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.contains(",")) {
            return -1L;
        }
        Long value = parseLong(text);
        return value == null ? -1L : value;
    }

    private JSONObject parseFormDataObject(String formDataJson) {
        if (StringUtils.isBlank(formDataJson)) {
            return new JSONObject();
        }
        try {
            JSONObject formDataObject = JSON.parseObject(formDataJson);
            return formDataObject == null ? new JSONObject() : formDataObject;
        } catch (Exception ex) {
            return new JSONObject();
        }
    }
```

- [ ] **Step 6: Run resolver tests to verify pass**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest' test
```

Expected: PASS, including the new `EMPLOYEE_SELECT_AT_START` tests.

- [ ] **Step 7: Commit Task 1**

Run:

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmCandidateResolverTypeEnum.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentContext.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskAssignmentResolver.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskAssignmentResolverTest.java
git commit -m "feat: 支持 BPM 发起时自选审批人解析"
```

Expected: commit succeeds with only Task 1 files.

---

### Task 2: Validator And Runtime Start Wiring

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidatorTest.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeStartAssignmentTest.java`

**Interfaces:**
- Consumes: `BpmTaskAssignmentContext` and resolver overload from Task 1.
- Produces: start and resubmit paths that pass current `formDataJson` into assignment resolution.

- [ ] **Step 1: Write failing validator tests**

In `SimpleModelValidatorTest.java`, update unsupported strategy assertion to include the new type:

```java
                .contains("START_DEPARTMENT_MANAGER")
                .contains("EMPLOYEE_SELECT_AT_START");
```

Add tests:

```java
    @Test
    void validateShouldAcceptEmployeeSelectAtStartWhenFieldKeyConfigured() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"id\":\"task_selected\",\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isTrue();
    }

    @Test
    void validateShouldRejectEmployeeSelectAtStartWhenFieldKeyMissing() {
        ResponseDTO<String> response = validator.validate(
                "{\"nodes\":[{\"id\":\"task_selected\",\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\"}]}",
                "{\"type\":\"ALL\"}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("发起时自选审批人字段");
    }
```

- [ ] **Step 2: Write failing runtime start test**

In `BpmRuntimeStartAssignmentTest.java`, add this test before helper methods:

```java
    @Test
    void startInstanceShouldPassEmployeeSelectedFromStartFormDataToFlowable() {
        BpmDefinitionEntity definitionEntity = new BpmDefinitionEntity();
        definitionEntity.setDefinitionId(1L);
        definitionEntity.setEngineProcessDefinitionId("expense:1:1000");
        definitionEntity.setDefinitionKey("expense");
        definitionEntity.setDefinitionVersion(1);
        definitionEntity.setCategoryIdSnapshot(7L);
        definitionEntity.setCategoryNameSnapshot("费用流程");
        definitionEntity.setInstanceNoRuleIdSnapshot(1);
        definitionEntity.setLifecycleState(1);
        definitionEntity.setStartState(1);

        BpmDefinitionNodeEntity nodeEntity = new BpmDefinitionNodeEntity();
        nodeEntity.setNodeKey("task_selected");
        nodeEntity.setNodeType("userTask");
        nodeEntity.setNodeNameSnapshot("发起时选择审批");
        nodeEntity.setAuthoredRuleSnapshotJson(
                "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
        );

        when(definitionDao.selectById(1L)).thenReturn(definitionEntity);
        when(definitionNodeDao.selectList(any())).thenReturn(List.of(nodeEntity));
        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(identityGateway().requireEmployee(100L)).thenReturn(new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null));
        when(serialNumberService().generate(any())).thenReturn("SN-2026-0004");
        when(processInstanceGateway.start("expense:1:1000", 100L, "{\"amount\":100,\"approverEmployeeId\":301}", Map.of("assignee_task_selected", "301")))
                .thenReturn("process-1003");
        when(instanceDao.insert(any(BpmInstanceEntity.class))).thenAnswer(invocation -> {
            BpmInstanceEntity entity = invocation.getArgument(0);
            entity.setInstanceId(11L);
            return 1;
        });

        BpmInstanceStartForm form = new BpmInstanceStartForm();
        form.setDefinitionId(1L);
        form.setFormDataJson("{\"amount\":100,\"approverEmployeeId\":301}");
        form.setTitle("费用申请");

        ResponseDTO<Long> response = service.startInstance(form);

        assertThat(response.getOk()).isTrue();

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(processInstanceGateway).start(
                Mockito.eq("expense:1:1000"),
                Mockito.eq(100L),
                Mockito.eq("{\"amount\":100,\"approverEmployeeId\":301}"),
                variablesCaptor.capture()
        );
        assertThat(variablesCaptor.getValue()).containsEntry("assignee_task_selected", "301");
    }
```

- [ ] **Step 3: Run tests to verify fail**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=SimpleModelValidatorTest,BpmRuntimeStartAssignmentTest' test
```

Expected: validator test fails because field key validation and error text are missing; runtime test fails until `BpmInstanceService` passes `formDataJson` to the context overload.

- [ ] **Step 4: Implement validator field-key check**

In `SimpleModelValidator.java`, update unsupported error text:

```java
                return ResponseDTO.userErrorParam("当前只支持 EMPLOYEE、DEPARTMENT_MANAGER、ROLE、START_EMPLOYEE、START_DEPARTMENT_MANAGER、EMPLOYEE_SELECT_AT_START 六类候选人解析类型");
```

After the unsupported-type check, add:

```java
            if ("EMPLOYEE_SELECT_AT_START".equalsIgnoreCase(resolverType)
                    && StringUtils.isBlank(firstNonBlank(
                    nodeObject.getString("employeeSelectFieldKey"),
                    nodeObject.getString("candidateFieldKey"),
                    nodeObject.getString("assigneeFieldKey")
            ))) {
                return ResponseDTO.userErrorParam("审批节点【" + firstNonBlank(nodeObject.getString("name"), nodeObject.getString("nodeKey"), nodeObject.getString("id")) + "】未配置发起时自选审批人字段");
            }
```

Change the helper signature from two arguments to varargs:

```java
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
```

- [ ] **Step 5: Wire start/resubmit to assignment context**

In `BpmInstanceService.java`, add import:

```java
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskAssignmentContext;
```

Because `BpmInstanceService` is in the same package as `BpmTaskAssignmentContext`, the import is optional; keep no import if the IDE/compiler flags same-package import as redundant.

In `resubmitMyInstance`, replace:

```java
            runtimeAssignmentVariables = bpmTaskAssignmentResolver.resolve(definitionNodes, employeeSnapshot);
```

with:

```java
            runtimeAssignmentVariables = bpmTaskAssignmentResolver.resolve(
                    definitionNodes,
                    new BpmTaskAssignmentContext(employeeSnapshot, resubmitForm.getFormDataJson())
            );
```

In `startInstanceWithDefinition`, replace:

```java
            runtimeAssignmentVariables = bpmTaskAssignmentResolver.resolve(definitionNodes, employeeSnapshot);
```

with:

```java
            runtimeAssignmentVariables = bpmTaskAssignmentResolver.resolve(
                    definitionNodes,
                    new BpmTaskAssignmentContext(employeeSnapshot, startForm.getFormDataJson())
            );
```

- [ ] **Step 6: Run backend focused tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,SimpleModelValidatorTest,BpmRuntimeStartAssignmentTest' test
```

Expected: PASS.

- [ ] **Step 7: Commit Task 2**

Run:

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidatorTest.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeStartAssignmentTest.java
git commit -m "feat: 校验并传递 BPM 发起时自选审批人"
```

Expected: commit succeeds with only Task 2 files.

---

### Task 3: Frontend Designer Contract

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/simple-model-bridge.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue`
- Test: `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts`

**Interfaces:**
- Consumes: backend node snapshot fields from Tasks 1 and 2.
- Produces: frontend node draft with optional `employeeSelectFieldKey?: string` and saved simple model JSON preserving that field.

- [ ] **Step 1: Write failing frontend contract test**

In `bpm-designer-adapters.test.ts`, add this test before the BPMN preview test:

```ts
  it('保留发起时自选审批人的字段 key 合同', () => {
    const parsedNodes = parseSimpleModelDraft(
      JSON.stringify({
        nodes: [
          {
            approvalMode: 'single',
            candidateResolverType: 'EMPLOYEE_SELECT_AT_START',
            employeeSelectFieldKey: 'approverEmployeeId',
            id: 'task_selected',
            listeners: [],
            name: '发起时选择审批',
            type: 'userTask',
          },
        ],
      }),
    );

    expect(parsedNodes).toEqual([
      {
        approvalMode: 'single',
        candidateResolverType: 'EMPLOYEE_SELECT_AT_START',
        employeeSelectFieldKey: 'approverEmployeeId',
        id: 'task_selected',
        listeners: [],
        name: '发起时选择审批',
        nodeKey: 'task_selected',
        type: 'userTask',
      },
    ]);

    expect(stringifySimpleModelDraft(parsedNodes)).toContain(
      '"employeeSelectFieldKey":"approverEmployeeId"',
    );
  });
```

- [ ] **Step 2: Run frontend test to verify fail**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts
```

Expected: FAIL because `employeeSelectFieldKey` is dropped by parse/stringify and `EMPLOYEE_SELECT_AT_START` is not in the TypeScript union.

- [ ] **Step 3: Extend TypeScript node type**

In `types.ts`, add the new resolver type and field:

```ts
  candidateResolverType?:
    | 'DEPARTMENT_MANAGER'
    | 'EMPLOYEE'
    | 'EMPLOYEE_SELECT_AT_START'
    | 'ROLE'
    | 'START_DEPARTMENT_MANAGER'
    | 'START_EMPLOYEE';
  employeeSelectFieldKey?: string;
```

- [ ] **Step 4: Preserve field key in simple-model bridge**

In `simple-model-bridge.ts`, add `employeeSelectFieldKey` in `normalizeNode`:

```ts
    employeeSelectFieldKey:
      typeof rawNode.employeeSelectFieldKey === 'string'
        ? rawNode.employeeSelectFieldKey.trim()
        : undefined,
```

In `stringifySimpleModelDraft`, add the field only when present:

```ts
      ...(node.employeeSelectFieldKey
        ? { employeeSelectFieldKey: node.employeeSelectFieldKey }
        : {}),
```

The resulting mapped object should still include `id`, `nodeKey`, `name`, `type`, `approvalMode`, `candidateResolverType`, `listeners`, and the conditional field.

- [ ] **Step 5: Add designer UI option and field input**

In `bpm-process-designer-adapter.vue`, add the option:

```vue
              <ElOption
                label="发起时自选审批人"
                value="EMPLOYEE_SELECT_AT_START"
              />
```

Place it near the other start-employee strategies.

Add the conditional field key input after the candidate resolver select:

```vue
          <ElFormItem
            v-if="selectedNode.candidateResolverType === 'EMPLOYEE_SELECT_AT_START'"
            label="自选字段"
          >
            <ElInput
              v-model="selectedNode.employeeSelectFieldKey"
              :disabled="disabled || readonly"
              placeholder="例如 approverEmployeeId"
              @change="handleStateChange"
            />
          </ElFormItem>
```

- [ ] **Step 6: Run frontend focused test**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts
```

Expected: PASS.

- [ ] **Step 7: Run frontend typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS with `vue-tsc --noEmit --skipLibCheck`.

- [ ] **Step 8: Commit Task 3**

Run:

```powershell
git add hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/types.ts hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/simple-model-bridge.ts hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts
git commit -m "feat: 前端支持 BPM 发起时自选审批人"
```

Expected: commit succeeds with only Task 3 files.

---

### Task 4: Contract Verification And Acceptance Record

**Files:**
- Create: `docs/superpowers/specs/2026-07-09-bpm-p3-employee-select-at-start-acceptance.md`

**Interfaces:**
- Consumes: all production and test changes from Tasks 1 through 3.
- Produces: durable Chinese UTF-8 acceptance record listing tests, scope, and remaining boundaries.

- [ ] **Step 1: Run backend focused gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,SimpleModelValidatorTest,BpmRuntimeStartAssignmentTest' test
```

Expected: PASS. Record test count and any warning text.

- [ ] **Step 2: Run full BPM module gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

Expected: PASS. Record test count and any existing local Maven warning.

- [ ] **Step 3: Run frontend BPM focused contracts**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: PASS with 3 files passing.

- [ ] **Step 4: Run frontend typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS.

- [ ] **Step 5: Run Flowable boundary gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin '-Dtest=BpmFlowableCompatibilityTest' test
```

Expected: PASS.

- [ ] **Step 6: Write acceptance record**

Create `docs/superpowers/specs/2026-07-09-bpm-p3-employee-select-at-start-acceptance.md`:

```markdown
# BPM P3.1b 发起时自选审批人验收记录

## 结论

BPM P3.1b 发起时自选审批人通过源级验收。

本轮新增 `EMPLOYEE_SELECT_AT_START`，使用户任务审批人可以在发起表单 `formDataJson` 中通过指定字段选择。实现保持 Hunyuan 原生边界：不新增 SQL、不新增依赖、不暴露 Flowable 原生对象、不迁移参考项目接口。

## 验收范围

- 后端候选人枚举、校验器、解析器支持 `EMPLOYEE_SELECT_AT_START`。
- 启动和重提链路把当前提交的 `formDataJson` 传入候选解析上下文。
- 解析器只接受单个员工 ID，拒绝数组和逗号字符串。
- 前端 BPM 设计器能选择 `发起时自选审批人` 并保存 `employeeSelectFieldKey`。

## 验收结果

| 门禁 | 命令 | 结果 |
| --- | --- | --- |
| 后端聚焦门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskAssignmentResolverTest,SimpleModelValidatorTest,BpmRuntimeStartAssignmentTest' test` | PASS；记录实际测试数和结束时间 |
| BPM 模块门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test` | PASS；记录实际测试数和结束时间 |
| 前端 BPM 合同测试 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom` | PASS；记录实际文件数和测试数 |
| 前端类型检查 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck` | PASS |
| Flowable 边界门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin '-Dtest=BpmFlowableCompatibilityTest' test` | PASS；记录实际测试数和结束时间 |

## 边界说明

- 本轮只支持单个员工 ID。
- 本轮不支持多人审批、用户组、岗位、表达式、表单字段选择器或多级主管。
- 本轮不校验员工是否存在，只保证启动前解析为单个员工 ID。
- 本轮不涉及 SQL 变更。

## 已知提示

- 如 Maven 仍提示本机 `F:\maven\apache-maven-3.9.11\conf\settings.xml` 第 235 行附近存在非预期文本，该提示属于既有本机配置问题，门禁通过即可。
- 如 `BpmFlowableCompatibilityTest` 编译阶段仍提示 `MockBean` 过时，该提示属于既有测试技术债，本轮不处理。
```

Write the final record with the observed command output. Do not leave generic result text if a command failed or was skipped.

- [ ] **Step 7: Self-review acceptance record**

Run:

```powershell
rg -n "待[补]|占[位]|稍后[补]|未[填]写" docs/superpowers/specs/2026-07-09-bpm-p3-employee-select-at-start-acceptance.md
```

Expected: no matches.

- [ ] **Step 8: Commit Task 4**

Run:

```powershell
git add docs/superpowers/specs/2026-07-09-bpm-p3-employee-select-at-start-acceptance.md
git commit -m "docs: 增加 BPM 发起时自选审批人验收记录"
```

Expected: commit succeeds with only the acceptance record.

---

## Final Verification

- [ ] Run `git status --short --branch`.
- [ ] Confirm no unrelated files are staged or modified.
- [ ] Summarize commits and verification evidence in Chinese.
- [ ] State explicitly that no SQL/dependency changes were made.

## Plan Self-Review

- Spec coverage: covered backend enum, context, resolver, start/resubmit wiring, validator, frontend type/bridge/UI, tests, verification, acceptance record, no SQL, no dependencies, single employee boundary.
- Marker scan: acceptance-record instructions require actual observed results before commit and include a concrete `rg` check for unresolved Chinese markers.
- Type consistency: `BpmTaskAssignmentContext`, `employeeSelectFieldKey`, and `EMPLOYEE_SELECT_AT_START` are used consistently across backend and frontend tasks.
