package com.hunyuan.sa.bpm.module.runtime.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartPageUtil;
import com.hunyuan.sa.bpm.api.business.BpmBusinessProcessApi;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessResultEvent;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmApprovalGroupCloseReasonEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmCopyTypeEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceResultStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskResultEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.engine.internal.FlowableTaskGateway;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmAdminTaskTransferForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskAddSignForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskApproveForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskDelegateForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskRecallForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskRejectForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskReduceSignForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskReturnForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskTransferForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmApprovalGroupSummaryVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 流程任务服务。
 */
@Service
public class BpmTaskService {

    @Resource
    private BpmTaskDao bpmTaskDao;

    @Resource
    private BpmInstanceDao bpmInstanceDao;

    @Resource
    private BpmTaskActionLogDao bpmTaskActionLogDao;

    @Resource
    private FlowableTaskGateway flowableTaskGateway;

    @Resource
    private FlowableProcessInstanceGateway flowableProcessInstanceGateway;

    @Resource
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Resource
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    @Resource
    private BpmTaskProjectionService bpmTaskProjectionService;

    @Resource
    private BpmInstanceCopyService bpmInstanceCopyService;

    @Resource
    private BpmBusinessProcessApi bpmBusinessProcessApi;

    @Resource
    private BpmApprovalGroupService bpmApprovalGroupService;

