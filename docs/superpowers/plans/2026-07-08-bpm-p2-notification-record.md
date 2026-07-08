# BPM P2 Notification Record Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add BPM-owned notification delivery records for the first real runtime trigger: notifying the assignee when a new pending BPM task is projected.

**Architecture:** Reuse Hunyuan base delivery services and the existing `BpmNotificationCommand` / `BpmNotificationListenerService` adapter. Add a BPM notification record model and record-aware dispatch path, invoke it only from `BpmTaskProjectionService.insertTaskIfMissing`, then surface notification records through the existing admin instance trace. Do not add a queue, retry executor, general message center, or broad SMS/mail redesign in this slice.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, JUnit 5, Mockito, AssertJ, Vue 3, TypeScript, Element Plus, Vitest, MySQL update SQL.

## Global Constraints

- Production code, contracts, routes, permissions, menus, tests, docs, and verification artifacts must stay in `E:\my-project\hunyuan-pro`.
- Yudao and RuoYi are reference lines only; borrow mechanisms, not code or API names.
- Public Hunyuan BPM APIs must not expose Flowable native objects, names, or IDs.
- Do not add new dependencies.
- P2.2 covers BPM notification records and the first new-task notification trigger only.
- Notification send failure must not roll back BPM task projection.
- Runtime employee-side detail must not show platform notification failure internals.
- The optional standalone notification monitoring list page is deferred until trace support is green.

---

## File Structure

- Create `数据库SQL脚本/mysql/sql-update-log/v3.40.0.sql`: create `t_bpm_notification_record`.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmNotificationSendStatusEnum.java`: status enum.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmNotificationChannelEnum.java`: channel enum.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmNotificationRecordEntity.java`: MyBatis entity.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmNotificationRecordDao.java`: mapper.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmNotificationRecordQueryForm.java`: admin query form for future list support.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmNotificationRecordVO.java`: trace/list record view.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmNotificationRecordService.java`: create pending, mark success/fail, query by instance.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmNotificationCommand.java`: add BPM context fields and stable request snapshots.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmNotificationListenerService.java`: record per-channel attempts.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskProjectionService.java`: dispatch notification only after inserting a new task with listener config.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceTraceVO.java`: add notification records.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceTraceService.java`: populate notification records.
- Create and update backend tests under `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/`.
- Modify `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`: add notification record type to trace type.
- Modify `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`: pin trace notification contract.
- Modify `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue`: add admin-only notification table inside reliability trace.
- Modify `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`: pin drawer notification section.
- Create `docs/superpowers/specs/2026-07-08-bpm-p2-notification-record-acceptance.md`: verification evidence.

---

### Task 1: Notification Record Model and Service

**Files:**
- Create: `数据库SQL脚本/mysql/sql-update-log/v3.40.0.sql`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmNotificationSendStatusEnum.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/common/enumeration/BpmNotificationChannelEnum.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmNotificationRecordEntity.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmNotificationRecordDao.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/form/BpmNotificationRecordQueryForm.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmNotificationRecordVO.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmNotificationRecordService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmNotificationRecordServiceTest.java`

**Interfaces:**
- Produces:
  - `BpmNotificationRecordService#createPendingRecord(BpmNotificationCommand command, String channel): BpmNotificationRecordVO`
  - `BpmNotificationRecordService#markSuccess(Long notificationRecordId, String responseSnapshotJson): void`
  - `BpmNotificationRecordService#markFail(Long notificationRecordId, String failReason): void`
  - `BpmNotificationRecordService#queryByInstanceId(Long instanceId): List<BpmNotificationRecordVO>`

- [ ] **Step 1: Write the failing service test**

Create `BpmNotificationRecordServiceTest.java` with tests that require the service API before production code exists:

