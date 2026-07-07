# BPM Runtime Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first-phase Hunyuan BPM runtime closure so a published process can be started, projected into `t_bpm_task`, approved, rejected, returned, transferred, queried, and verified without leaking Flowable objects outside `hunyuan-bpm`.

**Architecture:** Keep Flowable as the hidden kernel behind internal gateways. Add a Hunyuan task projection coordinator that reads Flowable active tasks, writes `t_bpm_task`, updates `t_bpm_instance`, and leaves controllers/frontend consuming only Hunyuan VO/form/API contracts. Implement backend runtime correctness before employee-facing UI, then bind the UI to the stable contract.

**Tech Stack:** Java 17, Spring Boot 3.5.4, Flowable 7.2.0, MyBatis-Plus, JUnit 5, Mockito, AssertJ, Vue 3, TypeScript, Element Plus, `@vben/art-hooks`, Vitest, pnpm.

## Global Constraints

- Use `E:\my-project\hunyuan-pro\docs\superpowers\specs\2026-07-06-bpm-runtime-closure-design.md` as the product boundary.
- Phase 1 only implements the general approval runtime closure.
- Do not migrate Yudao/Ruoyi BPM wholesale; borrow mechanisms, not the whole system.
- Flowable objects must remain inside `hunyuan-bpm` gateways/services and must not appear in controller responses or frontend contracts.
- External contracts use Hunyuan names, Hunyuan IDs, Hunyuan employee/org integration, and Hunyuan page shell.
- Supported phase-1 task actions are approve, reject, return to initiator, and transfer.
- Supported phase-1 candidate resolver types remain `EMPLOYEE`, `DEPARTMENT_MANAGER`, and `ROLE`.
- Exclude add-sign, subtract-sign, countersign, OR-sign, print, report, complex expression platform, HTTP trigger, arbitrary listener execution, and a second frontend shell.
- Make one incremental change at a time.
- Prefer existing project patterns over new abstractions.
- Do not add new dependencies without explicit approval.
- Keep changes tightly scoped to the task.
- Verify every meaningful change with a concrete check.
- Use UTF-8-safe PowerShell before reading/writing Chinese docs or command output: `$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)`.

---

## Current Evidence

- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java` starts a Flowable process and inserts `t_bpm_instance`, but does not project active Flowable tasks into `t_bpm_task`.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java` updates `BpmTaskEntity`, but assumes a platform task already exists.
- `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/FlowableTaskGateway.java` only supports `complete(...)` and `transfer(...)`.
- Existing app endpoints already exist for `/app/bpm/startable`, `/app/bpm/start`, `/app/bpm/my-instance`, `/app/bpm/my-todo`, `/app/bpm/my-done`, and four task actions.
- Existing frontend API contract is `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`.
- Existing frontend BPM guard test is `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`.

## File Structure

- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/FlowableActiveTaskSnapshot.java`: internal immutable snapshot of an active Flowable task.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/FlowableTaskGateway.java`: add active-task query and process-end check; keep Flowable APIs hidden.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskProjectionService.java`: synchronize active Flowable tasks into Hunyuan task and instance projections.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`: call task projection after the instance row is inserted.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`: call projection after task completion and centralize instance result updates.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmTaskDao.java`: add exact query methods for `engineTaskId` and action-log detail support if wrapper-based queries become hard to read.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmInstanceDao.java`: keep page query and add detail query only if the detail VO needs joined fields.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceDetailVO.java`: instance detail projection for frontend drawer/page.
- Create `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmTaskActionLogVO.java`: action timeline projection.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmTaskMapper.xml`: add action log/detail list SQL if the DAO owns the query.
- Modify `hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmInstanceMapper.xml`: add detail SQL if needed.
- Modify app/admin controllers only to expose Hunyuan detail contracts; do not expose Flowable IDs beyond existing internal diagnostic fields already in Hunyuan entities.
- Modify `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`: add detail/action-log TypeScript contract after backend contract is stable.
- Create or modify employee runtime pages under `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/`: startable list, start form, my instance, my todo, my done, and detail drawer/page.
- Modify `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`: add contract checks for employee runtime pages and API functions.

### Task 1: Flowable Active Task Gateway

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/FlowableActiveTaskSnapshot.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/FlowableTaskGateway.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/FlowableTaskGatewayTest.java`

**Interfaces:**
- Consumes: Flowable `TaskService`
- Produces: `List<FlowableActiveTaskSnapshot> queryActiveTasksByProcessInstanceId(String engineProcessInstanceId)` and `boolean hasActiveTask(String engineProcessInstanceId)`

- [ ] **Step 1: Create the failing gateway test**

Create `FlowableTaskGatewayTest.java` with this content:

```java
package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.engine.internal.FlowableActiveTaskSnapshot;
import com.hunyuan.sa.bpm.engine.internal.FlowableTaskGateway;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class FlowableTaskGatewayTest {

    @Test
    void queryActiveTasksShouldReturnInternalSnapshotsOnly() {
        FlowableTaskGateway gateway = new FlowableTaskGateway();
        TaskService taskService = Mockito.mock(TaskService.class);
        TaskQuery taskQuery = Mockito.mock(TaskQuery.class);
        Task firstTask = Mockito.mock(Task.class);

        setField(gateway, "taskService", taskService);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processInstanceId("process-1")).thenReturn(taskQuery);
        when(taskQuery.active()).thenReturn(taskQuery);
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.asc()).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of(firstTask));
        when(firstTask.getId()).thenReturn("task-1");
        when(firstTask.getExecutionId()).thenReturn("execution-1");
        when(firstTask.getProcessInstanceId()).thenReturn("process-1");
        when(firstTask.getTaskDefinitionKey()).thenReturn("approve_1");
        when(firstTask.getName()).thenReturn("一级审批");
        when(firstTask.getAssignee()).thenReturn("22");

        List<FlowableActiveTaskSnapshot> snapshots = gateway.queryActiveTasksByProcessInstanceId("process-1");

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).engineTaskId()).isEqualTo("task-1");
        assertThat(snapshots.get(0).taskKey()).isEqualTo("approve_1");
        assertThat(snapshots.get(0).assigneeEmployeeId()).isEqualTo(22L);
    }

    private static void setField(Object target, String fieldName, Object value) {
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

- [ ] **Step 2: Run the test and verify it fails**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm -Dtest=FlowableTaskGatewayTest test
```

