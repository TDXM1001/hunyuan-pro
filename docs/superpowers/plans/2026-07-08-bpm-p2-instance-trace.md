# BPM P2 Instance Trace Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first P2.1 reliability slice: an admin-only BPM instance trace that gathers Hunyuan instance detail, current tasks, action logs, callback records, and command records by `instanceId`.

**Architecture:** Keep Flowable hidden behind the existing Hunyuan BPM runtime projections. Reuse the current `BpmInstanceService.getDetail` result for instance/task/action-log data, add `instanceId` filters to the integration record service, then expose one admin trace endpoint and a compact admin-only drawer section. Do not create a new event table in this slice; the trace is an honest aggregation of existing Hunyuan records.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, JUnit 5, Mockito, AssertJ, Vue 3, TypeScript, Element Plus, Vitest.

## Global Constraints

- Production code, contracts, routes, permissions, menus, tests, docs, and verification artifacts must stay in `E:\my-project\hunyuan-pro`.
- `E:\my-project\huanyuan-pro-jichu\yudao-ui-admin-vue3-master` and `E:\my-project\huanyuan-pro-jichu\ruoyi-vue-pro-master` are reference lines only; borrow mechanisms, not code or API names.
- Public Hunyuan BPM APIs must not expose Flowable native objects, names, or IDs.
- Do not add new dependencies.
- Keep P2.1 focused on instance trace aggregation; notification delivery records, callback executor retry semantics, and a sample business module belong to later P2 slices.
- Runtime employee-side detail must not show platform reliability failure details; the trace is admin-only.
- Use existing source-level frontend tests before browser proof.

---

## Scope Decisions

- The trace endpoint path is `/bpm/instance/trace/{instanceId}` and uses the existing permission `bpm:instance:detail`.
- The response VO is Hunyuan-native: `BpmInstanceTraceVO` contains `BpmInstanceDetailVO`, `List<BpmTaskVO>`, `List<BpmTaskActionLogVO>`, `List<BpmCallbackRecordVO>`, and `List<BpmCommandRecordVO>`.
- Integration record pages and APIs gain `instanceId` as a query parameter, but this plan does not redesign those list pages.
- Callback and command records returned for trace are ordered ascending by `createTime` and primary key so the admin can read them as a timeline.
- The frontend drawer loads trace data only when `open(instanceId, 'admin')` is used. Runtime callers continue loading only `/app/bpm/instance/detail/{instanceId}`.

## File Structure

- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/form/BpmCallbackRecordQueryForm.java`: add `instanceId` to callback record query form.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/form/BpmCommandRecordQueryForm.java`: add `instanceId` to command record query form.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessIntegrationRecordService.java`: add `instanceId` filters and by-instance list methods.
- Modify `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessIntegrationRecordServiceTest.java`: cover `instanceId` query wiring and by-instance list mapping.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceTraceVO.java`: admin trace response contract.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceTraceService.java`: aggregate detail, tasks, action logs, callbacks, and commands.
- Create `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmInstanceTraceServiceTest.java`: test success and missing-instance responses.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmInstanceController.java`: add admin trace endpoint.
- Modify `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`: add trace record type and `getBpmAdminInstanceTrace`.
- Modify `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/integration.ts`: add `instanceId` query parameters.
- Modify `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`: pin new API route and payload contract.
- Modify `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue`: show admin-only reliability trace section.
- Modify `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`: pin admin-only trace gate and UI contract.
- Create `docs/superpowers/specs/2026-07-08-bpm-p2-instance-trace-acceptance.md`: record the exact verification evidence from execution.

---

### Task 1: Integration Record Instance Filters

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/form/BpmCallbackRecordQueryForm.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/form/BpmCommandRecordQueryForm.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessIntegrationRecordService.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessIntegrationRecordServiceTest.java`

**Interfaces:**
- Consumes: existing `BpmCallbackRecordEntity`, `BpmCommandRecordEntity`, `BpmCallbackRecordVO`, `BpmCommandRecordVO`.
- Produces: `BpmCallbackRecordQueryForm#getInstanceId()`, `BpmCommandRecordQueryForm#getInstanceId()`, `BpmBusinessIntegrationRecordService#queryCallbackRecordsByInstanceId(Long)`, `BpmBusinessIntegrationRecordService#queryCommandRecordsByInstanceId(Long)`.

- [ ] **Step 1: Write failing tests for `instanceId` query support and by-instance lists**