```java
package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.common.enumeration.BpmNotificationSendStatusEnum;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmNotificationRecordDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmNotificationRecordEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmNotificationRecordVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmNotificationCommand;
import com.hunyuan.sa.bpm.module.runtime.service.BpmNotificationRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmNotificationRecordServiceTest {

    private BpmNotificationRecordService recordService;

    private BpmNotificationRecordDao notificationRecordDao;

    @BeforeEach
    void setUp() {
        recordService = new BpmNotificationRecordService();
        notificationRecordDao = Mockito.mock(BpmNotificationRecordDao.class);
        setField(recordService, "notificationRecordDao", notificationRecordDao);
    }

    @Test
    void createPendingRecordShouldPersistBpmContext() {
        when(notificationRecordDao.insert(any(BpmNotificationRecordEntity.class))).thenAnswer(invocation -> {
            BpmNotificationRecordEntity entity = invocation.getArgument(0);
            entity.setNotificationRecordId(10L);
            return 1;
        });
        BpmNotificationCommand command = buildCommand();

        BpmNotificationRecordVO record = recordService.createPendingRecord(command, "MESSAGE");

        assertThat(record.getNotificationRecordId()).isEqualTo(10L);
        assertThat(record.getInstanceId()).isEqualTo(88L);
        assertThat(record.getTaskId()).isEqualTo(11L);
        assertThat(record.getEventKey()).isEqualTo("TASK_CREATED");
        assertThat(record.getChannel()).isEqualTo("MESSAGE");
        assertThat(record.getSendStatus()).isEqualTo(BpmNotificationSendStatusEnum.PENDING.getValue());
        verify(notificationRecordDao).insert(any(BpmNotificationRecordEntity.class));
    }

    @Test
    void markSuccessShouldUpdateStatusAndResponseSnapshot() {
        recordService.markSuccess(10L, "{\"messageId\":1}");

        verify(notificationRecordDao).updateById(any(BpmNotificationRecordEntity.class));
    }

    @Test
    void markFailShouldTruncateFailureReason() {
        String longReason = "x".repeat(1200);

        recordService.markFail(10L, longReason);

        verify(notificationRecordDao).updateById(any(BpmNotificationRecordEntity.class));
    }

    @Test
    void queryByInstanceIdShouldReturnMappedRecords() {
        BpmNotificationRecordEntity entity = new BpmNotificationRecordEntity();
        entity.setNotificationRecordId(10L);
        entity.setInstanceId(88L);
        entity.setTaskId(11L);
        entity.setEventKey("TASK_CREATED");
        entity.setChannel("MESSAGE");
        entity.setReceiverEmployeeId(1001L);
        entity.setSendStatus(BpmNotificationSendStatusEnum.SUCCESS.getValue());
        when(notificationRecordDao.selectList(any())).thenReturn(List.of(entity));

        List<BpmNotificationRecordVO> records = recordService.queryByInstanceId(88L);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getNotificationRecordId()).isEqualTo(10L);
        assertThat(records.get(0).getInstanceId()).isEqualTo(88L);
        assertThat(records.get(0).getChannel()).isEqualTo("MESSAGE");
    }

    private BpmNotificationCommand buildCommand() {
        return new BpmNotificationCommand(
                List.of("MESSAGE"),
                88L,
                11L,
                6L,
                7L,
                "TASK_CREATED",
                1001L,
                "{\"employeeId\":1001,\"actualName\":\"Alice\"}",
                null,
                List.of(),
                "待办提醒",
                "待办提醒",
                "你有一个新的待办任务",
                "bpm_task_created"
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

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmNotificationRecordServiceTest' test
```

Expected: FAIL because notification record classes and the expanded command constructor do not exist.

- [ ] **Step 3: Create SQL table**

Create `v3.40.0.sql`:

```sql
-- BPM P2.2: notification delivery records
CREATE TABLE `t_bpm_notification_record` (
  `notification_record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知记录ID',
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `task_id` bigint NULL COMMENT '流程任务ID',
  `definition_id` bigint NULL COMMENT '流程定义ID',
  `definition_node_id` bigint NULL COMMENT '流程定义节点ID',
  `event_key` varchar(64) NOT NULL COMMENT '事件键',
  `channel` varchar(32) NOT NULL COMMENT '通知渠道',
  `receiver_employee_id` bigint NULL COMMENT '接收员工ID',
  `receiver_snapshot_json` longtext NULL COMMENT '接收人快照',
  `template_code` varchar(128) NULL COMMENT '模板编码',
  `title` varchar(200) NULL COMMENT '通知标题',
  `content_snapshot` varchar(1000) NULL COMMENT '通知内容快照',
  `send_status` tinyint NOT NULL COMMENT '发送状态',
  `request_payload_json` longtext NULL COMMENT '请求快照',
  `response_snapshot_json` longtext NULL COMMENT '响应快照',
  `fail_reason` varchar(1000) NULL COMMENT '失败原因',
  `sent_at` datetime NULL COMMENT '发送完成时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`notification_record_id`),
  KEY `idx_bpm_notification_instance` (`instance_id`),
  KEY `idx_bpm_notification_task` (`task_id`),
  KEY `idx_bpm_notification_event` (`event_key`),
  KEY `idx_bpm_notification_channel_status` (`channel`, `send_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM通知投递记录';
```

- [ ] **Step 4: Create enums, entity, DAO, form, and VO**

Create `BpmNotificationSendStatusEnum.java`:

```java
package com.hunyuan.sa.bpm.common.enumeration;

import com.hunyuan.sa.base.common.enumeration.BaseEnum;

public enum BpmNotificationSendStatusEnum implements BaseEnum {

    PENDING(0, "待发送"),
    SUCCESS(1, "发送成功"),
    FAIL(2, "发送失败");

    private final Integer value;
    private final String desc;

    BpmNotificationSendStatusEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
```

Create `BpmNotificationChannelEnum.java`:

```java
package com.hunyuan.sa.bpm.common.enumeration;

public enum BpmNotificationChannelEnum {

    MESSAGE,
    SMS,
    MAIL
}
```

Create `BpmNotificationRecordEntity` with these fields and annotations:

```java
@Data
@TableName("t_bpm_notification_record")
public class BpmNotificationRecordEntity {
    @TableId(type = IdType.AUTO)
    private Long notificationRecordId;
    private Long instanceId;
    private Long taskId;
    private Long definitionId;
    private Long definitionNodeId;
    private String eventKey;
    private String channel;
    private Long receiverEmployeeId;
    private String receiverSnapshotJson;
    private String templateCode;
    private String title;
    private String contentSnapshot;
    private Integer sendStatus;
    private String requestPayloadJson;
    private String responseSnapshotJson;
    private String failReason;
    private LocalDateTime sentAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

Create `BpmNotificationRecordDao`:

```java
@Mapper
public interface BpmNotificationRecordDao extends BaseMapper<BpmNotificationRecordEntity> {
}
```

Create `BpmNotificationRecordQueryForm`:

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmNotificationRecordQueryForm extends PageParam {
    private Long instanceId;
    private Long taskId;
    private String eventKey;
    private String channel;
    private Long receiverEmployeeId;
    private Integer sendStatus;
}
```

Create `BpmNotificationRecordVO` with the same business fields as the entity except audit fill annotations.

- [ ] **Step 5: Implement `BpmNotificationRecordService`**

Create the service with exact public methods from this task's interface. The implementation should:

```java
public BpmNotificationRecordVO createPendingRecord(BpmNotificationCommand command, String channel) {
    BpmNotificationRecordEntity entity = new BpmNotificationRecordEntity();
    entity.setInstanceId(command.instanceId());
    entity.setTaskId(command.taskId());
    entity.setDefinitionId(command.definitionId());
    entity.setDefinitionNodeId(command.definitionNodeId());
    entity.setEventKey(command.eventKey());
    entity.setChannel(channel);
    entity.setReceiverEmployeeId(command.receiverEmployeeId());
    entity.setReceiverSnapshotJson(command.receiverSnapshotJson());
    entity.setTemplateCode(command.smsTemplateCode());
    entity.setTitle(command.title());
    entity.setContentSnapshot(StringUtils.left(command.content(), 1000));
    entity.setSendStatus(BpmNotificationSendStatusEnum.PENDING.getValue());
    entity.setRequestPayloadJson(buildRequestSnapshot(command, channel));
    notificationRecordDao.insert(entity);
    return toVO(entity);
}
```

`markSuccess` sets `sendStatus=SUCCESS`, `responseSnapshotJson`, and `sentAt=LocalDateTime.now()`. `markFail` sets `sendStatus=FAIL`, `failReason=StringUtils.left(failReason, 1000)`, and `sentAt=LocalDateTime.now()`. `queryByInstanceId` returns an empty list for null IDs and otherwise orders by `createTime asc, notificationRecordId asc`.

- [ ] **Step 6: Run Task 1 test**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmNotificationRecordServiceTest' test
```

Expected: PASS.

---

### Task 2: Record-Aware Notification Dispatch

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmNotificationCommand.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmNotificationListenerService.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmNotificationListenerServiceTest.java`

**Interfaces:**
- Consumes: `BpmNotificationRecordService`.
- Produces: `BpmNotificationListenerService#dispatch(BpmNotificationCommand)` that writes one BPM record per channel attempt.

- [ ] **Step 1: Write failing dispatch tests**

Create tests for:

- `dispatchShouldRecordSuccessForMessageChannel`
- `dispatchShouldRecordFailWhenMessageServiceThrows`
- `dispatchShouldContinueOtherChannelsAfterFailure`

The tests should mock `MessageService`, `SmsService`, `MailService`, and `BpmNotificationRecordService`, then verify `createPendingRecord`, `markSuccess`, and `markFail` calls.

- [ ] **Step 2: Run dispatch test to verify it fails**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmNotificationListenerServiceTest' test
```

Expected: FAIL because listener does not yet use `BpmNotificationRecordService`.

- [ ] **Step 3: Implement record-aware dispatch**

Inject `BpmNotificationRecordService` into `BpmNotificationListenerService`. For each channel:

```java
BpmNotificationRecordVO record = notificationRecordService.createPendingRecord(command, channel);
try {
    sendByChannel(command, channel);
    notificationRecordService.markSuccess(record.getNotificationRecordId(), "{}");
} catch (Exception ex) {
    notificationRecordService.markFail(record.getNotificationRecordId(), ex.getMessage());
}
```

Keep channel failure isolated so other channels still attempt delivery.

- [ ] **Step 4: Run dispatch test**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmNotificationListenerServiceTest' test
```

Expected: PASS.

---

### Task 3: New Pending Task Notification Trigger

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskProjectionService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskProjectionServiceTest.java`

**Interfaces:**
- Consumes: `BpmNotificationListenerService#dispatch(BpmNotificationCommand)`.
- Produces: notification dispatch from `insertTaskIfMissing` only after a new task insert with listener config and assignee.

- [ ] **Step 1: Write failing projection tests**

Add tests that prove:

- new inserted task with compiled node listener `{"listeners":[{"channel":"MESSAGE"}]}` dispatches one notification command.
- existing task does not dispatch.
- task without assignee does not dispatch.

- [ ] **Step 2: Run projection test to verify it fails**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskProjectionServiceTest' test
```

Expected: FAIL because projection service does not dispatch notification.

- [ ] **Step 3: Implement trigger**

Inject `BpmNotificationListenerService`. After `bpmTaskDao.insert(task)`, parse `node.getCompiledNodeSnapshotJson()` for `listeners`, build channels, and dispatch a `BpmNotificationCommand` with:

- `eventKey = "TASK_CREATED"`
- `instanceId = instance.getInstanceId()`
- `taskId = task.getTaskId()`
- `definitionId = instance.getDefinitionId()`
- `definitionNodeId = task.getDefinitionNodeId()`
- `receiverEmployeeId = task.getAssigneeEmployeeId()`
- `receiverSnapshotJson` from assignee snapshot fields
- `title = "流程待办提醒"`
- `subject = "流程待办提醒"`
- `content = "你有一个新的流程待办：" + task.getInstanceTitle()`
- `smsTemplateCode = "bpm_task_created"`

- [ ] **Step 4: Run projection test**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmTaskProjectionServiceTest' test
```

Expected: PASS.

---

### Task 4: Trace and Frontend Reliability View

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceTraceVO.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceTraceService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmInstanceTraceServiceTest.java`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

**Interfaces:**
- Produces: `BpmInstanceTraceVO#getNotificationRecords()` and frontend `notificationRecords` display in admin-only trace.

- [ ] **Step 1: Write failing backend trace test**

Update `BpmInstanceTraceServiceTest` to mock `BpmNotificationRecordService.queryByInstanceId(88L)` and assert `response.getData().getNotificationRecords()` has one record.

- [ ] **Step 2: Implement backend trace integration**

Inject `BpmNotificationRecordService` into `BpmInstanceTraceService`, add `notificationRecords` to `BpmInstanceTraceVO`, and populate it.

- [ ] **Step 3: Write failing frontend source contract tests**

Update:

- `bpm-api.test.ts`: require `notificationRecords`.
- `bpm-modules.test.ts`: require `通知记录` and `notificationRecords` in the drawer.

- [ ] **Step 4: Implement frontend type and drawer section**

In `runtime.ts`, add `BpmNotificationRecordVO` interface and include `notificationRecords` in `BpmInstanceTraceRecord`.

In `bpm-instance-detail-drawer.vue`, add a compact notification table under the reliability trace:

- event key
- channel
- receiver employee ID
- status
- sent time
- failure reason

- [ ] **Step 5: Run trace/frontend tests**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmInstanceTraceServiceTest' test
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: PASS.

---

### Task 5: Verification and Acceptance Record

**Files:**
- Create: `docs/superpowers/specs/2026-07-08-bpm-p2-notification-record-acceptance.md`

**Interfaces:**
- Produces: durable acceptance evidence for P2.2.

- [ ] **Step 1: Run backend focused gate**

Run:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmNotificationRecordServiceTest,BpmNotificationListenerServiceTest,BpmTaskProjectionServiceTest,BpmInstanceTraceServiceTest' test
```

Expected: PASS.

- [ ] **Step 2: Run backend module gate**

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

- [ ] **Step 6: Write acceptance record**

Record the scope, commands, observed pass counts, known warnings, and boundaries. State explicitly that the optional standalone notification list page is deferred.

---

## Self-Review Checklist

- Spec coverage: the plan covers the record table, record service, dispatch wrapper, first new-task trigger, trace integration, frontend trace display, and verification evidence.
- Scope boundary: the standalone notification list page, retry executor, full event ledger, queue dependency, SMS/mail redesign, and Flowable exposure are outside this first implementation slice.
- Type consistency: `BpmNotificationRecordVO`, `BpmNotificationRecordService`, `BpmNotificationCommand`, and `notificationRecords` are named consistently across backend and frontend tasks.
- Verification: every implementation task has a red test, a targeted green command, and a broader verification gate.
