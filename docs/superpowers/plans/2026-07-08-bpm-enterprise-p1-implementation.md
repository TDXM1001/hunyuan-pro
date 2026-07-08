# BPM Enterprise P1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the existing `hunyuan-bpm` runtime closure into a Hunyuan-native enterprise workflow platform P1 with definition governance, runtime governance, business integration reliability, and controlled approval semantics.

**Architecture:** Keep Flowable hidden behind `hunyuan-bpm.engine.internal` and expose only Hunyuan BPM names, IDs, forms, commands, and events. Implement P1 as four independently testable increments: definition governance, runtime governance, business integration reliability, and approval semantic expansion. Each increment must land with tests and a small acceptance record before the next increment starts.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, Flowable hidden kernel, MySQL migration SQL, Vue 3, TypeScript, Vben/Hunyuan Design, Vitest, Maven, persistent Playwright MCP for live browser proof when needed.

## Global Constraints

- Follow `AGENTS.md`: make one incremental change at a time.
- Explain why a change is needed before editing files.
- Prefer existing project patterns over new abstractions.
- Do not add new dependencies without explicit approval.
- Keep changes tightly scoped to the task.
- Verify every meaningful change with a concrete check.
- Production code, contracts, routes, permissions, menus, tests, docs, and verification artifacts must be completed in `E:/my-project/hunyuan-pro`.
- Reference repos are reference material only: borrow mechanisms, not names, APIs, page shells, dependency assumptions, or module boundaries.
- Keep Flowable and third-party BPM concepts behind Hunyuan BPM boundaries.
- Business modules must consume Hunyuan BPM contracts and must not depend on Flowable native types.
- Runtime Playwright MCP artifacts must stay under `G:/code-mcp/playwright-mcp-temp/cache` or `G:/code-mcp/playwright-mcp-temp/runtime`.

---

## Scope Split

The design spec covers four independent subsystems. Implement them in this order:

1. **P1.1 Definition Governance**
   - Publish validation report.
   - Publish diff summary.
   - Definition enable/suspend rules.
   - Start scope.

2. **P1.2 Runtime Governance**
   - Admin instance and task intervention.
   - Projection resync.
   - Unified action log semantics.

3. **P1.3 Business Integration Reliability**
   - Hunyuan BPM business API.
   - Business result event.
   - Callback/command records.
   - Retry and idempotency.
   - Minimal purchase/expense acceptance sample.

4. **P1.4 Approval Semantics**
   - Delegate.
   - Add sign.
   - Reduce sign.
   - Recall.

Do not start P1.2 until P1.1 has its acceptance record. Do not start P1.3 until P1.2 proves admin actions and action logs. Do not start P1.4 until P1.3 proves callbacks and command records are reliable.

## Planned File Structure

### Existing backend files to modify

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java`
  - Add structured validation findings instead of returning only a string.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/BpmDefinitionService.java`
  - Own publish validation, diff summary, enable/suspend, and current-definition rules.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmDefinitionController.java`
  - Expose validation report, diff preview, suspend/start endpoints.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
  - Add admin cancellation, business status query, and callback status integration.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`
  - Add admin transfer, delegate, add sign, reduce sign, recall, and unified action logs.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskProjectionService.java`
  - Add explicit admin resync entry and projection consistency tests.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmInstanceController.java`
  - Expose admin cancel and projection resync.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmTaskController.java`
  - Expose admin transfer and P1.4 task operations.

### New backend files to create

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/vo/BpmDefinitionValidationReportVO.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/vo/BpmDefinitionDiffVO.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/form/BpmDefinitionStartScopeSaveForm.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmAdminInstanceCancelForm.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmAdminTaskTransferForm.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/business/BpmBusinessProcessApi.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/business/BpmBusinessProcessApiImpl.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/business/domain/BpmBusinessStartCommand.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/business/domain/BpmBusinessInstanceStatus.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/business/domain/BpmBusinessResultEvent.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackService.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/entity/BpmCallbackRecordEntity.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/entity/BpmCommandRecordEntity.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/dao/BpmCallbackRecordDao.java`
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/dao/BpmCommandRecordDao.java`

### Existing backend tests to modify

- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/definition/BpmDefinitionPublishServiceTest.java`
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/authoring/BpmAuthoringServiceTest.java`
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java`
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskProjectionServiceTest.java`
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/architecture/BpmApiIsolationTest.java`

### New backend tests to create

- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/definition/BpmDefinitionGovernanceServiceTest.java`
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmAdminInterventionServiceTest.java`
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessProcessApiTest.java`
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessCallbackServiceTest.java`
- `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskAdvancedActionServiceTest.java`

### SQL files

- Create `数据库SQL脚本/mysql/sql-update-log/v3.38.0.sql`
  - Add definition start scope columns or table.
  - Add callback record table.
  - Add command record table.
  - Add indexes needed for idempotency and retry.

### Existing frontend files to modify

- `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/definition.ts`
- `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
- `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/definition/definition-list.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/instance/instance-list.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/task/task-list.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue`

### New frontend files to create

- `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/integration.ts`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/integration/callback-record-list.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/integration/command-record-list.vue`

### Documentation and acceptance records

- `docs/superpowers/specs/2026-07-08-bpm-enterprise-p1-design.md`
  - Source spec.
- Create `docs/superpowers/specs/2026-07-08-bpm-enterprise-p1-1-definition-governance-acceptance.md`
- Create `docs/superpowers/specs/2026-07-08-bpm-enterprise-p1-2-runtime-governance-acceptance.md`
- Create `docs/superpowers/specs/2026-07-08-bpm-enterprise-p1-3-business-integration-acceptance.md`
- Create `docs/superpowers/specs/2026-07-08-bpm-enterprise-p1-4-approval-semantics-acceptance.md`

---

### Task 1: P1.1 Definition Validation Report and Publish Diff

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/BpmDefinitionService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmDefinitionController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/vo/BpmDefinitionValidationReportVO.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/vo/BpmDefinitionDiffVO.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/definition/BpmDefinitionPublishServiceTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/definition/BpmDefinitionGovernanceServiceTest.java`

**Interfaces:**
- Consumes:
  - `BpmModelEntity.getSimpleModelJson()`
  - `BpmModelEntity.getStartRuleJson()`
  - `BpmDefinitionDao.selectCurrentByDefinitionKey(String definitionKey)`
- Produces:
  - `ResponseDTO<BpmDefinitionValidationReportVO> BpmDefinitionService.validateForPublish(Long modelId)`
  - `ResponseDTO<BpmDefinitionDiffVO> BpmDefinitionService.previewPublishDiff(Long modelId)`
  - `GET /bpm/definition/validateForPublish/{modelId}`
  - `GET /bpm/definition/publishDiff/{modelId}`

- [ ] **Step 1: Write failing tests for structured validation**

Add tests to `BpmDefinitionGovernanceServiceTest`:

```java
@Test
void validateForPublishShouldReturnBlockingFindingWhenUserTaskHasNoResolver() {
    BpmDefinitionService service = new BpmDefinitionService();
    BpmModelDao modelDao = Mockito.mock(BpmModelDao.class);
    SimpleModelValidator validator = new SimpleModelValidator();
    ReflectionTestUtils.setField(service, "bpmModelDao", modelDao);
    ReflectionTestUtils.setField(service, "simpleModelValidator", validator);

    BpmModelEntity model = new BpmModelEntity();
    model.setModelId(10L);
    model.setModelKey("expense_apply");
    model.setModelName("费用申请");
    model.setStartRuleJson("{\"scope\":\"ALL\"}");
    model.setSimpleModelJson("{\"nodes\":[{\"nodeKey\":\"approve\",\"type\":\"userTask\",\"name\":\"主管审批\",\"approvalMode\":\"single\"}]}");
    Mockito.when(modelDao.selectById(10L)).thenReturn(model);

    ResponseDTO<BpmDefinitionValidationReportVO> response = service.validateForPublish(10L);

    assertThat(response.getOk()).isTrue();
    assertThat(response.getData().getPass()).isFalse();
    assertThat(response.getData().getFindings())
            .anyMatch(item -> "USER_TASK_CANDIDATE_EMPTY".equals(item.getCode()));
}
```

- [ ] **Step 2: Run the failing test**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmDefinitionGovernanceServiceTest test
```

