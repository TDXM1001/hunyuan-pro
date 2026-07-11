package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.common.enumeration.BpmApprovalGroupCloseReasonEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmApprovalGroupStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceResultStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskResultEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.engine.internal.FlowableTaskGateway;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalGroupDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalGroupEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmApprovalGroupDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmApprovalGroupMemberVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmApprovalGroupSummaryVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskActionLogVO;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * 多人审批组运行时服务。
 */
@Service
public class BpmApprovalGroupService {

    private static final Logger LOGGER =
            Logger.getLogger(BpmApprovalGroupService.class.getName());

    @Resource
    private BpmApprovalGroupDao bpmApprovalGroupDao;

    @Resource
    private BpmTaskDao bpmTaskDao;

    @Resource
    private BpmInstanceDao bpmInstanceDao;

    @Resource
    private BpmTaskActionLogDao bpmTaskActionLogDao;

    @Resource
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;

    @Resource
    private FlowableTaskGateway flowableTaskGateway;

    @Resource
    private FlowableProcessInstanceGateway flowableProcessInstanceGateway;

    @Transactional(rollbackFor = Exception.class)
    public Long assignApprovalGroup(
            BpmInstanceEntity instance,
            BpmDefinitionNodeEntity definitionNode,
            BpmTaskEntity memberTask
    ) {
        ApprovalGroupNodeSnapshot snapshot = parseApprovalGroupSnapshot(definitionNode);
        if (snapshot == null) {
            return null;
        }
        BpmApprovalGroupEntity existing = bpmApprovalGroupDao.selectByEngineProcessInstanceIdAndGroupKey(
                instance.getEngineProcessInstanceId(),
                snapshot.groupKey()
        );
        if (existing != null) {
            verifyGroupRuntimeFacts(existing, instance, snapshot);
            if (snapshot.isSequential()) {
                restoreSequentialGroupMembers(instance, snapshot, existing.getApprovalGroupId(), memberTask);
            }
            return existing.getApprovalGroupId();
        }

        BpmApprovalGroupEntity group = new BpmApprovalGroupEntity();
        group.setInstanceId(instance.getInstanceId());
        group.setDefinitionId(instance.getDefinitionId());
        group.setEngineProcessInstanceId(instance.getEngineProcessInstanceId());
        group.setApprovalGroupKey(snapshot.groupKey());
        group.setApprovalGroupName(snapshot.groupName());
        group.setApprovalMode(snapshot.approvalMode());
        group.setGroupState(BpmApprovalGroupStateEnum.PENDING.name());
        group.setTotalMemberCount(snapshot.memberTotal());
        group.setProcessedMemberCount(0);
        group.setApprovedMemberCount(0);
        group.setRejectedMemberCount(0);
        try {
            bpmApprovalGroupDao.insert(group);
            if (snapshot.isSequential()) {
                restoreSequentialGroupMembers(instance, snapshot, group.getApprovalGroupId(), memberTask);
            }
            return group.getApprovalGroupId();
        } catch (DuplicateKeyException ex) {
            // 并行分支可能同时投影，唯一键冲突后读取已经创建的审批组。
            BpmApprovalGroupEntity concurrentGroup =
                    bpmApprovalGroupDao.selectByEngineProcessInstanceIdAndGroupKey(
                            instance.getEngineProcessInstanceId(),
                            snapshot.groupKey()
                    );
            if (concurrentGroup == null) {
                throw ex;
            }
            verifyGroupRuntimeFacts(concurrentGroup, instance, snapshot);
            if (snapshot.isSequential()) {
                restoreSequentialGroupMembers(instance, snapshot, concurrentGroup.getApprovalGroupId(), memberTask);
            }
            return concurrentGroup.getApprovalGroupId();
        }
    }

    /**
     * 判断审批组是否仍使用并行全员会签语义。
     */
    public boolean isParallelAllGroup(Long approvalGroupId) {
        if (approvalGroupId == null) {
            return false;
        }
        BpmApprovalGroupEntity group = bpmApprovalGroupDao.selectById(approvalGroupId);
        return group != null && "parallelAll".equals(group.getApprovalMode());
    }