Expected: compile failure because `FlowableActiveTaskSnapshot` and `queryActiveTasksByProcessInstanceId(...)` do not exist.

- [ ] **Step 3: Add the internal snapshot record**

Create `FlowableActiveTaskSnapshot.java`:

```java
package com.hunyuan.sa.bpm.engine.internal;

/**
 * Flowable active task snapshot used only inside the BPM module.
 */
public record FlowableActiveTaskSnapshot(
        String engineTaskId,
        String engineExecutionId,
        String engineProcessInstanceId,
        String taskKey,
        String taskName,
        Long assigneeEmployeeId
) {
}
```

- [ ] **Step 4: Extend `FlowableTaskGateway` minimally**

Add imports:

```java
import org.flowable.task.api.Task;
import java.util.List;
```

Add methods:

```java
public List<FlowableActiveTaskSnapshot> queryActiveTasksByProcessInstanceId(String engineProcessInstanceId) {
    return taskService.createTaskQuery()
            .processInstanceId(engineProcessInstanceId)
            .active()
            .orderByTaskCreateTime()
            .asc()
            .list()
            .stream()
            .map(this::toSnapshot)
            .toList();
}

public boolean hasActiveTask(String engineProcessInstanceId) {
    return taskService.createTaskQuery()
            .processInstanceId(engineProcessInstanceId)
            .active()
            .count() > 0;
}

private FlowableActiveTaskSnapshot toSnapshot(Task task) {
    return new FlowableActiveTaskSnapshot(
            task.getId(),
            task.getExecutionId(),
            task.getProcessInstanceId(),
            task.getTaskDefinitionKey(),
            task.getName(),
            parseEmployeeId(task.getAssignee())
    );
}

private Long parseEmployeeId(String assignee) {
    if (assignee == null || assignee.isBlank()) {
        return null;
    }
    return Long.valueOf(assignee);
}
```

- [ ] **Step 5: Run the gateway test**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm -Dtest=FlowableTaskGatewayTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/FlowableActiveTaskSnapshot.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/engine/internal/FlowableTaskGateway.java `
        hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/FlowableTaskGatewayTest.java
git commit -m "feat: add bpm flowable active task gateway"
```