Expected: FAIL because `BpmDefinitionGovernanceServiceTest` or `validateForPublish(Long)` does not exist.

- [ ] **Step 3: Add validation report VO**

Create `BpmDefinitionValidationReportVO`:

```java
package com.hunyuan.sa.bpm.module.definition.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BpmDefinitionValidationReportVO {

    private Boolean pass;

    private Integer blockingCount;

    private Integer warningCount;

    private List<Finding> findings = new ArrayList<>();

    @Data
    public static class Finding {
        private String level;
        private String code;
        private String message;
        private String nodeKey;
        private String field;
    }
}
```

- [ ] **Step 4: Add minimal validation report implementation**

Add to `BpmDefinitionService`:

```java
public ResponseDTO<BpmDefinitionValidationReportVO> validateForPublish(Long modelId) {
    BpmModelEntity modelEntity = bpmModelDao.selectById(modelId);
    if (modelEntity == null) {
        return ResponseDTO.userErrorParam("流程模型不存在");
    }
    BpmDefinitionValidationReportVO report = new BpmDefinitionValidationReportVO();
    report.setPass(Boolean.TRUE);
    report.setBlockingCount(0);
    report.setWarningCount(0);

    JSONObject modelJson = JSON.parseObject(modelEntity.getSimpleModelJson());
    JSONArray nodes = modelJson.getJSONArray("nodes");
    if (nodes != null) {
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if ("userTask".equals(node.getString("type"))
                    && !StringUtils.hasText(node.getString("candidateResolverType"))) {
                BpmDefinitionValidationReportVO.Finding finding = new BpmDefinitionValidationReportVO.Finding();
                finding.setLevel("BLOCKING");
                finding.setCode("USER_TASK_CANDIDATE_EMPTY");
                finding.setMessage("审批节点缺少处理人规则");
                finding.setNodeKey(node.getString("nodeKey"));
                finding.setField("candidateResolverType");
                report.getFindings().add(finding);
            }
        }
    }
    long blockingCount = report.getFindings().stream()
            .filter(item -> "BLOCKING".equals(item.getLevel()))
            .count();
    report.setBlockingCount((int) blockingCount);
    report.setWarningCount(report.getFindings().size() - report.getBlockingCount());
    report.setPass(report.getBlockingCount() == 0);
    return ResponseDTO.ok(report);
}
```

Use existing imports already present where possible; if needed add:

```java
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionValidationReportVO;
import org.springframework.util.StringUtils;
```

- [ ] **Step 5: Wire controller endpoints**

Add to `AdminBpmDefinitionController`:

```java
@GetMapping("/bpm/definition/validateForPublish/{modelId}")
@SaCheckPermission("bpm:definition:publish")
public ResponseDTO<BpmDefinitionValidationReportVO> validateForPublish(@PathVariable Long modelId) {
    return bpmDefinitionService.validateForPublish(modelId);
}
```

- [ ] **Step 6: Make publish consume validation report**

In `BpmDefinitionService.publish(BpmDefinitionPublishForm publishForm)`, call:

```java
ResponseDTO<BpmDefinitionValidationReportVO> reportResponse = validateForPublish(publishForm.getModelId());
if (!Boolean.TRUE.equals(reportResponse.getOk())) {
    return ResponseDTO.userErrorParam(reportResponse.getMsg());
}
if (!Boolean.TRUE.equals(reportResponse.getData().getPass())) {
    return ResponseDTO.userErrorParam("流程发布校验未通过");
}
```

Keep the existing `SimpleModelValidator.validate(...)` call until all old tests are migrated; remove it only in a later cleanup task if it becomes duplicate.

- [ ] **Step 7: Add publish diff VO**

Create `BpmDefinitionDiffVO`:

```java
package com.hunyuan.sa.bpm.module.definition.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BpmDefinitionDiffVO {

    private Long modelId;

    private Long previousDefinitionId;

    private Integer previousVersion;

    private List<String> changedItems = new ArrayList<>();
}
```

- [ ] **Step 8: Add minimal diff preview**

Add to `BpmDefinitionService`:

```java
public ResponseDTO<BpmDefinitionDiffVO> previewPublishDiff(Long modelId) {
    BpmModelEntity modelEntity = bpmModelDao.selectById(modelId);
    if (modelEntity == null) {
        return ResponseDTO.userErrorParam("流程模型不存在");
    }
    BpmDefinitionDiffVO diff = new BpmDefinitionDiffVO();
    diff.setModelId(modelId);
    diff.getChangedItems().add("发布后将生成新的不可变流程定义版本");
    if (modelEntity.getPublishedDefinitionId() != null) {
        BpmDefinitionEntity previous = bpmDefinitionDao.selectById(modelEntity.getPublishedDefinitionId());
        if (previous != null) {
            diff.setPreviousDefinitionId(previous.getDefinitionId());
            diff.setPreviousVersion(previous.getDefinitionVersion());
            if (!Objects.equals(previous.getSimpleModelSnapshotJson(), modelEntity.getSimpleModelJson())) {
                diff.getChangedItems().add("流程节点设计已变化");
            }
            if (!Objects.equals(previous.getStartRuleSnapshotJson(), modelEntity.getStartRuleJson())) {
                diff.getChangedItems().add("发起规则已变化");
            }
        }
    }
    return ResponseDTO.ok(diff);
}
```

Add imports:

```java
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionDiffVO;
import java.util.Objects;
```

- [ ] **Step 9: Wire diff endpoint**

Add to `AdminBpmDefinitionController`:

```java
@GetMapping("/bpm/definition/publishDiff/{modelId}")
@SaCheckPermission("bpm:definition:publish")
public ResponseDTO<BpmDefinitionDiffVO> publishDiff(@PathVariable Long modelId) {
    return bpmDefinitionService.previewPublishDiff(modelId);
}
```

- [ ] **Step 10: Run targeted backend tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmDefinitionGovernanceServiceTest,BpmDefinitionPublishServiceTest test
```

Expected: PASS with zero failures.

- [ ] **Step 11: Commit**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/compiler/SimpleModelValidator.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/BpmDefinitionService.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmDefinitionController.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/vo/BpmDefinitionValidationReportVO.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/vo/BpmDefinitionDiffVO.java `
        hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/definition/BpmDefinitionGovernanceServiceTest.java `
        hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/definition/BpmDefinitionPublishServiceTest.java
git commit -m "feat: 增加BPM发布校验报告"
```

