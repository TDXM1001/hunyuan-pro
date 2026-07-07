package com.hunyuan.sa.bpm.engine.internal;

import jakarta.annotation.Resource;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Flowable 任务运行网关，仅在 BPM 模块内部使用。
 */
@Component
public class FlowableTaskGateway {

    @Resource
    private TaskService taskService;

    /**
     * 完成指定任务。
     */
    public void complete(String engineTaskId) {
        taskService.complete(engineTaskId);
    }

    /**
     * 转办任务到新的员工。
     */
    public void transfer(String engineTaskId, Long toEmployeeId) {
        taskService.setAssignee(engineTaskId, String.valueOf(toEmployeeId));
    }

    /**
     * 查询指定流程实例下的活动任务，并转换成模块内部快照。
     */
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

    /**
     * 判断流程实例下是否仍有活动任务。
     */
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
}