### Task 2: Task Projection Coordinator

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskProjectionService.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskProjectionServiceTest.java`

**Interfaces:**
- Consumes: `BpmInstanceEntity`, `FlowableTaskGateway.queryActiveTasksByProcessInstanceId(...)`, `BpmDefinitionNodeDao`, `BpmOrgIdentityGateway`
- Produces: `int syncActiveTasksForInstance(Long instanceId)` and inserts missing `BpmTaskEntity` rows

- [ ] **Step 1: Write the failing projection service test**

Create `BpmTaskProjectionServiceTest.java`:

```java
package com.hunyuan.sa.bpm.runtime;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableActiveTaskSnapshot;
import com.hunyuan.sa.bpm.engine.internal.FlowableTaskGateway;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmTaskProjectionServiceTest {

    private BpmTaskProjectionService service;
    private BpmInstanceDao bpmInstanceDao;
    private BpmTaskDao bpmTaskDao;
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;
    private FlowableTaskGateway flowableTaskGateway;
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    @BeforeEach
    void setUp() {
        service = new BpmTaskProjectionService();
        bpmInstanceDao = Mockito.mock(BpmInstanceDao.class);
        bpmTaskDao = Mockito.mock(BpmTaskDao.class);
        bpmDefinitionNodeDao = Mockito.mock(BpmDefinitionNodeDao.class);
        flowableTaskGateway = Mockito.mock(FlowableTaskGateway.class);
        bpmOrgIdentityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        setField(service, "bpmInstanceDao", bpmInstanceDao);
        setField(service, "bpmTaskDao", bpmTaskDao);
        setField(service, "bpmDefinitionNodeDao", bpmDefinitionNodeDao);
        setField(service, "flowableTaskGateway", flowableTaskGateway);
        setField(service, "bpmOrgIdentityGateway", bpmOrgIdentityGateway);
    }

    @Test
    void syncActiveTasksShouldInsertMissingTaskProjectionAndUpdateActiveCount() {
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(8L);
        instance.setDefinitionId(2L);
        instance.setEngineProcessInstanceId("process-1");
        instance.setInstanceNo("SN-2026-0001");
        instance.setTitle("请假申请");
        instance.setStartEmployeeId(100L);
        instance.setStartEmployeeNameSnapshot("张三");
        instance.setCategoryIdSnapshot(7L);
        instance.setCategoryNameSnapshot("人事流程");

        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(5L);
        node.setNodeKey("approve_1");

        when(bpmInstanceDao.selectById(8L)).thenReturn(instance);
        when(flowableTaskGateway.queryActiveTasksByProcessInstanceId("process-1")).thenReturn(List.of(
                new FlowableActiveTaskSnapshot("task-1", "execution-1", "process-1", "approve_1", "一级审批", 22L)
        ));
        when(bpmTaskDao.selectOne(any(Wrapper.class))).thenReturn(null);
        when(bpmDefinitionNodeDao.selectOne(any(Wrapper.class))).thenReturn(node);
        when(bpmOrgIdentityGateway.requireEmployee(22L)).thenReturn(new BpmEmployeeSnapshot(22L, "李四", 9L, "财务部", null, null));

        int activeCount = service.syncActiveTasksForInstance(8L);

        assertThat(activeCount).isEqualTo(1);
        ArgumentCaptor<BpmTaskEntity> taskCaptor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(bpmTaskDao).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getEngineTaskId()).isEqualTo("task-1");
        assertThat(taskCaptor.getValue().getTaskKey()).isEqualTo("approve_1");
        assertThat(taskCaptor.getValue().getTaskState()).isEqualTo(BpmTaskStateEnum.PENDING.getValue());
        assertThat(taskCaptor.getValue().getAssigneeEmployeeId()).isEqualTo(22L);
        assertThat(taskCaptor.getValue().getAssigneeNameSnapshot()).isEqualTo("李四");

        ArgumentCaptor<BpmInstanceEntity> instanceCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(bpmInstanceDao).updateById(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getInstanceId()).isEqualTo(8L);
        assertThat(instanceCaptor.getValue().getActiveTaskCount()).isEqualTo(1);
        assertThat(instanceCaptor.getValue().getCurrentNodeSummaryJson()).contains("approve_1");
    }

    private static void setField(Object target, String fieldName, Object value) {
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

- [ ] **Step 2: Run the test and verify it fails**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm -Dtest=BpmTaskProjectionServiceTest test
```

Expected: compile failure because `BpmTaskProjectionService` does not exist.

- [ ] **Step 3: Implement `BpmTaskProjectionService`**

Create `BpmTaskProjectionService.java`:

```java
package com.hunyuan.sa.bpm.module.runtime.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.exception.BusinessException;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableActiveTaskSnapshot;
import com.hunyuan.sa.bpm.engine.internal.FlowableTaskGateway;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Synchronizes Flowable active tasks into Hunyuan platform task projections.
 */
@Service
public class BpmTaskProjectionService {

    @Resource
    private BpmInstanceDao bpmInstanceDao;

    @Resource
    private BpmTaskDao bpmTaskDao;

    @Resource
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;

    @Resource
    private FlowableTaskGateway flowableTaskGateway;

    @Resource
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    @Transactional(rollbackFor = Exception.class)
    public int syncActiveTasksForInstance(Long instanceId) {
        BpmInstanceEntity instance = bpmInstanceDao.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException(UserErrorCode.DATA_NOT_EXIST);
        }
        List<FlowableActiveTaskSnapshot> activeTasks =
                flowableTaskGateway.queryActiveTasksByProcessInstanceId(instance.getEngineProcessInstanceId());
        for (FlowableActiveTaskSnapshot activeTask : activeTasks) {
            insertTaskIfMissing(instance, activeTask);
        }
        updateInstanceActiveTaskSummary(instance.getInstanceId(), activeTasks);
        return activeTasks.size();
    }

    private void insertTaskIfMissing(BpmInstanceEntity instance, FlowableActiveTaskSnapshot activeTask) {
        BpmTaskEntity existing = bpmTaskDao.selectOne(Wrappers.<BpmTaskEntity>lambdaQuery()
                .eq(BpmTaskEntity::getEngineTaskId, activeTask.engineTaskId()));
        if (existing != null) {
            return;
        }
        BpmDefinitionNodeEntity node = bpmDefinitionNodeDao.selectOne(Wrappers.<BpmDefinitionNodeEntity>lambdaQuery()
                .eq(BpmDefinitionNodeEntity::getDefinitionId, instance.getDefinitionId())
                .eq(BpmDefinitionNodeEntity::getNodeKey, activeTask.taskKey()));
        BpmEmployeeSnapshot assigneeSnapshot = activeTask.assigneeEmployeeId() == null
                ? null
                : bpmOrgIdentityGateway.requireEmployee(activeTask.assigneeEmployeeId());
        LocalDateTime now = LocalDateTime.now();

        BpmTaskEntity entity = new BpmTaskEntity();
        entity.setInstanceId(instance.getInstanceId());
        entity.setDefinitionId(instance.getDefinitionId());
        entity.setDefinitionNodeId(node == null ? null : node.getDefinitionNodeId());
        entity.setEngineTaskId(activeTask.engineTaskId());
        entity.setEngineExecutionId(activeTask.engineExecutionId());
        entity.setEngineProcessInstanceId(activeTask.engineProcessInstanceId());
        entity.setTaskKey(activeTask.taskKey());
        entity.setTaskName(activeTask.taskName());
        entity.setInstanceNo(instance.getInstanceNo());
        entity.setInstanceTitle(instance.getTitle());
        entity.setStartEmployeeId(instance.getStartEmployeeId());
        entity.setStartEmployeeNameSnapshot(instance.getStartEmployeeNameSnapshot());
        entity.setCategoryIdSnapshot(instance.getCategoryIdSnapshot());
        entity.setCategoryNameSnapshot(instance.getCategoryNameSnapshot());
        entity.setAssigneeEmployeeId(activeTask.assigneeEmployeeId());
        if (assigneeSnapshot != null) {
            entity.setAssigneeNameSnapshot(assigneeSnapshot.actualName());
            entity.setAssigneeDepartmentIdSnapshot(assigneeSnapshot.departmentId());
            entity.setAssigneeDepartmentNameSnapshot(assigneeSnapshot.departmentName());
            entity.setRuntimeAssignmentSnapshotJson("{\"assigneeEmployeeId\":" + assigneeSnapshot.employeeId() + "}");
        }
        entity.setTaskState(BpmTaskStateEnum.PENDING.getValue());
        entity.setAssignedAt(now);
        entity.setLastActionAt(now);
        bpmTaskDao.insert(entity);
    }

    private void updateInstanceActiveTaskSummary(Long instanceId, List<FlowableActiveTaskSnapshot> activeTasks) {
        BpmInstanceEntity updateEntity = new BpmInstanceEntity();
        updateEntity.setInstanceId(instanceId);
        updateEntity.setActiveTaskCount(activeTasks.size());
        updateEntity.setCurrentNodeSummaryJson(buildCurrentNodeSummaryJson(activeTasks));
        updateEntity.setLastActionAt(LocalDateTime.now());
        bpmInstanceDao.updateById(updateEntity);
    }

    private String buildCurrentNodeSummaryJson(List<FlowableActiveTaskSnapshot> activeTasks) {
        if (activeTasks.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < activeTasks.size(); index++) {
            FlowableActiveTaskSnapshot task = activeTasks.get(index);
            if (index > 0) {
                builder.append(',');
            }
            builder.append("{\"taskKey\":\"").append(task.taskKey())
                    .append("\",\"taskName\":\"").append(task.taskName())
                    .append("\",\"assigneeEmployeeId\":").append(task.assigneeEmployeeId())
                    .append('}');
        }
        builder.append(']');
        return builder.toString();
    }
}
```

- [ ] **Step 4: Run projection tests**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm -Dtest=BpmTaskProjectionServiceTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Run existing runtime command tests**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest test
```