---

### Task 2: P1.1 Definition Start Scope and State Governance

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/form/BpmDefinitionStartScopeSaveForm.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/entity/BpmDefinitionEntity.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmDefinitionController.java`
- Modify: `数据库SQL脚本/mysql/sql-update-log/v3.38.0.sql`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeStartAssignmentTest.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/definition/BpmDefinitionGovernanceServiceTest.java`

**Interfaces:**
- Consumes:
  - `BpmCurrentActorProvider.getCurrentEmployeeId()`
  - `BpmOrgIdentityGateway` employee and role/department snapshot lookups
- Produces:
  - `POST /bpm/definition/startScope/save`
  - `POST /bpm/definition/suspendStart/{definitionId}`
  - `POST /bpm/definition/enableStart/{definitionId}`
  - `BpmDefinitionEntity.startScopeJson`

- [ ] **Step 1: Add failing tests for start scope**

Add to `BpmRuntimeStartAssignmentTest`:

```java
@Test
void queryStartableDefinitionsShouldHideDefinitionOutsideEmployeeStartScope() {
    BpmInstanceService service = new BpmInstanceService();
    BpmDefinitionDao definitionDao = Mockito.mock(BpmDefinitionDao.class);
    BpmCurrentActorProvider actorProvider = Mockito.mock(BpmCurrentActorProvider.class);
    ReflectionTestUtils.setField(service, "bpmDefinitionDao", definitionDao);
    ReflectionTestUtils.setField(service, "bpmCurrentActorProvider", actorProvider);

    BpmDefinitionEntity definition = new BpmDefinitionEntity();
    definition.setDefinitionId(100L);
    definition.setDefinitionName("费用申请");
    definition.setStartScopeJson("{\"type\":\"EMPLOYEE\",\"employeeIds\":[200]}");
    Mockito.when(actorProvider.getCurrentEmployeeId()).thenReturn(100L);
    Mockito.when(definitionDao.selectCurrentStartableList()).thenReturn(List.of(definition));

    ResponseDTO<List<BpmStartableDefinitionVO>> response = service.queryStartableDefinitions();

    assertThat(response.getOk()).isTrue();
    assertThat(response.getData()).isEmpty();
}
```

- [ ] **Step 2: Run failing test**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmRuntimeStartAssignmentTest test
```

Expected: FAIL because start scope is not implemented or not filtered.

- [ ] **Step 3: Add SQL columns**

Append to `v3.38.0.sql`:

```sql
ALTER TABLE `t_bpm_definition`
    ADD COLUMN `start_scope_json` longtext NULL COMMENT '可发起范围快照JSON' AFTER `start_state`;

CREATE INDEX `idx_definition_start_state` ON `t_bpm_definition` (`start_state`);
```

- [ ] **Step 4: Add entity field**

Add to `BpmDefinitionEntity`:

```java
private String startScopeJson;
```

- [ ] **Step 5: Add form**

Create `BpmDefinitionStartScopeSaveForm`:

```java
package com.hunyuan.sa.bpm.module.definition.domain.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BpmDefinitionStartScopeSaveForm {

    @NotNull(message = "定义ID不能为空")
    private Long definitionId;

    @NotBlank(message = "可发起范围不能为空")
    private String startScopeJson;
}
```

- [ ] **Step 6: Add service methods**

Add to `BpmDefinitionService`:

```java
public ResponseDTO<String> saveStartScope(BpmDefinitionStartScopeSaveForm form) {
    BpmDefinitionEntity entity = bpmDefinitionDao.selectById(form.getDefinitionId());
    if (entity == null) {
        return ResponseDTO.userErrorParam("流程定义不存在");
    }
    BpmDefinitionEntity updateEntity = new BpmDefinitionEntity();
    updateEntity.setDefinitionId(form.getDefinitionId());
    updateEntity.setStartScopeJson(form.getStartScopeJson());
    bpmDefinitionDao.updateById(updateEntity);
    return ResponseDTO.ok();
}

public ResponseDTO<String> suspendStart(Long definitionId) {
    return updateStartState(definitionId, BpmDefinitionStartStateEnum.SUSPENDED.getValue());
}

public ResponseDTO<String> enableStart(Long definitionId) {
    return updateStartState(definitionId, BpmDefinitionStartStateEnum.STARTABLE.getValue());
}

private ResponseDTO<String> updateStartState(Long definitionId, Integer startState) {
    BpmDefinitionEntity entity = bpmDefinitionDao.selectById(definitionId);
    if (entity == null) {
        return ResponseDTO.userErrorParam("流程定义不存在");
    }
    BpmDefinitionEntity updateEntity = new BpmDefinitionEntity();
    updateEntity.setDefinitionId(definitionId);
    updateEntity.setStartState(startState);
    bpmDefinitionDao.updateById(updateEntity);
    return ResponseDTO.ok();
}
```

- [ ] **Step 7: Filter startable list**

In `BpmInstanceService.queryStartableDefinitions()`, after loading current startable definitions, filter with:

```java
private boolean canCurrentEmployeeStart(BpmDefinitionEntity definitionEntity) {
    String startScopeJson = definitionEntity.getStartScopeJson();
    if (!StringUtils.hasText(startScopeJson)) {
        return true;
    }
    Long currentEmployeeId = bpmCurrentActorProvider.getCurrentEmployeeId();
    JSONObject scope = JSON.parseObject(startScopeJson);
    String type = scope.getString("type");
    if ("ALL".equals(type)) {
        return true;
    }
    if ("EMPLOYEE".equals(type)) {
        JSONArray employeeIds = scope.getJSONArray("employeeIds");
        return employeeIds != null && employeeIds.contains(currentEmployeeId);
    }
    return false;
}
```

- [ ] **Step 8: Wire controller endpoints**

Add to `AdminBpmDefinitionController`:

```java
@PostMapping("/bpm/definition/startScope/save")
@SaCheckPermission("bpm:definition:update")
public ResponseDTO<String> saveStartScope(@RequestBody @Valid BpmDefinitionStartScopeSaveForm form) {
    return bpmDefinitionService.saveStartScope(form);
}

@PostMapping("/bpm/definition/suspendStart/{definitionId}")
@SaCheckPermission("bpm:definition:update")
public ResponseDTO<String> suspendStart(@PathVariable Long definitionId) {
    return bpmDefinitionService.suspendStart(definitionId);
}

@PostMapping("/bpm/definition/enableStart/{definitionId}")
@SaCheckPermission("bpm:definition:update")
public ResponseDTO<String> enableStart(@PathVariable Long definitionId) {
    return bpmDefinitionService.enableStart(definitionId);
}
```

- [ ] **Step 9: Run tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmDefinitionGovernanceServiceTest,BpmRuntimeStartAssignmentTest test
```

Expected: PASS with zero failures.