In `queryCallbackPageShouldReturnCallbackRecordVOs`, add an `instanceId` query value and response assertion:

```java
        queryForm.setInstanceId(88L);
```

```java
        assertThat(response.getData().getList().get(0).getInstanceId()).isEqualTo(88L);
```

In `queryCommandPageShouldReturnCommandRecordVOs`, add an `instanceId` query value and response assertion:

```java
        queryForm.setInstanceId(88L);
```

```java
        assertThat(response.getData().getList().get(0).getInstanceId()).isEqualTo(88L);
```

Add these tests before `setField`:

```java

    @Test
    void queryCallbackRecordsByInstanceIdShouldReturnMappedRecords() {
        BpmCallbackRecordEntity record = new BpmCallbackRecordEntity();
        record.setCallbackRecordId(1L);
        record.setEventId("event-88");
        record.setInstanceId(88L);
        record.setBusinessType("expense");
        record.setBusinessId(1001L);
        record.setCallbackStatus(2);
        record.setRetryCount(1);
        when(bpmCallbackRecordDao.selectList(any())).thenReturn(List.of(record));

        List<BpmCallbackRecordVO> records = recordService.queryCallbackRecordsByInstanceId(88L);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getCallbackRecordId()).isEqualTo(1L);
        assertThat(records.get(0).getInstanceId()).isEqualTo(88L);
        assertThat(records.get(0).getEventId()).isEqualTo("event-88");
    }

    @Test
    void queryCommandRecordsByInstanceIdShouldReturnMappedRecords() {
        BpmCommandRecordEntity record = new BpmCommandRecordEntity();
        record.setCommandRecordId(2L);
        record.setCommandKey("START:expense:1001:expense_apply");
        record.setCommandType("START");
        record.setInstanceId(88L);
        record.setBusinessType("expense");
        record.setBusinessId(1001L);
        record.setCommandStatus(1);
        when(bpmCommandRecordDao.selectList(any())).thenReturn(List.of(record));

        List<BpmCommandRecordVO> records = recordService.queryCommandRecordsByInstanceId(88L);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getCommandRecordId()).isEqualTo(2L);
        assertThat(records.get(0).getInstanceId()).isEqualTo(88L);
        assertThat(records.get(0).getCommandKey()).isEqualTo("START:expense:1001:expense_apply");
    }
```

- [ ] **Step 2: Run focused test to verify it fails**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmBusinessIntegrationRecordServiceTest test
```

Expected: FAIL because `setInstanceId`, `queryCallbackRecordsByInstanceId`, and `queryCommandRecordsByInstanceId` do not exist.

- [ ] **Step 3: Add `instanceId` to both query forms**

In `BpmCallbackRecordQueryForm.java`, add the field after `eventId`:

```java
    @Schema(description = "流程实例ID")
    private Long instanceId;
```

In `BpmCommandRecordQueryForm.java`, add the field after `commandKey`:

```java
    @Schema(description = "流程实例ID")
    private Long instanceId;
```

- [ ] **Step 4: Implement instance filters and list methods**

In `BpmBusinessIntegrationRecordService.java`, update `buildCallbackQuery`:

```java
    private LambdaQueryWrapper<BpmCallbackRecordEntity> buildCallbackQuery(BpmCallbackRecordQueryForm queryForm) {
        return Wrappers.<BpmCallbackRecordEntity>lambdaQuery()
                .eq(StringUtils.isNotBlank(queryForm.getEventId()), BpmCallbackRecordEntity::getEventId, queryForm.getEventId())
                .eq(queryForm.getInstanceId() != null, BpmCallbackRecordEntity::getInstanceId, queryForm.getInstanceId())
                .eq(StringUtils.isNotBlank(queryForm.getBusinessType()), BpmCallbackRecordEntity::getBusinessType, queryForm.getBusinessType())
                .eq(queryForm.getBusinessId() != null, BpmCallbackRecordEntity::getBusinessId, queryForm.getBusinessId())
                .eq(queryForm.getCallbackStatus() != null, BpmCallbackRecordEntity::getCallbackStatus, queryForm.getCallbackStatus())
                .orderByDesc(BpmCallbackRecordEntity::getCallbackRecordId);
    }