Expected: existing tests still pass.

- [ ] **Step 6: Commit**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskProjectionService.java `
        hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmTaskProjectionServiceTest.java
git commit -m "feat: add bpm task projection coordinator"
```

### Task 3: Start Instance Projection Hook

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java`

**Interfaces:**
- Consumes: `BpmTaskProjectionService.syncActiveTasksForInstance(Long instanceId)`
- Produces: every successful `startInstance(...)` immediately syncs active Flowable tasks into Hunyuan task projections

- [ ] **Step 1: Extend the existing start-instance test**

In `BpmRuntimeCommandServiceTest`, add field setup:

```java
setField(bpmInstanceService, "bpmTaskProjectionService", Mockito.mock(BpmTaskProjectionService.class));
```

Add helper:

```java
@SuppressWarnings("unchecked")
private BpmTaskProjectionService taskProjectionService() {
    return (BpmTaskProjectionService) getFieldValue(bpmInstanceService, "bpmTaskProjectionService");
}
```

In `startInstanceShouldKeepInitialAndCurrentFormSnapshotsSeparated`, after verifying the insert, add:

```java
verify(taskProjectionService()).syncActiveTasksForInstance(8L);
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest#startInstanceShouldKeepInitialAndCurrentFormSnapshotsSeparated test
```

Expected: compile failure because `BpmInstanceService` has no `bpmTaskProjectionService` field, or verification failure because the service is not called.

- [ ] **Step 3: Wire projection into `BpmInstanceService`**

Add import:

```java
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
```

Add field:

```java
@Resource
private BpmTaskProjectionService bpmTaskProjectionService;
```

After `bpmInstanceDao.insert(entity);` and before `return ResponseDTO.ok(entity.getInstanceId());`, add:

```java
bpmTaskProjectionService.syncActiveTasksForInstance(entity.getInstanceId());
```

- [ ] **Step 4: Run the focused test**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest#startInstanceShouldKeepInitialAndCurrentFormSnapshotsSeparated test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Run all BPM runtime tests**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest,BpmTaskProjectionServiceTest,FlowableTaskGatewayTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java `
        hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java
git commit -m "feat: sync bpm task projection after start"
```

### Task 4: Approve and Reject Runtime State Closure

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java`

**Interfaces:**
- Consumes: `BpmTaskProjectionService.syncActiveTasksForInstance(Long instanceId)`
- Produces: approve creates next task projections or finishes instance as approved; reject finishes instance as rejected

- [ ] **Step 1: Add approve-completes-and-projects-next-task test**

In `BpmRuntimeCommandServiceTest`, inject projection service into `bpmTaskService`:

```java
setField(bpmTaskService, "bpmTaskProjectionService", Mockito.mock(BpmTaskProjectionService.class));
```

Add helper:

```java
@SuppressWarnings("unchecked")
private BpmTaskProjectionService taskServiceProjectionService() {
    return (BpmTaskProjectionService) getFieldValue(bpmTaskService, "bpmTaskProjectionService");
}
```

Add test:

```java
@Test
void approveShouldCompleteTaskAndSyncNextActiveTasks() {
    BpmTaskEntity taskEntity = new BpmTaskEntity();
    taskEntity.setTaskId(1L);
    taskEntity.setInstanceId(8L);
    taskEntity.setDefinitionId(2L);
    taskEntity.setDefinitionNodeId(5L);
    taskEntity.setEngineTaskId("task-1");
    taskEntity.setTaskState(BpmTaskStateEnum.PENDING.getValue());
    taskEntity.setAssigneeEmployeeId(10L);

    when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
    when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
    when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));
    when(taskServiceProjectionService().syncActiveTasksForInstance(8L)).thenReturn(1);

    BpmTaskApproveForm form = new BpmTaskApproveForm();
    form.setTaskId(1L);
    form.setCommentText("同意");

    ResponseDTO<String> response = bpmTaskService.approve(form);

    assertThat(response.getOk()).isTrue();
    verify(taskGateway()).complete("task-1");
    verify(taskServiceProjectionService()).syncActiveTasksForInstance(8L);

    ArgumentCaptor<BpmTaskEntity> taskCaptor = ArgumentCaptor.forClass(BpmTaskEntity.class);
    verify(bpmTaskDao).updateById(taskCaptor.capture());
    assertThat(taskCaptor.getValue().getTaskState()).isEqualTo(BpmTaskStateEnum.COMPLETED.getValue());
    assertThat(taskCaptor.getValue().getTaskResult()).isEqualTo(BpmTaskResultEnum.APPROVED.getValue());
}
```

- [ ] **Step 2: Add reject-finishes-instance test**

Add test:

```java
@Test
void rejectShouldFinishInstanceAsRejectedWhenNoActiveTaskRemains() {
    BpmTaskEntity taskEntity = new BpmTaskEntity();
    taskEntity.setTaskId(1L);
    taskEntity.setInstanceId(8L);
    taskEntity.setDefinitionId(2L);
    taskEntity.setDefinitionNodeId(5L);
    taskEntity.setEngineTaskId("task-1");
    taskEntity.setTaskState(BpmTaskStateEnum.PENDING.getValue());
    taskEntity.setAssigneeEmployeeId(10L);

    when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
    when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
    when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));
    when(taskServiceProjectionService().syncActiveTasksForInstance(8L)).thenReturn(0);

    BpmTaskRejectForm form = new BpmTaskRejectForm();
    form.setTaskId(1L);
    form.setCommentText("不同意");

    ResponseDTO<String> response = bpmTaskService.reject(form);

    assertThat(response.getOk()).isTrue();
    ArgumentCaptor<BpmInstanceEntity> instanceCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
    verify(bpmInstanceDao).updateById(instanceCaptor.capture());
    assertThat(instanceCaptor.getValue().getInstanceId()).isEqualTo(8L);
    assertThat(instanceCaptor.getValue().getRunState()).isEqualTo(BpmInstanceRunStateEnum.FINISHED.getValue());
    assertThat(instanceCaptor.getValue().getResultState()).isEqualTo(BpmInstanceResultStateEnum.REJECTED.getValue());
}
```

- [ ] **Step 3: Run tests and verify failure**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest#approveShouldCompleteTaskAndSyncNextActiveTasks,BpmRuntimeCommandServiceTest#rejectShouldFinishInstanceAsRejectedWhenNoActiveTaskRemains test
```

Expected: compile failure for missing import/field, then behavior failure until service is wired.

- [ ] **Step 4: Wire projection and final instance state**

In `BpmTaskService`, add imports:

```java
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceResultStateEnum;
```

Add field:

```java
@Resource
private BpmTaskProjectionService bpmTaskProjectionService;
```

In `completeTask(...)`, after `bpmTaskActionLogDao.insert(...)`, add:

```java
int activeTaskCount = bpmTaskProjectionService.syncActiveTasksForInstance(taskEntity.getInstanceId());
if (BpmTaskResultEnum.REJECTED.equals(resultEnum)) {
    finishInstance(taskEntity.getInstanceId(), BpmInstanceResultStateEnum.REJECTED);
} else if (activeTaskCount == 0) {
    finishInstance(taskEntity.getInstanceId(), BpmInstanceResultStateEnum.APPROVED);
}
```

Add private method:

```java
private void finishInstance(Long instanceId, BpmInstanceResultStateEnum resultStateEnum) {
    LocalDateTime now = LocalDateTime.now();
    BpmInstanceEntity updateInstanceEntity = new BpmInstanceEntity();
    updateInstanceEntity.setInstanceId(instanceId);
    updateInstanceEntity.setRunState(BpmInstanceRunStateEnum.FINISHED.getValue());
    updateInstanceEntity.setResultState(resultStateEnum.getValue());
    updateInstanceEntity.setActiveTaskCount(0);
    updateInstanceEntity.setCurrentNodeSummaryJson(null);
    updateInstanceEntity.setFinishedAt(now);
    updateInstanceEntity.setLastActionAt(now);
    bpmInstanceDao.updateById(updateInstanceEntity);
}
```

- [ ] **Step 5: Run focused runtime tests**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java `
        hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java
git commit -m "feat: close bpm approve reject runtime state"
```

### Task 5: Return and Transfer Runtime Consistency

**Files:**
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java`

**Interfaces:**
- Consumes: existing `returnToInitiator(...)` and `transfer(...)`
- Produces: return leaves the instance in `WAIT_RESUBMIT`; transfer keeps one pending task assigned to the new employee and logs the handoff

- [ ] **Step 1: Strengthen return test**

In `returnToInitiatorShouldMoveInstanceToWaitResubmitInsteadOfRejectingIt`, add:

```java
verify(taskGateway()).complete("task-1");
```

Also verify no finish result was written:

```java
assertThat(instanceCaptor.getValue().getResultState()).isNull();
assertThat(instanceCaptor.getValue().getActiveTaskCount()).isEqualTo(0);
```

- [ ] **Step 2: Strengthen transfer test**

In `transferShouldWriteActionLogAndReassignTask`, after existing assertions add:

```java
assertThat(taskCaptor.getValue().getTaskState()).isNull();
assertThat(taskCaptor.getValue().getTaskResult()).isNull();
assertThat(taskCaptor.getValue().getRuntimeAssignmentSnapshotJson()).contains("\"assigneeEmployeeId\":22");
```

This intentionally verifies transfer does not complete the task.

- [ ] **Step 3: Run focused tests**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest#returnToInitiatorShouldMoveInstanceToWaitResubmitInsteadOfRejectingIt,BpmRuntimeCommandServiceTest#transferShouldWriteActionLogAndReassignTask test
```

Expected: either `BUILD SUCCESS` if existing implementation already matches, or assertion failure exposing the exact drift.

- [ ] **Step 4: Apply minimal fixes only if tests fail**

If transfer overwrites task state/result, remove those assignments from `transfer(...)`.

If return writes a result state, remove that result assignment and keep:

```java
updateInstanceEntity.setRunState(BpmInstanceRunStateEnum.WAIT_RESUBMIT.getValue());
updateInstanceEntity.setActiveTaskCount(0);
updateInstanceEntity.setCurrentNodeSummaryJson(null);
```

- [ ] **Step 5: Run runtime test suite**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest,BpmTaskProjectionServiceTest,FlowableTaskGatewayTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmTaskService.java `
        hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java
