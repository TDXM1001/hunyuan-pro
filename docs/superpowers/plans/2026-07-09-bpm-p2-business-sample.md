# BPM P2.4 Business Sample Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the P2.4 BPM sample expense closed loop so a Hunyuan-native business record can start BPM, receive approval result callbacks, expose failures in the existing reliability UI, and recover through manual retry.

**Architecture:** First make real task completion publish `BpmBusinessResultEvent` for business-bound instances. Then add a small `sampleexpense` boundary inside `hunyuan-bpm` with its own table, service, callback handler, admin API, and frontend API contract. Existing P2.3 callback executor, callback record list, and instance trace remain the reliability surface; this slice does not add a new menu or a full expense product.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, JUnit 5, Mockito, AssertJ, Vue 3, TypeScript, Vitest, MySQL update SQL.

## Global Constraints

- Production code, contracts, routes, permissions, menus, tests, docs, and verification artifacts must stay in `E:\my-project\hunyuan-pro`.
- Yudao and RuoYi are reference lines only; borrow mechanisms, not code or API names.
- Public Hunyuan BPM APIs must not expose Flowable native objects, names, or IDs.
- Do not add new dependencies.
- P2.4 uses `hunyuan-bpm/module/sampleexpense` as a BPM acceptance sample, not a real reimbursement module.
- Do not create a generic event bus, MQ callback platform, HTTP callback node platform, or external scheduler.
- Do not add a sample expense menu or complex frontend page in this slice.
- Verification output and handoff must be Chinese and UTF-8 safe.

---

## File Structure

- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`: publish `BpmBusinessResultEvent` after approved/rejected terminal instance updates.
- Modify `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java`: pin terminal result event publishing.
- Create `数据库SQL脚本/mysql/sql-update-log/v3.42.0.sql`: create `t_bpm_sample_expense`.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/domain/entity/BpmSampleExpenseEntity.java`: sample expense persistence model.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/dao/BpmSampleExpenseDao.java`: MyBatis-Plus mapper.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/domain/form/BpmSampleExpenseCreateForm.java`: create form.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/domain/vo/BpmSampleExpenseVO.java`: detail response.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/service/BpmSampleExpenseService.java`: sample business create/start/detail/failure-injection/callback rules.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/service/BpmSampleExpenseCallbackHandler.java`: `sample_expense` callback handler.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmSampleExpenseController.java`: minimal admin sample API.
- Create backend tests under `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/sampleexpense/`.
- Modify `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessCallbackExecutorTest.java`: prove executor can call a real sample handler.
- Create `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/sample-expense.ts`: frontend sample API contract.
- Modify `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/index.ts`: export sample API and endpoint index.
- Modify `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`: pin sample API paths and exports.
- Create `docs/superpowers/specs/2026-07-09-bpm-p2-business-sample-acceptance.md`: final acceptance evidence.

---

### Task 1: Publish Business Result Events From Real Task Completion

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java`

**Interfaces:**
- Consumes:
  - `BpmBusinessProcessApi#publishResultEvent(BpmBusinessResultEvent event): void`
  - `BpmInstanceResultStateEnum.APPROVED.getValue(): Integer`
  - `BpmInstanceResultStateEnum.REJECTED.getValue(): Integer`
- Produces:
  - Event id format: `RESULT:{instanceId}:{resultState}`
  - Result event payload with `payloadJson == null`
  - No event for instances without `businessType` or `businessId`

- [ ] **Step 1: Write the failing publishing tests**

Modify `BpmRuntimeCommandServiceTest.java`.

Add imports:

```java
import com.hunyuan.sa.bpm.api.business.BpmBusinessProcessApi;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessResultEvent;

import static org.mockito.Mockito.never;
```

Add a field:

```java
private BpmBusinessProcessApi bpmBusinessProcessApi;
```

In `setUp()`, initialize and inject it after `bpmInstanceCopyService`:

```java
bpmBusinessProcessApi = Mockito.mock(BpmBusinessProcessApi.class);
setField(bpmTaskService, "bpmBusinessProcessApi", bpmBusinessProcessApi);
```

Add test methods:

```java
@Test
void approveShouldPublishBusinessResultEventWhenLastActiveTaskCompletes() {
    BpmTaskEntity taskEntity = buildPendingTask();
    BpmInstanceEntity instanceEntity = buildBusinessInstance();

    when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
    when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
    when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
    when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));
    when(taskServiceProjectionService().syncActiveTasksForInstance(8L)).thenReturn(0);

    BpmTaskApproveForm form = new BpmTaskApproveForm();
    form.setTaskId(1L);
    form.setCommentText("同意");

    ResponseDTO<String> response = bpmTaskService.approve(form);

    assertThat(response.getOk()).isTrue();
    ArgumentCaptor<BpmBusinessResultEvent> eventCaptor = ArgumentCaptor.forClass(BpmBusinessResultEvent.class);
    verify(bpmBusinessProcessApi).publishResultEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getEventId()).isEqualTo("RESULT:8:1");
    assertThat(eventCaptor.getValue().getInstanceId()).isEqualTo(8L);
    assertThat(eventCaptor.getValue().getBusinessType()).isEqualTo("sample_expense");
    assertThat(eventCaptor.getValue().getBusinessId()).isEqualTo(1001L);
    assertThat(eventCaptor.getValue().getResultState()).isEqualTo(BpmInstanceResultStateEnum.APPROVED.getValue());
    assertThat(eventCaptor.getValue().getPayloadJson()).isNull();
    assertThat(eventCaptor.getValue().getOccurredAt()).isNotNull();
}

@Test
void rejectShouldPublishBusinessResultEventWhenInstanceFinishes() {
    BpmTaskEntity taskEntity = buildPendingTask();
    BpmInstanceEntity instanceEntity = buildBusinessInstance();

    when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
    when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
    when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
    when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));
    when(taskServiceProjectionService().syncActiveTasksForInstance(8L)).thenReturn(0);

    BpmTaskRejectForm form = new BpmTaskRejectForm();
    form.setTaskId(1L);
    form.setCommentText("不同意");

    ResponseDTO<String> response = bpmTaskService.reject(form);

    assertThat(response.getOk()).isTrue();
    ArgumentCaptor<BpmBusinessResultEvent> eventCaptor = ArgumentCaptor.forClass(BpmBusinessResultEvent.class);
    verify(bpmBusinessProcessApi).publishResultEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getEventId()).isEqualTo("RESULT:8:2");
    assertThat(eventCaptor.getValue().getResultState()).isEqualTo(BpmInstanceResultStateEnum.REJECTED.getValue());
}

@Test
void finishInstanceShouldNotPublishBusinessResultEventWhenBusinessKeyIsMissing() {
    BpmTaskEntity taskEntity = buildPendingTask();
    BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
    instanceEntity.setInstanceId(8L);

    when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
    when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
    when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
    when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));
    when(taskServiceProjectionService().syncActiveTasksForInstance(8L)).thenReturn(0);

    BpmTaskApproveForm form = new BpmTaskApproveForm();
    form.setTaskId(1L);
    form.setCommentText("同意");

    ResponseDTO<String> response = bpmTaskService.approve(form);

    assertThat(response.getOk()).isTrue();
    verify(bpmBusinessProcessApi, never()).publishResultEvent(any());
}
```