```

Update `buildCommandQuery`:

```java
    private LambdaQueryWrapper<BpmCommandRecordEntity> buildCommandQuery(BpmCommandRecordQueryForm queryForm) {
        return Wrappers.<BpmCommandRecordEntity>lambdaQuery()
                .eq(StringUtils.isNotBlank(queryForm.getCommandKey()), BpmCommandRecordEntity::getCommandKey, queryForm.getCommandKey())
                .eq(queryForm.getInstanceId() != null, BpmCommandRecordEntity::getInstanceId, queryForm.getInstanceId())
                .eq(StringUtils.isNotBlank(queryForm.getBusinessType()), BpmCommandRecordEntity::getBusinessType, queryForm.getBusinessType())
                .eq(queryForm.getBusinessId() != null, BpmCommandRecordEntity::getBusinessId, queryForm.getBusinessId())
                .eq(queryForm.getCommandStatus() != null, BpmCommandRecordEntity::getCommandStatus, queryForm.getCommandStatus())
                .orderByDesc(BpmCommandRecordEntity::getCommandRecordId);
    }
```

Add public list methods after `queryCommandPage`:

```java
    public List<BpmCallbackRecordVO> queryCallbackRecordsByInstanceId(Long instanceId) {
        if (instanceId == null) {
            return List.of();
        }
        return bpmCallbackRecordDao.selectList(Wrappers.<BpmCallbackRecordEntity>lambdaQuery()
                        .eq(BpmCallbackRecordEntity::getInstanceId, instanceId)
                        .orderByAsc(BpmCallbackRecordEntity::getCreateTime, BpmCallbackRecordEntity::getCallbackRecordId))
                .stream()
                .map(this::toCallbackRecordVO)
                .toList();
    }

    public List<BpmCommandRecordVO> queryCommandRecordsByInstanceId(Long instanceId) {
        if (instanceId == null) {
            return List.of();
        }
        return bpmCommandRecordDao.selectList(Wrappers.<BpmCommandRecordEntity>lambdaQuery()
                        .eq(BpmCommandRecordEntity::getInstanceId, instanceId)
                        .orderByAsc(BpmCommandRecordEntity::getCreateTime, BpmCommandRecordEntity::getCommandRecordId))
                .stream()
                .map(this::toCommandRecordVO)
                .toList();
    }
```

- [ ] **Step 5: Run focused test to verify it passes**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmBusinessIntegrationRecordServiceTest test
```

Expected: PASS.

- [ ] **Step 6: Commit this task**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/form/BpmCallbackRecordQueryForm.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/form/BpmCommandRecordQueryForm.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessIntegrationRecordService.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessIntegrationRecordServiceTest.java
git commit -m "feat: add bpm integration instance filters"
```

---

### Task 2: Backend Instance Trace Service and Admin API

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceTraceVO.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceTraceService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmInstanceTraceServiceTest.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmInstanceController.java`

**Interfaces:**
- Consumes: `BpmInstanceService#getDetail(Long)`, `BpmBusinessIntegrationRecordService#queryCallbackRecordsByInstanceId(Long)`, `BpmBusinessIntegrationRecordService#queryCommandRecordsByInstanceId(Long)`.
- Produces: `BpmInstanceTraceVO`, `BpmInstanceTraceService#getTrace(Long)`, admin endpoint `GET /bpm/instance/trace/{instanceId}`.

- [ ] **Step 1: Write the failing service test**

Create `BpmInstanceTraceServiceTest.java`:

```java
package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCallbackRecordVO;
import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCommandRecordVO;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessIntegrationRecordService;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceTraceVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskActionLogVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceTraceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class BpmInstanceTraceServiceTest {

    private BpmInstanceTraceService traceService;

    private BpmInstanceService bpmInstanceService;

    private BpmBusinessIntegrationRecordService integrationRecordService;

    @BeforeEach
    void setUp() {
        traceService = new BpmInstanceTraceService();
        bpmInstanceService = Mockito.mock(BpmInstanceService.class);
        integrationRecordService = Mockito.mock(BpmBusinessIntegrationRecordService.class);
        setField(traceService, "bpmInstanceService", bpmInstanceService);
        setField(traceService, "integrationRecordService", integrationRecordService);
    }

    @Test
    void getTraceShouldAggregateInstanceTasksActionsCallbacksAndCommands() {
        BpmTaskVO task = new BpmTaskVO();
        task.setTaskId(11L);
        task.setTaskName("Manager approval");
        BpmTaskActionLogVO actionLog = new BpmTaskActionLogVO();
        actionLog.setActionLogId(21L);
        actionLog.setActionType("APPROVED");
        BpmInstanceDetailVO detail = new BpmInstanceDetailVO();
        detail.setInstanceId(88L);
        detail.setInstanceNo("DK20260708NO00001");
        detail.setTitle("Expense approval");
        detail.setCurrentTasks(List.of(task));
        detail.setActionLogs(List.of(actionLog));
        BpmCallbackRecordVO callbackRecord = new BpmCallbackRecordVO();
        callbackRecord.setCallbackRecordId(31L);
        callbackRecord.setInstanceId(88L);
        BpmCommandRecordVO commandRecord = new BpmCommandRecordVO();
        commandRecord.setCommandRecordId(41L);
        commandRecord.setInstanceId(88L);
        when(bpmInstanceService.getDetail(88L)).thenReturn(ResponseDTO.ok(detail));
        when(integrationRecordService.queryCallbackRecordsByInstanceId(88L)).thenReturn(List.of(callbackRecord));
        when(integrationRecordService.queryCommandRecordsByInstanceId(88L)).thenReturn(List.of(commandRecord));

        ResponseDTO<BpmInstanceTraceVO> response = traceService.getTrace(88L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getInstance().getInstanceId()).isEqualTo(88L);
        assertThat(response.getData().getCurrentTasks()).hasSize(1);
        assertThat(response.getData().getActionLogs()).hasSize(1);
        assertThat(response.getData().getCallbackRecords()).hasSize(1);
        assertThat(response.getData().getCommandRecords()).hasSize(1);
    }

    @Test
    void getTraceShouldReturnDataNotExistWhenInstanceIsMissing() {
        when(bpmInstanceService.getDetail(404L)).thenReturn(ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST));

        ResponseDTO<BpmInstanceTraceVO> response = traceService.getTrace(404L);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getCode()).isEqualTo(UserErrorCode.DATA_NOT_EXIST.getCode());
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("设置测试字段失败: " + fieldName, ex);
        }
    }
}
```

- [ ] **Step 2: Run trace service test to verify it fails**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmInstanceTraceServiceTest test
```

Expected: FAIL because `BpmInstanceTraceVO` and `BpmInstanceTraceService` do not exist.

- [ ] **Step 3: Create `BpmInstanceTraceVO`**

Create `BpmInstanceTraceVO.java`:

```java
package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCallbackRecordVO;
import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCommandRecordVO;
import lombok.Data;

import java.util.List;

/**
 * BPM 实例可靠性追踪。
 */
@Data
public class BpmInstanceTraceVO {

    private BpmInstanceDetailVO instance;

    private List<BpmTaskVO> currentTasks;

    private List<BpmTaskActionLogVO> actionLogs;

    private List<BpmCallbackRecordVO> callbackRecords;

    private List<BpmCommandRecordVO> commandRecords;
}
```

- [ ] **Step 4: Create `BpmInstanceTraceService`**

Create `BpmInstanceTraceService.java`:

```java
package com.hunyuan.sa.bpm.module.runtime.service;

import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessIntegrationRecordService;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceTraceVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * BPM 实例可靠性追踪服务。
 */
@Service
public class BpmInstanceTraceService {

    @Resource
    private BpmInstanceService bpmInstanceService;

    @Resource
    private BpmBusinessIntegrationRecordService integrationRecordService;

    public ResponseDTO<BpmInstanceTraceVO> getTrace(Long instanceId) {
        ResponseDTO<BpmInstanceDetailVO> detailResponse = bpmInstanceService.getDetail(instanceId);
        if (!Boolean.TRUE.equals(detailResponse.getOk())) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        BpmInstanceDetailVO detail = detailResponse.getData();
        BpmInstanceTraceVO trace = new BpmInstanceTraceVO();
        trace.setInstance(detail);
        trace.setCurrentTasks(detail.getCurrentTasks() == null ? List.of() : detail.getCurrentTasks());
        trace.setActionLogs(detail.getActionLogs() == null ? List.of() : detail.getActionLogs());
        trace.setCallbackRecords(integrationRecordService.queryCallbackRecordsByInstanceId(instanceId));
        trace.setCommandRecords(integrationRecordService.queryCommandRecordsByInstanceId(instanceId));
        return ResponseDTO.ok(trace);
    }
}
```

- [ ] **Step 5: Add admin controller endpoint**

Modify `AdminBpmInstanceController.java`.

Add imports:

```java
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceTraceVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceTraceService;
```

Add field:

```java
    @Resource
    private BpmInstanceTraceService bpmInstanceTraceService;
