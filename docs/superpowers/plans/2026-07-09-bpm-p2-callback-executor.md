# BPM P2.3 Callback Executor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the P2.3 BPM callback executor so callback records can be executed, retried, and manually compensated through one Hunyuan-native reliability path.

**Architecture:** Extend the existing `t_bpm_callback_record` contract with compensation audit fields, then add a small handler/executor layer inside `hunyuan-bpm`. Manual retry, scheduled retry, and failure-to-compensation transitions all use the same executor; the frontend only surfaces the existing reliability state and the compensation action.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, JUnit 5, Mockito, AssertJ, Vue 3, TypeScript, Element Plus, Vitest, MySQL update SQL.

## Global Constraints

- Production code, contracts, routes, permissions, menus, tests, docs, and verification artifacts must stay in `E:\my-project\hunyuan-pro`.
- Yudao and RuoYi are reference lines only; borrow mechanisms, not code or API names.
- Public Hunyuan BPM APIs must not expose Flowable native objects, names, or IDs.
- Do not add new dependencies.
- P2.3 covers callback execution, retry, scheduled scan, and manual compensation only.
- Do not implement a complete BPM event ledger in this slice.
- Do not implement a generic HTTP callback node platform in this slice.
- Do not introduce MQ, a new Job platform, or third-party scheduling dependencies.
- Manual retry and automatic retry must use the same executor path.
- Runtime employee-side detail must not show platform callback failure internals.

---

## File Structure

- Create `数据库SQL脚本/mysql/sql-update-log/v3.41.0.sql`: add compensation audit fields to `t_bpm_callback_record`.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmCallbackStatusEnum.java`: callback status values `0/1/2/3/4`.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/entity/BpmCallbackRecordEntity.java`: add `compensatedAt`, `compensatedBy`, `compensationReason`.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/form/BpmCallbackCompensateForm.java`: manual compensation request form.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/vo/BpmCallbackRecordVO.java`: expose compensation audit fields.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessIntegrationRecordService.java`: map compensation fields.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackContext.java`: immutable callback execution context.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackResult.java`: handler result contract.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackHandler.java`: business callback handler extension point.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackTriggerType.java`: trigger source enum.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackExecuteResult.java`: executor result record.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackExecutor.java`: unified executor and due-record scanner.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackService.java`: route retry and compensation through the executor/data contract.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackScheduler.java`: scheduled scanner.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmIntegrationController.java`: add compensation endpoint.
- Update backend tests under `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/`.
- Modify `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/integration.ts`: add compensation fields and API.
- Modify `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/integration/callback-record-list.vue`: show retry/compensation state and actions.
- Modify `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue`: show callback status text and next retry in admin reliability trace.
- Modify `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`: pin compensation API contract.
- Modify `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`: pin callback list and drawer reliability text.
- Create `docs/superpowers/specs/2026-07-09-bpm-p2-callback-executor-acceptance.md`: final verification evidence.

---

### Task 1: Callback Status and Compensation Data Contract

**Files:**
- Create: `数据库SQL脚本/mysql/sql-update-log/v3.41.0.sql`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmCallbackStatusEnum.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/form/BpmCallbackCompensateForm.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/entity/BpmCallbackRecordEntity.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/vo/BpmCallbackRecordVO.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessIntegrationRecordService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessIntegrationRecordServiceTest.java`

**Interfaces:**
- Produces:
  - `BpmCallbackStatusEnum#PENDING`, `SUCCEEDED`, `FAILED`, `NEEDS_COMPENSATION`, `COMPENSATED`
  - `BpmCallbackStatusEnum#getValue(): Integer`
  - `BpmCallbackStatusEnum#equalsValue(Integer value): boolean`
  - `BpmCallbackCompensateForm#getReason(): String`
  - `BpmCallbackRecordEntity#getCompensatedAt(): LocalDateTime`
  - `BpmCallbackRecordVO#getCompensationReason(): String`

- [ ] **Step 1: Write the failing mapping test**

Modify `BpmBusinessIntegrationRecordServiceTest.java` so existing callback mapping tests require the new compensation fields:

```java
import java.time.LocalDateTime;
```

Inside `queryCallbackPageShouldReturnCallbackRecordVOs`, add the fields before the DAO stub:

```java
LocalDateTime compensatedAt = LocalDateTime.of(2026, 7, 9, 10, 30);
record.setCompensatedAt(compensatedAt);
record.setCompensatedBy(900L);
record.setCompensationReason("业务侧已线下补偿");
```

Add assertions after the existing retry count assertion:

```java
assertThat(response.getData().getList().get(0).getCompensatedAt()).isEqualTo(compensatedAt);
assertThat(response.getData().getList().get(0).getCompensatedBy()).isEqualTo(900L);
assertThat(response.getData().getList().get(0).getCompensationReason()).isEqualTo("业务侧已线下补偿");
```

Inside `queryCallbackRecordsByInstanceIdShouldReturnMappedRecords`, add:

```java
LocalDateTime compensatedAt = LocalDateTime.of(2026, 7, 9, 11, 0);
record.setCompensatedAt(compensatedAt);
record.setCompensatedBy(901L);
record.setCompensationReason("管理员确认补偿完成");
```

Add assertions after the existing event assertion:

```java
assertThat(records.get(0).getCompensatedAt()).isEqualTo(compensatedAt);
assertThat(records.get(0).getCompensatedBy()).isEqualTo(901L);
assertThat(records.get(0).getCompensationReason()).isEqualTo("管理员确认补偿完成");
```

