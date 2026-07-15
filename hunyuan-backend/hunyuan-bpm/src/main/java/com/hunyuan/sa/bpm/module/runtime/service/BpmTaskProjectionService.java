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
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalMemberState;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageMemberDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageMemberEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
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
    private BpmApprovalStageMemberDao bpmApprovalStageMemberDao;

    @Resource
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;

    @Resource
    private BpmGraphRuntimeMetadataService bpmGraphRuntimeMetadataService;

    @Resource
    private FlowableTaskGateway flowableTaskGateway;

    @Resource
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    @Resource
    private BpmNotificationListenerService bpmNotificationListenerService;

    @Resource
    private BpmApprovalGroupService bpmApprovalGroupService;

    @Resource
    private BpmTimeEventService bpmTimeEventService;

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

    /**
     * 仅消费已冻结的审批阶段成员，不查询或完成任何 Flowable user task。
     */
    @Transactional(rollbackFor = Exception.class)
    public int projectActiveApprovalStageMembers(BpmApprovalStageEntity stage) {
        if (stage == null || stage.getApprovalStageId() == null || stage.getInstanceId() == null
                || stage.getEngineProcessInstanceId() == null || stage.getEngineExecutionId() == null) {
            throw new IllegalArgumentException("审批阶段任务投影参数不完整");
        }
        BpmInstanceEntity instance = bpmInstanceDao.selectById(stage.getInstanceId());
        if (instance == null) {
            throw new BusinessException(UserErrorCode.DATA_NOT_EXIST);
        }
        int created = 0;
        for (BpmApprovalStageMemberEntity member : bpmApprovalStageMemberDao
                .selectByApprovalStageId(stage.getApprovalStageId())) {
            if (!ApprovalMemberState.ACTIVE.name().equals(member.getMemberState())) {
                continue;
            }
            BpmTaskEntity existing = bpmTaskDao.selectByApprovalStageMemberId(member.getApprovalStageMemberId());
            if (existing != null) {
                linkMemberTaskIfNeeded(member, existing.getTaskId());
                continue;
            }
            BpmEmployeeSnapshot assignee = bpmOrgIdentityGateway.requireEmployee(member.getCurrentEmployeeId());
            BpmTaskEntity task = buildApprovalStageMemberTask(instance, stage, member, assignee);
            try {
                if (bpmTaskDao.insert(task) != 1 || task.getTaskId() == null) {
                    throw new IllegalStateException("审批阶段成员任务投影创建失败");
                }
                linkMemberTaskIfNeeded(member, task.getTaskId());
                created++;
            } catch (DuplicateKeyException ex) {
                BpmTaskEntity concurrentTask = bpmTaskDao.selectByApprovalStageMemberId(member.getApprovalStageMemberId());
                if (concurrentTask == null || concurrentTask.getTaskId() == null) {
                    throw ex;
                }
                linkMemberTaskIfNeeded(member, concurrentTask.getTaskId());
            }
        }
        return created;
    }

    private BpmTaskEntity buildApprovalStageMemberTask(
            BpmInstanceEntity instance,
            BpmApprovalStageEntity stage,
            BpmApprovalStageMemberEntity member,
            BpmEmployeeSnapshot assignee
    ) {
        LocalDateTime now = LocalDateTime.now();
        BpmTaskEntity task = new BpmTaskEntity();
        task.setInstanceId(instance.getInstanceId());
        task.setDefinitionId(instance.getDefinitionId());
        task.setGraphDefinitionVersionId(instance.getGraphDefinitionVersionId());
        task.setDefinitionSource(instance.getDefinitionSource());
        task.setApprovalStageId(stage.getApprovalStageId());
        task.setApprovalStageMemberId(member.getApprovalStageMemberId());
        task.setEngineExecutionId(stage.getEngineExecutionId());
        task.setEngineProcessInstanceId(stage.getEngineProcessInstanceId());
        task.setTaskKey(stage.getAuthoredNodeId());
        task.setTaskName("GRAPH".equals(instance.getDefinitionSource())
                ? bpmGraphRuntimeMetadataService.requireNode(
                        instance.getGraphDefinitionVersionId(), stage.getAuthoredNodeId()).nodeName()
                : "审批：" + stage.getAuthoredNodeId());
        task.setInstanceNo(instance.getInstanceNo());
        task.setInstanceTitle(instance.getTitle());
        task.setStartEmployeeId(instance.getStartEmployeeId());
        task.setStartEmployeeNameSnapshot(instance.getStartEmployeeNameSnapshot());
        task.setCategoryIdSnapshot(instance.getCategoryIdSnapshot());
        task.setCategoryNameSnapshot(instance.getCategoryNameSnapshot());
        task.setAssigneeEmployeeId(member.getCurrentEmployeeId());
        fillAssigneeSnapshot(task, assignee);
        task.setTaskState(BpmTaskStateEnum.PENDING.getValue());
        task.setTaskVersion(1L);
        task.setAssignedAt(now);
        task.setLastActionAt(now);
        return task;
    }

    private void linkMemberTaskIfNeeded(BpmApprovalStageMemberEntity member, Long taskId) {
        if (taskId == null || taskId.equals(member.getTaskId())) {
            return;
        }
        BpmApprovalStageMemberEntity update = new BpmApprovalStageMemberEntity();
        update.setApprovalStageMemberId(member.getApprovalStageMemberId());
        update.setTaskId(taskId);
        bpmApprovalStageMemberDao.updateById(update);
    }

    private void insertTaskIfMissing(BpmInstanceEntity instance, FlowableActiveTaskSnapshot activeTask) {
        BpmTaskEntity existing = bpmTaskDao.selectOne(Wrappers.<BpmTaskEntity>lambdaQuery()
                .eq(BpmTaskEntity::getEngineTaskId, activeTask.engineTaskId()));
        BpmDefinitionNodeEntity node = "GRAPH".equals(instance.getDefinitionSource()) ? null
                : bpmDefinitionNodeDao.selectOne(Wrappers.<BpmDefinitionNodeEntity>lambdaQuery()
                .eq(BpmDefinitionNodeEntity::getDefinitionId, instance.getDefinitionId())
                .eq(BpmDefinitionNodeEntity::getNodeKey, activeTask.taskKey()));
        BpmGraphRuntimeMetadataService.GraphNodeMetadata graphNode =
                "GRAPH".equals(instance.getDefinitionSource()) && bpmGraphRuntimeMetadataService != null
                        ? bpmGraphRuntimeMetadataService.resolveCompiledNode(
                                instance.getGraphDefinitionVersionId(), activeTask.taskKey())
                        : null;
        if (existing != null) {
            attachApprovalGroupIfMissing(instance, node, existing);
            scheduleTaskSla(instance, node, existing);
            return;
        }
        BpmEmployeeSnapshot assigneeSnapshot = activeTask.assigneeEmployeeId() == null
                ? null
                : bpmOrgIdentityGateway.requireEmployee(activeTask.assigneeEmployeeId());
        LocalDateTime now = LocalDateTime.now();

        BpmTaskEntity task = new BpmTaskEntity();
        task.setInstanceId(instance.getInstanceId());
        task.setDefinitionId(instance.getDefinitionId());
        task.setGraphDefinitionVersionId(instance.getGraphDefinitionVersionId());
        task.setDefinitionSource(instance.getDefinitionSource());
        task.setDefinitionNodeId(node == null ? null : node.getDefinitionNodeId());
        task.setEngineTaskId(activeTask.engineTaskId());
        task.setEngineExecutionId(activeTask.engineExecutionId());
        task.setEngineProcessInstanceId(activeTask.engineProcessInstanceId());
        task.setTaskKey(graphNode == null ? activeTask.taskKey() : graphNode.authoredNodeId());
        task.setTaskName(graphNode == null ? activeTask.taskName() : graphNode.nodeName());
        task.setInstanceNo(instance.getInstanceNo());
        task.setInstanceTitle(instance.getTitle());
        task.setStartEmployeeId(instance.getStartEmployeeId());
        task.setStartEmployeeNameSnapshot(instance.getStartEmployeeNameSnapshot());
        task.setCategoryIdSnapshot(instance.getCategoryIdSnapshot());
        task.setCategoryNameSnapshot(instance.getCategoryNameSnapshot());
        task.setAssigneeEmployeeId(activeTask.assigneeEmployeeId());
        fillAssigneeSnapshot(task, assigneeSnapshot);
        task.setTaskState(BpmTaskStateEnum.PENDING.getValue());
        task.setTaskVersion(1L);
        task.setAssignedAt(now);
        task.setLastActionAt(now);
        if (isApprovalGroupNode(node)) {
            task.setApprovalGroupId(bpmApprovalGroupService.assignApprovalGroup(instance, node, task));
        }
        bpmTaskDao.insert(task);
        scheduleTaskSla(instance, node, task);
        dispatchTaskCreatedNotification(instance, task, node, assigneeSnapshot);
    }

    private void scheduleTaskSla(
            BpmInstanceEntity instance,
            BpmDefinitionNodeEntity node,
            BpmTaskEntity task
    ) {
        if (bpmTimeEventService != null) {
            bpmTimeEventService.scheduleTaskSla(instance, node, task);
        }
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

    private void attachApprovalGroupIfMissing(
            BpmInstanceEntity instance,
            BpmDefinitionNodeEntity node,
            BpmTaskEntity task
    ) {
        if (task.getApprovalGroupId() != null || !isApprovalGroupNode(node)) {
            return;
        }
        Long groupId = bpmApprovalGroupService.assignApprovalGroup(instance, node, task);
        if (groupId == null || task.getApprovalGroupId() != null) {
            return;
        }
        BpmTaskEntity updateTask = new BpmTaskEntity();
        updateTask.setTaskId(task.getTaskId());
        updateTask.setApprovalGroupId(groupId);
        bpmTaskDao.updateById(updateTask);
    }

    private boolean isApprovalGroupNode(BpmDefinitionNodeEntity node) {
        if (node == null || node.getCompiledNodeSnapshotJson() == null) {
            return false;
        }
        try {
            JSONObject snapshot = JSON.parseObject(node.getCompiledNodeSnapshotJson());
            String mode = snapshot.getString("approvalMode");
            Integer memberIndex = "parallelAll".equals(mode)
                    ? snapshot.getInteger("parallelIndex")
                    : snapshot.getInteger("sequentialIndex");
            Integer memberTotal = "parallelAll".equals(mode)
                    ? snapshot.getInteger("parallelTotal")
                    : snapshot.getInteger("sequentialTotal");
            return ("parallelAll".equals(mode) || "sequential".equals(mode))
                    && snapshot.getString("approvalGroupKey") != null
                    && snapshot.getString("approvalGroupName") != null
                    && memberIndex != null
                    && memberTotal != null
                    && memberIndex >= 1
                    && memberIndex <= memberTotal
                    && memberTotal >= 2;
        } catch (Exception ex) {
            return false;
        }
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
        TaskCreatedNotification notification = buildTaskCreatedNotification(instance, node);

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
                notification.title(),
                notification.title(),
                notification.content(),
                "bpm_task_created"
        );
        bpmNotificationListenerService.dispatch(command);
    }

    private TaskCreatedNotification buildTaskCreatedNotification(
            BpmInstanceEntity instance,
            BpmDefinitionNodeEntity node
    ) {
        String ordinaryContent = "你有一个新的流程待办：" + instance.getTitle();
        if (node == null || node.getCompiledNodeSnapshotJson() == null) {
            return new TaskCreatedNotification("流程待办提醒", ordinaryContent);
        }
        try {
            // 通知只读取冻结的编译快照，避免向用户暴露 Flowable 内部任务标识。
            JSONObject snapshot = JSON.parseObject(node.getCompiledNodeSnapshotJson());
            String groupName = snapshot.getString("approvalGroupName");
            Integer parallelIndex = snapshot.getInteger("parallelIndex");
            Integer parallelTotal = snapshot.getInteger("parallelTotal");
            if (!"parallelAll".equals(snapshot.getString("approvalMode"))
                    || groupName == null
                    || groupName.isBlank()
                    || parallelIndex == null
                    || parallelTotal == null
                    || parallelIndex < 1
                    || parallelIndex > parallelTotal
                    || parallelTotal < 2) {
                return new TaskCreatedNotification("流程待办提醒", ordinaryContent);
            }
            return new TaskCreatedNotification(
                    "流程会签待办提醒",
                    ordinaryContent + "；审批组：" + groupName + "，成员 " + parallelIndex + "/" + parallelTotal
            );
        } catch (Exception ex) {
            return new TaskCreatedNotification("流程待办提醒", ordinaryContent);
        }
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

    private record TaskCreatedNotification(String title, String content) {
    }
}