```

Add endpoint after `detail`:

```java
    @Operation(summary = "查询流程实例可靠性追踪")
    @GetMapping("/bpm/instance/trace/{instanceId}")
    @SaCheckPermission("bpm:instance:detail")
    public ResponseDTO<BpmInstanceTraceVO> trace(@PathVariable Long instanceId) {
        return bpmInstanceTraceService.getTrace(instanceId);
    }
```

- [ ] **Step 6: Run backend focused tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmBusinessIntegrationRecordServiceTest,BpmInstanceTraceServiceTest' test
```

Expected: PASS.

- [ ] **Step 7: Commit this task**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceTraceVO.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceTraceService.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmInstanceTraceServiceTest.java hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmInstanceController.java
git commit -m "feat: add bpm instance trace api"
```

---

### Task 3: Frontend API Contracts

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/integration.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`

**Interfaces:**
- Consumes: backend `GET /bpm/instance/trace/{instanceId}`, backend integration query forms with `instanceId`.
- Produces: `BpmInstanceTraceRecord`, `getBpmAdminInstanceTrace(instanceId: number)`, `instanceId` payload support in callback and command page queries.

- [ ] **Step 1: Write failing API contract needles**

In `bpm-api.test.ts`, update the `runtime` needles:

```ts
      '/bpm/instance/detail/',
      '/bpm/instance/trace/',
      'getBpmAdminInstanceTrace',
      'BpmInstanceTraceRecord',
      '/bpm/task/detail/',
```

Update the `integration` needles:

```ts
      'queryBpmCallbackRecordPage',
      '/bpm/integration/callback/query',
      'instanceId: data.instanceId',
      'retryBpmCallbackRecord',
      '/bpm/integration/callback/retry/',
      'queryBpmCommandRecordPage',
      '/bpm/integration/command/query',
```

- [ ] **Step 2: Run frontend API test to verify it fails**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts --dom
```

Expected: FAIL because the trace API and `instanceId` payload needles do not exist.

- [ ] **Step 3: Add trace types and API function**

In `runtime.ts`, add this type import below the existing `PageResult` import:

```ts
import type {
  BpmCallbackRecordVO,
  BpmCommandRecordVO,
} from '#/api/system/bpm/integration';
```

Add the trace record interface after `BpmInstanceDetailRecord`:

```ts
export interface BpmInstanceTraceRecord {
  actionLogs: BpmTaskActionLogRecord[];
  callbackRecords: BpmCallbackRecordVO[];
  commandRecords: BpmCommandRecordVO[];
  currentTasks: BpmTaskRecord[];
  instance: BpmInstanceDetailRecord;
}
```

Add the admin trace function after `getBpmAdminInstanceDetail`:

```ts
export async function getBpmAdminInstanceTrace(instanceId: number) {
  return requestClient.get<BpmInstanceTraceRecord>(
    `/bpm/instance/trace/${instanceId}`,
  );
}
```

- [ ] **Step 4: Add `instanceId` to integration query params and request bodies**

In `integration.ts`, update `BpmCallbackRecordPageQueryParams`:

```ts
export interface BpmCallbackRecordPageQueryParams {
  businessId?: null | number;
  businessType?: string;
  callbackStatus?: null | number;
  eventId?: string;
  instanceId?: null | number;
  pageNum: number;
  pageSize: number;
}
```

Update `BpmCommandRecordPageQueryParams`:

```ts
export interface BpmCommandRecordPageQueryParams {
  businessId?: null | number;
  businessType?: string;
  commandKey?: string;
  commandStatus?: null | number;
  instanceId?: null | number;
  pageNum: number;
  pageSize: number;
}
```

Add `instanceId` to both request bodies:

```ts
      instanceId: data.instanceId ?? undefined,
```

- [ ] **Step 5: Run frontend API test to verify it passes**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts --dom
```

Expected: PASS.

- [ ] **Step 6: Commit this task**

```powershell
git add hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts hunyuan-design/apps/hunyuan-system/src/api/system/bpm/integration.ts hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts
git commit -m "feat: add bpm instance trace frontend api"
```

---

### Task 4: Admin-Only Reliability Trace Drawer Section

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

**Interfaces:**
- Consumes: `getBpmAdminInstanceTrace(instanceId: number)` and `BpmInstanceTraceRecord`.
- Produces: admin-only drawer section with trace counts, callback records, and command records. Runtime source keeps the existing detail-only behavior.