- [ ] **Step 2: Run the focused test and confirm it fails**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmBusinessIntegrationRecordServiceTest test
```

Expected: FAIL because `BpmCallbackRecordEntity` and `BpmCallbackRecordVO` do not yet expose compensation fields.

- [ ] **Step 3: Add the SQL increment**

Create `数据库SQL脚本/mysql/sql-update-log/v3.41.0.sql`:

```sql
-- BPM P2.3：业务回调人工补偿审计
ALTER TABLE `t_bpm_callback_record`
    ADD COLUMN `compensated_at` datetime NULL COMMENT '人工补偿时间' AFTER `next_retry_at`,
    ADD COLUMN `compensated_by` bigint NULL COMMENT '人工补偿操作人ID' AFTER `compensated_at`,
    ADD COLUMN `compensation_reason` varchar(500) NULL COMMENT '人工补偿说明' AFTER `compensated_by`;
```

- [ ] **Step 4: Add the callback status enum**

Create `BpmCallbackStatusEnum.java`:

```java
package com.hunyuan.sa.bpm.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * BPM 业务回调状态。
 */
@Getter
@AllArgsConstructor
public enum BpmCallbackStatusEnum {

    PENDING(0, "待处理"),
    SUCCEEDED(1, "成功"),
    FAILED(2, "失败"),
    NEEDS_COMPENSATION(3, "需人工补偿"),
    COMPENSATED(4, "已补偿");

    private final Integer value;

    private final String desc;

    public boolean equalsValue(Integer value) {
        return this.value.equals(value);
    }
}
```

- [ ] **Step 5: Add the compensation form**

Create `BpmCallbackCompensateForm.java`:

```java
package com.hunyuan.sa.bpm.module.integration.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * BPM 业务回调人工补偿表单。
 */
@Data
public class BpmCallbackCompensateForm {

    @Schema(description = "人工补偿说明")
    @NotBlank(message = "人工补偿说明不能为空")
    @Size(max = 500, message = "人工补偿说明最多500个字符")
    private String reason;
}
```

- [ ] **Step 6: Extend entity and VO fields**

Add to `BpmCallbackRecordEntity.java` after `nextRetryAt`:

```java
private LocalDateTime compensatedAt;

private Long compensatedBy;

private String compensationReason;
```

Add to `BpmCallbackRecordVO.java` after `nextRetryAt`:

```java
private LocalDateTime compensatedAt;

private Long compensatedBy;

private String compensationReason;
```

- [ ] **Step 7: Map compensation fields**

In `BpmBusinessIntegrationRecordService#toCallbackRecordVO`, add after `vo.setNextRetryAt(entity.getNextRetryAt());`:

```java
vo.setCompensatedAt(entity.getCompensatedAt());
vo.setCompensatedBy(entity.getCompensatedBy());
vo.setCompensationReason(entity.getCompensationReason());
```

- [ ] **Step 8: Run the focused test and confirm it passes**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmBusinessIntegrationRecordServiceTest test
```

Expected: PASS, 4 tests.

- [ ] **Step 9: Commit**

```powershell
git add -- "数据库SQL脚本/mysql/sql-update-log/v3.41.0.sql" `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmCallbackStatusEnum.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/form/BpmCallbackCompensateForm.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/entity/BpmCallbackRecordEntity.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/domain/vo/BpmCallbackRecordVO.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessIntegrationRecordService.java `
  hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessIntegrationRecordServiceTest.java
git commit -m "feat: 增加 BPM 回调补偿数据合同"
```

---

### Task 2: Unified Callback Executor

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackContext.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackResult.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackHandler.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackTriggerType.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackExecuteResult.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackExecutor.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessCallbackExecutorTest.java`

**Interfaces:**
- Consumes:
  - `BpmCallbackRecordDao#selectById(Serializable id)`
  - `BpmCallbackRecordDao#selectList(Wrapper<BpmCallbackRecordEntity> queryWrapper)`
  - `BpmCallbackRecordDao#updateById(BpmCallbackRecordEntity entity)`
  - `BpmCallbackStatusEnum`
- Produces:
  - `BpmBusinessCallbackExecutor#execute(Long callbackRecordId, BpmBusinessCallbackTriggerType triggerType): BpmBusinessCallbackExecuteResult`
  - `BpmBusinessCallbackExecutor#executeDueRecords(LocalDateTime now, int batchSize): int`
  - `BpmBusinessCallbackHandler#businessType(): String`
  - `BpmBusinessCallbackHandler#handle(BpmBusinessCallbackContext context): BpmBusinessCallbackResult`

- [ ] **Step 1: Write the failing executor test**

Create `BpmBusinessCallbackExecutorTest.java` with these tests:

```java
package com.hunyuan.sa.bpm.integration;

import com.hunyuan.sa.bpm.common.enumeration.BpmCallbackStatusEnum;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackContext;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackExecuteResult;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackExecutor;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackHandler;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackResult;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackTriggerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmBusinessCallbackExecutorTest {

    private BpmBusinessCallbackExecutor executor;

    private BpmCallbackRecordDao bpmCallbackRecordDao;

    private AtomicInteger handlerCalls;

    @BeforeEach
    void setUp() {
        executor = new BpmBusinessCallbackExecutor();
        bpmCallbackRecordDao = Mockito.mock(BpmCallbackRecordDao.class);
        handlerCalls = new AtomicInteger();
        setField(executor, "bpmCallbackRecordDao", bpmCallbackRecordDao);
    }

    @Test
    void executeShouldMarkSucceededWhenHandlerSucceeds() {
        setHandlers(List.of(successHandler("expense", "{\"handled\":true}")));
        BpmCallbackRecordEntity record = buildRecord(BpmCallbackStatusEnum.PENDING.getValue(), 0);
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);

        BpmBusinessCallbackExecuteResult result = executor.execute(1L, BpmBusinessCallbackTriggerType.MANUAL);

        assertThat(result.processed()).isTrue();
        assertThat(result.succeeded()).isTrue();
        ArgumentCaptor<BpmCallbackRecordEntity> captor = ArgumentCaptor.forClass(BpmCallbackRecordEntity.class);
        verify(bpmCallbackRecordDao).updateById(captor.capture());
        assertThat(captor.getValue().getCallbackStatus()).isEqualTo(BpmCallbackStatusEnum.SUCCEEDED.getValue());
        assertThat(captor.getValue().getResponsePayloadJson()).isEqualTo("{\"handled\":true}");
        assertThat(captor.getValue().getFailureReason()).isNull();
        assertThat(captor.getValue().getNextRetryAt()).isNull();
        assertThat(handlerCalls.get()).isEqualTo(1);
    }

    @Test
    void executeShouldMarkFailedAndScheduleRetryWhenHandlerFails() {
        setHandlers(List.of(failingHandler("expense", "业务侧暂不可用")));
        BpmCallbackRecordEntity record = buildRecord(BpmCallbackStatusEnum.PENDING.getValue(), 0);
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);

        BpmBusinessCallbackExecuteResult result = executor.execute(1L, BpmBusinessCallbackTriggerType.AUTO);

        assertThat(result.processed()).isTrue();
        assertThat(result.succeeded()).isFalse();
        ArgumentCaptor<BpmCallbackRecordEntity> captor = ArgumentCaptor.forClass(BpmCallbackRecordEntity.class);
        verify(bpmCallbackRecordDao).updateById(captor.capture());
        assertThat(captor.getValue().getCallbackStatus()).isEqualTo(BpmCallbackStatusEnum.FAILED.getValue());
        assertThat(captor.getValue().getRetryCount()).isEqualTo(1);
        assertThat(captor.getValue().getFailureReason()).isEqualTo("业务侧暂不可用");
        assertThat(captor.getValue().getNextRetryAt()).isNotNull();
    }

    @Test
    void executeShouldMoveToCompensationAfterMaxRetryCount() {
        setHandlers(List.of(failingHandler("expense", "仍然失败")));
        BpmCallbackRecordEntity record = buildRecord(BpmCallbackStatusEnum.FAILED.getValue(), 2);
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);

        executor.execute(1L, BpmBusinessCallbackTriggerType.AUTO);

        ArgumentCaptor<BpmCallbackRecordEntity> captor = ArgumentCaptor.forClass(BpmCallbackRecordEntity.class);
        verify(bpmCallbackRecordDao).updateById(captor.capture());
        assertThat(captor.getValue().getCallbackStatus()).isEqualTo(BpmCallbackStatusEnum.NEEDS_COMPENSATION.getValue());
        assertThat(captor.getValue().getRetryCount()).isEqualTo(3);
        assertThat(captor.getValue().getNextRetryAt()).isNull();
    }

    @Test
    void executeShouldSkipTerminalRecords() {
        setHandlers(List.of(successHandler("expense", "{}")));
        BpmCallbackRecordEntity record = buildRecord(BpmCallbackStatusEnum.SUCCEEDED.getValue(), 0);
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);

        BpmBusinessCallbackExecuteResult result = executor.execute(1L, BpmBusinessCallbackTriggerType.MANUAL);

        assertThat(result.processed()).isFalse();
        verify(bpmCallbackRecordDao, never()).updateById(any(BpmCallbackRecordEntity.class));
        assertThat(handlerCalls.get()).isZero();
    }

    @Test
    void executeDueRecordsShouldProcessPendingAndDueFailedRecords() {
        setHandlers(List.of(successHandler("expense", "{}")));
        BpmCallbackRecordEntity pending = buildRecord(BpmCallbackStatusEnum.PENDING.getValue(), 0);
        pending.setCallbackRecordId(1L);
        BpmCallbackRecordEntity failed = buildRecord(BpmCallbackStatusEnum.FAILED.getValue(), 1);
        failed.setCallbackRecordId(2L);
        failed.setNextRetryAt(LocalDateTime.of(2026, 7, 9, 10, 0));
        when(bpmCallbackRecordDao.selectList(any())).thenReturn(List.of(pending, failed));
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(pending);
        when(bpmCallbackRecordDao.selectById(2L)).thenReturn(failed);

        int processed = executor.executeDueRecords(LocalDateTime.of(2026, 7, 9, 10, 1), 50);

        assertThat(processed).isEqualTo(2);
        assertThat(handlerCalls.get()).isEqualTo(2);
    }

    private BpmCallbackRecordEntity buildRecord(Integer status, Integer retryCount) {
        BpmCallbackRecordEntity record = new BpmCallbackRecordEntity();
        record.setCallbackRecordId(1L);
        record.setEventId("event-1");
        record.setInstanceId(88L);
        record.setBusinessType("expense");
        record.setBusinessId(1001L);
        record.setCallbackStatus(status);
        record.setRequestPayloadJson("{\"result\":\"APPROVED\"}");
        record.setRetryCount(retryCount);
        return record;
    }

    private BpmBusinessCallbackHandler successHandler(String businessType, String response) {
        return new BpmBusinessCallbackHandler() {
            @Override
            public String businessType() {
                return businessType;
            }

            @Override
            public BpmBusinessCallbackResult handle(BpmBusinessCallbackContext context) {
                handlerCalls.incrementAndGet();
                return BpmBusinessCallbackResult.success(response);
            }
        };
    }

    private BpmBusinessCallbackHandler failingHandler(String businessType, String reason) {
        return new BpmBusinessCallbackHandler() {
            @Override
            public String businessType() {
                return businessType;
            }

            @Override
            public BpmBusinessCallbackResult handle(BpmBusinessCallbackContext context) {
                handlerCalls.incrementAndGet();
                return BpmBusinessCallbackResult.failed(reason, "{\"ok\":false}");
            }
        };
    }

    private void setHandlers(List<BpmBusinessCallbackHandler> handlers) {
        setField(executor, "callbackHandlers", handlers);
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

- [ ] **Step 2: Run the focused test and confirm it fails**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmBusinessCallbackExecutorTest test
```

Expected: FAIL because executor contracts do not exist.

- [ ] **Step 3: Create the callback contracts**

Create `BpmBusinessCallbackContext.java`:

```java
package com.hunyuan.sa.bpm.module.integration.service;

/**
 * BPM 业务回调执行上下文。
 */
public record BpmBusinessCallbackContext(
        Long callbackRecordId,
        String eventId,
        Long instanceId,
        String businessType,
        Long businessId,
        String requestPayloadJson
) {
}
```