Add helper near `buildPendingTask()`:

```java
private BpmInstanceEntity buildBusinessInstance() {
    BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
    instanceEntity.setInstanceId(8L);
    instanceEntity.setBusinessType("sample_expense");
    instanceEntity.setBusinessId(1001L);
    return instanceEntity;
}
```

- [ ] **Step 2: Run the focused test and confirm it fails**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest test
```

Expected: FAIL because `BpmTaskService` does not have `bpmBusinessProcessApi` and does not publish result events.

- [ ] **Step 3: Implement the minimal publisher in `BpmTaskService`**

Add imports:

```java
import com.hunyuan.sa.bpm.api.business.BpmBusinessProcessApi;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessResultEvent;
import org.springframework.util.StringUtils;
```

Add the dependency after `bpmInstanceCopyService`:

```java
@Resource
private BpmBusinessProcessApi bpmBusinessProcessApi;
```

Replace `finishInstance` with:

```java
private void finishInstance(Long instanceId, BpmInstanceResultStateEnum resultStateEnum) {
    LocalDateTime now = LocalDateTime.now();
    BpmInstanceEntity instanceEntity = bpmInstanceDao.selectById(instanceId);
    BpmInstanceEntity updateInstanceEntity = new BpmInstanceEntity();
    updateInstanceEntity.setInstanceId(instanceId);
    updateInstanceEntity.setRunState(BpmInstanceRunStateEnum.FINISHED.getValue());
    updateInstanceEntity.setResultState(resultStateEnum.getValue());
    updateInstanceEntity.setActiveTaskCount(0);
    updateInstanceEntity.setCurrentNodeSummaryJson(null);
    updateInstanceEntity.setFinishedAt(now);
    updateInstanceEntity.setLastActionAt(now);
    bpmInstanceDao.updateById(updateInstanceEntity);
    publishBusinessResultEventIfNeeded(instanceEntity, resultStateEnum, now);
}

private void publishBusinessResultEventIfNeeded(
        BpmInstanceEntity instanceEntity,
        BpmInstanceResultStateEnum resultStateEnum,
        LocalDateTime occurredAt
) {
    if (instanceEntity == null
            || !StringUtils.hasText(instanceEntity.getBusinessType())
            || instanceEntity.getBusinessId() == null) {
        return;
    }
    BpmBusinessResultEvent event = new BpmBusinessResultEvent();
    event.setEventId("RESULT:%s:%s".formatted(instanceEntity.getInstanceId(), resultStateEnum.getValue()));
    event.setInstanceId(instanceEntity.getInstanceId());
    event.setBusinessType(instanceEntity.getBusinessType());
    event.setBusinessId(instanceEntity.getBusinessId());
    event.setResultState(resultStateEnum.getValue());
    event.setOccurredAt(occurredAt);
    bpmBusinessProcessApi.publishResultEvent(event);
}
```

- [ ] **Step 4: Run the focused test and confirm it passes**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java
git commit -m "feat: 发布 BPM 业务审批结果事件"
```

---

### Task 2: Sample Expense Data Contract

