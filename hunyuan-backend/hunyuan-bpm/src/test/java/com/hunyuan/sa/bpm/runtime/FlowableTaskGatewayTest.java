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