- [ ] **Step 1: Write failing module contract needles**

In `bpm-modules.test.ts`, add these assertions to the existing test named `keeps the runtime detail drawer in an explicit error state on detail-load failure`:

```ts
    expect(detailSource).toContain('getBpmAdminInstanceTrace');
    expect(detailSource).toContain("source === 'admin'");
    expect(detailSource).toContain('trace.value = undefined;');
    expect(detailSource).toContain('可靠性追踪');
    expect(detailSource).toContain('callbackRecords');
    expect(detailSource).toContain('commandRecords');
```

- [ ] **Step 2: Run module contract test to verify it fails**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: FAIL because the drawer does not import the trace API or render the trace section.

- [ ] **Step 3: Update drawer imports and state**

In `bpm-instance-detail-drawer.vue`, update the type import:

```ts
import type {
  BpmInstanceDetailRecord,
  BpmInstanceTraceRecord,
} from '#/api/system/bpm/runtime';
```

Update the runtime API import:

```ts
import {
  getBpmAdminInstanceDetail,
  getBpmAdminInstanceTrace,
  getBpmInstanceDetail,
} from '#/api/system/bpm/runtime';
```

Add Element Plus table imports:

```ts
  ElTable,
  ElTableColumn,
```

Add trace state after `detail`:

```ts
const trace = ref<BpmInstanceTraceRecord>();
```

Add computed collections after `actionLogs`:

```ts
const callbackRecords = computed(() => trace.value?.callbackRecords ?? []);
const commandRecords = computed(() => trace.value?.commandRecords ?? []);
const traceCurrentTasks = computed(() => trace.value?.currentTasks ?? []);
const traceActionLogs = computed(() => trace.value?.actionLogs ?? []);
```

- [ ] **Step 4: Gate trace loading to admin source**

Replace the `try` block inside `open` with:

```ts
  try {
    if (source === 'admin') {
      const [detailRecord, traceRecord] = await Promise.all([
        getBpmAdminInstanceDetail(instanceId),
        getBpmAdminInstanceTrace(instanceId),
      ]);
      detail.value = detailRecord;
      trace.value = traceRecord;
    } else {
      detail.value = await getBpmInstanceDetail(instanceId);
    }
  } catch (error: any) {
```

Add reset before `loadErrorMessage.value = '';`:

```ts
  trace.value = undefined;
```

- [ ] **Step 5: Render the admin-only trace section**

Insert this template block after the action-log section:

```vue
      <template v-if="trace">
        <div class="bpm-instance-detail__section-title">可靠性追踪</div>
        <div class="bpm-instance-detail__trace-summary">
          <div class="bpm-instance-detail__trace-item">
            <span>当前任务</span>
            <strong>{{ traceCurrentTasks.length }}</strong>
          </div>
          <div class="bpm-instance-detail__trace-item">
            <span>动作轨迹</span>
            <strong>{{ traceActionLogs.length }}</strong>
          </div>
          <div class="bpm-instance-detail__trace-item">
            <span>回调记录</span>
            <strong>{{ callbackRecords.length }}</strong>
          </div>
          <div class="bpm-instance-detail__trace-item">
            <span>命令记录</span>
            <strong>{{ commandRecords.length }}</strong>
          </div>
        </div>

        <div class="bpm-instance-detail__sub-title">回调记录</div>
        <ElTable
          v-if="callbackRecords.length > 0"
          :data="callbackRecords"
          border
          size="small"
        >
          <ElTableColumn label="事件ID" min-width="150" prop="eventId" show-overflow-tooltip />
          <ElTableColumn label="业务类型" min-width="100" prop="businessType" />
          <ElTableColumn label="业务ID" min-width="90" prop="businessId" />
          <ElTableColumn label="状态" min-width="80" prop="callbackStatus" />
          <ElTableColumn label="重试" min-width="70" prop="retryCount" />
          <ElTableColumn label="失败原因" min-width="160" prop="failureReason" show-overflow-tooltip />
        </ElTable>
        <ElEmpty v-else description="暂无回调记录" />

        <div class="bpm-instance-detail__sub-title">命令记录</div>
        <ElTable
          v-if="commandRecords.length > 0"
          :data="commandRecords"
          border
          size="small"
        >
          <ElTableColumn label="命令键" min-width="180" prop="commandKey" show-overflow-tooltip />
          <ElTableColumn label="命令类型" min-width="90" prop="commandType" />
          <ElTableColumn label="业务类型" min-width="100" prop="businessType" />
          <ElTableColumn label="业务ID" min-width="90" prop="businessId" />
          <ElTableColumn label="状态" min-width="80" prop="commandStatus" />
          <ElTableColumn label="失败原因" min-width="160" prop="failureReason" show-overflow-tooltip />
        </ElTable>
        <ElEmpty v-else description="暂无命令记录" />
      </template>
```