    @Transactional(rollbackFor = Exception.class)
    public BpmApprovalGroupActionResult handleMemberAction(
            Long taskId,
            BpmApprovalMemberAction action,
            BpmEmployeeSnapshot actor,
            String commentText
    ) {
        BpmTaskEntity taskLookup = bpmTaskDao.selectById(taskId);
        if (taskLookup == null || taskLookup.getApprovalGroupId() == null) {
            return BpmApprovalGroupActionResult.forOrdinaryTask();
        }

        // 只用普通查询取得组 ID，真正改变状态时严格先锁审批组、再锁任务。
        BpmApprovalGroupEntity group = bpmApprovalGroupDao.selectByIdForUpdate(taskLookup.getApprovalGroupId());
        if (group == null) {
            throw new IllegalArgumentException("并行审批组不存在");
        }
        BpmApprovalGroupStateEnum currentGroupState = parseGroupState(group.getGroupState());
        if (currentGroupState != BpmApprovalGroupStateEnum.PENDING) {
            return BpmApprovalGroupActionResult.ignored(group.getApprovalGroupId(), currentGroupState);
        }

        BpmTaskEntity currentTask = bpmTaskDao.selectByIdForUpdate(taskId);
        if (currentTask == null || !BpmTaskStateEnum.PENDING.equalsValue(currentTask.getTaskState())) {
            return BpmApprovalGroupActionResult.ignored(group.getApprovalGroupId(), currentGroupState);
        }
        if (actor == null || !Objects.equals(currentTask.getAssigneeEmployeeId(), actor.employeeId())) {
            throw new IllegalArgumentException("只能处理自己的待办任务");
        }
        List<BpmTaskEntity> pendingTasks =
                bpmTaskDao.selectPendingByApprovalGroupIdForUpdate(group.getApprovalGroupId());
        LocalDateTime now = LocalDateTime.now();
        if ("parallelAll".equals(group.getApprovalMode())) {
            return handleParallelMemberAction(
                    group, currentTask, pendingTasks, action, actor, commentText, now);
        }
        if ("sequential".equals(group.getApprovalMode())) {
            return handleSequentialMemberAction(group, currentTask, action, actor, commentText, now);
        }
        throw new IllegalStateException("不支持的审批组模式: " + group.getApprovalMode());
    }

    @Transactional(rollbackFor = Exception.class)
    public void closePendingGroupsForInstance(
            Long instanceId,
            BpmApprovalGroupCloseReasonEnum closeReason,
            BpmTaskResultEnum memberCancelledResult,
            LocalDateTime actionAt
    ) {
        List<BpmApprovalGroupEntity> pendingGroups =
                bpmApprovalGroupDao.selectPendingByInstanceIdForUpdate(instanceId);
        for (BpmApprovalGroupEntity group : pendingGroups) {
            List<BpmTaskEntity> pendingTasks =
                    bpmTaskDao.selectPendingByApprovalGroupIdForUpdate(group.getApprovalGroupId());
            for (BpmTaskEntity pendingTask : pendingTasks) {
                cancelTask(pendingTask, memberCancelledResult, actionAt);
            }
            BpmApprovalGroupEntity updateGroup = new BpmApprovalGroupEntity();
            updateGroup.setApprovalGroupId(group.getApprovalGroupId());
            updateGroup.setGroupState(BpmApprovalGroupStateEnum.CANCELLED.name());
            updateGroup.setCloseReason(closeReason.name());
            updateGroup.setClosedAt(actionAt);
            bpmApprovalGroupDao.updateById(updateGroup);
        }
    }

    public Map<Long, BpmApprovalGroupSummaryVO> mapSummariesById(
            Collection<Long> approvalGroupIds
    ) {
        if (approvalGroupIds == null || approvalGroupIds.isEmpty()) {
            return Map.of();
        }
        List<Long> distinctIds = approvalGroupIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        List<BpmApprovalGroupEntity> groups = bpmApprovalGroupDao.queryByIds(distinctIds);
        if (groups == null || groups.isEmpty()) {
            return Map.of();
        }
        Map<Long, BpmApprovalGroupSummaryVO> result = new LinkedHashMap<>();
        for (BpmApprovalGroupEntity group : groups) {
            result.put(group.getApprovalGroupId(), buildSummary(group));
        }
        return result;
    }

    public BpmApprovalGroupDetailVO getDetailById(Long approvalGroupId) {
        if (approvalGroupId == null) {
            return null;
        }
        BpmApprovalGroupEntity group = bpmApprovalGroupDao.selectById(approvalGroupId);
        if (group == null) {
            return null;
        }
        List<BpmApprovalGroupDetailVO> details = buildDetails(List.of(group));
        return details.isEmpty() ? null : details.get(0);
    }

    public List<BpmApprovalGroupDetailVO> listDetailsByInstanceId(Long instanceId) {
        if (instanceId == null) {
            return List.of();
        }
        List<BpmApprovalGroupEntity> groups = bpmApprovalGroupDao.selectByInstanceId(instanceId);
        return buildDetails(groups);
    }

    private List<BpmApprovalGroupDetailVO> buildDetails(List<BpmApprovalGroupEntity> groups) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        List<Long> groupIds = groups.stream()
                .map(BpmApprovalGroupEntity::getApprovalGroupId)
                .filter(Objects::nonNull)
                .toList();
        List<BpmTaskEntity> tasks = groupIds.isEmpty()
                ? List.of()
                : bpmTaskDao.selectByApprovalGroupIds(groupIds);
        if (tasks == null) {
            tasks = List.of();
        }

