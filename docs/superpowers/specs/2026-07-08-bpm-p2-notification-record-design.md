# BPM P2.2 Notification Record Design

## Verdict

P2.2 should implement **BPM-owned notification delivery records with a first real trigger: new pending task notification**.

Hunyuan already has the general delivery capabilities in `hunyuan-base` and a BPM notification adapter in `hunyuan-bpm`:

- `BpmNotificationCommand`
- `BpmNotificationListenerService`
- `MessageService`
- `SmsService`
- `MailService`

The missing enterprise capability is not another message system. The missing capability is that BPM cannot yet answer: for this workflow instance, which notification did BPM attempt to send, to whom, through which channel, did it succeed, and why did it fail?

## Current Evidence

- `BpmNotificationListenerService.dispatch(BpmNotificationCommand)` already dispatches `MESSAGE`, `SMS`, and `MAIL` channels.
- `BpmNotificationCommand` converts BPM notification intent into base message and SMS forms.
- Definition publishing already preserves node listener configuration in `BpmDefinitionNodeEntity.compiledNodeSnapshotJson`; existing tests assert listener JSON survives publish.
- `BpmTaskProjectionService.insertTaskIfMissing` is the first reliable runtime point where a new Hunyuan task is inserted only once. This makes it the best first notification trigger and avoids duplicate notification attempts during repeated task projection sync.
- The P2.1 trace endpoint already aggregates instance detail, current tasks, action logs, callback records, and command records. P2.2 can add notification records to the same trace shape without exposing Flowable objects.
- There is currently no BPM module call site for `BpmNotificationListenerService.dispatch`, so a record table alone would not produce real traceability.

## Approaches Considered

### Approach A: Record-and-dispatch wrapper on new task creation

Add a BPM notification record table and service, invoke it from the new-task insertion path, and wrap channel dispatch so each channel gets a BPM-owned success/failure record.

Pros:
- Produces immediate operational value for the most important notification: a new pending task.
- Uses the existing Hunyuan task projection boundary, not Flowable internals.
- Avoids duplicate sends because `insertTaskIfMissing` only inserts absent tasks.
- Fits P2.1 trace cleanly by adding `notificationRecords`.

Cons:
- Covers only the first notification scenario in this slice.
- Callback retry and notification retry remain separate future concerns.

### Approach B: Full multi-event notification platform

Implement notifications for start, approve, reject, return, recall, cancel, copy, finish, SMS, mail, and retries in one slice.

Pros:
- Closer to the final enterprise target.

Cons:
- Too large for one safe increment.
- Mixes trigger design, channel delivery, retry policy, and UI monitoring into one change.
- Higher risk of duplicate sends and hidden side effects.

### Approach C: Build the full BPM event ledger first

Create a unified event ledger before notification records, then attach notifications to event rows.

Pros:
- Architecturally elegant once the full event model exists.

Cons:
- Delays the practical notification trace value.
- P2.1 already established a trace aggregation path without a full ledger.

## Recommendation

Use **Approach A**.

P2.2 should be the smallest enterprise-grade notification slice:

1. Add `t_bpm_notification_record`.
2. Add Hunyuan-native entity, DAO, query form, VO, and service.
3. Extend `BpmNotificationCommand` with BPM context: `instanceId`, `taskId`, `definitionNodeId`, `eventKey`, `receiverSnapshot`, and optional channel payload fields.
4. Dispatch notifications from `BpmTaskProjectionService` only when a new pending task is inserted.
5. Record one row per channel attempt.
6. Add notification records to the P2.1 instance trace response.
7. Add an admin monitoring list only if the backend record and trace contract are already green.

## Data Model

Create `t_bpm_notification_record` in the next SQL update log.

Required fields:

- `notification_record_id`: primary key.
- `instance_id`: workflow instance ID.
- `task_id`: Hunyuan BPM task ID, nullable for later instance-level events.
- `definition_id`: workflow definition ID.
- `definition_node_id`: Hunyuan definition node ID.
- `event_key`: first value is `TASK_CREATED`; later values can include `INSTANCE_STARTED`, `TASK_APPROVED`, `INSTANCE_FINISHED`, and copy events.
- `channel`: `MESSAGE`, `SMS`, or `MAIL`.
- `receiver_employee_id`: Hunyuan employee ID.
- `receiver_snapshot_json`: compact receiver snapshot with employee name, department name, phone, and mail when available.
- `template_code`: channel template code or BPM built-in code such as `bpm_task_created`.
- `title`: notification title.
- `content_snapshot`: rendered notification content.
- `send_status`: `0=PENDING`, `1=SUCCESS`, `2=FAIL`.
- `request_payload_json`: compact outbound request snapshot for BPM troubleshooting.
- `response_snapshot_json`: compact channel response snapshot when a channel returns a value.
- `fail_reason`: failure reason, truncated to a safe length.
- `sent_at`: completion time for success or failure.
- `create_time`, `update_time`: audit fields.

Indexes:

- `idx_instance_id(instance_id)`
- `idx_task_id(task_id)`
- `idx_event_key(event_key)`
- `idx_channel_status(channel, send_status)`

## Backend Components

### Notification domain

Add under `com.hunyuan.sa.bpm.module.runtime`:

- `BpmNotificationRecordEntity`
- `BpmNotificationRecordDao`
- `BpmNotificationRecordQueryForm`
- `BpmNotificationRecordVO`
- `BpmNotificationRecordService`
- `BpmNotificationSendStatusEnum`
- `BpmNotificationChannelEnum`

The service owns:

- `createPendingRecord(command, channel)`
- `markSuccess(recordId, responseSnapshotJson)`
- `markFail(recordId, failReason)`
- `queryByInstanceId(instanceId)`
- `queryPage(queryForm)`

### Dispatch wrapper

Keep `BpmNotificationListenerService` as the class that knows how to call base services, but make it record-aware:

1. Resolve `command.safeChannels()`.
2. For each channel, create a pending BPM notification record.
3. Call the existing channel service.
4. Mark the record success or failure.
5. Let notification failure be recorded, not silently swallowed.

For P2.2:

- `MESSAGE` should perform real base message delivery.
- `SMS` and `MAIL` can be record-capable and dispatch through the existing services, but no new SMS/mail platform behavior should be invented.
- No automatic retry executor is added in this slice.

### First trigger

Use `BpmTaskProjectionService.insertTaskIfMissing`.

When a new task is inserted:

1. Load the node listener config from `BpmDefinitionNodeEntity.compiledNodeSnapshotJson`.
2. Extract listeners with channel values.
3. If no listener is configured, skip notification.
4. If the task has no `assigneeEmployeeId`, skip notification and do not create a failed record.
5. Build one `BpmNotificationCommand` for the inserted task and assignee.
6. Dispatch through the record-aware listener service.

This trigger is intentionally narrow because it is idempotent at the Hunyuan task projection boundary.

## Trace Integration

Extend `BpmInstanceTraceVO` with:

```java
private List<BpmNotificationRecordVO> notificationRecords;
```

Extend `BpmInstanceTraceService#getTrace` to populate it through `BpmNotificationRecordService.queryByInstanceId(instanceId)`.

Frontend trace drawer adds a notification section under the existing reliability trace:

- Count summary: notification records count.
- Table columns: event, channel, receiver, status, sent time, fail reason.
- Runtime employee detail remains unchanged.

## Admin Monitoring

The existing integration monitoring pages cover callback and command records. P2.2 can add a separate notification record list only after backend trace is working.

Recommended first UI surface:

1. Instance trace drawer notification table.
2. Optional admin list at `/system/bpm/integration/notification-record-list` with filters:
   - `instanceId`
   - `taskId`
   - `eventKey`
   - `channel`
   - `sendStatus`
   - receiver employee name or ID

If implementation time needs to stay narrow, the admin list can be deferred while trace support ships.

## Error Handling

- Notification send failure must not roll back the BPM task insertion.
- A failed channel must mark its own record as `FAIL` with `failReason`.
- One channel failure must not prevent other configured channels from attempting delivery.
- Invalid listener channel values should be ignored and covered by definition publish validation in a later authoring hardening slice.
- Large request and response payloads must be compact snapshots, not full object dumps.

## Testing Strategy

Backend tests:

- `BpmNotificationRecordServiceTest`
  - creates pending records from a command.
  - marks success and failure.
  - queries records by `instanceId`.
- `BpmNotificationListenerServiceTest`
  - records success for `MESSAGE`.
  - records failure when a channel service throws.
  - continues other channels after one channel fails.
- `BpmTaskProjectionServiceTest`
  - dispatches notification when a new task is inserted and the node has a `MESSAGE` listener.
  - does not dispatch when task already exists.
  - does not dispatch when no assignee exists.
- `BpmInstanceTraceServiceTest`
  - includes `notificationRecords`.

Frontend tests:

- `bpm-api.test.ts`
  - pins notification record API and trace type.
- `bpm-modules.test.ts`
  - pins notification section in admin-only trace drawer.

Verification gates:

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm '-Dtest=BpmNotificationRecordServiceTest,BpmNotificationListenerServiceTest,BpmTaskProjectionServiceTest,BpmInstanceTraceServiceTest' test
```

```powershell
mvn -f E:/my-project/hunyuan-pro/hunyuan-backend/pom.xml -pl hunyuan-bpm test
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design exec vitest run apps/hunyuan-system/src/api/system/bpm/bpm-api.test.ts apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

```powershell
pnpm --dir E:/my-project/hunyuan-pro/hunyuan-design -F @hunyuan/system run typecheck
```

## Non-Goals

- No new general message center.
- No new queue dependency.
- No automatic notification retry executor.
- No full event ledger.
- No broad SMS/mail provider redesign.
- No reference-repo API path migration.
- No Flowable native object exposure.

## Acceptance Criteria

- New pending task with a configured `MESSAGE` listener creates a BPM notification record.
- Successful station-message delivery marks the record `SUCCESS`.
- Channel failure marks the record `FAIL` and stores a bounded failure reason.
- Notification failure does not roll back BPM task projection.
- Duplicate projection sync does not create duplicate notification records for the same existing task.
- Admin instance trace includes notification records.
- Runtime employee-side detail does not show notification failure internals.
- Backend tests, frontend source contract tests, typecheck, and Flowable boundary tests pass.

## Open Decision

The first implementation plan should choose whether the optional admin notification list ships in the same slice or waits until after trace support is green. The recommended default is to defer the list page and ship trace support first.