Create `BpmBusinessCallbackResult.java`:

```java
package com.hunyuan.sa.bpm.module.integration.service;

/**
 * BPM 业务回调处理结果。
 */
public record BpmBusinessCallbackResult(
        boolean success,
        String responsePayloadJson,
        String failureReason
) {

    public static BpmBusinessCallbackResult success(String responsePayloadJson) {
        return new BpmBusinessCallbackResult(true, responsePayloadJson, null);
    }

    public static BpmBusinessCallbackResult failed(String failureReason, String responsePayloadJson) {
        return new BpmBusinessCallbackResult(false, responsePayloadJson, failureReason);
    }
}
```

Create `BpmBusinessCallbackHandler.java`:

```java
package com.hunyuan.sa.bpm.module.integration.service;

/**
 * BPM 业务回调处理器。
 */
public interface BpmBusinessCallbackHandler {

    /**
     * 当前处理器支持的业务类型。
     */
    String businessType();

    /**
     * 执行业务回调。业务侧必须保证同一 eventId 幂等。
     */
    BpmBusinessCallbackResult handle(BpmBusinessCallbackContext context);
}
```

Create `BpmBusinessCallbackTriggerType.java`:

```java
package com.hunyuan.sa.bpm.module.integration.service;

/**
 * BPM 业务回调触发来源。
 */
public enum BpmBusinessCallbackTriggerType {
    AUTO,
    MANUAL
}
```

Create `BpmBusinessCallbackExecuteResult.java`:

```java
package com.hunyuan.sa.bpm.module.integration.service;

/**
 * BPM 业务回调执行结果。
 */
public record BpmBusinessCallbackExecuteResult(
        boolean processed,
        boolean succeeded,
        String message
) {

    public static BpmBusinessCallbackExecuteResult skipped(String message) {
        return new BpmBusinessCallbackExecuteResult(false, false, message);
    }

    public static BpmBusinessCallbackExecuteResult succeeded() {
        return new BpmBusinessCallbackExecuteResult(true, true, "回调执行成功");
    }

    public static BpmBusinessCallbackExecuteResult failed(String message) {
        return new BpmBusinessCallbackExecuteResult(true, false, message);
    }
}
```

- [ ] **Step 4: Create the executor**

Create `BpmBusinessCallbackExecutor.java`:

```java
package com.hunyuan.sa.bpm.module.integration.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.common.enumeration.BpmCallbackStatusEnum;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * BPM 业务回调统一执行器。
 */
@Service
public class BpmBusinessCallbackExecutor {

    private static final int MAX_RETRY_COUNT = 3;

    private static final int DEFAULT_BATCH_SIZE = 50;

    @Resource
    private BpmCallbackRecordDao bpmCallbackRecordDao;

    @Resource
    private List<BpmBusinessCallbackHandler> callbackHandlers = List.of();

    public BpmBusinessCallbackExecuteResult execute(Long callbackRecordId, BpmBusinessCallbackTriggerType triggerType) {
        BpmCallbackRecordEntity record = bpmCallbackRecordDao.selectById(callbackRecordId);
        if (record == null) {
            return BpmBusinessCallbackExecuteResult.skipped("回调记录不存在");
        }
        if (BpmCallbackStatusEnum.SUCCEEDED.equalsValue(record.getCallbackStatus())
                || BpmCallbackStatusEnum.COMPENSATED.equalsValue(record.getCallbackStatus())) {
            return BpmBusinessCallbackExecuteResult.skipped("回调记录已处于终态");
        }
        if (BpmCallbackStatusEnum.NEEDS_COMPENSATION.equalsValue(record.getCallbackStatus())) {
            return BpmBusinessCallbackExecuteResult.skipped("回调记录需要人工补偿");
        }

        try {
            BpmBusinessCallbackHandler handler = findHandler(record.getBusinessType());
            if (handler == null) {
                return markFailed(record, "未找到业务回调处理器: " + record.getBusinessType(), null);
            }
            BpmBusinessCallbackResult result = handler.handle(toContext(record));
            if (result != null && result.success()) {
                markSucceeded(record, result.responsePayloadJson());
                return BpmBusinessCallbackExecuteResult.succeeded();
            }
            String failureReason = result == null ? "业务回调处理器返回空结果" : result.failureReason();
            String responsePayloadJson = result == null ? null : result.responsePayloadJson();
            return markFailed(record, failureReason, responsePayloadJson);
        } catch (RuntimeException ex) {
            return markFailed(record, limit(ex.getMessage(), 1000), ex.getClass().getSimpleName());
        }
    }

    public int executeDueRecords(LocalDateTime now, int batchSize) {
        int limit = batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
        List<BpmCallbackRecordEntity> dueRecords = bpmCallbackRecordDao.selectList(
                Wrappers.<BpmCallbackRecordEntity>lambdaQuery()
                        .and(wrapper -> wrapper
                                .eq(BpmCallbackRecordEntity::getCallbackStatus, BpmCallbackStatusEnum.PENDING.getValue())
                                .or(orWrapper -> orWrapper
                                        .eq(BpmCallbackRecordEntity::getCallbackStatus, BpmCallbackStatusEnum.FAILED.getValue())
                                        .le(BpmCallbackRecordEntity::getNextRetryAt, now)))
                        .orderByAsc(BpmCallbackRecordEntity::getCallbackRecordId)
                        .last("LIMIT " + limit)
        );
        int processed = 0;
        for (BpmCallbackRecordEntity record : dueRecords) {
            BpmBusinessCallbackExecuteResult result = execute(record.getCallbackRecordId(), BpmBusinessCallbackTriggerType.AUTO);
            if (result.processed()) {
                processed++;
            }
        }
        return processed;
    }

    private BpmBusinessCallbackHandler findHandler(String businessType) {
        return callbackHandlers.stream()
                .filter(handler -> Objects.equals(handler.businessType(), businessType))
                .findFirst()
                .orElse(null);
    }

    private BpmBusinessCallbackContext toContext(BpmCallbackRecordEntity record) {
        return new BpmBusinessCallbackContext(
                record.getCallbackRecordId(),
                record.getEventId(),
                record.getInstanceId(),
                record.getBusinessType(),
                record.getBusinessId(),
                record.getRequestPayloadJson()
        );
    }

    private void markSucceeded(BpmCallbackRecordEntity record, String responsePayloadJson) {
        BpmCallbackRecordEntity update = new BpmCallbackRecordEntity();
        update.setCallbackRecordId(record.getCallbackRecordId());
        update.setCallbackStatus(BpmCallbackStatusEnum.SUCCEEDED.getValue());
        update.setResponsePayloadJson(limit(responsePayloadJson, 4000));
        update.setFailureReason(null);
        update.setNextRetryAt(null);
        update.setUpdateTime(LocalDateTime.now());
        bpmCallbackRecordDao.updateById(update);
    }

    private BpmBusinessCallbackExecuteResult markFailed(BpmCallbackRecordEntity record, String failureReason, String responsePayloadJson) {
        int retryCount = record.getRetryCount() == null ? 1 : record.getRetryCount() + 1;
        BpmCallbackRecordEntity update = new BpmCallbackRecordEntity();
        update.setCallbackRecordId(record.getCallbackRecordId());
        update.setRetryCount(retryCount);
        update.setFailureReason(limit(StringUtils.hasText(failureReason) ? failureReason : "业务回调执行失败", 1000));
        update.setResponsePayloadJson(limit(responsePayloadJson, 4000));
        if (retryCount >= MAX_RETRY_COUNT) {
            update.setCallbackStatus(BpmCallbackStatusEnum.NEEDS_COMPENSATION.getValue());
            update.setNextRetryAt(null);
        } else {
            update.setCallbackStatus(BpmCallbackStatusEnum.FAILED.getValue());
            update.setNextRetryAt(LocalDateTime.now().plusMinutes(nextBackoffMinutes(retryCount)));
        }
        update.setUpdateTime(LocalDateTime.now());
        bpmCallbackRecordDao.updateById(update);
        return BpmBusinessCallbackExecuteResult.failed(update.getFailureReason());
    }

    private int nextBackoffMinutes(int retryCount) {
        if (retryCount <= 1) {
            return 1;
        }
        if (retryCount == 2) {
            return 5;
        }
        return 15;
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
```