- [ ] **Step 10: Commit**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/form/BpmDefinitionStartScopeSaveForm.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/domain/entity/BpmDefinitionEntity.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/definition/service/BpmDefinitionService.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmDefinitionController.java `
        hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeStartAssignmentTest.java `
        hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/definition/BpmDefinitionGovernanceServiceTest.java `
        数据库SQL脚本/mysql/sql-update-log/v3.38.0.sql
git commit -m "feat: 增加BPM定义发起范围治理"
```

---

### Task 3: P1.1 Frontend Contract for Definition Governance

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/definition.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/definition/definition-list.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- Create: `docs/superpowers/specs/2026-07-08-bpm-enterprise-p1-1-definition-governance-acceptance.md`

**Interfaces:**
- Consumes:
  - `GET /bpm/definition/validateForPublish/{modelId}`
  - `GET /bpm/definition/publishDiff/{modelId}`
  - `POST /bpm/definition/startScope/save`
  - `POST /bpm/definition/suspendStart/{definitionId}`
  - `POST /bpm/definition/enableStart/{definitionId}`
- Produces:
  - `validateBpmDefinitionForPublish(modelId: number)`
  - `getBpmDefinitionPublishDiff(modelId: number)`
  - `saveBpmDefinitionStartScope(data: BpmDefinitionStartScopeSaveForm)`
  - `suspendBpmDefinitionStart(definitionId: number)`
  - `enableBpmDefinitionStart(definitionId: number)`

- [ ] **Step 1: Write failing API contract tests**

Add expectations to `bpm-api.test.ts`:

```ts
expect(definitionApiSource).toContain('validateBpmDefinitionForPublish');
expect(definitionApiSource).toContain('/bpm/definition/validateForPublish/');
expect(definitionApiSource).toContain('getBpmDefinitionPublishDiff');
expect(definitionApiSource).toContain('/bpm/definition/publishDiff/');
expect(definitionApiSource).toContain('saveBpmDefinitionStartScope');
expect(definitionApiSource).toContain('/bpm/definition/startScope/save');
expect(definitionApiSource).toContain('suspendBpmDefinitionStart');
expect(definitionApiSource).toContain('/bpm/definition/suspendStart/');
expect(definitionApiSource).toContain('enableBpmDefinitionStart');
expect(definitionApiSource).toContain('/bpm/definition/enableStart/');
```

- [ ] **Step 2: Run failing Vitest**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts --dom
```

Expected: FAIL because new API names are missing.

- [ ] **Step 3: Add TypeScript API contracts**

Add to `definition.ts`:

```ts
export interface BpmDefinitionValidationFinding {
  code: string;
  field?: string;
  level: 'BLOCKING' | 'WARNING';
  message: string;
  nodeKey?: string;
}

export interface BpmDefinitionValidationReport {
  blockingCount: number;
  findings: BpmDefinitionValidationFinding[];
  pass: boolean;
  warningCount: number;
}

export interface BpmDefinitionDiff {
  changedItems: string[];
  modelId: number;
  previousDefinitionId?: number;
  previousVersion?: number;
}

export interface BpmDefinitionStartScopeSaveForm {
  definitionId: number;
  startScopeJson: string;
}

export function validateBpmDefinitionForPublish(modelId: number) {
  return requestClient.get<BpmDefinitionValidationReport>(`/bpm/definition/validateForPublish/${modelId}`);
}

export function getBpmDefinitionPublishDiff(modelId: number) {
  return requestClient.get<BpmDefinitionDiff>(`/bpm/definition/publishDiff/${modelId}`);
}

export function saveBpmDefinitionStartScope(data: BpmDefinitionStartScopeSaveForm) {
  return requestClient.post('/bpm/definition/startScope/save', data);
}

export function suspendBpmDefinitionStart(definitionId: number) {
  return requestClient.post(`/bpm/definition/suspendStart/${definitionId}`);
}

export function enableBpmDefinitionStart(definitionId: number) {
  return requestClient.post(`/bpm/definition/enableStart/${definitionId}`);
}
```

- [ ] **Step 4: Add minimal UI entry points**

In `definition-list.vue`, add row actions:

```ts
async function handleSuspendStart(row: BpmDefinitionApi.BpmDefinitionVO) {
  await BpmDefinitionApi.suspendBpmDefinitionStart(row.definitionId);
  await loadData();
}

async function handleEnableStart(row: BpmDefinitionApi.BpmDefinitionVO) {
  await BpmDefinitionApi.enableBpmDefinitionStart(row.definitionId);
  await loadData();
}
```

Use the existing table action pattern in the file; do not add explanatory page copy.

- [ ] **Step 5: Add publish precheck in model editor**

In `model-editor.vue`, before publish:

```ts
const report = await BpmDefinitionApi.validateBpmDefinitionForPublish(modelId.value);
if (!report.pass) {
  validationReport.value = report;
  return;
}
const diff = await BpmDefinitionApi.getBpmDefinitionPublishDiff(modelId.value);
publishDiff.value = diff;
```

Keep the existing publish request after the user confirms the diff.

- [ ] **Step 6: Run frontend contract tests**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: PASS.

- [ ] **Step 7: Run typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: command exits 0.

- [ ] **Step 8: Write acceptance record**

Create `docs/superpowers/specs/2026-07-08-bpm-enterprise-p1-1-definition-governance-acceptance.md` with:

```markdown
# BPM P1.1 Definition Governance Acceptance

## Scope

- Publish validation report endpoint and frontend contract.
- Publish diff preview endpoint and frontend contract.
- Definition start scope persistence.
- Definition start enable/suspend controls.

## Verification

- `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmDefinitionGovernanceServiceTest,BpmDefinitionPublishServiceTest,BpmRuntimeStartAssignmentTest test`
- `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom`
- `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck`

## Boundaries

- No Flowable type is exposed in public API or frontend types.
- No Yudao/RuoYi route or contract is migrated.
- No advanced approval action is implemented in P1.1.
```

- [ ] **Step 9: Commit**

```powershell
git add hunyuan-design/apps/hunyuan-system/src/api/system/bpm/definition.ts `
        hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts `
        hunyuan-design/apps/hunyuan-system/src/views/system/bpm/definition/definition-list.vue `
        hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue `
        hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts `
        docs/superpowers/specs/2026-07-08-bpm-enterprise-p1-1-definition-governance-acceptance.md
git commit -m "feat: 接入BPM定义治理前端契约"
```

---