    public ResponseDTO<PageResult<BpmTaskVO>> queryAdminPage(BpmTaskQueryForm queryForm) {
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<BpmTaskVO> list = bpmTaskDao.queryPage(page, queryForm);
        attachApprovalGroupSummaries(list);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, list));
    }

    public ResponseDTO<PageResult<BpmTaskVO>> queryMyTodoPage(BpmTaskQueryForm queryForm) {
        queryForm.setAssigneeEmployeeId(bpmCurrentActorProvider.requireCurrentEmployeeId());
        queryForm.setTaskState(BpmTaskStateEnum.PENDING.getValue());
        return queryAdminPage(queryForm);
    }

    public ResponseDTO<PageResult<BpmTaskVO>> queryMyDonePage(BpmTaskQueryForm queryForm) {
        queryForm.setAssigneeEmployeeId(bpmCurrentActorProvider.requireCurrentEmployeeId());
        queryForm.setTaskState(BpmTaskStateEnum.COMPLETED.getValue());
        return queryAdminPage(queryForm);
    }

    public ResponseDTO<BpmTaskDetailVO> getDetail(Long taskId) {
        BpmTaskEntity taskEntity = bpmTaskDao.selectById(taskId);
        if (taskEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return ResponseDTO.ok(buildDetail(taskEntity));
    }

    public ResponseDTO<BpmTaskDetailVO> getMyDetail(Long taskId) {
        BpmTaskEntity taskEntity = bpmTaskDao.selectById(taskId);
        if (taskEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        Long currentEmployeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        // 员工端详情只开放给当前任务处理人，避免绕过管理端任务详情权限。
        if (!Objects.equals(taskEntity.getAssigneeEmployeeId(), currentEmployeeId)) {
            return ResponseDTO.error(UserErrorCode.NO_PERMISSION);
        }
        return ResponseDTO.ok(buildDetail(taskEntity));
    }

    private BpmTaskDetailVO buildDetail(BpmTaskEntity taskEntity) {
        BpmTaskDetailVO detailVO = new BpmTaskDetailVO();
        detailVO.setTaskId(taskEntity.getTaskId());
        detailVO.setInstanceId(taskEntity.getInstanceId());
        detailVO.setInstanceNo(taskEntity.getInstanceNo());
        detailVO.setInstanceTitle(taskEntity.getInstanceTitle());
        detailVO.setTaskKey(taskEntity.getTaskKey());
        detailVO.setTaskName(taskEntity.getTaskName());
        detailVO.setStartEmployeeNameSnapshot(taskEntity.getStartEmployeeNameSnapshot());
        detailVO.setAssigneeNameSnapshot(taskEntity.getAssigneeNameSnapshot());
        detailVO.setAssigneeDepartmentNameSnapshot(taskEntity.getAssigneeDepartmentNameSnapshot());
        detailVO.setRuntimeAssignmentSnapshotJson(taskEntity.getRuntimeAssignmentSnapshotJson());
        detailVO.setTaskState(taskEntity.getTaskState());
        detailVO.setTaskResult(taskEntity.getTaskResult());
        detailVO.setAssignedAt(taskEntity.getAssignedAt());
        detailVO.setDueAt(taskEntity.getDueAt());
        detailVO.setCompletedAt(taskEntity.getCompletedAt());
        detailVO.setActionLogs(bpmTaskActionLogDao.queryByInstanceId(taskEntity.getInstanceId()));
        if (taskEntity.getApprovalGroupId() != null) {
            detailVO.setApprovalGroup(
                    bpmApprovalGroupService.getDetailById(taskEntity.getApprovalGroupId())
            );
        }
        return detailVO;
    }

    private void attachApprovalGroupSummaries(List<BpmTaskVO> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        List<Long> approvalGroupIds = tasks.stream()
                .map(BpmTaskVO::getApprovalGroupId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (approvalGroupIds.isEmpty()) {
            return;
        }
        Map<Long, BpmApprovalGroupSummaryVO> summaryMap =
                bpmApprovalGroupService.mapSummariesById(approvalGroupIds);
        tasks.stream()
                .filter(task -> task.getApprovalGroupId() != null)
                .forEach(task -> task.setApprovalGroup(summaryMap.get(task.getApprovalGroupId())));
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> approve(BpmTaskApproveForm approveForm) {
        return completeTask(
                approveForm.getTaskId(),
                approveForm.getCommentText(),
                BpmTaskResultEnum.APPROVED,
                "APPROVED",
                approveForm.getCopyEmployeeIds(),
                BpmCopyTypeEnum.MANUAL_APPROVE_COPY
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> reject(BpmTaskRejectForm rejectForm) {
        return completeTask(
                rejectForm.getTaskId(),
                rejectForm.getCommentText(),
                BpmTaskResultEnum.REJECTED,
                "REJECTED",
                rejectForm.getCopyEmployeeIds(),
                BpmCopyTypeEnum.MANUAL_REJECT_COPY
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> returnToInitiator(BpmTaskReturnForm returnForm) {
        BpmTaskEntity taskEntity = bpmTaskDao.selectById(returnForm.getTaskId());
        if (taskEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (taskEntity.getApprovalGroupId() != null) {
            return handleApprovalGroupAction(
                    taskEntity,
                    BpmApprovalMemberAction.RETURN,
                    returnForm.getCommentText(),
                    returnForm.getCopyEmployeeIds(),
                    BpmCopyTypeEnum.MANUAL_RETURN_COPY
            );
        }
        BpmInstanceEntity instanceEntity = bpmInstanceDao.selectById(taskEntity.getInstanceId());
        if (instanceEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot employeeSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        flowableProcessInstanceGateway.cancel(
                taskEntity.getEngineProcessInstanceId(),
                "审批退回发起人"
        );

        LocalDateTime now = LocalDateTime.now();
        BpmTaskEntity updateTaskEntity = new BpmTaskEntity();
        updateTaskEntity.setTaskId(taskEntity.getTaskId());
        updateTaskEntity.setTaskState(BpmTaskStateEnum.COMPLETED.getValue());
        updateTaskEntity.setTaskResult(BpmTaskResultEnum.RETURNED.getValue());
        updateTaskEntity.setCompletedAt(now);
        updateTaskEntity.setLastActionAt(now);
        bpmTaskDao.updateById(updateTaskEntity);
        closeOtherPendingTasks(
                taskEntity.getInstanceId(),
                taskEntity.getTaskId(),
                BpmTaskResultEnum.RETURNED,
                now
        );

        BpmInstanceEntity updateInstanceEntity = new BpmInstanceEntity();
        updateInstanceEntity.setInstanceId(instanceEntity.getInstanceId());
        updateInstanceEntity.setRunState(BpmInstanceRunStateEnum.WAIT_RESUBMIT.getValue());
        updateInstanceEntity.setActiveTaskCount(0);
        updateInstanceEntity.setCurrentNodeSummaryJson(null);
        updateInstanceEntity.setLastActionAt(now);
        bpmInstanceDao.updateById(updateInstanceEntity);

        bpmTaskActionLogDao.insert(buildActionLog(
                taskEntity,
                employeeSnapshot,
                "RETURNED_TO_INITIATOR",
                returnForm.getCommentText(),
                taskEntity.getAssigneeEmployeeId(),
                taskEntity.getAssigneeEmployeeId(),
                now
        ));
        ResponseDTO<String> copyResponse = bpmInstanceCopyService.createManualCopies(
                taskEntity,
                returnForm.getCopyEmployeeIds(),
                returnForm.getCommentText(),
                BpmCopyTypeEnum.MANUAL_RETURN_COPY
        );
        if (!copyResponse.getOk()) {
            return copyResponse;
        }
        return ResponseDTO.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> transfer(BpmTaskTransferForm transferForm) {
        BpmTaskEntity taskEntity = bpmTaskDao.selectById(transferForm.getTaskId());
        if (taskEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot actorSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        BpmEmployeeSnapshot targetSnapshot = bpmOrgIdentityGateway.requireEmployee(transferForm.getToEmployeeId());
        flowableTaskGateway.transfer(taskEntity.getEngineTaskId(), transferForm.getToEmployeeId());

        updateTaskAssigneeAndWriteLog(
                taskEntity,
                actorSnapshot,
                "TRANSFERRED",
                transferForm.getCommentText(),
                targetSnapshot
        );
        return ResponseDTO.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> adminTransfer(BpmAdminTaskTransferForm transferForm) {
        BpmTaskEntity taskEntity = bpmTaskDao.selectById(transferForm.getTaskId());
        if (taskEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot actorSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        BpmEmployeeSnapshot targetSnapshot = bpmOrgIdentityGateway.requireEmployee(transferForm.getTargetEmployeeId());
        flowableTaskGateway.transfer(taskEntity.getEngineTaskId(), transferForm.getTargetEmployeeId());

        updateTaskAssigneeAndWriteLog(
                taskEntity,
                actorSnapshot,
                "ADMIN_TRANSFERRED",
                transferForm.getReason(),
                targetSnapshot
        );
        return ResponseDTO.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> delegate(BpmTaskDelegateForm delegateForm) {
        return delegateInternal(delegateForm, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> adminDelegate(BpmTaskDelegateForm delegateForm) {
        return delegateInternal(delegateForm, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> addSign(BpmTaskAddSignForm addSignForm) {
        BpmTaskEntity taskEntity = bpmTaskDao.selectById(addSignForm.getTaskId());
        if (taskEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (bpmApprovalGroupService.isParallelAllGroup(taskEntity.getApprovalGroupId())) {
            return ResponseDTO.userErrorParam("并行全员会签成员不支持加签或减签");
        }
        if (!BpmTaskStateEnum.PENDING.equalsValue(taskEntity.getTaskState())) {
            return ResponseDTO.userErrorParam("当前流程任务状态不支持加签");
        }

        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        if (!Objects.equals(taskEntity.getAssigneeEmployeeId(), employeeId)) {
            return ResponseDTO.userErrorParam("只能对自己的待办任务加签");
        }
        BpmEmployeeSnapshot actorSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        BpmEmployeeSnapshot targetSnapshot = bpmOrgIdentityGateway.requireEmployee(addSignForm.getTargetEmployeeId());

        LocalDateTime now = LocalDateTime.now();
        BpmTaskEntity addSignTask = buildAddSignTask(taskEntity, targetSnapshot, now);
        bpmTaskDao.insert(addSignTask);
        bpmTaskActionLogDao.insert(buildActionLog(
                taskEntity,
                actorSnapshot,
                "ADD_SIGNED",
                addSignForm.getReason(),
                taskEntity.getAssigneeEmployeeId(),
                targetSnapshot.employeeId(),
                now
        ));
        return ResponseDTO.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> reduceSign(BpmTaskReduceSignForm reduceSignForm) {
        BpmTaskEntity taskEntity = bpmTaskDao.selectById(reduceSignForm.getTaskId());
        if (taskEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (bpmApprovalGroupService.isParallelAllGroup(taskEntity.getApprovalGroupId())) {
            return ResponseDTO.userErrorParam("并行全员会签成员不支持加签或减签");
        }
        if (!BpmTaskStateEnum.PENDING.equalsValue(taskEntity.getTaskState())) {
            return ResponseDTO.userErrorParam("当前流程任务状态不支持减签");
        }
        if (taskEntity.getRuntimeAssignmentSnapshotJson() == null
                || !taskEntity.getRuntimeAssignmentSnapshotJson().contains("\"addSign\":true")) {
            return ResponseDTO.userErrorParam("只能减签加签任务");
        }

        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot actorSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        LocalDateTime now = LocalDateTime.now();
        BpmTaskEntity updateTaskEntity = new BpmTaskEntity();
        updateTaskEntity.setTaskId(taskEntity.getTaskId());
        updateTaskEntity.setTaskState(BpmTaskStateEnum.CANCELLED.getValue());
        updateTaskEntity.setTaskResult(BpmTaskResultEnum.ADD_SIGN_REDUCED.getValue());
        updateTaskEntity.setCancelledAt(now);
        updateTaskEntity.setLastActionAt(now);
        bpmTaskDao.updateById(updateTaskEntity);

        bpmTaskActionLogDao.insert(buildActionLog(
                taskEntity,
                actorSnapshot,
                "REDUCE_SIGNED",
                reduceSignForm.getReason(),
                taskEntity.getAssigneeEmployeeId(),
                null,
                now
        ));
        return ResponseDTO.ok();
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> recall(BpmTaskRecallForm recallForm) {
        BpmTaskEntity taskEntity = bpmTaskDao.selectById(recallForm.getTaskId());
        if (taskEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        BpmInstanceEntity instanceEntity = bpmInstanceDao.selectById(taskEntity.getInstanceId());
        if (instanceEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        if (!Objects.equals(instanceEntity.getStartEmployeeId(), employeeId)) {
            return ResponseDTO.userErrorParam("只能撤回自己发起的流程");
        }
        if (!BpmInstanceRunStateEnum.RUNNING.equalsValue(instanceEntity.getRunState())) {
            return ResponseDTO.userErrorParam("当前流程实例状态不支持撤回");
        }

        BpmEmployeeSnapshot actorSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        LocalDateTime now = LocalDateTime.now();
        flowableProcessInstanceGateway.cancel(instanceEntity.getEngineProcessInstanceId(), "发起人撤回");
        bpmApprovalGroupService.closePendingGroupsForInstance(
                instanceEntity.getInstanceId(),
                BpmApprovalGroupCloseReasonEnum.INSTANCE_RECALLED,
                BpmTaskResultEnum.RECALLED,
                now
        );
        List<BpmTaskEntity> pendingTasks = bpmTaskDao.selectList(Wrappers.<BpmTaskEntity>lambdaQuery()
                .eq(BpmTaskEntity::getInstanceId, instanceEntity.getInstanceId())
                .isNull(BpmTaskEntity::getApprovalGroupId)
                .eq(BpmTaskEntity::getTaskState, BpmTaskStateEnum.PENDING.getValue()));
        for (BpmTaskEntity pendingTask : pendingTasks) {
            BpmTaskEntity updateTaskEntity = new BpmTaskEntity();
            updateTaskEntity.setTaskId(pendingTask.getTaskId());
            updateTaskEntity.setTaskState(BpmTaskStateEnum.CANCELLED.getValue());
            updateTaskEntity.setTaskResult(BpmTaskResultEnum.RECALLED.getValue());
            updateTaskEntity.setCancelledAt(now);
            updateTaskEntity.setLastActionAt(now);
            bpmTaskDao.updateById(updateTaskEntity);
        }

        BpmInstanceEntity updateInstanceEntity = new BpmInstanceEntity();
        updateInstanceEntity.setInstanceId(instanceEntity.getInstanceId());
        updateInstanceEntity.setRunState(BpmInstanceRunStateEnum.WAIT_RESUBMIT.getValue());
        updateInstanceEntity.setActiveTaskCount(0);
        updateInstanceEntity.setCurrentNodeSummaryJson(null);
        updateInstanceEntity.setLastActionAt(now);
        bpmInstanceDao.updateById(updateInstanceEntity);

        bpmTaskActionLogDao.insert(buildActionLog(
                taskEntity,
                actorSnapshot,
                "RECALLED",
                recallForm.getReason(),
                taskEntity.getAssigneeEmployeeId(),
                null,
                now
        ));
        return ResponseDTO.ok();
    }

    private ResponseDTO<String> delegateInternal(BpmTaskDelegateForm delegateForm, boolean requireCurrentAssignee) {
        BpmTaskEntity taskEntity = bpmTaskDao.selectById(delegateForm.getTaskId());
        if (taskEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (!BpmTaskStateEnum.PENDING.equalsValue(taskEntity.getTaskState())) {
            return ResponseDTO.userErrorParam("当前流程任务状态不支持委派");
        }

        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        if (requireCurrentAssignee && !Objects.equals(taskEntity.getAssigneeEmployeeId(), employeeId)) {
            return ResponseDTO.userErrorParam("只能委派自己的待办任务");
        }
        BpmEmployeeSnapshot actorSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        BpmEmployeeSnapshot targetSnapshot = bpmOrgIdentityGateway.requireEmployee(delegateForm.getTargetEmployeeId());
        flowableTaskGateway.transfer(taskEntity.getEngineTaskId(), targetSnapshot.employeeId());
        updateTaskAssigneeAndWriteLog(
                taskEntity,
                actorSnapshot,
                "DELEGATED",
                delegateForm.getReason(),
                targetSnapshot
        );
        return ResponseDTO.ok();
    }

    private BpmTaskEntity buildAddSignTask(
            BpmTaskEntity sourceTask,
            BpmEmployeeSnapshot targetSnapshot,
            LocalDateTime assignedAt
    ) {
        BpmTaskEntity task = new BpmTaskEntity();
        task.setInstanceId(sourceTask.getInstanceId());
        task.setDefinitionId(sourceTask.getDefinitionId());
        task.setDefinitionNodeId(sourceTask.getDefinitionNodeId());
        task.setEngineTaskId(sourceTask.getEngineTaskId() + ":add-sign:" + targetSnapshot.employeeId());
        task.setEngineExecutionId(sourceTask.getEngineExecutionId());
        task.setEngineProcessInstanceId(sourceTask.getEngineProcessInstanceId());
        task.setTaskKey(sourceTask.getTaskKey());
        task.setTaskName(sourceTask.getTaskName() + "加签");
        task.setInstanceNo(sourceTask.getInstanceNo());
        task.setInstanceTitle(sourceTask.getInstanceTitle());
        task.setStartEmployeeId(sourceTask.getStartEmployeeId());
        task.setStartEmployeeNameSnapshot(sourceTask.getStartEmployeeNameSnapshot());
        task.setCategoryIdSnapshot(sourceTask.getCategoryIdSnapshot());
        task.setCategoryNameSnapshot(sourceTask.getCategoryNameSnapshot());
        task.setAssigneeEmployeeId(targetSnapshot.employeeId());
        task.setAssigneeNameSnapshot(targetSnapshot.actualName());
        task.setAssigneeDepartmentIdSnapshot(targetSnapshot.departmentId());
        task.setAssigneeDepartmentNameSnapshot(targetSnapshot.departmentName());
        task.setRuntimeAssignmentSnapshotJson("{\"addSign\":true,\"sourceTaskId\":"
                + sourceTask.getTaskId()
                + ",\"assigneeEmployeeId\":"
                + targetSnapshot.employeeId()
                + "}");
        task.setTaskState(BpmTaskStateEnum.PENDING.getValue());
        task.setAssignedAt(assignedAt);
        task.setLastActionAt(assignedAt);
        return task;
    }

    private ResponseDTO<String> completeTask(
            Long taskId,
            String commentText,
            BpmTaskResultEnum resultEnum,
            String actionType,
            Collection<Long> copyEmployeeIds,
            BpmCopyTypeEnum copyTypeEnum
    ) {
        BpmTaskEntity taskEntity = bpmTaskDao.selectById(taskId);
        if (taskEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (taskEntity.getApprovalGroupId() != null) {
            BpmApprovalMemberAction memberAction = BpmTaskResultEnum.REJECTED.equals(resultEnum)
                    ? BpmApprovalMemberAction.REJECT
                    : BpmApprovalMemberAction.APPROVE;
            return handleApprovalGroupAction(
                    taskEntity,
                    memberAction,
                    commentText,
                    copyEmployeeIds,
                    copyTypeEnum
            );
        }
        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot employeeSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        if (BpmTaskResultEnum.REJECTED.equals(resultEnum)) {
            flowableProcessInstanceGateway.cancel(taskEntity.getEngineProcessInstanceId(), "审批驳回");
        } else {
            flowableTaskGateway.complete(taskEntity.getEngineTaskId());
        }

        LocalDateTime now = LocalDateTime.now();
        BpmTaskEntity updateTaskEntity = new BpmTaskEntity();
        updateTaskEntity.setTaskId(taskEntity.getTaskId());
        updateTaskEntity.setTaskState(BpmTaskStateEnum.COMPLETED.getValue());
        updateTaskEntity.setTaskResult(resultEnum.getValue());
        updateTaskEntity.setCompletedAt(now);
        updateTaskEntity.setLastActionAt(now);
        bpmTaskDao.updateById(updateTaskEntity);
        if (BpmTaskResultEnum.REJECTED.equals(resultEnum)) {
            closeOtherPendingTasks(
                    taskEntity.getInstanceId(),
                    taskEntity.getTaskId(),
                    BpmTaskResultEnum.REJECTED,
                    now
            );
        }

        bpmTaskActionLogDao.insert(buildActionLog(
                taskEntity,
                employeeSnapshot,
                actionType,
                commentText,
                taskEntity.getAssigneeEmployeeId(),
                taskEntity.getAssigneeEmployeeId(),
                now
        ));
        int activeTaskCount = bpmTaskProjectionService.syncActiveTasksForInstance(taskEntity.getInstanceId());
        if (BpmTaskResultEnum.REJECTED.equals(resultEnum)) {
            finishInstance(taskEntity.getInstanceId(), BpmInstanceResultStateEnum.REJECTED);
        } else if (activeTaskCount == 0) {
            finishInstance(taskEntity.getInstanceId(), BpmInstanceResultStateEnum.APPROVED);
        }
        ResponseDTO<String> copyResponse = bpmInstanceCopyService.createManualCopies(
                taskEntity,
                copyEmployeeIds,
                commentText,
                copyTypeEnum
        );
        if (!copyResponse.getOk()) {
            return copyResponse;
        }
        return ResponseDTO.ok();
    }

    private ResponseDTO<String> handleApprovalGroupAction(
            BpmTaskEntity taskEntity,
            BpmApprovalMemberAction action,
            String commentText,
            Collection<Long> copyEmployeeIds,
            BpmCopyTypeEnum copyTypeEnum
    ) {
        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot actorSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        BpmApprovalGroupActionResult actionResult;
        try {
            actionResult = bpmApprovalGroupService.handleMemberAction(
                    taskEntity.getTaskId(),
                    action,
                    actorSnapshot,
                    commentText
            );
        } catch (IllegalArgumentException ex) {
            return ResponseDTO.userErrorParam(ex.getMessage());
        }
        if (!actionResult.processed()) {
            return ResponseDTO.userErrorParam("审批组已关闭或任务已处理");
        }

        if (actionResult.waitResubmit()) {
            closeOtherPendingTasks(
                    taskEntity.getInstanceId(),
                    taskEntity.getTaskId(),
                    BpmTaskResultEnum.RETURNED,
                    LocalDateTime.now()
            );
        } else if (actionResult.finishInstanceResultState() == BpmInstanceResultStateEnum.REJECTED) {
            closeOtherPendingTasks(
                    taskEntity.getInstanceId(),
                    taskEntity.getTaskId(),
                    BpmTaskResultEnum.REJECTED,
                    LocalDateTime.now()
            );
        }

        if (action == BpmApprovalMemberAction.APPROVE) {
            int activeTaskCount = bpmTaskProjectionService.syncActiveTasksForInstance(taskEntity.getInstanceId());
            if (actionResult.finishInstanceResultState() == BpmInstanceResultStateEnum.APPROVED
                    && activeTaskCount == 0) {
                finishInstance(taskEntity.getInstanceId(), BpmInstanceResultStateEnum.APPROVED);
            }
        } else if (actionResult.finishInstanceResultState() == BpmInstanceResultStateEnum.REJECTED) {
            finishInstance(taskEntity.getInstanceId(), BpmInstanceResultStateEnum.REJECTED);
        }

        ResponseDTO<String> copyResponse = bpmInstanceCopyService.createManualCopies(
                taskEntity,
                copyEmployeeIds,
                commentText,
                copyTypeEnum
        );
        return copyResponse.getOk() ? ResponseDTO.ok() : copyResponse;
    }

    private void closeOtherPendingTasks(
            Long instanceId,
            Long currentTaskId,
            BpmTaskResultEnum closeResult,
            LocalDateTime actionAt
    ) {
        List<BpmTaskEntity> pendingTasks = bpmTaskDao.selectList(Wrappers.<BpmTaskEntity>lambdaQuery()
                .eq(BpmTaskEntity::getInstanceId, instanceId)
                .eq(BpmTaskEntity::getTaskState, BpmTaskStateEnum.PENDING.getValue()));
        if (pendingTasks == null) {
            return;
        }
        for (BpmTaskEntity pendingTask : pendingTasks) {
            if (Objects.equals(pendingTask.getTaskId(), currentTaskId)) {
                continue;
            }
            BpmTaskEntity updateTask = new BpmTaskEntity();
            updateTask.setTaskId(pendingTask.getTaskId());
            updateTask.setTaskState(BpmTaskStateEnum.CANCELLED.getValue());
            updateTask.setTaskResult(closeResult.getValue());
            updateTask.setCancelledAt(actionAt);
            updateTask.setLastActionAt(actionAt);
            bpmTaskDao.updateById(updateTask);
        }
    }

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

    private void updateTaskAssigneeAndWriteLog(
            BpmTaskEntity taskEntity,
            BpmEmployeeSnapshot actorSnapshot,
            String actionType,
            String commentText,
            BpmEmployeeSnapshot targetSnapshot
    ) {
        LocalDateTime now = LocalDateTime.now();
        BpmTaskEntity updateTaskEntity = new BpmTaskEntity();
        updateTaskEntity.setTaskId(taskEntity.getTaskId());
        updateTaskEntity.setAssigneeEmployeeId(targetSnapshot.employeeId());
        updateTaskEntity.setAssigneeNameSnapshot(targetSnapshot.actualName());
        updateTaskEntity.setAssigneeDepartmentIdSnapshot(targetSnapshot.departmentId());
        updateTaskEntity.setAssigneeDepartmentNameSnapshot(targetSnapshot.departmentName());
        updateTaskEntity.setRuntimeAssignmentSnapshotJson("{\"assigneeEmployeeId\":" + targetSnapshot.employeeId() + "}");
        updateTaskEntity.setLastActionAt(now);
        bpmTaskDao.updateById(updateTaskEntity);

        bpmTaskActionLogDao.insert(buildActionLog(
                taskEntity,
                actorSnapshot,
                actionType,
                commentText,
                taskEntity.getAssigneeEmployeeId(),
                targetSnapshot.employeeId(),
                now
        ));
    }

    private BpmTaskActionLogEntity buildActionLog(
            BpmTaskEntity taskEntity,
            BpmEmployeeSnapshot actorSnapshot,
            String actionType,
            String commentText,
            Long fromAssigneeEmployeeId,
            Long toAssigneeEmployeeId,
            LocalDateTime actionAt
    ) {
        BpmTaskActionLogEntity actionLogEntity = new BpmTaskActionLogEntity();
        actionLogEntity.setInstanceId(taskEntity.getInstanceId());
        actionLogEntity.setTaskId(taskEntity.getTaskId());
        actionLogEntity.setDefinitionId(taskEntity.getDefinitionId());
        actionLogEntity.setDefinitionNodeId(taskEntity.getDefinitionNodeId());
        actionLogEntity.setEngineTaskId(taskEntity.getEngineTaskId());
        actionLogEntity.setActionType(actionType);
        actionLogEntity.setActorEmployeeId(actorSnapshot.employeeId());
        actionLogEntity.setActorNameSnapshot(actorSnapshot.actualName());
        actionLogEntity.setFromAssigneeEmployeeId(fromAssigneeEmployeeId);
        actionLogEntity.setToAssigneeEmployeeId(toAssigneeEmployeeId);
        actionLogEntity.setCommentText(commentText);
        actionLogEntity.setActionAt(actionAt);
        return actionLogEntity;
    }
}