- [ ] **Step 5: Run the focused test and confirm it passes**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmBusinessCallbackExecutorTest test
```

Expected: PASS, 5 tests.

- [ ] **Step 6: Commit**

```powershell
git add -- hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackContext.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackResult.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackHandler.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackTriggerType.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackExecuteResult.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackExecutor.java `
  hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessCallbackExecutorTest.java
git commit -m "feat: 增加 BPM 业务回调执行器"
```

---

### Task 3: Retry Service, Compensation Endpoint, and Scheduler

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackScheduler.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmIntegrationController.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessCallbackServiceTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessCallbackSchedulerTest.java`

**Interfaces:**
- Consumes:
  - `BpmBusinessCallbackExecutor#execute(Long, BpmBusinessCallbackTriggerType)`
  - `BpmCurrentActorProvider#requireCurrentEmployeeId()`
  - `BpmCallbackCompensateForm#getReason()`
- Produces:
  - `BpmBusinessCallbackService#retry(Long callbackRecordId): ResponseDTO<String>`
  - `BpmBusinessCallbackService#compensate(Long callbackRecordId, BpmCallbackCompensateForm form): ResponseDTO<String>`
  - `BpmBusinessCallbackScheduler#scanDueCallbackRecords(): void`
  - `POST /bpm/integration/callback/compensate/{callbackRecordId}`

- [ ] **Step 1: Replace the service tests with executor and compensation expectations**

Rewrite `BpmBusinessCallbackServiceTest.java` around the new service contract:

```java
package com.hunyuan.sa.bpm.integration;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.common.enumeration.BpmCallbackStatusEnum;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.integration.domain.form.BpmCallbackCompensateForm;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackExecuteResult;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackExecutor;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackService;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackTriggerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmBusinessCallbackServiceTest {

    private BpmBusinessCallbackService callbackService;

    private BpmCallbackRecordDao bpmCallbackRecordDao;

    private BpmBusinessCallbackExecutor callbackExecutor;

    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @BeforeEach
    void setUp() {
        callbackService = new BpmBusinessCallbackService();
        bpmCallbackRecordDao = Mockito.mock(BpmCallbackRecordDao.class);
        callbackExecutor = Mockito.mock(BpmBusinessCallbackExecutor.class);
        bpmCurrentActorProvider = Mockito.mock(BpmCurrentActorProvider.class);
        setField(callbackService, "bpmCallbackRecordDao", bpmCallbackRecordDao);
        setField(callbackService, "callbackExecutor", callbackExecutor);
        setField(callbackService, "bpmCurrentActorProvider", bpmCurrentActorProvider);
    }

    @Test
    void retryShouldUseUnifiedExecutor() {
        when(callbackExecutor.execute(1L, BpmBusinessCallbackTriggerType.MANUAL))
                .thenReturn(BpmBusinessCallbackExecuteResult.succeeded());

        ResponseDTO<String> response = callbackService.retry(1L);

        assertThat(response.getOk()).isTrue();
        verify(callbackExecutor).execute(1L, BpmBusinessCallbackTriggerType.MANUAL);
        verify(bpmCallbackRecordDao, never()).updateById(Mockito.any(BpmCallbackRecordEntity.class));
    }

    @Test
    void compensateShouldMarkNeedsCompensationRecordAsCompensated() {
        BpmCallbackRecordEntity record = new BpmCallbackRecordEntity();
        record.setCallbackRecordId(1L);
        record.setCallbackStatus(BpmCallbackStatusEnum.NEEDS_COMPENSATION.getValue());
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);
        when(bpmCurrentActorProvider.requireCurrentEmployeeId()).thenReturn(900L);
        BpmCallbackCompensateForm form = new BpmCallbackCompensateForm();
        form.setReason("业务侧已线下补偿");

        ResponseDTO<String> response = callbackService.compensate(1L, form);

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmCallbackRecordEntity> captor = ArgumentCaptor.forClass(BpmCallbackRecordEntity.class);
        verify(bpmCallbackRecordDao).updateById(captor.capture());
        assertThat(captor.getValue().getCallbackStatus()).isEqualTo(BpmCallbackStatusEnum.COMPENSATED.getValue());
        assertThat(captor.getValue().getCompensatedBy()).isEqualTo(900L);
        assertThat(captor.getValue().getCompensationReason()).isEqualTo("业务侧已线下补偿");
        assertThat(captor.getValue().getCompensatedAt()).isNotNull();
        assertThat(captor.getValue().getNextRetryAt()).isNull();
    }

    @Test
    void compensateShouldRejectNonCompensationRecord() {
        BpmCallbackRecordEntity record = new BpmCallbackRecordEntity();
        record.setCallbackRecordId(1L);
        record.setCallbackStatus(BpmCallbackStatusEnum.FAILED.getValue());
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);
        BpmCallbackCompensateForm form = new BpmCallbackCompensateForm();
        form.setReason("业务侧已线下补偿");

        ResponseDTO<String> response = callbackService.compensate(1L, form);

        assertThat(response.getOk()).isFalse();
        verify(bpmCallbackRecordDao, never()).updateById(Mockito.any(BpmCallbackRecordEntity.class));
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

- [ ] **Step 2: Add the failing scheduler test**

Create `BpmBusinessCallbackSchedulerTest.java`:

```java
package com.hunyuan.sa.bpm.integration;