git commit -m "test: lock bpm return transfer runtime semantics"
```

### Task 6: Instance Detail and Action Timeline API

**Files:**
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmTaskActionLogVO.java`
- Create: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceDetailVO.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmTaskActionLogDao.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmInstanceController.java`
- Modify: `hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmInstanceController.java`
- Test: `hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeDetailServiceTest.java`

**Interfaces:**
- Produces: `ResponseDTO<BpmInstanceDetailVO> getDetail(Long instanceId)`
- Produces endpoints: `GET /app/bpm/instance/detail/{instanceId}` and `GET /bpm/instance/detail/{instanceId}`

- [ ] **Step 1: Create VO classes**

Create `BpmTaskActionLogVO.java`:

```java
package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BpmTaskActionLogVO {

    @Schema(description = "动作日志ID")
    private Long actionLogId;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "节点ID")
    private Long definitionNodeId;

    @Schema(description = "动作类型")
    private String actionType;

    @Schema(description = "操作人员工ID")
    private Long actorEmployeeId;

    @Schema(description = "操作人姓名快照")
    private String actorNameSnapshot;

    @Schema(description = "原处理人")
    private Long fromAssigneeEmployeeId;

    @Schema(description = "新处理人")
    private Long toAssigneeEmployeeId;

    @Schema(description = "审批意见")
    private String commentText;

    @Schema(description = "动作时间")
    private LocalDateTime actionAt;
}
```

Create `BpmInstanceDetailVO.java`:

```java
package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BpmInstanceDetailVO {

    @Schema(description = "实例ID")
    private Long instanceId;

    @Schema(description = "实例编号")
    private String instanceNo;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "运行状态")
    private Integer runState;

    @Schema(description = "结果状态")
    private Integer resultState;

    @Schema(description = "发起人姓名快照")
    private String startEmployeeNameSnapshot;

    @Schema(description = "发起部门姓名快照")
    private String startDepartmentNameSnapshot;

    @Schema(description = "当前表单数据快照JSON")
    private String currentFormDataSnapshotJson;

    @Schema(description = "当前节点摘要JSON")
    private String currentNodeSummaryJson;

    @Schema(description = "发起时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    private LocalDateTime finishedAt;

    @Schema(description = "动作轨迹")
    private List<BpmTaskActionLogVO> actionLogs;
}
```

- [ ] **Step 2: Add failing detail service test**

Create `BpmRuntimeDetailServiceTest.java`:

```java
package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskActionLogVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class BpmRuntimeDetailServiceTest {

    @Test
    void getDetailShouldReturnInstanceAndActionLogs() {
        BpmInstanceService service = new BpmInstanceService();
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        BpmTaskActionLogDao actionLogDao = Mockito.mock(BpmTaskActionLogDao.class);
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "bpmTaskActionLogDao", actionLogDao);

        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(8L);
        instance.setInstanceNo("SN-2026-0001");
        instance.setTitle("请假申请");
        instance.setStartEmployeeNameSnapshot("张三");
        instance.setCurrentFormDataSnapshotJson("{\"days\":1}");

        BpmTaskActionLogVO log = new BpmTaskActionLogVO();
        log.setActionType("APPROVED");
        log.setActorNameSnapshot("李四");

        when(instanceDao.selectById(8L)).thenReturn(instance);
        when(actionLogDao.queryByInstanceId(8L)).thenReturn(List.of(log));

        ResponseDTO<BpmInstanceDetailVO> response = service.getDetail(8L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getInstanceNo()).isEqualTo("SN-2026-0001");
        assertThat(response.getData().getActionLogs()).hasSize(1);
        assertThat(response.getData().getActionLogs().get(0).getActionType()).isEqualTo("APPROVED");
    }

    private static void setField(Object target, String fieldName, Object value) {
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

- [ ] **Step 3: Run the test and verify it fails**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm -Dtest=BpmRuntimeDetailServiceTest test
```

Expected: compile failure for missing VO/query/service method.

- [ ] **Step 4: Add DAO query and service method**

Modify `BpmTaskActionLogDao.java`:

```java
List<BpmTaskActionLogVO> queryByInstanceId(@Param("instanceId") Long instanceId);
```

Add imports:

```java
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskActionLogVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;
```

Modify `BpmTaskActionLogMapper.xml`:

```xml
<select id="queryByInstanceId" resultType="com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskActionLogVO">
    select
        action_log_id as actionLogId,
        task_id as taskId,
        definition_node_id as definitionNodeId,
        action_type as actionType,
        actor_employee_id as actorEmployeeId,
        actor_name_snapshot as actorNameSnapshot,
        from_assignee_employee_id as fromAssigneeEmployeeId,
        to_assignee_employee_id as toAssigneeEmployeeId,
        comment_text as commentText,
        action_at as actionAt
    from t_bpm_task_action_log
    where instance_id = #{instanceId}
    order by action_at asc, action_log_id asc
</select>
```

In `BpmInstanceService`, add field:

```java
@Resource
private BpmTaskActionLogDao bpmTaskActionLogDao;
```

Add method:

```java
public ResponseDTO<BpmInstanceDetailVO> getDetail(Long instanceId) {
    BpmInstanceEntity instance = bpmInstanceDao.selectById(instanceId);
    if (instance == null) {
        return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
    }
    BpmInstanceDetailVO detail = new BpmInstanceDetailVO();
    detail.setInstanceId(instance.getInstanceId());
    detail.setInstanceNo(instance.getInstanceNo());
    detail.setTitle(instance.getTitle());
    detail.setSummary(instance.getSummary());
    detail.setRunState(instance.getRunState());
    detail.setResultState(instance.getResultState());
    detail.setStartEmployeeNameSnapshot(instance.getStartEmployeeNameSnapshot());
    detail.setStartDepartmentNameSnapshot(instance.getStartDepartmentNameSnapshot());
    detail.setCurrentFormDataSnapshotJson(instance.getCurrentFormDataSnapshotJson());
    detail.setCurrentNodeSummaryJson(instance.getCurrentNodeSummaryJson());
    detail.setStartedAt(instance.getStartedAt());
    detail.setFinishedAt(instance.getFinishedAt());
    detail.setActionLogs(bpmTaskActionLogDao.queryByInstanceId(instanceId));
    return ResponseDTO.ok(detail);
}
```

- [ ] **Step 5: Add controller endpoints**

In `AppBpmInstanceController`, add:

```java
@Operation(summary = "查询流程实例详情")
@GetMapping("/app/bpm/instance/detail/{instanceId}")
public ResponseDTO<BpmInstanceDetailVO> detail(@PathVariable Long instanceId) {
    return bpmInstanceService.getDetail(instanceId);
}
```

In `AdminBpmInstanceController`, add:

```java
@Operation(summary = "查询流程实例详情")
@GetMapping("/bpm/instance/detail/{instanceId}")
@SaCheckPermission("bpm:instance:detail")
public ResponseDTO<BpmInstanceDetailVO> detail(@PathVariable Long instanceId) {
    return bpmInstanceService.getDetail(instanceId);
}
```

Add required imports in each controller:

```java
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceDetailVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
```

- [ ] **Step 6: Run detail and runtime tests**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm -Dtest=BpmRuntimeDetailServiceTest,BpmRuntimeCommandServiceTest,BpmTaskProjectionServiceTest,FlowableTaskGatewayTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```powershell
git add hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmTaskActionLogVO.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo/BpmInstanceDetailVO.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/dao/BpmTaskActionLogDao.java `
        hunyuan-backend/hunyuan-bpm/src/main/resources/mapper/bpm/runtime/BpmTaskActionLogMapper.xml `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/service/BpmInstanceService.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/app/AppBpmInstanceController.java `
        hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmInstanceController.java `
        hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeDetailServiceTest.java
git commit -m "feat: add bpm instance detail timeline api"
```

### Task 7: Backend Runtime Verification Gate

**Files:**
- Modify tests only if compilation exposes missing imports or mapper registration gaps.

**Interfaces:**
- Produces: backend module-level confidence that runtime closure compiles and focused tests pass

- [ ] **Step 1: Run focused BPM tests**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm -Dtest=BpmRuntimeCommandServiceTest,BpmRuntimeDetailServiceTest,BpmTaskProjectionServiceTest,FlowableTaskGatewayTest,BpmApiIsolationTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Run all `hunyuan-bpm` tests**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run admin compatibility compile/test if admin adapters were touched**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit verification-only fixes if any were required**

```powershell
git status --short
git add hunyuan-backend/hunyuan-bpm/src/test/java/com/hunyuan/sa/bpm/runtime/BpmRuntimeCommandServiceTest.java
git commit -m "test: verify bpm runtime closure backend"
```

If verification fixes touched a different exact file, replace the `git add` path with the exact path shown by `git status --short`. If no files changed, skip the commit.

### Task 8: Frontend Runtime Contract and Employee Pages

**Files:**
- Read before editing list pages: `docs/frontend-list-table-page-standard.md`
- Read before editing detail/edit pages: `docs/frontend-edit-detail-page-standard.md`
- Modify: `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts`
- Modify: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/startable-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-instance-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-todo-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-done-list.vue`
- Create: `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue`

**Interfaces:**
- Consumes: `runtime.ts` API functions
- Produces: employee-side startable, my instance, my todo, my done, and detail timeline surfaces using Hunyuan shell

- [ ] **Step 1: Read frontend standards**

Run:

```powershell
Get-Content -LiteralPath 'E:\my-project\hunyuan-pro\docs\frontend-list-table-page-standard.md' -Encoding UTF8
Get-Content -LiteralPath 'E:\my-project\hunyuan-pro\docs\frontend-edit-detail-page-standard.md' -Encoding UTF8
```

Expected: standards are read before editing runtime pages.

- [ ] **Step 2: Add frontend API detail contract**

In `runtime.ts`, add:

```ts
export interface BpmTaskActionLogRecord {
  actionAt?: null | string;
  actionLogId: number;
  actionType: string;
  actorEmployeeId?: null | number;
  actorNameSnapshot?: null | string;
  commentText?: null | string;
  definitionNodeId?: null | number;
  fromAssigneeEmployeeId?: null | number;
  taskId?: null | number;
  toAssigneeEmployeeId?: null | number;
}

export interface BpmInstanceDetailRecord extends BpmInstanceRecord {
  actionLogs: BpmTaskActionLogRecord[];
  currentFormDataSnapshotJson?: null | string;
  currentNodeSummaryJson?: null | string;
  startDepartmentNameSnapshot?: null | string;
  summary?: null | string;
}

export async function getBpmInstanceDetail(instanceId: number) {
  return requestClient.get<BpmInstanceDetailRecord>(
    `/app/bpm/instance/detail/${instanceId}`,
  );
}
```

- [ ] **Step 3: Add failing frontend contract test**

In `bpm-modules.test.ts`, add paths:

```ts
const runtimeStartablePath =
  'apps/hunyuan-system/src/views/system/bpm/runtime/startable-list.vue';
const runtimeMyInstancePath =
  'apps/hunyuan-system/src/views/system/bpm/runtime/my-instance-list.vue';
const runtimeMyTodoPath =
  'apps/hunyuan-system/src/views/system/bpm/runtime/my-todo-list.vue';
const runtimeMyDonePath =
  'apps/hunyuan-system/src/views/system/bpm/runtime/my-done-list.vue';
const runtimeDetailDrawerPath =
  'apps/hunyuan-system/src/views/system/bpm/runtime/components/bpm-instance-detail-drawer.vue';
const runtimeApiPath = 'apps/hunyuan-system/src/api/system/bpm/runtime.ts';
```

Add test:

```ts
it('提供员工端 BPM 运行闭环页面和详情接口', () => {
  [
    runtimeStartablePath,
    runtimeMyInstancePath,
    runtimeMyTodoPath,
    runtimeMyDonePath,
    runtimeDetailDrawerPath,
  ].forEach((path) => {
    expect(existsSync(resolve(process.cwd(), path))).toBe(true);
  });

  const runtimeApiSource = readFileSync(resolve(process.cwd(), runtimeApiPath), 'utf8');
  expect(runtimeApiSource).toContain('/app/bpm/startable');
  expect(runtimeApiSource).toContain('/app/bpm/start');
  expect(runtimeApiSource).toContain('/app/bpm/my-instance');
  expect(runtimeApiSource).toContain('/app/bpm/my-todo');
  expect(runtimeApiSource).toContain('/app/bpm/my-done');
  expect(runtimeApiSource).toContain('/app/bpm/instance/detail/');
});
```

- [ ] **Step 4: Run frontend contract test and verify it fails**

Run:

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-design
pnpm exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
```

Expected: failure because runtime pages/detail drawer do not exist.

- [ ] **Step 5: Create list pages using existing BPM list patterns**

For each list page, copy the structural pattern from existing BPM list pages:

- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/instance/instance-list.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/task/task-list.vue`

Keep these rules:

- Use `ArtSearchPanel`, `ArtTablePanel`, `ArtTableHeader`, and `ArtTable`.
- Do not add standalone explanatory title copy.
- If there is only one natural search row, set `:collapsible="false"`.
- Use `runtime.ts` functions, not hard-coded request calls in components.
- Use the detail drawer component for instance and task row details.

- [ ] **Step 6: Create detail drawer**

Create `bpm-instance-detail-drawer.vue` with these minimum contract anchors:

```vue
<script setup lang="ts">
import type { BpmInstanceDetailRecord } from '#/api/system/bpm/runtime';

import { ref } from 'vue';

import { getBpmInstanceDetail } from '#/api/system/bpm/runtime';

const visible = ref(false);
const loading = ref(false);
const detail = ref<BpmInstanceDetailRecord>();

async function open(instanceId: number) {
  visible.value = true;
  loading.value = true;
  try {
    detail.value = await getBpmInstanceDetail(instanceId);
  } finally {
    loading.value = false;
  }
}

defineExpose({ open });
</script>

<template>
  <ElDrawer v-model="visible" title="流程详情" size="640px">
    <ElSkeleton v-if="loading" animated />
    <div v-else-if="detail" class="bpm-instance-detail">
      <ElDescriptions :column="1" border>
        <ElDescriptionsItem label="流程编号">
          {{ detail.instanceNo }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="标题">
          {{ detail.title }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="发起人">
          {{ detail.startEmployeeNameSnapshot || '-' }}
        </ElDescriptionsItem>
      </ElDescriptions>
      <ElTimeline class="bpm-instance-detail__timeline">
        <ElTimelineItem
          v-for="log in detail.actionLogs"
          :key="log.actionLogId"
          :timestamp="log.actionAt || ''"
        >
          <strong>{{ log.actorNameSnapshot || '-' }}</strong>
          <span>{{ log.actionType }}</span>
          <p v-if="log.commentText">{{ log.commentText }}</p>
        </ElTimelineItem>
      </ElTimeline>
    </div>
  </ElDrawer>
</template>
```

- [ ] **Step 7: Run frontend tests and typecheck**

Run:

```powershell
cd E:\my-project\hunyuan-pro
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: both commands pass.

- [ ] **Step 8: Commit frontend runtime pages**

```powershell
git add hunyuan-design/apps/hunyuan-system/src/api/system/bpm/runtime.ts `
        hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts `
        hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime
git commit -m "feat: add bpm employee runtime pages"
```

### Task 9: Final Runtime Closure Verification

**Files:**
- Modify only files needed to fix verification failures.

**Interfaces:**
- Produces: evidence that backend runtime closure and frontend contract pass together

- [ ] **Step 1: Run backend focused tests**

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-bpm test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Run admin compatibility test**

```powershell
cd E:\my-project\hunyuan-pro\hunyuan-backend
mvn -pl hunyuan-admin -Dtest=BpmFlowableCompatibilityTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run frontend contract and typecheck**

```powershell
cd E:\my-project\hunyuan-pro
pnpm --dir hunyuan-design exec vitest run apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts --dom
pnpm --dir hunyuan-design -F @hunyuan/system run typecheck
```

Expected: both commands pass.

- [ ] **Step 4: Check accidental Flowable API leakage**

Run:

```powershell
cd E:\my-project\hunyuan-pro
rg -n "org\\.flowable|Flowable" hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/controller hunyuan-backend/hunyuan-bpm/src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/vo hunyuan-design/apps/hunyuan-system/src/api/system/bpm
```

Expected: no matches in controllers, public VOs, or frontend API files except human-readable comments that do not affect contracts. If a match appears in a public contract, move that data behind a Hunyuan field name or remove it.

- [ ] **Step 5: Final status evidence**

Run:

```powershell
cd E:\my-project\hunyuan-pro
git status --short
git log --oneline -10
```

Expected: only intentional runtime closure changes remain uncommitted, or the task commits from this plan appear in the last ten commits.

## Self-Review

- Spec coverage: this plan implements task projection, start hook, approve/reject closure, return/transfer semantics, detail/action log API, employee runtime frontend, and backend/frontend verification.
- Non-goal coverage: this plan does not add add-sign, subtract-sign, countersign, OR-sign, print, report, complex expression platform, HTTP trigger, arbitrary listener execution, or a second frontend shell.
- Flowable boundary: Flowable is read only through `FlowableTaskGateway` and snapshots; controllers, VOs, and frontend APIs remain Hunyuan contracts.
- Type consistency: produced backend method names are `queryActiveTasksByProcessInstanceId`, `hasActiveTask`, `syncActiveTasksForInstance`, and `getDetail`; frontend method name is `getBpmInstanceDetail`.
- Placeholder scan: no task relies on unspecified implementation names; conditional verification-only fixes are explicitly bounded by failing assertions.