- [ ] **Step 6: Add compact styles**

Add these styles before `.bpm-instance-detail__timeline`:

```css
.bpm-instance-detail__trace-summary {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.bpm-instance-detail__trace-item {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 54px;
  padding: 8px 10px;
}

.bpm-instance-detail__trace-item span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.bpm-instance-detail__trace-item strong {
  color: var(--el-text-color-primary);
  font-size: 18px;
  line-height: 24px;
}

.bpm-instance-detail__sub-title {
  color: var(--el-text-color-regular);
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
}
```

- [ ] **Step 7: Run module contract test**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: PASS.

- [ ] **Step 8: Commit this task**

```powershell
git add hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts
git commit -m "feat: show admin bpm instance reliability trace"
```

---

### Task 5: Verification and Acceptance Record

**Files:**
- Create: `docs/superpowers/specs/2026-07-08-bpm-p2-instance-trace-acceptance.md`

**Interfaces:**
- Consumes: all implementation tasks.
- Produces: a repo-local acceptance record with commands, observed results, and known boundaries.

- [ ] **Step 1: Run backend targeted gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmBusinessIntegrationRecordServiceTest,BpmInstanceTraceServiceTest' test
```

Expected: PASS.

- [ ] **Step 2: Run backend module gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

Expected: PASS.

- [ ] **Step 3: Run frontend BPM source contract gates**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: PASS.

- [ ] **Step 4: Run frontend package typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS.

- [ ] **Step 5: Run Flowable boundary regression**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test
```

Expected: PASS.

- [ ] **Step 6: Create acceptance record**

Create `2026-07-08-bpm-p2-instance-trace-acceptance.md` with this content, replacing each command result with the observed PASS output line and timestamp from the execution terminal:

```markdown
# BPM P2.1 Instance Trace Acceptance

## Scope

- Added admin-only BPM instance trace endpoint: `GET /bpm/instance/trace/{instanceId}`.
- Added `instanceId` filters for callback and command integration records.
- Added frontend trace API and admin-only reliability trace section in the existing instance detail drawer.
- Kept runtime employee-side detail unchanged.
- Kept Flowable native objects out of public API and frontend types.

## Verification

| Gate | Command | Result |
| --- | --- | --- |
| Backend targeted | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmBusinessIntegrationRecordServiceTest,BpmInstanceTraceServiceTest' test` | PASS |
| Backend module | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test` | PASS |
| Frontend BPM contracts | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom` | PASS |
| Frontend typecheck | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck` | PASS |
| Flowable boundary | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test` | PASS |

## Boundaries

- This slice aggregates existing action logs, callback records, and command records; it does not create the full BPM event ledger yet.
- Notification delivery records are not included in this slice.
- Callback execution and retry behavior remains the current implementation path.
- Browser proof is not required for this source-level slice unless the admin instance drawer behavior becomes disputed.
```

- [ ] **Step 7: Commit verification record**

```powershell
git add docs/superpowers/specs/2026-07-08-bpm-p2-instance-trace-acceptance.md
git commit -m "docs: record bpm p2 instance trace acceptance"
```

---

## Self-Review Checklist

- Spec coverage: P2.1 instance trace is covered by backend aggregation, admin API, frontend API, admin-only drawer display, and focused verification. P2.2 notification records, P2.3 callback executor retry, and P2.4 business sample are intentionally outside this plan.
- Placeholder scan: every code-changing step includes concrete file names, code blocks, commands, and expected outcomes.
- Type consistency: backend produces `BpmInstanceTraceVO`; frontend consumes it as `BpmInstanceTraceRecord`; method names are `getTrace`, `getBpmAdminInstanceTrace`, `queryCallbackRecordsByInstanceId`, and `queryCommandRecordsByInstanceId`.
- Boundary check: no new dependency, no Flowable public exposure, no reference-repo route names, and no runtime employee-side reliability failure disclosure.