**Files:**
- Create: `数据库SQL脚本/mysql/sql-update-log/v3.42.0.sql`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/domain/entity/BpmSampleExpenseEntity.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/dao/BpmSampleExpenseDao.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/domain/form/BpmSampleExpenseCreateForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/domain/vo/BpmSampleExpenseVO.java`

**Interfaces:**
- Produces:
  - Table `t_bpm_sample_expense`
  - Entity `BpmSampleExpenseEntity`
  - Mapper `BpmSampleExpenseDao extends BaseMapper<BpmSampleExpenseEntity>`
  - Form fields `title`, `amount`, `applicantEmployeeId`
  - VO fields matching the table plus status/time fields

- [ ] **Step 1: Create the SQL increment**

Create `数据库SQL脚本/mysql/sql-update-log/v3.42.0.sql`:

```sql
-- BPM P2.4：业务样板费用申请
CREATE TABLE `t_bpm_sample_expense` (
  `expense_id` bigint NOT NULL AUTO_INCREMENT COMMENT '样板费用申请ID',
  `title` varchar(100) NOT NULL COMMENT '申请标题',
  `amount` decimal(12,2) NOT NULL COMMENT '申请金额',
  `applicant_employee_id` bigint NOT NULL COMMENT '申请人员工ID',
  `approval_status` int NOT NULL COMMENT '业务审批状态：0草稿 1审批中 2已通过 3已拒绝',
  `instance_id` bigint NULL COMMENT '关联BPM实例ID',
  `callback_event_id` varchar(100) NULL COMMENT '最近一次回调事件ID',
  `callback_fail_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '下一次回调是否故意失败',
  `approved_at` datetime NULL COMMENT '审批通过回写时间',
  `rejected_at` datetime NULL COMMENT '审批拒绝回写时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`expense_id`),
  KEY `idx_bpm_sample_expense_instance` (`instance_id`),
  KEY `idx_bpm_sample_expense_applicant` (`applicant_employee_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM样板费用申请';
```

- [ ] **Step 2: Add the entity**

Create `BpmSampleExpenseEntity.java`:

```java
package com.hunyuan.sa.bpm.module.sampleexpense.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BPM 样板费用申请。
 */
@Data
@TableName("t_bpm_sample_expense")
public class BpmSampleExpenseEntity {

    @TableId(type = IdType.AUTO)
    private Long expenseId;

    private String title;

    private BigDecimal amount;

    private Long applicantEmployeeId;

    private Integer approvalStatus;

    private Long instanceId;

    private String callbackEventId;

    private Boolean callbackFailFlag;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

- [ ] **Step 3: Add the DAO**

Create `BpmSampleExpenseDao.java`:

```java
package com.hunyuan.sa.bpm.module.sampleexpense.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.sampleexpense.domain.entity.BpmSampleExpenseEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * BPM 样板费用申请 DAO。
 */
@Mapper
public interface BpmSampleExpenseDao extends BaseMapper<BpmSampleExpenseEntity> {
}
```

- [ ] **Step 4: Add the create form**

Create `BpmSampleExpenseCreateForm.java`:

```java
package com.hunyuan.sa.bpm.module.sampleexpense.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * BPM 样板费用申请创建表单。
 */
@Data
public class BpmSampleExpenseCreateForm {

    @Schema(description = "申请标题")
    @NotBlank(message = "申请标题不能为空")
    @Size(max = 100, message = "申请标题最多100个字符")
    private String title;

    @Schema(description = "申请金额")
    @NotNull(message = "申请金额不能为空")
    @DecimalMin(value = "0.01", message = "申请金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "申请人员工ID")
    @NotNull(message = "申请人员工ID不能为空")
    private Long applicantEmployeeId;
}
```

- [ ] **Step 5: Add the detail VO**

Create `BpmSampleExpenseVO.java`:

```java
package com.hunyuan.sa.bpm.module.sampleexpense.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BPM 样板费用申请详情。
 */
@Data
public class BpmSampleExpenseVO {

    private Long expenseId;

    private String title;

    private BigDecimal amount;

    private Long applicantEmployeeId;

    private Integer approvalStatus;

    private Long instanceId;

    private String callbackEventId;

    private Boolean callbackFailFlag;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

- [ ] **Step 6: Run a compile gate for the new contract**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -DskipTests compile
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add "数据库SQL脚本/mysql/sql-update-log/v3.42.0.sql" hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense
git commit -m "feat: 增加 BPM 样板费用申请数据合同"
```

---

### Task 3: Sample Expense Service and Callback Handler

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/service/BpmSampleExpenseService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense/service/BpmSampleExpenseCallbackHandler.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/sampleexpense/BpmSampleExpenseServiceTest.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/sampleexpense/BpmSampleExpenseCallbackHandlerTest.java`

**Interfaces:**
- Consumes:
  - `BpmBusinessProcessApi#start(BpmBusinessStartCommand command): Long`
  - `BpmBusinessCallbackContext`
  - `BpmBusinessCallbackResult`
- Produces:
  - `BpmSampleExpenseService#create(BpmSampleExpenseCreateForm form): ResponseDTO<Long>`
  - `BpmSampleExpenseService#start(Long expenseId): ResponseDTO<Long>`
  - `BpmSampleExpenseService#detail(Long expenseId): ResponseDTO<BpmSampleExpenseVO>`
  - `BpmSampleExpenseService#markNextCallbackFailed(Long expenseId): ResponseDTO<String>`
  - `BpmSampleExpenseService#handleCallback(BpmBusinessCallbackContext context): BpmBusinessCallbackResult`
  - `BpmSampleExpenseCallbackHandler#businessType(): "sample_expense"`

- [ ] **Step 1: Write failing service tests**

Create `BpmSampleExpenseServiceTest.java` with these tests:

```java
package com.hunyuan.sa.bpm.sampleexpense;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.business.BpmBusinessProcessApi;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessStartCommand;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackContext;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackResult;
import com.hunyuan.sa.bpm.module.sampleexpense.dao.BpmSampleExpenseDao;
import com.hunyuan.sa.bpm.module.sampleexpense.domain.entity.BpmSampleExpenseEntity;
import com.hunyuan.sa.bpm.module.sampleexpense.domain.form.BpmSampleExpenseCreateForm;
import com.hunyuan.sa.bpm.module.sampleexpense.service.BpmSampleExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmSampleExpenseServiceTest {

    private BpmSampleExpenseService service;
    private BpmSampleExpenseDao dao;
    private BpmBusinessProcessApi processApi;

    @BeforeEach
    void setUp() {
        service = new BpmSampleExpenseService();
        dao = Mockito.mock(BpmSampleExpenseDao.class);
        processApi = Mockito.mock(BpmBusinessProcessApi.class);
        setField(service, "bpmSampleExpenseDao", dao);
        setField(service, "bpmBusinessProcessApi", processApi);
    }

    @Test
    void createShouldInsertDraftExpense() {
        when(dao.insert(any(BpmSampleExpenseEntity.class))).thenAnswer(invocation -> {
            BpmSampleExpenseEntity entity = invocation.getArgument(0);
            entity.setExpenseId(1001L);
            return 1;
        });

        ResponseDTO<Long> response = service.create(buildCreateForm());

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(1001L);
        ArgumentCaptor<BpmSampleExpenseEntity> captor = ArgumentCaptor.forClass(BpmSampleExpenseEntity.class);
        verify(dao).insert(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("差旅费用样板");
        assertThat(captor.getValue().getApprovalStatus()).isEqualTo(0);
        assertThat(captor.getValue().getCallbackFailFlag()).isFalse();
    }

    @Test
    void startShouldCallBusinessProcessApiAndMarkExpenseApproving() {
        BpmSampleExpenseEntity entity = draftExpense();
        when(dao.selectById(1001L)).thenReturn(entity);
        when(processApi.start(any(BpmBusinessStartCommand.class))).thenReturn(88L);

        ResponseDTO<Long> response = service.start(1001L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(88L);
        ArgumentCaptor<BpmBusinessStartCommand> commandCaptor = ArgumentCaptor.forClass(BpmBusinessStartCommand.class);
        verify(processApi).start(commandCaptor.capture());
        assertThat(commandCaptor.getValue().getBusinessType()).isEqualTo("sample_expense");
        assertThat(commandCaptor.getValue().getBusinessId()).isEqualTo(1001L);
        assertThat(commandCaptor.getValue().getDefinitionKey()).isEqualTo("sample_expense_apply");
        assertThat(commandCaptor.getValue().getStartEmployeeId()).isEqualTo(10L);
        assertThat(commandCaptor.getValue().getFormDataJson()).contains("\"expenseId\":1001");
        ArgumentCaptor<BpmSampleExpenseEntity> updateCaptor = ArgumentCaptor.forClass(BpmSampleExpenseEntity.class);
        verify(dao).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getInstanceId()).isEqualTo(88L);
        assertThat(updateCaptor.getValue().getApprovalStatus()).isEqualTo(1);
    }

    @Test
    void startShouldReturnExistingInstanceWithoutStartingAgain() {
        BpmSampleExpenseEntity entity = draftExpense();
        entity.setApprovalStatus(1);
        entity.setInstanceId(88L);
        when(dao.selectById(1001L)).thenReturn(entity);

        ResponseDTO<Long> response = service.start(1001L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(88L);
        verify(processApi, never()).start(any());
    }

    @Test
    void handleCallbackShouldApproveExpense() {
        BpmSampleExpenseEntity entity = approvingExpense();
        when(dao.selectById(1001L)).thenReturn(entity);

        BpmBusinessCallbackResult result = service.handleCallback(approvedContext("event-1"));

        assertThat(result.success()).isTrue();
        ArgumentCaptor<BpmSampleExpenseEntity> updateCaptor = ArgumentCaptor.forClass(BpmSampleExpenseEntity.class);
        verify(dao).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getApprovalStatus()).isEqualTo(2);
        assertThat(updateCaptor.getValue().getCallbackEventId()).isEqualTo("event-1");
        assertThat(updateCaptor.getValue().getApprovedAt()).isNotNull();
    }

    @Test
    void handleCallbackShouldRejectExpense() {
        BpmSampleExpenseEntity entity = approvingExpense();
        when(dao.selectById(1001L)).thenReturn(entity);

        BpmBusinessCallbackResult result = service.handleCallback(rejectedContext("event-2"));

        assertThat(result.success()).isTrue();
        ArgumentCaptor<BpmSampleExpenseEntity> updateCaptor = ArgumentCaptor.forClass(BpmSampleExpenseEntity.class);
        verify(dao).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getApprovalStatus()).isEqualTo(3);
        assertThat(updateCaptor.getValue().getCallbackEventId()).isEqualTo("event-2");
        assertThat(updateCaptor.getValue().getRejectedAt()).isNotNull();
    }

    @Test
    void handleCallbackShouldFailOnceWhenFailFlagIsSetAndClearFlag() {
        BpmSampleExpenseEntity entity = approvingExpense();
        entity.setCallbackFailFlag(true);
        when(dao.selectById(1001L)).thenReturn(entity);

        BpmBusinessCallbackResult result = service.handleCallback(approvedContext("event-1"));

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).contains("样板费用申请模拟回调失败");
        ArgumentCaptor<BpmSampleExpenseEntity> updateCaptor = ArgumentCaptor.forClass(BpmSampleExpenseEntity.class);
        verify(dao).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getCallbackFailFlag()).isFalse();
        assertThat(updateCaptor.getValue().getApprovalStatus()).isNull();
    }

    @Test
    void handleCallbackShouldKeepIdempotentForSameEventId() {
        BpmSampleExpenseEntity entity = approvingExpense();
        entity.setApprovalStatus(2);
        entity.setCallbackEventId("event-1");
        entity.setApprovedAt(LocalDateTime.of(2026, 7, 9, 10, 0));
        when(dao.selectById(1001L)).thenReturn(entity);

        BpmBusinessCallbackResult result = service.handleCallback(approvedContext("event-1"));

        assertThat(result.success()).isTrue();
        verify(dao, never()).updateById(any());
    }

    @Test
    void handleCallbackShouldFailWhenTerminalStateConflicts() {
        BpmSampleExpenseEntity entity = approvingExpense();
        entity.setApprovalStatus(2);
        entity.setCallbackEventId("event-1");
        when(dao.selectById(1001L)).thenReturn(entity);

        BpmBusinessCallbackResult result = service.handleCallback(rejectedContext("event-2"));

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).contains("结果冲突");
        verify(dao, never()).updateById(any());
    }

    private BpmSampleExpenseCreateForm buildCreateForm() {
        BpmSampleExpenseCreateForm form = new BpmSampleExpenseCreateForm();
        form.setTitle("差旅费用样板");
        form.setAmount(new BigDecimal("1280.50"));
        form.setApplicantEmployeeId(10L);
        return form;
    }

    private BpmSampleExpenseEntity draftExpense() {
        BpmSampleExpenseEntity entity = new BpmSampleExpenseEntity();
        entity.setExpenseId(1001L);
        entity.setTitle("差旅费用样板");
        entity.setAmount(new BigDecimal("1280.50"));
        entity.setApplicantEmployeeId(10L);
        entity.setApprovalStatus(0);
        entity.setCallbackFailFlag(false);
        return entity;
    }

    private BpmSampleExpenseEntity approvingExpense() {
        BpmSampleExpenseEntity entity = draftExpense();
        entity.setApprovalStatus(1);
        entity.setInstanceId(88L);
        return entity;
    }

    private BpmBusinessCallbackContext approvedContext(String eventId) {
        return callbackContext(eventId, 1);
    }

    private BpmBusinessCallbackContext rejectedContext(String eventId) {
        return callbackContext(eventId, 2);
    }

    private BpmBusinessCallbackContext callbackContext(String eventId, int resultState) {
        return new BpmBusinessCallbackContext(
                31L,
                eventId,
                88L,
                "sample_expense",
                1001L,
                "{\"eventId\":\"" + eventId + "\",\"instanceId\":88,\"businessType\":\"sample_expense\",\"businessId\":1001,\"resultState\":" + resultState + "}"
        );
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

- [ ] **Step 2: Write the failing handler test**

Create `BpmSampleExpenseCallbackHandlerTest.java`:

```java
package com.hunyuan.sa.bpm.sampleexpense;

import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackContext;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackResult;
import com.hunyuan.sa.bpm.module.sampleexpense.service.BpmSampleExpenseCallbackHandler;
import com.hunyuan.sa.bpm.module.sampleexpense.service.BpmSampleExpenseService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmSampleExpenseCallbackHandlerTest {

    @Test
    void businessTypeShouldBeSampleExpense() {
        BpmSampleExpenseCallbackHandler handler = new BpmSampleExpenseCallbackHandler();

        assertThat(handler.businessType()).isEqualTo("sample_expense");
    }

    @Test
    void handleShouldDelegateToService() {
        BpmSampleExpenseCallbackHandler handler = new BpmSampleExpenseCallbackHandler();
        BpmSampleExpenseService service = Mockito.mock(BpmSampleExpenseService.class);
        setField(handler, "bpmSampleExpenseService", service);
        BpmBusinessCallbackContext context = new BpmBusinessCallbackContext(1L, "event-1", 88L, "sample_expense", 1001L, "{}");
        when(service.handleCallback(context)).thenReturn(BpmBusinessCallbackResult.success("{\"handled\":true}"));

        BpmBusinessCallbackResult result = handler.handle(context);

        assertThat(result.success()).isTrue();
        verify(service).handleCallback(context);
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

- [ ] **Step 3: Run tests and confirm they fail**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmSampleExpenseServiceTest,BpmSampleExpenseCallbackHandlerTest' test
```

Expected: FAIL because service and handler do not exist.

- [ ] **Step 4: Implement `BpmSampleExpenseService`**

Create `BpmSampleExpenseService.java`:

```java
package com.hunyuan.sa.bpm.module.sampleexpense.service;

import com.alibaba.fastjson.JSON;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.business.BpmBusinessProcessApi;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessResultEvent;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessStartCommand;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackContext;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackResult;
import com.hunyuan.sa.bpm.module.sampleexpense.dao.BpmSampleExpenseDao;
import com.hunyuan.sa.bpm.module.sampleexpense.domain.entity.BpmSampleExpenseEntity;
import com.hunyuan.sa.bpm.module.sampleexpense.domain.form.BpmSampleExpenseCreateForm;
import com.hunyuan.sa.bpm.module.sampleexpense.domain.vo.BpmSampleExpenseVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BPM 样板费用申请服务。
 */
@Service
public class BpmSampleExpenseService {

    public static final String BUSINESS_TYPE = "sample_expense";

    private static final String DEFINITION_KEY = "sample_expense_apply";

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_APPROVING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;

    @Resource
    private BpmSampleExpenseDao bpmSampleExpenseDao;

    @Resource
    private BpmBusinessProcessApi bpmBusinessProcessApi;

    public ResponseDTO<Long> create(BpmSampleExpenseCreateForm form) {
        BpmSampleExpenseEntity entity = new BpmSampleExpenseEntity();
        entity.setTitle(form.getTitle().trim());
        entity.setAmount(form.getAmount());
        entity.setApplicantEmployeeId(form.getApplicantEmployeeId());
        entity.setApprovalStatus(STATUS_DRAFT);
        entity.setCallbackFailFlag(false);
        bpmSampleExpenseDao.insert(entity);
        return ResponseDTO.ok(entity.getExpenseId());
    }

    public ResponseDTO<Long> start(Long expenseId) {
        BpmSampleExpenseEntity entity = bpmSampleExpenseDao.selectById(expenseId);
        if (entity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (entity.getInstanceId() != null) {
            return ResponseDTO.ok(entity.getInstanceId());
        }
        if (STATUS_APPROVED == entity.getApprovalStatus() || STATUS_REJECTED == entity.getApprovalStatus()) {
            return ResponseDTO.userErrorParam("样板费用申请已终态，不能再次发起");
        }
        BpmBusinessStartCommand command = new BpmBusinessStartCommand();
        command.setBusinessType(BUSINESS_TYPE);
        command.setBusinessId(entity.getExpenseId());
        command.setBusinessKey(BUSINESS_TYPE + ":" + entity.getExpenseId());
        command.setDefinitionKey(DEFINITION_KEY);
        command.setStartEmployeeId(entity.getApplicantEmployeeId());
        command.setTitle(entity.getTitle());
        command.setSummary("金额：" + entity.getAmount() + "，申请人：" + entity.getApplicantEmployeeId());
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("expenseId", entity.getExpenseId());
        formData.put("amount", entity.getAmount());
        command.setFormDataJson(JSON.toJSONString(formData));
        Long instanceId = bpmBusinessProcessApi.start(command);

        BpmSampleExpenseEntity update = new BpmSampleExpenseEntity();
        update.setExpenseId(entity.getExpenseId());
        update.setInstanceId(instanceId);
        update.setApprovalStatus(STATUS_APPROVING);
        bpmSampleExpenseDao.updateById(update);
        return ResponseDTO.ok(instanceId);
    }

    public ResponseDTO<BpmSampleExpenseVO> detail(Long expenseId) {
        BpmSampleExpenseEntity entity = bpmSampleExpenseDao.selectById(expenseId);
        if (entity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return ResponseDTO.ok(toVO(entity));
    }

    public ResponseDTO<String> markNextCallbackFailed(Long expenseId) {
        BpmSampleExpenseEntity entity = bpmSampleExpenseDao.selectById(expenseId);
        if (entity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        BpmSampleExpenseEntity update = new BpmSampleExpenseEntity();
        update.setExpenseId(expenseId);
        update.setCallbackFailFlag(true);
        bpmSampleExpenseDao.updateById(update);
        return ResponseDTO.ok();
    }

    public BpmBusinessCallbackResult handleCallback(BpmBusinessCallbackContext context) {
        BpmSampleExpenseEntity entity = bpmSampleExpenseDao.selectById(context.businessId());
        if (entity == null) {
            return BpmBusinessCallbackResult.failed("样板费用申请不存在: " + context.businessId(), null);
        }
        BpmBusinessResultEvent event;
        try {
            event = JSON.parseObject(context.requestPayloadJson(), BpmBusinessResultEvent.class);
        } catch (RuntimeException ex) {
            return BpmBusinessCallbackResult.failed("样板费用申请回调载荷解析失败", ex.getClass().getSimpleName());
        }
        if (event == null || event.getResultState() == null) {
            return BpmBusinessCallbackResult.failed("样板费用申请回调缺少审批结果", context.requestPayloadJson());
        }
        if (StringUtils.hasText(entity.getCallbackEventId())
                && entity.getCallbackEventId().equals(context.eventId())) {
            return BpmBusinessCallbackResult.success("{\"idempotent\":true}");
        }
        if (isTerminal(entity.getApprovalStatus())) {
            if (matchesTerminalResult(entity.getApprovalStatus(), event.getResultState())) {
                return BpmBusinessCallbackResult.success("{\"terminalRepeated\":true}");
            }
            return BpmBusinessCallbackResult.failed("样板费用申请已终态且结果冲突", context.requestPayloadJson());
        }
        if (Boolean.TRUE.equals(entity.getCallbackFailFlag())) {
            BpmSampleExpenseEntity update = new BpmSampleExpenseEntity();
            update.setExpenseId(entity.getExpenseId());
            update.setCallbackFailFlag(false);
            bpmSampleExpenseDao.updateById(update);
            return BpmBusinessCallbackResult.failed("样板费用申请模拟回调失败", "{\"failOnce\":true}");
        }
        if (Integer.valueOf(1).equals(event.getResultState())) {
            updateResult(entity.getExpenseId(), STATUS_APPROVED, context.eventId(), true);
            return BpmBusinessCallbackResult.success("{\"approvalStatus\":2}");
        }
        if (Integer.valueOf(2).equals(event.getResultState())) {
            updateResult(entity.getExpenseId(), STATUS_REJECTED, context.eventId(), false);
            return BpmBusinessCallbackResult.success("{\"approvalStatus\":3}");
        }
        return BpmBusinessCallbackResult.failed("未知审批结果: " + event.getResultState(), context.requestPayloadJson());
    }

    private void updateResult(Long expenseId, int approvalStatus, String eventId, boolean approved) {
        BpmSampleExpenseEntity update = new BpmSampleExpenseEntity();
        update.setExpenseId(expenseId);
        update.setApprovalStatus(approvalStatus);
        update.setCallbackEventId(eventId);
        if (approved) {
            update.setApprovedAt(LocalDateTime.now());
        } else {
            update.setRejectedAt(LocalDateTime.now());
        }
        bpmSampleExpenseDao.updateById(update);
    }

    private boolean isTerminal(Integer status) {
        return Integer.valueOf(STATUS_APPROVED).equals(status) || Integer.valueOf(STATUS_REJECTED).equals(status);
    }

    private boolean matchesTerminalResult(Integer approvalStatus, Integer resultState) {
        return (Integer.valueOf(STATUS_APPROVED).equals(approvalStatus) && Integer.valueOf(1).equals(resultState))
                || (Integer.valueOf(STATUS_REJECTED).equals(approvalStatus) && Integer.valueOf(2).equals(resultState));
    }

    private BpmSampleExpenseVO toVO(BpmSampleExpenseEntity entity) {
        BpmSampleExpenseVO vo = new BpmSampleExpenseVO();
        vo.setExpenseId(entity.getExpenseId());
        vo.setTitle(entity.getTitle());
        vo.setAmount(entity.getAmount());
        vo.setApplicantEmployeeId(entity.getApplicantEmployeeId());
        vo.setApprovalStatus(entity.getApprovalStatus());
        vo.setInstanceId(entity.getInstanceId());
        vo.setCallbackEventId(entity.getCallbackEventId());
        vo.setCallbackFailFlag(entity.getCallbackFailFlag());
        vo.setApprovedAt(entity.getApprovedAt());
        vo.setRejectedAt(entity.getRejectedAt());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
```

- [ ] **Step 5: Implement `BpmSampleExpenseCallbackHandler`**

Create `BpmSampleExpenseCallbackHandler.java`:

```java
package com.hunyuan.sa.bpm.module.sampleexpense.service;

import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackContext;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackHandler;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * BPM 样板费用申请回调处理器。
 */
@Component
public class BpmSampleExpenseCallbackHandler implements BpmBusinessCallbackHandler {

    @Resource
    private BpmSampleExpenseService bpmSampleExpenseService;

    @Override
    public String businessType() {
        return BpmSampleExpenseService.BUSINESS_TYPE;
    }

    @Override
    public BpmBusinessCallbackResult handle(BpmBusinessCallbackContext context) {
        return bpmSampleExpenseService.handleCallback(context);
    }
}
```

- [ ] **Step 6: Run focused tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmSampleExpenseServiceTest,BpmSampleExpenseCallbackHandlerTest' test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/sampleexpense hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/sampleexpense
git commit -m "feat: 增加 BPM 样板费用申请回调处理"
```

---

### Task 4: Admin API and Frontend API Contract

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmSampleExpenseController.java`
- Create: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/sample-expense.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/index.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`

**Interfaces:**
- Produces backend endpoints:
  - `POST /bpm/sample/expense/create`
  - `POST /bpm/sample/expense/start/{expenseId}`
  - `GET /bpm/sample/expense/detail/{expenseId}`
  - `POST /bpm/sample/expense/markNextCallbackFailed/{expenseId}`
- Produces frontend functions:
  - `createBpmSampleExpense(data): Promise<number>`
  - `startBpmSampleExpense(expenseId): Promise<number>`
  - `getBpmSampleExpenseDetail(expenseId): Promise<BpmSampleExpenseVO>`
  - `markNextBpmSampleExpenseCallbackFailed(expenseId): Promise<string>`

- [ ] **Step 1: Write the failing frontend API contract test**

In `bpm-api.test.ts`, add this object to `apiFiles` before `integration`:

```ts
{
  label: 'sampleExpense',
  needles: [
    'createBpmSampleExpense',
    '/bpm/sample/expense/create',
    'startBpmSampleExpense',
    '/bpm/sample/expense/start/',
    'getBpmSampleExpenseDetail',
    '/bpm/sample/expense/detail/',
    'markNextBpmSampleExpenseCallbackFailed',
    '/bpm/sample/expense/markNextCallbackFailed/',
    'BpmSampleExpenseVO',
  ],
  path: 'apps/hunyuan-system/src/api/system/bpm/sample-expense.ts',
},
```

- [ ] **Step 2: Run the frontend test and confirm it fails**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts --dom
```

Expected: FAIL because `sample-expense.ts` does not exist.

- [ ] **Step 3: Add the admin controller**

Create `AdminBpmSampleExpenseController.java`:

```java
package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.sampleexpense.domain.form.BpmSampleExpenseCreateForm;
import com.hunyuan.sa.bpm.module.sampleexpense.domain.vo.BpmSampleExpenseVO;
import com.hunyuan.sa.bpm.module.sampleexpense.service.BpmSampleExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * BPM 样板费用申请管理接口。
 */
@RestController
@Tag(name = "BPM Sample Expense")
public class AdminBpmSampleExpenseController {

    @Resource
    private BpmSampleExpenseService bpmSampleExpenseService;

    @Operation(summary = "创建 BPM 样板费用申请")
    @PostMapping("/bpm/sample/expense/create")
    @SaCheckPermission("bpm:integration:update")
    public ResponseDTO<Long> create(@RequestBody @Valid BpmSampleExpenseCreateForm form) {
        return bpmSampleExpenseService.create(form);
    }

    @Operation(summary = "发起 BPM 样板费用申请流程")
    @PostMapping("/bpm/sample/expense/start/{expenseId}")
    @SaCheckPermission("bpm:integration:update")
    public ResponseDTO<Long> start(@PathVariable Long expenseId) {
        return bpmSampleExpenseService.start(expenseId);
    }

    @Operation(summary = "查询 BPM 样板费用申请详情")
    @GetMapping("/bpm/sample/expense/detail/{expenseId}")
    @SaCheckPermission("bpm:integration:query")
    public ResponseDTO<BpmSampleExpenseVO> detail(@PathVariable Long expenseId) {
        return bpmSampleExpenseService.detail(expenseId);
    }

    @Operation(summary = "设置 BPM 样板费用申请下一次回调失败")
    @PostMapping("/bpm/sample/expense/markNextCallbackFailed/{expenseId}")
    @SaCheckPermission("bpm:integration:update")
    public ResponseDTO<String> markNextCallbackFailed(@PathVariable Long expenseId) {
        return bpmSampleExpenseService.markNextCallbackFailed(expenseId);
    }
}
```

- [ ] **Step 4: Add the frontend API module**

Create `sample-expense.ts`:

```ts
import { requestClient } from '#/api/request';

export interface BpmSampleExpenseCreateParams {
  amount: number;
  applicantEmployeeId: number;
  title: string;
}

export interface BpmSampleExpenseVO {
  amount: number;
  applicantEmployeeId: number;
  approvalStatus: number;
  approvedAt?: null | string;
  callbackEventId?: null | string;
  callbackFailFlag: boolean;
  createTime?: null | string;
  expenseId: number;
  instanceId?: null | number;
  rejectedAt?: null | string;
  title: string;
  updateTime?: null | string;
}

export async function createBpmSampleExpense(
  data: BpmSampleExpenseCreateParams,
) {
  return requestClient.post<number>('/bpm/sample/expense/create', {
    amount: data.amount,
    applicantEmployeeId: data.applicantEmployeeId,
    title: data.title.trim(),
  });
}

export async function startBpmSampleExpense(expenseId: number) {
  return requestClient.post<number>(`/bpm/sample/expense/start/${expenseId}`);
}

export async function getBpmSampleExpenseDetail(expenseId: number) {
  return requestClient.get<BpmSampleExpenseVO>(
    `/bpm/sample/expense/detail/${expenseId}`,
  );
}

export async function markNextBpmSampleExpenseCallbackFailed(
  expenseId: number,
) {
  return requestClient.post<string>(
    `/bpm/sample/expense/markNextCallbackFailed/${expenseId}`,
  );
}
```

- [ ] **Step 5: Export the sample module**

In `index.ts`, add endpoint index:

```ts
sampleExpense: '/bpm/sample/expense/',
```

Add export:

```ts
export * from './sample-expense';
```

- [ ] **Step 6: Run frontend contract and typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts --dom
```

Expected: PASS.

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS.

- [ ] **Step 7: Run backend compile**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm -DskipTests compile
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmSampleExpenseController.java hunyuan-design/apps/hunyuan-system/src/api/system/bpm/sample-expense.ts hunyuan-design/apps/hunyuan-system/src/api/system/bpm/index.ts hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts
git commit -m "feat: 接入 BPM 样板费用申请 API"
```

---

### Task 5: Executor Integration, Gates, and Acceptance Record

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessCallbackExecutorTest.java`
- Create: `docs/superpowers/specs/2026-07-09-bpm-p2-business-sample-acceptance.md`

**Interfaces:**
- Consumes:
  - `BpmSampleExpenseCallbackHandler#businessType(): String`
  - `BpmBusinessCallbackExecutor#execute(Long, BpmBusinessCallbackTriggerType): BpmBusinessCallbackExecuteResult`
  - Existing instance trace and callback record frontend reliability surfaces
- Produces:
  - Proof that real sample handler can be discovered by `businessType = "sample_expense"`
  - Acceptance record with exact commands and boundaries

- [ ] **Step 1: Add executor integration test for the real sample handler**

Modify `BpmBusinessCallbackExecutorTest.java`.

Add imports:

```java
import com.hunyuan.sa.bpm.module.sampleexpense.service.BpmSampleExpenseCallbackHandler;
import com.hunyuan.sa.bpm.module.sampleexpense.service.BpmSampleExpenseService;
```

Add this test:

```java
@Test
void executeShouldCallSampleExpenseHandlerByBusinessType() {
    BpmSampleExpenseService sampleService = Mockito.mock(BpmSampleExpenseService.class);
    BpmSampleExpenseCallbackHandler sampleHandler = new BpmSampleExpenseCallbackHandler();
    setField(sampleHandler, "bpmSampleExpenseService", sampleService);
    setHandlers(List.of(sampleHandler));
    BpmCallbackRecordEntity record = buildRecord(BpmCallbackStatusEnum.PENDING.getValue(), 0);
    record.setBusinessType("sample_expense");
    record.setRequestPayloadJson("{\"eventId\":\"event-1\",\"instanceId\":88,\"businessType\":\"sample_expense\",\"businessId\":1001,\"resultState\":1}");
    when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);
    when(sampleService.handleCallback(any(BpmBusinessCallbackContext.class)))
            .thenReturn(BpmBusinessCallbackResult.success("{\"approvalStatus\":2}"));

    BpmBusinessCallbackExecuteResult result = executor.execute(1L, BpmBusinessCallbackTriggerType.MANUAL);

    assertThat(result.processed()).isTrue();
    assertThat(result.succeeded()).isTrue();
    verify(sampleService).handleCallback(any(BpmBusinessCallbackContext.class));
    ArgumentCaptor<BpmCallbackRecordEntity> captor = ArgumentCaptor.forClass(BpmCallbackRecordEntity.class);
    verify(bpmCallbackRecordDao).update(captor.capture(), any());
    assertThat(captor.getValue().getCallbackStatus()).isEqualTo(BpmCallbackStatusEnum.SUCCEEDED.getValue());
    assertThat(captor.getValue().getResponsePayloadJson()).isEqualTo("{\"approvalStatus\":2}");
}
```

- [ ] **Step 2: Run the integrated backend focused gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmRuntimeCommandServiceTest,BpmSampleExpenseServiceTest,BpmSampleExpenseCallbackHandlerTest,BpmBusinessCallbackExecutorTest,BpmBusinessProcessApiTest' test
```

Expected: PASS.

- [ ] **Step 3: Run the full BPM backend gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

Expected: PASS.

- [ ] **Step 4: Run frontend BPM source contract gate**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: PASS.

- [ ] **Step 5: Run frontend typecheck**

Run:

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

Expected: PASS.

- [ ] **Step 6: Run Flowable compatibility gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test
```

Expected: PASS.

- [ ] **Step 7: Create the acceptance record**

Create `docs/superpowers/specs/2026-07-09-bpm-p2-business-sample-acceptance.md`:

```markdown
# BPM P2.4 业务样板闭环验收记录

## 结论

P2.4 业务样板闭环通过源级验收。样板费用申请已经证明业务单据可以通过 `BpmBusinessProcessApi.start` 发起流程，真实审批终态可以发布 `BpmBusinessResultEvent`，P2.3 回调执行器可以找到 `sample_expense` handler 并回写业务状态。

## 已完成范围

- 新增 `t_bpm_sample_expense` 样板费用申请表。
- 新增 `sampleexpense` 后端边界：实体、DAO、表单、VO、service、callback handler。
- 新增最小管理端 API：创建、发起、查询详情、设置下一次回调失败。
- 真实审批完成路径发布业务结果事件，不再只依赖测试手动调用 `publishResultEvent`。
- 样板 handler 覆盖通过、拒绝、失败注入、同事件幂等、终态重复和冲突结果。
- 前端只新增 API 合同，不新增菜单和复杂页面。

## 验证命令

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmRuntimeCommandServiceTest,BpmSampleExpenseServiceTest,BpmSampleExpenseCallbackHandlerTest,BpmBusinessCallbackExecutorTest,BpmBusinessProcessApiTest' test
```

结果：PASS。

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

结果：PASS。

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

结果：PASS。

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

结果：PASS。

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test
```

结果：PASS。

## 边界

- 未新增真实费用报销产品、采购、合同或资产模块。
- 未新增样板菜单或复杂业务页面。
- 未暴露 Flowable 原生对象。
- 未新增 MQ、HTTP 回调平台、事件总线或外部调度。
- 可视化可靠性仍复用实例 trace 和回调记录列表。

## 后续

- 如果需要业务人员演示，再单独建设安静的样板页。
- 如果需要浏览器级业务证明，按 `AGENTS.md` 使用持久 Playwright MCP 会话。
```

- [ ] **Step 8: Commit**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/integration/BpmBusinessCallbackExecutorTest.java docs/superpowers/specs/2026-07-09-bpm-p2-business-sample-acceptance.md
git commit -m "test: 验证 BPM 样板回调执行闭环"
```

---

## Final Verification

After all tasks and commits, run:

```powershell
git status --short
```

Expected: no uncommitted implementation files. Runtime evidence files, screenshots, logs, or local Playwright artifacts must not be committed.

Run:

```powershell
git log --oneline -5
```

Expected: the latest commits show the P2.4 event publisher, data contract, service/handler, API contract, and closure test/acceptance record.

## Self-Review

- Spec coverage: Task 1 covers real approval result event publishing. Task 2 covers `t_bpm_sample_expense`. Task 3 covers service, callback handler, failure injection, and idempotency. Task 4 covers admin and frontend API contracts without adding a menu. Task 5 covers executor integration and acceptance evidence.
- Placeholder scan: no `TBD`, `TODO`, or unstated edge handling remains in the task steps.
- Type consistency: service signatures, endpoint paths, business type `sample_expense`, definition key `sample_expense_apply`, and event id format `RESULT:{instanceId}:{resultState}` match the P2.4 design document.