### Task 4: P1.2 Runtime Admin Intervention

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmAdminInstanceCancelForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmAdminTaskTransferForm.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskProjectionService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmInstanceController.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmTaskController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmAdminInterventionServiceTest.java`

**Interfaces:**
- Produces:
  - `POST /bpm/instance/adminCancel`
  - `POST /bpm/instance/resyncProjection/{instanceId}`
  - `POST /bpm/task/adminTransfer`
  - `ResponseDTO<String> BpmInstanceService.adminCancel(BpmAdminInstanceCancelForm form)`
  - `ResponseDTO<String> BpmInstanceService.resyncProjection(Long instanceId)`
  - `ResponseDTO<String> BpmTaskService.adminTransfer(BpmAdminTaskTransferForm form)`

- [ ] **Step 1: Write failing admin cancel test**

Create `BpmAdminInterventionServiceTest`:

```java
@Test
void adminCancelShouldWriteAdminActionLogAndCancelRunningInstance() {
    BpmInstanceService service = new BpmInstanceService();
    BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
    BpmTaskActionLogDao logDao = Mockito.mock(BpmTaskActionLogDao.class);
    BpmCurrentActorProvider actorProvider = Mockito.mock(BpmCurrentActorProvider.class);
    ReflectionTestUtils.setField(service, "bpmInstanceDao", instanceDao);
    ReflectionTestUtils.setField(service, "bpmTaskActionLogDao", logDao);
    ReflectionTestUtils.setField(service, "bpmCurrentActorProvider", actorProvider);

    BpmInstanceEntity instance = new BpmInstanceEntity();
    instance.setInstanceId(1L);
    instance.setRunState(BpmInstanceRunStateEnum.RUNNING.getValue());
    Mockito.when(instanceDao.selectById(1L)).thenReturn(instance);
    Mockito.when(actorProvider.getCurrentEmployeeId()).thenReturn(900L);
    Mockito.when(actorProvider.getCurrentEmployeeName()).thenReturn("管理员");

    BpmAdminInstanceCancelForm form = new BpmAdminInstanceCancelForm();
    form.setInstanceId(1L);
    form.setCancelReason("录入错误");
    ResponseDTO<String> response = service.adminCancel(form);

    assertThat(response.getOk()).isTrue();
    Mockito.verify(instanceDao).updateById(Mockito.argThat(update ->
            BpmInstanceRunStateEnum.CANCELLED.getValue().equals(update.getRunState())));
    Mockito.verify(logDao).insert(Mockito.argThat(log ->
            "ADMIN_INSTANCE_CANCELLED".equals(log.getActionType())));
}
```

- [ ] **Step 2: Run failing test**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmAdminInterventionServiceTest test
```

Expected: FAIL because admin intervention forms/services are missing.

- [ ] **Step 3: Add forms**

Create `BpmAdminInstanceCancelForm`:

```java
package com.hunyuan.sa.bpm.module.runtime.domain.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BpmAdminInstanceCancelForm {
    @NotNull(message = "实例ID不能为空")
    private Long instanceId;

    @NotBlank(message = "取消原因不能为空")
    @Size(max = 500, message = "取消原因最多 500 个字符")
    private String cancelReason;
}
```

Create `BpmAdminTaskTransferForm`:

```java
package com.hunyuan.sa.bpm.module.runtime.domain.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BpmAdminTaskTransferForm {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "转办员工不能为空")
    private Long targetEmployeeId;

    @NotBlank(message = "转办原因不能为空")
    @Size(max = 500, message = "转办原因最多 500 个字符")
    private String reason;
}
```

- [ ] **Step 4: Implement admin cancel**

Add `adminCancel(BpmAdminInstanceCancelForm form)` to `BpmInstanceService`.
Use action type `ADMIN_INSTANCE_CANCELLED`.
Set:

```java
updateEntity.setRunState(BpmInstanceRunStateEnum.CANCELLED.getValue());
updateEntity.setResultState(BpmInstanceResultStateEnum.CANCELLED_BY_ADMIN.getValue());
updateEntity.setCancelByEmployeeId(actor.employeeId());
updateEntity.setCancelByNameSnapshot(actor.employeeName());
updateEntity.setCancelReason(form.getCancelReason());
updateEntity.setCancelledAt(LocalDateTime.now());
updateEntity.setFinishedAt(LocalDateTime.now());
```

- [ ] **Step 5: Implement admin transfer**

Add `adminTransfer(BpmAdminTaskTransferForm form)` to `BpmTaskService`.
Reuse existing transfer mechanics where possible, but set action type `ADMIN_TRANSFERRED` and actor to current admin.

- [ ] **Step 6: Implement projection resync**

Add to `BpmInstanceService`:

```java
public ResponseDTO<String> resyncProjection(Long instanceId) {
    BpmInstanceEntity instance = bpmInstanceDao.selectById(instanceId);
    if (instance == null) {
        return ResponseDTO.userErrorParam("流程实例不存在");
    }
    bpmTaskProjectionService.syncActiveTasks(instance);
    return ResponseDTO.ok();
}
```

If `syncActiveTasks` is private, expose a narrow public method:

```java
public void syncActiveTasks(BpmInstanceEntity instance) {
    List<FlowableActiveTaskSnapshot> activeTasks =
            flowableTaskGateway.queryActiveTasksByProcessInstanceId(instance.getEngineProcessInstanceId());
    for (FlowableActiveTaskSnapshot activeTask : activeTasks) {
        insertTaskIfMissing(instance, activeTask);
    }
    updateInstanceActiveTaskSummary(instance.getInstanceId(), activeTasks);
}
```

- [ ] **Step 7: Wire admin endpoints**

Add to `AdminBpmInstanceController`:

```java
@PostMapping("/bpm/instance/adminCancel")
@SaCheckPermission("bpm:instance:update")
public ResponseDTO<String> adminCancel(@RequestBody @Valid BpmAdminInstanceCancelForm form) {
    return bpmInstanceService.adminCancel(form);
}

@PostMapping("/bpm/instance/resyncProjection/{instanceId}")
@SaCheckPermission("bpm:instance:update")
public ResponseDTO<String> resyncProjection(@PathVariable Long instanceId) {
    return bpmInstanceService.resyncProjection(instanceId);
}
```

Add to `AdminBpmTaskController`:

```java
@PostMapping("/bpm/task/adminTransfer")
@SaCheckPermission("bpm:task:update")
public ResponseDTO<String> adminTransfer(@RequestBody @Valid BpmAdminTaskTransferForm form) {
    return bpmTaskService.adminTransfer(form);
}
```

- [ ] **Step 8: Run targeted tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmAdminInterventionServiceTest,BpmRuntimeCommandServiceTest,BpmTaskProjectionServiceTest test
```

Expected: PASS with zero failures.

- [ ] **Step 9: Commit**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmAdminInstanceCancelForm.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmAdminTaskTransferForm.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskProjectionService.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmInstanceController.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmTaskController.java `
        hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmAdminInterventionServiceTest.java
git commit -m "feat: 增加BPM运行治理干预能力"
```

---

### Task 5: P1.3 Business Integration API and Reliability Records

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/business/BpmBusinessProcessApi.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/business/BpmBusinessProcessApiImpl.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/business/domain/BpmBusinessStartCommand.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/business/domain/BpmBusinessInstanceStatus.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/business/domain/BpmBusinessResultEvent.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/entity/BpmCallbackRecordEntity.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/entity/BpmCommandRecordEntity.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/dao/BpmCallbackRecordDao.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/dao/BpmCommandRecordDao.java`
- Modify: `数据库SQL脚本/mysql/sql-update-log/v3.38.0.sql`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessProcessApiTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessCallbackServiceTest.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/architecture/BpmApiIsolationTest.java`

**Interfaces:**
- Produces:
  - `Long BpmBusinessProcessApi.start(BpmBusinessStartCommand command)`
  - `BpmBusinessInstanceStatus BpmBusinessProcessApi.getStatus(String businessType, Long businessId)`
  - `void BpmBusinessProcessApi.publishResultEvent(BpmBusinessResultEvent event)`
  - `ResponseDTO<String> BpmBusinessCallbackService.retry(Long callbackRecordId)`

- [ ] **Step 1: Write failing business API tests**

Create `BpmBusinessProcessApiTest`:

```java
@Test
void startShouldRequireBusinessTypeBusinessIdAndDefinitionKey() {
    BpmBusinessProcessApi api = new BpmBusinessProcessApiImpl();
    BpmBusinessStartCommand command = new BpmBusinessStartCommand();
    assertThatThrownBy(() -> api.start(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("businessType");
}
```

- [ ] **Step 2: Run failing tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmBusinessProcessApiTest,BpmBusinessCallbackServiceTest test
```

Expected: FAIL because integration API does not exist.

- [ ] **Step 3: Add business API domain objects**

Create `BpmBusinessStartCommand`:

```java
package com.hunyuan.sa.bpm.api.business.domain;

import lombok.Data;

@Data
public class BpmBusinessStartCommand {
    private String businessType;
    private Long businessId;
    private String businessKey;
    private String definitionKey;
    private Long startEmployeeId;
    private String formDataJson;
    private String title;
    private String summary;
}
```

Create `BpmBusinessInstanceStatus`:

```java
package com.hunyuan.sa.bpm.api.business.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BpmBusinessInstanceStatus {
    private Long instanceId;
    private String instanceNo;
    private String businessType;
    private Long businessId;
    private Integer runState;
    private Integer resultState;
    private LocalDateTime lastActionAt;
}
```

Create `BpmBusinessResultEvent`:

```java
package com.hunyuan.sa.bpm.api.business.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BpmBusinessResultEvent {
    private String eventId;
    private Long instanceId;
    private String businessType;
    private Long businessId;
    private Integer resultState;
    private String payloadJson;
    private LocalDateTime occurredAt;
}
```

- [ ] **Step 4: Add API interface**

Create `BpmBusinessProcessApi`:

```java
package com.hunyuan.sa.bpm.api.business;

import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessInstanceStatus;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessResultEvent;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessStartCommand;

public interface BpmBusinessProcessApi {

    Long start(BpmBusinessStartCommand command);

    BpmBusinessInstanceStatus getStatus(String businessType, Long businessId);

    void publishResultEvent(BpmBusinessResultEvent event);
}
```

- [ ] **Step 5: Add minimal API implementation**

Create `BpmBusinessProcessApiImpl`:

```java
package com.hunyuan.sa.bpm.api.business;

import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessInstanceStatus;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessResultEvent;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessStartCommand;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BpmBusinessProcessApiImpl implements BpmBusinessProcessApi {

    @Override
    public Long start(BpmBusinessStartCommand command) {
        validateStartCommand(command);
        throw new UnsupportedOperationException("business process start wiring is implemented after command records are available");
    }

    @Override
    public BpmBusinessInstanceStatus getStatus(String businessType, Long businessId) {
        if (!StringUtils.hasText(businessType)) {
            throw new IllegalArgumentException("businessType不能为空");
        }
        if (businessId == null) {
            throw new IllegalArgumentException("businessId不能为空");
        }
        return null;
    }

    @Override
    public void publishResultEvent(BpmBusinessResultEvent event) {
        if (event == null || !StringUtils.hasText(event.getEventId())) {
            throw new IllegalArgumentException("eventId不能为空");
        }
    }

    private void validateStartCommand(BpmBusinessStartCommand command) {
        if (command == null || !StringUtils.hasText(command.getBusinessType())) {
            throw new IllegalArgumentException("businessType不能为空");
        }
        if (command.getBusinessId() == null) {
            throw new IllegalArgumentException("businessId不能为空");
        }
        if (!StringUtils.hasText(command.getDefinitionKey())) {
            throw new IllegalArgumentException("definitionKey不能为空");
        }
    }
}
```

This implementation intentionally validates the public contract first. Replace the `UnsupportedOperationException` in the same task after command records are added and `BpmInstanceService.startInstance(...)` has a business-facing adapter.

- [ ] **Step 6: Add SQL tables**

Append to `v3.38.0.sql`:

```sql
CREATE TABLE `t_bpm_callback_record` (
  `callback_record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '回调记录ID',
  `event_id` varchar(128) NOT NULL COMMENT '事件ID',
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `business_type` varchar(64) NOT NULL COMMENT '业务类型',
  `business_id` bigint NOT NULL COMMENT '业务ID',
  `callback_status` tinyint NOT NULL COMMENT '回调状态',
  `request_payload_json` longtext NULL COMMENT '请求载荷',
  `response_payload_json` longtext NULL COMMENT '响应载荷',
  `failure_reason` varchar(1000) NULL COMMENT '失败原因',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `next_retry_at` datetime NULL COMMENT '下次重试时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`callback_record_id`),
  UNIQUE KEY `uk_bpm_callback_event` (`event_id`),
  KEY `idx_bpm_callback_status` (`callback_status`, `next_retry_at`),
  KEY `idx_bpm_callback_business` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM业务回调记录';

CREATE TABLE `t_bpm_command_record` (
  `command_record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '命令记录ID',
  `command_key` varchar(128) NOT NULL COMMENT '命令幂等键',
  `command_type` varchar(64) NOT NULL COMMENT '命令类型',
  `instance_id` bigint NULL COMMENT '流程实例ID',
  `business_type` varchar(64) NULL COMMENT '业务类型',
  `business_id` bigint NULL COMMENT '业务ID',
  `command_status` tinyint NOT NULL COMMENT '命令状态',
  `request_payload_json` longtext NULL COMMENT '请求载荷',
  `failure_reason` varchar(1000) NULL COMMENT '失败原因',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`command_record_id`),
  UNIQUE KEY `uk_bpm_command_key` (`command_key`),
  KEY `idx_bpm_command_business` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM命令执行记录';
```

- [ ] **Step 7: Add entities and DAOs**

Create entity classes with `@TableName("t_bpm_callback_record")` and `@TableName("t_bpm_command_record")`.
Include the fields from the SQL exactly, using `LocalDateTime` for datetime columns.
Create DAOs extending `BaseMapper<BpmCallbackRecordEntity>` and `BaseMapper<BpmCommandRecordEntity>`.

- [ ] **Step 8: Implement callback retry service**

Create `BpmBusinessCallbackService`:

```java
@Service
public class BpmBusinessCallbackService {

    @Resource
    private BpmCallbackRecordDao bpmCallbackRecordDao;

    public ResponseDTO<String> retry(Long callbackRecordId) {
        BpmCallbackRecordEntity record = bpmCallbackRecordDao.selectById(callbackRecordId);
        if (record == null) {
            return ResponseDTO.userErrorParam("回调记录不存在");
        }
        if (record.getCallbackStatus() == 1) {
            return ResponseDTO.ok();
        }
        BpmCallbackRecordEntity update = new BpmCallbackRecordEntity();
        update.setCallbackRecordId(callbackRecordId);
        update.setRetryCount(record.getRetryCount() + 1);
        update.setUpdateTime(LocalDateTime.now());
        bpmCallbackRecordDao.updateById(update);
        return ResponseDTO.ok();
    }
}
```

- [ ] **Step 9: Extend architecture isolation test**

In `BpmApiIsolationTest`, include `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api` in the public contract scan and assert it does not contain `org.flowable`.

- [ ] **Step 10: Run tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmBusinessProcessApiTest,BpmBusinessCallbackServiceTest,BpmApiIsolationTest test
```

Expected: PASS with zero failures.

- [ ] **Step 11: Commit**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/api/business `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration `
        hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration `
        hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/architecture/BpmApiIsolationTest.java `
        数据库SQL脚本/mysql/sql-update-log/v3.38.0.sql
git commit -m "feat: 增加BPM业务接入可靠性记录"
```

---

### Task 6: P1.3 Frontend Monitoring and Acceptance Sample

**Files:**
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/integration.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/integration/callback-record-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/integration/command-record-list.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
- Create: `docs/superpowers/specs/2026-07-08-bpm-enterprise-p1-3-business-integration-acceptance.md`

**Interfaces:**
- Produces:
  - `queryBpmCallbackRecordPage(data)`
  - `retryBpmCallbackRecord(callbackRecordId: number)`
  - `queryBpmCommandRecordPage(data)`

- [ ] **Step 1: Write failing frontend module tests**

Add to `bpm-modules.test.ts`:

```ts
expect(viewFiles).toContain('integration/callback-record-list.vue');
expect(viewFiles).toContain('integration/command-record-list.vue');
```

Add to `bpm-api.test.ts`:

```ts
expect(integrationApiSource).toContain('queryBpmCallbackRecordPage');
expect(integrationApiSource).toContain('/bpm/integration/callback/query');
expect(integrationApiSource).toContain('retryBpmCallbackRecord');
expect(integrationApiSource).toContain('/bpm/integration/callback/retry/');
expect(integrationApiSource).toContain('queryBpmCommandRecordPage');
expect(integrationApiSource).toContain('/bpm/integration/command/query');
```

- [ ] **Step 2: Run failing Vitest**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: FAIL because API/view files do not exist.

- [ ] **Step 3: Add integration API**

Create `integration.ts`:

```ts
import { requestClient } from '#/api/request';

export interface BpmCallbackRecordVO {
  businessId: number;
  businessType: string;
  callbackRecordId: number;
  callbackStatus: number;
  eventId: string;
  failureReason?: string;
  instanceId: number;
  retryCount: number;
}

export interface BpmCommandRecordVO {
  businessId?: number;
  businessType?: string;
  commandKey: string;
  commandRecordId: number;
  commandStatus: number;
  commandType: string;
  failureReason?: string;
  instanceId?: number;
}

export function queryBpmCallbackRecordPage(data: Record<string, unknown>) {
  return requestClient.post('/bpm/integration/callback/query', data);
}

export function retryBpmCallbackRecord(callbackRecordId: number) {
  return requestClient.post(`/bpm/integration/callback/retry/${callbackRecordId}`);
}

export function queryBpmCommandRecordPage(data: Record<string, unknown>) {
  return requestClient.post('/bpm/integration/command/query', data);
}
```

- [ ] **Step 4: Add quiet list pages**

Create `callback-record-list.vue` and `command-record-list.vue` following sibling BPM list pages:

- Use the existing ProTable/list pattern in `instance-list.vue`.
- Do not add explanatory page copy.
- Include retry action only for callback records whose status is failed.
- Keep one natural search row; if only one row is needed, disable collapse behavior.

- [ ] **Step 5: Run frontend checks**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: both commands exit 0.

- [ ] **Step 6: Write acceptance record**

Create `docs/superpowers/specs/2026-07-08-bpm-enterprise-p1-3-business-integration-acceptance.md` with:

```markdown
# BPM P1.3 Business Integration Acceptance

## Scope

- Business start/status/result contract exists under Hunyuan BPM API.
- Callback records and command records are persisted.
- Retry operation is available for failed callback records.
- Frontend monitoring pages expose callback and command record lists.

## Verification

- `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmBusinessProcessApiTest,BpmBusinessCallbackServiceTest,BpmApiIsolationTest test`
- `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom`
- `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck`

## Boundaries

- No production business module is forced to depend on the sample.
- No Flowable object appears in business API signatures.
- Callback retry is idempotent by `eventId` or `commandKey`.
```

- [ ] **Step 7: Commit**

```powershell
git add hunyuan-design/apps/hunyuan-system/src/api/system/bpm/integration.ts `
        hunyuan-design/apps/hunyuan-system/src/views/system/bpm/integration/callback-record-list.vue `
        hunyuan-design/apps/hunyuan-system/src/views/system/bpm/integration/command-record-list.vue `
        hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts `
        hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts `
        docs/superpowers/specs/2026-07-08-bpm-enterprise-p1-3-business-integration-acceptance.md
git commit -m "feat: 增加BPM集成可靠性监控"
```

---

### Task 7: P1.4 Controlled Advanced Approval Actions

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmTaskResultEnum.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmTaskController.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmTaskController.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskDelegateForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskAddSignForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskReduceSignForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskRecallForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskAdvancedActionServiceTest.java`

**Interfaces:**
- Produces:
  - `POST /app/bpm/task/delegate`
  - `POST /app/bpm/task/addSign`
  - `POST /app/bpm/task/reduceSign`
  - `POST /app/bpm/task/recall`
  - `POST /bpm/task/adminDelegate`
  - Action types: `DELEGATED`, `ADD_SIGNED`, `REDUCE_SIGNED`, `RECALLED`

- [ ] **Step 1: Write failing delegate test**

Create `BpmTaskAdvancedActionServiceTest`:

```java
@Test
void delegateShouldChangeAssigneeAndWriteActionLog() {
    BpmTaskService service = new BpmTaskService();
    BpmTaskDao taskDao = Mockito.mock(BpmTaskDao.class);
    BpmTaskActionLogDao logDao = Mockito.mock(BpmTaskActionLogDao.class);
    BpmCurrentActorProvider actorProvider = Mockito.mock(BpmCurrentActorProvider.class);
    BpmOrgIdentityGateway identityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
    ReflectionTestUtils.setField(service, "bpmTaskDao", taskDao);
    ReflectionTestUtils.setField(service, "bpmTaskActionLogDao", logDao);
    ReflectionTestUtils.setField(service, "bpmCurrentActorProvider", actorProvider);
    ReflectionTestUtils.setField(service, "bpmOrgIdentityGateway", identityGateway);

    BpmTaskEntity task = new BpmTaskEntity();
    task.setTaskId(1L);
    task.setTaskState(BpmTaskStateEnum.PENDING.getValue());
    task.setAssigneeEmployeeId(100L);
    Mockito.when(taskDao.selectById(1L)).thenReturn(task);
    Mockito.when(actorProvider.getCurrentEmployeeId()).thenReturn(100L);
    Mockito.when(identityGateway.getEmployeeSnapshot(200L))
            .thenReturn(new BpmEmployeeSnapshot(200L, "被委派人", null, null, null));

    BpmTaskDelegateForm form = new BpmTaskDelegateForm();
    form.setTaskId(1L);
    form.setTargetEmployeeId(200L);
    form.setReason("请协助处理");

    ResponseDTO<String> response = service.delegate(form);

    assertThat(response.getOk()).isTrue();
    Mockito.verify(taskDao).updateById(Mockito.argThat(update ->
            Long.valueOf(200L).equals(update.getAssigneeEmployeeId())));
    Mockito.verify(logDao).insert(Mockito.argThat(log ->
            "DELEGATED".equals(log.getActionType())));
}
```

- [ ] **Step 2: Run failing test**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmTaskAdvancedActionServiceTest test
```

Expected: FAIL because advanced action forms/services do not exist.

- [ ] **Step 3: Add forms**

Create the four forms with `taskId`, target IDs where needed, and `reason` or `commentText` fields.
Each form must validate `taskId` with `@NotNull(message = "任务ID不能为空")`.
Each reason/comment must use `@Size(max = 500, message = "原因最多 500 个字符")`.

- [ ] **Step 4: Implement delegate first**

Add to `BpmTaskService`:

```java
public ResponseDTO<String> delegate(BpmTaskDelegateForm form) {
    BpmTaskEntity task = bpmTaskDao.selectById(form.getTaskId());
    if (task == null) {
        return ResponseDTO.userErrorParam("流程任务不存在");
    }
    Long currentEmployeeId = bpmCurrentActorProvider.getCurrentEmployeeId();
    if (!Objects.equals(task.getAssigneeEmployeeId(), currentEmployeeId)) {
        return ResponseDTO.userErrorParam("只能委派自己的待办任务");
    }
    BpmEmployeeSnapshot target = bpmOrgIdentityGateway.getEmployeeSnapshot(form.getTargetEmployeeId());
    BpmTaskEntity update = new BpmTaskEntity();
    update.setTaskId(task.getTaskId());
    update.setAssigneeEmployeeId(target.employeeId());
    update.setAssigneeNameSnapshot(target.employeeName());
    update.setLastActionAt(LocalDateTime.now());
    bpmTaskDao.updateById(update);
    insertActionLog(task, "DELEGATED", form.getReason(), form.getTargetEmployeeId());
    return ResponseDTO.ok();
}
```

If `insertActionLog` does not exist with this signature, add a private helper local to `BpmTaskService` that writes `BpmTaskActionLogEntity` using the existing action log pattern.

- [ ] **Step 5: Implement add sign, reduce sign, recall as narrow P1 actions**

Rules:

- `addSign` creates an extra pending task projection for the same instance and writes `ADD_SIGNED`.
- `reduceSign` cancels a pending extra-sign task and writes `REDUCE_SIGNED`.
- `recall` is allowed only when the instance is running and the start employee is current actor; it sets instance `WAIT_RESUBMIT` and writes `RECALLED`.
- Do not implement arbitrary node jump.
- Do not directly update Flowable runtime tables.

- [ ] **Step 6: Wire app/admin endpoints**

Add app endpoints for user-owned actions:

```java
@PostMapping("/app/bpm/task/delegate")
public ResponseDTO<String> delegate(@RequestBody @Valid BpmTaskDelegateForm form) {
    return bpmTaskService.delegate(form);
}
```

Add admin endpoints only where the action is intentionally admin-owned, using `@SaCheckPermission("bpm:task:update")`.

- [ ] **Step 7: Run targeted tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmTaskAdvancedActionServiceTest,BpmRuntimeCommandServiceTest test
```

Expected: PASS with zero failures.

- [ ] **Step 8: Commit**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmTaskResultEnum.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmTaskController.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmTaskController.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskDelegateForm.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskAddSignForm.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskReduceSignForm.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmTaskRecallForm.java `
        hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskAdvancedActionServiceTest.java
git commit -m "feat: 增加BPM高级审批动作"
```

---

### Task 8: P1 Final Verification and Acceptance

**Files:**
- Create: `docs/superpowers/specs/2026-07-08-bpm-enterprise-p1-2-runtime-governance-acceptance.md`
- Create: `docs/superpowers/specs/2026-07-08-bpm-enterprise-p1-4-approval-semantics-acceptance.md`
- Modify: `docs/superpowers/specs/2026-07-08-bpm-enterprise-p1-design.md` only if actual implementation boundaries differ and the change is approved.

**Interfaces:**
- Consumes all previous task deliverables.
- Produces final acceptance evidence and the go/no-go status for P1.

- [ ] **Step 1: Run full BPM backend tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

Expected: PASS with zero failures.

- [ ] **Step 2: Run Flowable compatibility test**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test
```

Expected: PASS with zero failures.

- [ ] **Step 3: Run frontend BPM contract tests**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts apps/hunyuan-system/src/components/bpm/adapters/bpm-designer-adapters.test.ts --dom
```

Expected: PASS.

- [ ] **Step 4: Run frontend typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: command exits 0.

- [ ] **Step 5: Run architecture leak check**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmApiIsolationTest test
```

Expected: PASS and no `org.flowable` appears in public API contracts.

- [ ] **Step 6: Run live browser acceptance if services are available**

Precheck:

```powershell
Test-NetConnection 127.0.0.1 -Port 5788
Test-NetConnection 127.0.0.1 -Port 1024
Test-NetConnection 127.0.0.1 -Port 8934
```

Expected:

- `TcpTestSucceeded : True` for frontend.
- `TcpTestSucceeded : True` for backend.
- `TcpTestSucceeded : True` for persistent controller when browser proof is requested.

If any precheck fails, record the missing service in the acceptance record and do not invent browser proof.

- [ ] **Step 7: Write final acceptance notes**

For each acceptance record, include:

```markdown
## Verification

- Command:
- Result:
- Evidence:

## Boundaries

- What passed:
- What was not covered:
- What remains for P2:
```

- [ ] **Step 8: Commit final acceptance**

```powershell
git add docs/superpowers/specs/2026-07-08-bpm-enterprise-p1-2-runtime-governance-acceptance.md `
        docs/superpowers/specs/2026-07-08-bpm-enterprise-p1-4-approval-semantics-acceptance.md
git commit -m "docs: 记录BPM企业级P1验收"
```

---

## Self-Review

### Spec coverage

- Definition governance is covered by Tasks 1-3.
- Runtime governance is covered by Task 4 and Task 8.
- Business integration reliability is covered by Tasks 5-6 and Task 8.
- Approval semantics are covered by Task 7 and Task 8.
- Frontend contracts are covered by Tasks 3 and 6.
- Flowable isolation is covered by Tasks 5 and 8.
- Acceptance records are covered by Tasks 3, 6, and 8.

### Placeholder scan

This plan intentionally avoids placeholder markers and undefined task names. Any implementation that discovers a mismatch must update the plan or create a narrower follow-up plan before editing unrelated files.

### Type consistency

- `BpmBusinessStartCommand`, `BpmBusinessInstanceStatus`, and `BpmBusinessResultEvent` are defined in Task 5 and consumed only after Task 5.
- `BpmDefinitionValidationReportVO` and `BpmDefinitionDiffVO` are defined in Task 1 and consumed by Task 3.
- `BpmAdminInstanceCancelForm` and `BpmAdminTaskTransferForm` are defined in Task 4 and consumed only in Task 4.
- Advanced task action forms are defined in Task 7 and consumed only in Task 7.

### Execution rule

Before executing this plan, create or switch to an appropriate `codex/` feature branch or isolated worktree. Keep unrelated dirty files untouched. Each task must finish with its own verification output before commit.
