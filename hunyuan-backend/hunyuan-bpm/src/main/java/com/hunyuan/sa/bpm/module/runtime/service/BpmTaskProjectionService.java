package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.exception.BusinessException;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmNotificationChannelEnum;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 将 Flowable 当前活动任务同步为 Hunyuan 平台任务投影。
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

    @Resource
    private BpmNotificationListenerService bpmNotificationListenerService;

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

        BpmTaskEntity task = new BpmTaskEntity();
        task.setInstanceId(instance.getInstanceId());
        task.setDefinitionId(instance.getDefinitionId());
        task.setDefinitionNodeId(node == null ? null : node.getDefinitionNodeId());
        task.setEngineTaskId(activeTask.engineTaskId());
        task.setEngineExecutionId(activeTask.engineExecutionId());
        task.setEngineProcessInstanceId(activeTask.engineProcessInstanceId());
        task.setTaskKey(activeTask.taskKey());
        task.setTaskName(activeTask.taskName());
        task.setInstanceNo(instance.getInstanceNo());
        task.setInstanceTitle(instance.getTitle());
        task.setStartEmployeeId(instance.getStartEmployeeId());
        task.setStartEmployeeNameSnapshot(instance.getStartEmployeeNameSnapshot());
        task.setCategoryIdSnapshot(instance.getCategoryIdSnapshot());
        task.setCategoryNameSnapshot(instance.getCategoryNameSnapshot());
        task.setAssigneeEmployeeId(activeTask.assigneeEmployeeId());
        fillAssigneeSnapshot(task, assigneeSnapshot);
        task.setTaskState(BpmTaskStateEnum.PENDING.getValue());
        task.setAssignedAt(now);
        task.setLastActionAt(now);
        bpmTaskDao.insert(task);
        dispatchTaskCreatedNotification(instance, task, node, assigneeSnapshot);
    }

    private void fillAssigneeSnapshot(BpmTaskEntity task, BpmEmployeeSnapshot assigneeSnapshot) {
        if (assigneeSnapshot == null) {
            return;
        }
        task.setAssigneeNameSnapshot(assigneeSnapshot.actualName());
        task.setAssigneeDepartmentIdSnapshot(assigneeSnapshot.departmentId());
        task.setAssigneeDepartmentNameSnapshot(assigneeSnapshot.departmentName());
        JSONObject assignment = new JSONObject();
        assignment.put("assigneeEmployeeId", assigneeSnapshot.employeeId());
        task.setRuntimeAssignmentSnapshotJson(assignment.toJSONString());
    }

    private void dispatchTaskCreatedNotification(
            BpmInstanceEntity instance,
            BpmTaskEntity task,
            BpmDefinitionNodeEntity node,
            BpmEmployeeSnapshot assigneeSnapshot
    ) {
        if (assigneeSnapshot == null) {
            return;
        }
        List<String> channels = parseNotificationChannels(node);
        if (channels.isEmpty()) {
            return;
        }

        BpmNotificationCommand command = new BpmNotificationCommand(
                channels,
                instance.getInstanceId(),
                task.getTaskId(),
                instance.getDefinitionId(),
                task.getDefinitionNodeId(),
                "TASK_CREATED",
                task.getAssigneeEmployeeId(),
                buildReceiverSnapshotJson(assigneeSnapshot),
                assigneeSnapshot.phone(),
                assigneeSnapshot.email() == null ? List.of() : List.of(assigneeSnapshot.email()),
                "流程待办提醒",
                "流程待办提醒",
                "你有一个新的流程待办：" + task.getInstanceTitle(),
                "bpm_task_created"
        );
        bpmNotificationListenerService.dispatch(command);
    }

    private List<String> parseNotificationChannels(BpmDefinitionNodeEntity node) {
        if (node == null || node.getCompiledNodeSnapshotJson() == null) {
            return List.of();
        }
        try {
            JSONObject nodeSnapshot = JSON.parseObject(node.getCompiledNodeSnapshotJson());
            JSONArray listeners = nodeSnapshot.getJSONArray("listeners");
            if (listeners == null || listeners.isEmpty()) {
                return List.of();
            }
            List<String> channels = new ArrayList<>();
            for (Object listener : listeners) {
                if (!(listener instanceof JSONObject listenerObject)) {
                    continue;
                }
                String channel = listenerObject.getString("channel");
                if (isSupportedChannel(channel) && !channels.contains(channel)) {
                    channels.add(channel);
                }
            }
            return channels;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private boolean isSupportedChannel(String channel) {
        if (channel == null) {
            return false;
        }
        for (BpmNotificationChannelEnum channelEnum : BpmNotificationChannelEnum.values()) {
            if (channelEnum.name().equals(channel)) {
                return true;
            }
        }
        return false;
    }

    private String buildReceiverSnapshotJson(BpmEmployeeSnapshot assigneeSnapshot) {
        JSONObject snapshot = new JSONObject();
        snapshot.put("employeeId", assigneeSnapshot.employeeId());
        snapshot.put("actualName", assigneeSnapshot.actualName());
        snapshot.put("departmentId", assigneeSnapshot.departmentId());
        snapshot.put("departmentName", assigneeSnapshot.departmentName());
        snapshot.put("phone", assigneeSnapshot.phone());
        snapshot.put("email", assigneeSnapshot.email());
        return snapshot.toJSONString();
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
        JSONArray nodeSummaryArray = new JSONArray();
        for (FlowableActiveTaskSnapshot activeTask : activeTasks) {
            JSONObject nodeSummary = new JSONObject();
            nodeSummary.put("taskKey", activeTask.taskKey());
            nodeSummary.put("taskName", activeTask.taskName());
            nodeSummary.put("assigneeEmployeeId", activeTask.assigneeEmployeeId());
            nodeSummaryArray.add(nodeSummary);
        }
        return nodeSummaryArray.toJSONString();
    }
}