        Map<Long, List<BpmTaskEntity>> tasksByGroupId = new HashMap<>();
        for (BpmTaskEntity task : tasks) {
            tasksByGroupId.computeIfAbsent(task.getApprovalGroupId(), key -> new ArrayList<>()).add(task);
        }
        Map<Long, Integer> memberIndexByDefinitionNodeId = loadMemberIndexes(tasks);
        Map<Long, BpmTaskActionLogVO> lastActionByTaskId = loadLastActions(groups);

        List<BpmApprovalGroupDetailVO> details = new ArrayList<>(groups.size());
        for (BpmApprovalGroupEntity group : groups) {
            List<BpmTaskEntity> groupTasks = new ArrayList<>(
                    tasksByGroupId.getOrDefault(group.getApprovalGroupId(), List.of())
            );
            groupTasks.sort(Comparator
                    .comparing(
                            (BpmTaskEntity task) ->
                                    memberIndexByDefinitionNodeId.get(task.getDefinitionNodeId()),
                            Comparator.nullsLast(Integer::compareTo)
                    )
                    .thenComparing(
                            BpmTaskEntity::getAssignedAt,
                            Comparator.nullsLast(LocalDateTime::compareTo)
                    )
                    .thenComparing(
                            BpmTaskEntity::getTaskId,
                            Comparator.nullsLast(Long::compareTo)
                    ));

            boolean hasFallbackIndex = groupTasks.stream()
                    .anyMatch(task -> !memberIndexByDefinitionNodeId.containsKey(task.getDefinitionNodeId()));
            if (hasFallbackIndex) {
                // 编译快照异常时保持返回顺序稳定，页面不参与序号推断。
                LOGGER.warning(
                        "审批组成员缺少有效成员序号，已按到达时间和任务ID回退排序，approvalGroupId="
                                + group.getApprovalGroupId()
                );
            }

            BpmApprovalGroupDetailVO detail = buildDetail(group);
            List<BpmApprovalGroupMemberVO> members = new ArrayList<>(groupTasks.size());
            for (int index = 0; index < groupTasks.size(); index++) {
                BpmTaskEntity task = groupTasks.get(index);
                Integer snapshotIndex =
                        memberIndexByDefinitionNodeId.get(task.getDefinitionNodeId());
                members.add(buildMember(
                        task,
                        snapshotIndex == null ? index + 1 : snapshotIndex,
                        group.getTotalMemberCount(),
                        lastActionByTaskId.get(task.getTaskId())
                ));
            }
            detail.setMembers(members);
            details.add(detail);
        }
        return details;
    }

    private Map<Long, Integer> loadMemberIndexes(List<BpmTaskEntity> tasks) {
        if (tasks.isEmpty() || bpmDefinitionNodeDao == null) {
            return Map.of();
        }
        List<Long> definitionNodeIds = tasks.stream()
                .map(BpmTaskEntity::getDefinitionNodeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (definitionNodeIds.isEmpty()) {
            return Map.of();
        }
        List<BpmDefinitionNodeEntity> nodes = bpmDefinitionNodeDao.selectBatchIds(definitionNodeIds);
        if (nodes == null || nodes.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new HashMap<>();
        for (BpmDefinitionNodeEntity node : nodes) {
            ApprovalGroupNodeSnapshot snapshot = parseApprovalGroupSnapshot(node);
            if (snapshot != null) {
                result.put(node.getDefinitionNodeId(), snapshot.memberIndex());
            }
        }
        return result;
    }

    private Map<Long, BpmTaskActionLogVO> loadLastActions(
            List<BpmApprovalGroupEntity> groups
    ) {
        Map<Long, BpmTaskActionLogVO> result = new HashMap<>();
        List<Long> instanceIds = groups.stream()
                .map(BpmApprovalGroupEntity::getInstanceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        for (Long instanceId : instanceIds) {
            List<BpmTaskActionLogVO> actionLogs =
                    bpmTaskActionLogDao.queryByInstanceId(instanceId);
            if (actionLogs == null) {
                continue;
            }
            for (BpmTaskActionLogVO actionLog : actionLogs) {
                if (actionLog.getTaskId() != null) {
                    result.put(actionLog.getTaskId(), actionLog);
                }
            }
        }
        return result;
    }

    private BpmApprovalGroupSummaryVO buildSummary(BpmApprovalGroupEntity group) {
        BpmApprovalGroupSummaryVO summary = new BpmApprovalGroupSummaryVO();
        copySummaryFields(group, summary);
        return summary;
    }

    private BpmApprovalGroupDetailVO buildDetail(BpmApprovalGroupEntity group) {
        BpmApprovalGroupDetailVO detail = new BpmApprovalGroupDetailVO();
        copySummaryFields(group, detail);
        detail.setCloseReason(group.getCloseReason());
        detail.setClosedAt(group.getClosedAt());
        return detail;
    }

    private void copySummaryFields(
            BpmApprovalGroupEntity group,
            BpmApprovalGroupSummaryVO target
    ) {
        target.setApprovalGroupId(group.getApprovalGroupId());
        target.setApprovalGroupKey(group.getApprovalGroupKey());
        target.setApprovalGroupName(group.getApprovalGroupName());
        target.setApprovalMode(group.getApprovalMode());
        target.setGroupState(group.getGroupState());
        target.setTotalMemberCount(group.getTotalMemberCount());
        target.setProcessedMemberCount(group.getProcessedMemberCount());
        target.setApprovedMemberCount(group.getApprovedMemberCount());
        target.setRejectedMemberCount(group.getRejectedMemberCount());
    }

    private BpmApprovalGroupMemberVO buildMember(
            BpmTaskEntity task,
            Integer memberIndex,
            Integer memberTotal,
            BpmTaskActionLogVO lastAction
    ) {
        BpmApprovalGroupMemberVO member = new BpmApprovalGroupMemberVO();
        member.setTaskId(task.getTaskId());
        member.setMemberIndex(memberIndex);
        member.setMemberTotal(memberTotal);
        member.setAssigneeEmployeeId(task.getAssigneeEmployeeId());
        member.setAssigneeNameSnapshot(task.getAssigneeNameSnapshot());
        member.setAssigneeDepartmentNameSnapshot(task.getAssigneeDepartmentNameSnapshot());
        member.setTaskName(task.getTaskName());
        member.setTaskState(task.getTaskState());
        member.setTaskResult(task.getTaskResult());
        member.setAssignedAt(task.getAssignedAt());
        member.setCompletedAt(task.getCompletedAt());
        member.setCancelledAt(task.getCancelledAt());
        member.setLastAction(lastAction);
        return member;
    }

    private BpmApprovalGroupActionResult approveMember(
            BpmApprovalGroupEntity group,
            BpmTaskEntity currentTask,
            BpmEmployeeSnapshot actor,
            String commentText,
            LocalDateTime actionAt
    ) {
        flowableTaskGateway.complete(currentTask.getEngineTaskId());
        completeTask(currentTask, BpmTaskResultEnum.APPROVED, actionAt);
        bpmTaskActionLogDao.insert(buildActionLog(
                currentTask,
                actor,
                "PARALLEL_MEMBER_APPROVED",
                commentText,
                actionAt
        ));

        int processedCount = safeCount(group.getProcessedMemberCount()) + 1;
        int approvedCount = safeCount(group.getApprovedMemberCount()) + 1;
        boolean allApproved = approvedCount >= safeCount(group.getTotalMemberCount());
        BpmApprovalGroupStateEnum nextState = allApproved
                ? BpmApprovalGroupStateEnum.APPROVED
                : BpmApprovalGroupStateEnum.PENDING;

        BpmApprovalGroupEntity updateGroup = new BpmApprovalGroupEntity();
        updateGroup.setApprovalGroupId(group.getApprovalGroupId());
        updateGroup.setProcessedMemberCount(processedCount);
        updateGroup.setApprovedMemberCount(approvedCount);
        updateGroup.setRejectedMemberCount(safeCount(group.getRejectedMemberCount()));
        updateGroup.setGroupState(nextState.name());
        if (allApproved) {
            updateGroup.setCloseReason(BpmApprovalGroupCloseReasonEnum.ALL_APPROVED.name());
            updateGroup.setClosedAt(actionAt);
        }
        bpmApprovalGroupDao.updateById(updateGroup);
        return new BpmApprovalGroupActionResult(
                false,
                true,
                true,
                true,
                false,
                allApproved ? BpmInstanceResultStateEnum.APPROVED : null,
                false,
                group.getApprovalGroupId(),
                nextState
        );
    }

    private BpmApprovalGroupActionResult handleParallelMemberAction(
            BpmApprovalGroupEntity group,
            BpmTaskEntity currentTask,
            List<BpmTaskEntity> pendingTasks,
            BpmApprovalMemberAction action,
            BpmEmployeeSnapshot actor,
            String commentText,
            LocalDateTime actionAt
    ) {
        return action == BpmApprovalMemberAction.APPROVE
                ? approveMember(group, currentTask, actor, commentText, actionAt)
                : closeByMember(
                        group,
                        currentTask,
                        pendingTasks,
                        actor,
                        commentText,
                        action == BpmApprovalMemberAction.REJECT
                                ? BpmApprovalGroupStateEnum.REJECTED
                                : BpmApprovalGroupStateEnum.RETURNED,
                        action == BpmApprovalMemberAction.REJECT
                                ? BpmApprovalGroupCloseReasonEnum.MEMBER_REJECTED
                                : BpmApprovalGroupCloseReasonEnum.MEMBER_RETURNED,
                        action == BpmApprovalMemberAction.REJECT
                                ? BpmTaskResultEnum.REJECTED
                                : BpmTaskResultEnum.RETURNED,
                        action == BpmApprovalMemberAction.REJECT
                                ? "PARALLEL_MEMBER_REJECTED"
                                : "PARALLEL_MEMBER_RETURNED",
                        action == BpmApprovalMemberAction.REJECT
                                ? "并行会签成员拒绝"
                                : "并行会签成员退回发起人",
                        action == BpmApprovalMemberAction.RETURN,
                        actionAt
                );
    }

    private BpmApprovalGroupActionResult handleSequentialMemberAction(
            BpmApprovalGroupEntity group,
            BpmTaskEntity currentTask,
            BpmApprovalMemberAction action,
            BpmEmployeeSnapshot actor,
            String commentText,
            LocalDateTime actionAt
    ) {
        return switch (action) {
            case APPROVE -> approveSequentialMember(group, currentTask, actor, commentText, actionAt);
            case REJECT, RETURN -> closeSequentialMember(
                    group, currentTask, action, actor, commentText, actionAt);
        };
    }

    private BpmApprovalGroupActionResult approveSequentialMember(
            BpmApprovalGroupEntity group,
            BpmTaskEntity currentTask,
            BpmEmployeeSnapshot actor,
            String commentText,
            LocalDateTime actionAt
    ) {
        flowableTaskGateway.complete(currentTask.getEngineTaskId());
        completeTask(currentTask, BpmTaskResultEnum.APPROVED, actionAt);
        bpmTaskActionLogDao.insert(buildActionLog(currentTask, actor, "APPROVED", commentText, actionAt));

        int processedCount = safeCount(group.getProcessedMemberCount()) + 1;
        int approvedCount = safeCount(group.getApprovedMemberCount()) + 1;
        boolean allApproved = approvedCount == safeCount(group.getTotalMemberCount());
        BpmApprovalGroupEntity updateGroup = new BpmApprovalGroupEntity();
        updateGroup.setApprovalGroupId(group.getApprovalGroupId());
        updateGroup.setProcessedMemberCount(processedCount);
        updateGroup.setApprovedMemberCount(approvedCount);
        updateGroup.setRejectedMemberCount(safeCount(group.getRejectedMemberCount()));
        updateGroup.setGroupState((allApproved
                ? BpmApprovalGroupStateEnum.APPROVED
                : BpmApprovalGroupStateEnum.PENDING).name());
        if (allApproved) {
            updateGroup.setCloseReason(BpmApprovalGroupCloseReasonEnum.ALL_APPROVED.name());
            updateGroup.setClosedAt(actionAt);
        }
        bpmApprovalGroupDao.updateById(updateGroup);
        return new BpmApprovalGroupActionResult(
                false,
                true,
                true,
                true,
                false,
                allApproved ? BpmInstanceResultStateEnum.APPROVED : null,
                false,
                group.getApprovalGroupId(),
                allApproved ? BpmApprovalGroupStateEnum.APPROVED : BpmApprovalGroupStateEnum.PENDING
        );
    }

    private BpmApprovalGroupActionResult closeSequentialMember(
            BpmApprovalGroupEntity group,
            BpmTaskEntity currentTask,
            BpmApprovalMemberAction action,
            BpmEmployeeSnapshot actor,
            String commentText,
            LocalDateTime actionAt
    ) {
        boolean returned = action == BpmApprovalMemberAction.RETURN;
        BpmApprovalGroupStateEnum groupState = returned
                ? BpmApprovalGroupStateEnum.RETURNED
                : BpmApprovalGroupStateEnum.REJECTED;
        BpmTaskResultEnum taskResult = returned
                ? BpmTaskResultEnum.RETURNED
                : BpmTaskResultEnum.REJECTED;
        flowableProcessInstanceGateway.cancel(
                group.getEngineProcessInstanceId(),
                returned ? "审批退回发起人" : "审批驳回"
        );
        completeTask(currentTask, taskResult, actionAt);
        bpmTaskActionLogDao.insert(buildActionLog(
                currentTask,
                actor,
                returned ? "RETURNED_TO_INITIATOR" : "REJECTED",
                commentText,
                actionAt
        ));

        BpmApprovalGroupEntity updateGroup = new BpmApprovalGroupEntity();
        updateGroup.setApprovalGroupId(group.getApprovalGroupId());
        updateGroup.setGroupState(groupState.name());
        updateGroup.setCloseReason((returned
                ? BpmApprovalGroupCloseReasonEnum.MEMBER_RETURNED
                : BpmApprovalGroupCloseReasonEnum.MEMBER_REJECTED).name());
        updateGroup.setProcessedMemberCount(safeCount(group.getProcessedMemberCount()) + 1);
        updateGroup.setApprovedMemberCount(safeCount(group.getApprovedMemberCount()));
        updateGroup.setRejectedMemberCount(safeCount(group.getRejectedMemberCount()) + (returned ? 0 : 1));
        updateGroup.setClosedAt(actionAt);
        bpmApprovalGroupDao.updateById(updateGroup);

        if (returned) {
            BpmInstanceEntity updateInstance = new BpmInstanceEntity();
            updateInstance.setInstanceId(group.getInstanceId());
            updateInstance.setRunState(BpmInstanceRunStateEnum.WAIT_RESUBMIT.getValue());
            updateInstance.setActiveTaskCount(0);
            updateInstance.setCurrentNodeSummaryJson(null);
            updateInstance.setLastActionAt(actionAt);
            bpmInstanceDao.updateById(updateInstance);
        }
        return new BpmApprovalGroupActionResult(
                false,
                true,
                true,
                false,
                true,
                returned ? null : BpmInstanceResultStateEnum.REJECTED,
                returned,
                group.getApprovalGroupId(),
                groupState
        );
    }

    private BpmApprovalGroupActionResult closeByMember(
            BpmApprovalGroupEntity group,
            BpmTaskEntity currentTask,
            List<BpmTaskEntity> pendingTasks,
            BpmEmployeeSnapshot actor,
            String commentText,
            BpmApprovalGroupStateEnum nextState,
            BpmApprovalGroupCloseReasonEnum closeReason,
            BpmTaskResultEnum currentTaskResult,
            String actionType,
            String engineCancelReason,
            boolean waitResubmit,
            LocalDateTime actionAt
    ) {
        flowableProcessInstanceGateway.cancel(group.getEngineProcessInstanceId(), engineCancelReason);
        completeTask(currentTask, currentTaskResult, actionAt);
        for (BpmTaskEntity pendingTask : pendingTasks) {
            if (!Objects.equals(pendingTask.getTaskId(), currentTask.getTaskId())) {
                cancelTask(pendingTask, BpmTaskResultEnum.INSTANCE_CANCELLED, actionAt);
            }
        }
        bpmTaskActionLogDao.insert(buildActionLog(currentTask, actor, actionType, commentText, actionAt));

        BpmApprovalGroupEntity updateGroup = new BpmApprovalGroupEntity();
        updateGroup.setApprovalGroupId(group.getApprovalGroupId());
        updateGroup.setGroupState(nextState.name());
        updateGroup.setCloseReason(closeReason.name());
        updateGroup.setProcessedMemberCount(safeCount(group.getProcessedMemberCount()) + 1);
        updateGroup.setApprovedMemberCount(safeCount(group.getApprovedMemberCount()));
        updateGroup.setRejectedMemberCount(safeCount(group.getRejectedMemberCount())
                + (nextState == BpmApprovalGroupStateEnum.REJECTED ? 1 : 0));
        updateGroup.setClosedAt(actionAt);
        bpmApprovalGroupDao.updateById(updateGroup);

        if (waitResubmit) {
            BpmInstanceEntity updateInstance = new BpmInstanceEntity();
            updateInstance.setInstanceId(group.getInstanceId());
            updateInstance.setRunState(BpmInstanceRunStateEnum.WAIT_RESUBMIT.getValue());
            updateInstance.setActiveTaskCount(0);
            updateInstance.setCurrentNodeSummaryJson(null);
            updateInstance.setLastActionAt(actionAt);
            bpmInstanceDao.updateById(updateInstance);
        }
        return new BpmApprovalGroupActionResult(
                false,
                true,
                true,
                false,
                true,
                nextState == BpmApprovalGroupStateEnum.REJECTED
                        ? BpmInstanceResultStateEnum.REJECTED
                        : null,
                waitResubmit,
                group.getApprovalGroupId(),
                nextState
        );
    }

    private void completeTask(
            BpmTaskEntity task,
            BpmTaskResultEnum result,
            LocalDateTime actionAt
    ) {
        BpmTaskEntity updateTask = new BpmTaskEntity();
        updateTask.setTaskId(task.getTaskId());
        updateTask.setTaskState(BpmTaskStateEnum.COMPLETED.getValue());
        updateTask.setTaskResult(result.getValue());
        updateTask.setCompletedAt(actionAt);
        updateTask.setLastActionAt(actionAt);
        bpmTaskDao.updateById(updateTask);
    }

    private void cancelTask(
            BpmTaskEntity task,
            BpmTaskResultEnum result,
            LocalDateTime actionAt
    ) {
        BpmTaskEntity updateTask = new BpmTaskEntity();
        updateTask.setTaskId(task.getTaskId());
        updateTask.setTaskState(BpmTaskStateEnum.CANCELLED.getValue());
        updateTask.setTaskResult(result.getValue());
        updateTask.setCancelledAt(actionAt);
        updateTask.setLastActionAt(actionAt);
        bpmTaskDao.updateById(updateTask);
    }

    private BpmTaskActionLogEntity buildActionLog(
            BpmTaskEntity task,
            BpmEmployeeSnapshot actor,
            String actionType,
            String commentText,
            LocalDateTime actionAt
    ) {
        BpmTaskActionLogEntity log = new BpmTaskActionLogEntity();
        log.setInstanceId(task.getInstanceId());
        log.setTaskId(task.getTaskId());
        log.setDefinitionId(task.getDefinitionId());
        log.setDefinitionNodeId(task.getDefinitionNodeId());
        log.setEngineTaskId(task.getEngineTaskId());
        log.setActionType(actionType);
        log.setActorEmployeeId(actor.employeeId());
        log.setActorNameSnapshot(actor.actualName());
        log.setFromAssigneeEmployeeId(task.getAssigneeEmployeeId());
        log.setToAssigneeEmployeeId(task.getAssigneeEmployeeId());
        log.setCommentText(commentText);
        log.setActionAt(actionAt);
        return log;
    }

    private ApprovalGroupNodeSnapshot parseApprovalGroupSnapshot(BpmDefinitionNodeEntity definitionNode) {
        if (definitionNode == null || !StringUtils.hasText(definitionNode.getCompiledNodeSnapshotJson())) {
            return null;
        }
        try {
            JSONObject snapshot = JSON.parseObject(definitionNode.getCompiledNodeSnapshotJson());
            String approvalMode = snapshot.getString("approvalMode");
            String groupKey = snapshot.getString("approvalGroupKey");
            String groupName = snapshot.getString("approvalGroupName");
            Integer memberIndex = "parallelAll".equals(approvalMode)
                    ? snapshot.getInteger("parallelIndex")
                    : snapshot.getInteger("sequentialIndex");
            Integer memberTotal = "parallelAll".equals(approvalMode)
                    ? snapshot.getInteger("parallelTotal")
                    : snapshot.getInteger("sequentialTotal");
            if (!"parallelAll".equals(approvalMode) && !"sequential".equals(approvalMode)) {
                warnInvalidApprovalGroupSnapshot(definitionNode);
                return null;
            }
            if (!StringUtils.hasText(groupKey)
                    || !StringUtils.hasText(groupName)
                    || memberIndex == null
                    || memberTotal == null
                    || memberIndex < 1
                    || memberIndex > memberTotal
                    || memberTotal < 2) {
                warnInvalidApprovalGroupSnapshot(definitionNode);
                return null;
            }
            return new ApprovalGroupNodeSnapshot(
                    groupKey,
                    groupName,
                    approvalMode,
                    memberIndex,
                    memberTotal
            );
        } catch (Exception ex) {
            warnInvalidApprovalGroupSnapshot(definitionNode);
            return null;
        }
    }

    private void warnInvalidApprovalGroupSnapshot(BpmDefinitionNodeEntity definitionNode) {
        LOGGER.warning("审批组编译快照无效，definitionNodeId=" + definitionNode.getDefinitionNodeId());
    }

    private void verifyGroupRuntimeFacts(
            BpmApprovalGroupEntity group,
            BpmInstanceEntity instance,
            ApprovalGroupNodeSnapshot snapshot
    ) {
        if (!Objects.equals(group.getInstanceId(), instance.getInstanceId())
                || !Objects.equals(group.getDefinitionId(), instance.getDefinitionId())
                || !Objects.equals(group.getEngineProcessInstanceId(), instance.getEngineProcessInstanceId())
                || !Objects.equals(group.getApprovalMode(), snapshot.approvalMode())
                || !Objects.equals(group.getTotalMemberCount(), snapshot.memberTotal())) {
            throw new IllegalStateException("审批组快照与运行时事实不一致");
        }
    }

    private void restoreSequentialGroupMembers(
            BpmInstanceEntity instance,
            ApprovalGroupNodeSnapshot snapshot,
            Long approvalGroupId,
            BpmTaskEntity currentTask
    ) {
        if (approvalGroupId == null) {
            return;
        }
        List<BpmTaskEntity> instanceTasks = bpmTaskDao.selectList(Wrappers.<BpmTaskEntity>lambdaQuery()
                .eq(BpmTaskEntity::getInstanceId, instance.getInstanceId())
                .eq(BpmTaskEntity::getEngineProcessInstanceId, instance.getEngineProcessInstanceId()));
        if (instanceTasks == null) {
            instanceTasks = new ArrayList<>();
        } else {
            // DAO 查询条件是第一道边界；这里再次过滤，避免重提后的旧引擎流程混入恢复计算。
            instanceTasks = instanceTasks.stream()
                    .filter(task -> Objects.equals(task.getInstanceId(), instance.getInstanceId()))
                    .filter(task -> Objects.equals(
                            task.getEngineProcessInstanceId(),
                            instance.getEngineProcessInstanceId()
                    ))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }
        boolean containsCurrentTask = currentTask != null && instanceTasks.stream()
                .anyMatch(task -> Objects.equals(task.getTaskId(), currentTask.getTaskId())
                        || Objects.equals(task.getEngineTaskId(), currentTask.getEngineTaskId()));
        if (currentTask != null && !containsCurrentTask) {
            instanceTasks.add(currentTask);
        }

        List<Long> nodeIds = instanceTasks.stream()
                .map(BpmTaskEntity::getDefinitionNodeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (nodeIds.isEmpty()) {
            return;
        }
        List<BpmDefinitionNodeEntity> nodes = bpmDefinitionNodeDao.selectBatchIds(nodeIds);
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        Map<Long, ApprovalGroupNodeSnapshot> snapshotsByNodeId = new HashMap<>();
        for (BpmDefinitionNodeEntity node : nodes) {
            ApprovalGroupNodeSnapshot nodeSnapshot = parseApprovalGroupSnapshot(node);
            if (nodeSnapshot != null
                    && nodeSnapshot.isSequential()
                    && Objects.equals(nodeSnapshot.groupKey(), snapshot.groupKey())) {
                snapshotsByNodeId.put(node.getDefinitionNodeId(), nodeSnapshot);
            }
        }

        List<BpmTaskEntity> authoredTasks = new ArrayList<>();
        for (BpmTaskEntity task : instanceTasks) {
            if (!snapshotsByNodeId.containsKey(task.getDefinitionNodeId())) {
                continue;
            }
            authoredTasks.add(task);
            if (task.getApprovalGroupId() == null && task.getTaskId() != null) {
                BpmTaskEntity updateTask = new BpmTaskEntity();
                updateTask.setTaskId(task.getTaskId());
                updateTask.setApprovalGroupId(approvalGroupId);
                bpmTaskDao.updateById(updateTask);
                task.setApprovalGroupId(approvalGroupId);
            }
        }
        BpmApprovalGroupEntity group = bpmApprovalGroupDao.selectByEngineProcessInstanceIdAndGroupKey(
                instance.getEngineProcessInstanceId(),
                snapshot.groupKey()
        );
        if (group != null) {
            recalculateSequentialGroup(group, authoredTasks);
        }
    }

    private void recalculateSequentialGroup(
            BpmApprovalGroupEntity group,
            List<BpmTaskEntity> authoredTasks
    ) {
        int approvedCount = (int) authoredTasks.stream()
                .filter(task -> BpmTaskResultEnum.APPROVED.equalsValue(task.getTaskResult()))
                .count();
        int rejectedCount = (int) authoredTasks.stream()
                .filter(task -> BpmTaskResultEnum.REJECTED.equalsValue(task.getTaskResult()))
                .count();
        boolean hasReturned = authoredTasks.stream()
                .anyMatch(task -> BpmTaskResultEnum.RETURNED.equalsValue(task.getTaskResult()));
        int processedCount = (int) authoredTasks.stream()
                .filter(task -> task.getTaskResult() != null)
                .count();
        BpmApprovalGroupStateEnum groupState = hasReturned
                ? BpmApprovalGroupStateEnum.RETURNED
                : rejectedCount > 0
                        ? BpmApprovalGroupStateEnum.REJECTED
                        : approvedCount == safeCount(group.getTotalMemberCount())
                                ? BpmApprovalGroupStateEnum.APPROVED
                                : BpmApprovalGroupStateEnum.PENDING;

        BpmApprovalGroupEntity updateGroup = new BpmApprovalGroupEntity();
        updateGroup.setApprovalGroupId(group.getApprovalGroupId());
        updateGroup.setProcessedMemberCount(processedCount);
        updateGroup.setApprovedMemberCount(approvedCount);
        updateGroup.setRejectedMemberCount(rejectedCount);
        updateGroup.setGroupState(groupState.name());
        bpmApprovalGroupDao.updateById(updateGroup);
    }

    private BpmApprovalGroupStateEnum parseGroupState(String groupState) {
        try {
            return BpmApprovalGroupStateEnum.valueOf(groupState);
        } catch (Exception ex) {
            throw new IllegalArgumentException("并行审批组状态无效");
        }
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : count;
    }

    private record ApprovalGroupNodeSnapshot(
            String groupKey,
            String groupName,
            String approvalMode,
            Integer memberIndex,
            Integer memberTotal
    ) {

        boolean isParallelAll() {
            return "parallelAll".equals(approvalMode);
        }

        boolean isSequential() {
            return "sequential".equals(approvalMode);
        }
    }
}