import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackExecutor;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class BpmBusinessCallbackSchedulerTest {

    private BpmBusinessCallbackScheduler scheduler;

    private BpmBusinessCallbackExecutor callbackExecutor;

    @BeforeEach
    void setUp() {
        scheduler = new BpmBusinessCallbackScheduler();
        callbackExecutor = Mockito.mock(BpmBusinessCallbackExecutor.class);
        setField(scheduler, "callbackExecutor", callbackExecutor);
    }

    @Test
    void scanDueCallbackRecordsShouldUseExecutorBatchScan() {
        scheduler.scanDueCallbackRecords();

        verify(callbackExecutor).executeDueRecords(any(LocalDateTime.class), Mockito.eq(50));
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

- [ ] **Step 3: Run focused tests and confirm they fail**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmBusinessCallbackServiceTest,BpmBusinessCallbackSchedulerTest' test
```

Expected: FAIL because `compensate` and scheduler do not exist.

- [ ] **Step 4: Update the callback service**

Replace `BpmBusinessCallbackService.java` with:

```java
package com.hunyuan.sa.bpm.module.integration.service;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.common.enumeration.BpmCallbackStatusEnum;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.integration.domain.form.BpmCallbackCompensateForm;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * BPM 业务回调记录服务。
 */
@Service
public class BpmBusinessCallbackService {

    @Resource
    private BpmCallbackRecordDao bpmCallbackRecordDao;

    @Resource
    private BpmBusinessCallbackExecutor callbackExecutor;

    @Resource
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    public ResponseDTO<String> retry(Long callbackRecordId) {
        BpmBusinessCallbackExecuteResult result = callbackExecutor.execute(
                callbackRecordId,
                BpmBusinessCallbackTriggerType.MANUAL
        );
        if (!result.processed() && "回调记录不存在".equals(result.message())) {
            return ResponseDTO.userErrorParam(result.message());
        }
        return ResponseDTO.ok();
    }

    public ResponseDTO<String> compensate(Long callbackRecordId, BpmCallbackCompensateForm form) {
        BpmCallbackRecordEntity record = bpmCallbackRecordDao.selectById(callbackRecordId);
        if (record == null) {
            return ResponseDTO.userErrorParam("回调记录不存在");
        }
        if (!BpmCallbackStatusEnum.NEEDS_COMPENSATION.equalsValue(record.getCallbackStatus())) {
            return ResponseDTO.userErrorParam("只有需人工补偿的回调记录才能标记补偿");
        }
        BpmCallbackRecordEntity update = new BpmCallbackRecordEntity();
        update.setCallbackRecordId(callbackRecordId);
        update.setCallbackStatus(BpmCallbackStatusEnum.COMPENSATED.getValue());
        update.setCompensatedAt(LocalDateTime.now());
        update.setCompensatedBy(bpmCurrentActorProvider.requireCurrentEmployeeId());
        update.setCompensationReason(limit(form.getReason(), 500));
        update.setNextRetryAt(null);
        update.setUpdateTime(LocalDateTime.now());
        bpmCallbackRecordDao.updateById(update);
        return ResponseDTO.ok();
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
```

- [ ] **Step 5: Add the scheduler**

Create `BpmBusinessCallbackScheduler.java`:

```java
package com.hunyuan.sa.bpm.module.integration.service;

import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * BPM 业务回调自动重试扫描器。
 */
@Component
public class BpmBusinessCallbackScheduler {

    private static final int BATCH_SIZE = 50;

    @Resource
    private BpmBusinessCallbackExecutor callbackExecutor;

    @Scheduled(fixedDelay = 60_000L)
    public void scanDueCallbackRecords() {
        callbackExecutor.executeDueRecords(LocalDateTime.now(), BATCH_SIZE);
    }
}
```

- [ ] **Step 6: Add the compensation endpoint**

Modify `AdminBpmIntegrationController.java` imports:

```java
import com.hunyuan.sa.bpm.module.integration.domain.form.BpmCallbackCompensateForm;
```

Add after `retryCallback`:

```java
@Operation(summary = "人工补偿 BPM 业务回调")
@PostMapping("/bpm/integration/callback/compensate/{callbackRecordId}")
@SaCheckPermission("bpm:integration:update")
public ResponseDTO<String> compensateCallback(
        @PathVariable Long callbackRecordId,
        @RequestBody @Valid BpmCallbackCompensateForm form
) {
    return bpmBusinessCallbackService.compensate(callbackRecordId, form);
}
```

- [ ] **Step 7: Run focused tests and confirm they pass**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmBusinessCallbackServiceTest,BpmBusinessCallbackSchedulerTest,BpmBusinessCallbackExecutorTest' test
```

Expected: PASS, all focused callback tests.

- [ ] **Step 8: Commit**

```powershell
git add -- hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackService.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/integration/service/BpmBusinessCallbackScheduler.java `
  hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmIntegrationController.java `
  hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessCallbackServiceTest.java `
  hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessCallbackSchedulerTest.java
git commit -m "feat: 接入 BPM 回调重试与人工补偿"
```

---

### Task 4: Frontend Reliability View and Contract Tests

**Files:**
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/integration.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/integration/callback-record-list.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

**Interfaces:**
- Consumes:
  - `BpmCallbackRecordVO.callbackStatus`
  - `BpmCallbackRecordVO.nextRetryAt`
  - `BpmCallbackRecordVO.compensatedAt`
  - `BpmCallbackRecordVO.compensatedBy`
  - `BpmCallbackRecordVO.compensationReason`
- Produces:
  - `compensateBpmCallbackRecord(callbackRecordId: number, data: BpmCallbackCompensateParams): Promise<string>`
  - Callback list actions for status `2` and `3`
  - Admin trace callback status labels for `0/1/2/3/4`

- [ ] **Step 1: Add failing frontend contract assertions**

In `bpm-api.test.ts`, update the `integration` needles:

```ts
'compensateBpmCallbackRecord',
'/bpm/integration/callback/compensate/',
'compensationReason',
'compensatedAt',
'compensatedBy',
```

In `bpm-modules.test.ts`, update `keeps bpm integration monitoring pages wired to reliability APIs`:

```ts
expect(callbackSource).toContain('compensateBpmCallbackRecord');
expect(callbackSource).toContain('callbackStatus === 3');
expect(callbackSource).toContain('需人工补偿');
expect(callbackSource).toContain('nextRetryAt');
```

Add to the runtime detail drawer test block that already reads `detailSource`:

```ts
expect(detailSource).toContain('getCallbackStatusLabel');
expect(detailSource).toContain('nextRetryAt');
expect(detailSource).toContain('需人工补偿');
```

- [ ] **Step 2: Run frontend contract tests and confirm they fail**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: FAIL because the frontend API and page do not yet expose compensation.

- [ ] **Step 3: Extend the frontend API**

Modify `integration.ts`:

```ts
export interface BpmCallbackRecordVO {
  businessId: number;
  businessType: string;
  callbackRecordId: number;
  callbackStatus: number;
  compensatedAt?: null | string;
  compensatedBy?: null | number;
  compensationReason?: null | string;
  createTime?: null | string;
  eventId: string;
  failureReason?: null | string;
  instanceId: number;
  nextRetryAt?: null | string;
  retryCount: number;
  updateTime?: null | string;
}

export interface BpmCallbackCompensateParams {
  reason: string;
}
```

Add after `retryBpmCallbackRecord`:

```ts
export async function compensateBpmCallbackRecord(
  callbackRecordId: number,
  data: BpmCallbackCompensateParams,
) {
  return requestClient.post<string>(
    `/bpm/integration/callback/compensate/${callbackRecordId}`,
    {
      reason: data.reason.trim(),
    },
  );
}
```

- [ ] **Step 4: Update callback list imports and actions**

In `callback-record-list.vue`, import `ElMessageBox` and the new API:

```ts
import {
  ElButton,
  ElCard,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElSelect,
  ElSpace,
  ElTag,
} from 'element-plus';

import {
  compensateBpmCallbackRecord,
  queryBpmCallbackRecordPage,
  retryBpmCallbackRecord,
} from '#/api/system/bpm';
```

Add the next retry column after retry count:

```ts
{ prop: 'nextRetryAt', label: '下次重试', minWidth: 180 },
```

Change action column width to `160`:

```ts
width: 160,
```

Replace status helpers:

```ts
function getCallbackStatusLabel(value?: null | number) {
  if (value === 0) {
    return '待回调';
  }
  if (value === 1) {
    return '成功';
  }
  if (value === 2) {
    return '失败';
  }
  if (value === 3) {
    return '需人工补偿';
  }
  if (value === 4) {
    return '已补偿';
  }
  return '未知';
}

function getCallbackStatusType(value?: null | number) {
  if (value === 1 || value === 4) {
    return 'success';
  }
  if (value === 2) {
    return 'danger';
  }
  if (value === 3) {
    return 'warning';
  }
  return 'info';
}
```

Add compensation handler after `handleRetry`:

```ts
async function handleCompensate(row: BpmCallbackRecordVO) {
  const result = await ElMessageBox.prompt('请输入人工补偿说明', '标记已补偿', {
    confirmButtonText: '确认',
    inputPattern: /\S+/,
    inputErrorMessage: '请输入人工补偿说明',
    inputPlaceholder: '说明业务侧已如何补偿',
    type: 'warning',
  });
  const reason = String(result.value || '').trim();
  await compensateBpmCallbackRecord(row.callbackRecordId, { reason });
  ElMessage.success('已标记为人工补偿');
  void loadData();
}
```

Add status options:

```vue
<ElOption label="需人工补偿" :value="3" />
<ElOption label="已补偿" :value="4" />
```

Add compensation action after retry:

```vue
<ElButton
  v-if="row.callbackStatus === 3"
  link
  size="small"
  type="warning"
  @click="handleCompensate(row)"
>
  标记补偿
</ElButton>
```

- [ ] **Step 5: Update the admin trace callback table**

In `bpm-instance-detail-drawer.vue`, add local helpers in the script block:

```ts
function getCallbackStatusLabel(value?: null | number) {
  if (value === 0) {
    return '待回调';
  }
  if (value === 1) {
    return '成功';
  }
  if (value === 2) {
    return '失败';
  }
  if (value === 3) {
    return '需人工补偿';
  }
  if (value === 4) {
    return '已补偿';
  }
  return '未知';
}

function getCallbackStatusType(value?: null | number) {
  if (value === 1 || value === 4) {
    return 'success';
  }
  if (value === 2) {
    return 'danger';
  }
  if (value === 3) {
    return 'warning';
  }
  return 'info';
}
```

Replace the status column:

```vue
<ElTableColumn label="状态" min-width="110">
  <template #default="{ row }">
    <ElTag :type="getCallbackStatusType(row.callbackStatus)" effect="plain" size="small">
      {{ getCallbackStatusLabel(row.callbackStatus) }}
    </ElTag>
  </template>
</ElTableColumn>
```

Add next retry and compensation columns after retry count:

```vue
<ElTableColumn label="下次重试" min-width="150" prop="nextRetryAt" />
<ElTableColumn
  label="补偿说明"
  min-width="160"
  prop="compensationReason"
  show-overflow-tooltip
/>
```

- [ ] **Step 6: Run frontend contract tests**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: PASS, 2 files.

- [ ] **Step 7: Run frontend typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS, `vue-tsc --noEmit --skipLibCheck` exits 0.

- [ ] **Step 8: Commit**

```powershell
git add -- hunyuan-design/apps/hunyuan-system/src/api/system/bpm/integration.ts `
  hunyuan-design/apps/hunyuan-system/src/views/system/bpm/integration/callback-record-list.vue `
  hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue `
  hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts `
  hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts
git commit -m "feat: 展示 BPM 回调重试与补偿状态"
```

---

### Task 5: Full Verification and Acceptance Record

**Files:**
- Create: `docs/superpowers/specs/2026-07-09-bpm-p2-callback-executor-acceptance.md`

**Interfaces:**
- Consumes all deliverables from Tasks 1-4.
- Produces a durable acceptance record with exact command output summaries.

- [ ] **Step 1: Run backend focused callback gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmBusinessCallbackServiceTest,BpmBusinessCallbackExecutorTest,BpmBusinessCallbackSchedulerTest,BpmBusinessIntegrationRecordServiceTest,BpmInstanceTraceServiceTest' test
```

Expected: PASS.

- [ ] **Step 2: Run full BPM module gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

Expected: PASS.

- [ ] **Step 3: Run frontend BPM contract gate**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: PASS.

- [ ] **Step 4: Run frontend typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS.

- [ ] **Step 5: Run Flowable boundary gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test
```

Expected: PASS.

- [ ] **Step 6: Write the acceptance record**

Create `docs/superpowers/specs/2026-07-09-bpm-p2-callback-executor-acceptance.md`:

```markdown
# BPM P2.3 回调执行器与重试闭环验收记录

## 验收范围

- 业务结果事件生成的回调记录可由统一 executor 执行。
- 手动重试和自动到期重试复用同一执行路径。
- 成功回调写入成功状态和响应摘要。
- 失败回调写入失败原因、递增失败次数、设置下次重试时间。
- 达到最大失败次数后进入需人工补偿状态。
- 管理员可标记需补偿记录为已补偿，并保存补偿人、补偿时间和补偿说明。
- 管理端回调记录列表和实例可靠性区域展示回调状态、下次重试和补偿信息。
- 员工运行端详情不展示平台级失败细节。

## 验收结果

| 门禁 | 命令 | 结果 |
| --- | --- | --- |
| 后端聚焦门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmBusinessCallbackServiceTest,BpmBusinessCallbackExecutorTest,BpmBusinessCallbackSchedulerTest,BpmBusinessIntegrationRecordServiceTest,BpmInstanceTraceServiceTest' test` | PASS |
| 后端模块门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test` | PASS |
| 前端 BPM 合同测试 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom` | PASS |
| 前端类型检查 | `pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck` | PASS |
| Flowable 边界门禁 | `mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test` | PASS |

## 边界说明

- 本轮只覆盖 P2.3：回调执行、失败重试、自动扫描和人工补偿。
- 不新增 MQ、通用 HTTP 节点平台、完整 BPM 事件账本或 P2.4 业务样板。
- 业务处理器接口已具备扩展点，真实业务回写样板进入 P2.4。

## 结论

P2.3 回调执行器与重试闭环通过。BPM 回调记录已经从“可查 + 手动计数”推进为可执行、可失败、可自动重试、可人工补偿的可靠性闭环。
```

Replace each `PASS` result with the concrete test count and timestamp from the commands you ran.

- [ ] **Step 7: Commit**

```powershell
git add -- docs/superpowers/specs/2026-07-09-bpm-p2-callback-executor-acceptance.md
git commit -m "docs: 增加 BPM P2.3 回调执行器验收记录"
```

---

## Self-Review Checklist

- Spec coverage:
  - Data contract and status model: Task 1.
  - Unified executor and handler interface: Task 2.
  - Manual retry through executor: Task 3.
  - Scheduled scan for pending and due failed records: Task 3.
  - Manual compensation state and audit fields: Tasks 1 and 3.
  - Frontend callback list and reliability trace visibility: Task 4.
  - Verification and acceptance record: Task 5.
- Placeholder scan:
  - This plan must not contain placeholder markers or unnamed file paths.
  - Every created class has a concrete package, method signature, and test command.
- Type consistency:
  - Backend status enum values are `0/1/2/3/4`.
  - Frontend status checks use `callbackStatus === 2` for retry and `callbackStatus === 3` for compensation.
  - Compensation fields are named `compensatedAt`, `compensatedBy`, and `compensationReason` in Java and TypeScript.
  - Manual compensation request field is named `reason`.
