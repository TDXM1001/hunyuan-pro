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
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmApprovalSubjectViewService;
import com.hunyuan.sa.bpm.common.enumeration.BpmApprovalGroupCloseReasonEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmCopyTypeEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceResultStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskResultEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskAction;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.engine.internal.FlowableTaskGateway;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmAdminTaskTransferForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskAddSignForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskApproveForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskCompleteForm;
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

    @Resource
    private BpmApprovalStageCommandService bpmApprovalStageCommandService;

    @Resource
    private BpmTaskFormContextService bpmTaskFormContextService;

    @Resource
    private BpmFormDataMutationService bpmFormDataMutationService;

    @Resource
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;

    @Resource
    private BpmTaskActionPolicy bpmTaskActionPolicy;

    @Resource
    private BpmApprovalSubjectViewService bpmApprovalSubjectViewService;

    @Resource
    private BpmSubProcessService bpmSubProcessService;

    public ResponseDTO<PageResult<BpmTaskVO>> queryAdminPage(BpmTaskQueryForm queryForm) {
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<BpmTaskVO> list = bpmTaskDao.queryPage(page, queryForm);
        attachApprovalGroupSummaries(list);
        attachTaskActions(list);
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
        detailVO.setTaskVersion(taskEntity.getTaskVersion());
        if (bpmTaskActionPolicy != null) {
            BpmTaskActionPolicy.TaskActions actions = bpmTaskActionPolicy.describe(taskEntity);
            detailVO.setTaskKind(actions.taskKind());
            detailVO.setAvailableActions(actions.availableActions().stream().map(Enum::name).toList());
        }
        detailVO.setAssignedAt(taskEntity.getAssignedAt());
        detailVO.setDueAt(taskEntity.getDueAt());
        detailVO.setCompletedAt(taskEntity.getCompletedAt());
        detailVO.setActionLogs(bpmTaskActionLogDao.queryByInstanceId(taskEntity.getInstanceId()));
        if (taskEntity.getApprovalGroupId() != null) {
            detailVO.setApprovalGroup(
                    bpmApprovalGroupService.getDetailById(taskEntity.getApprovalGroupId())
            );
        }
        BpmInstanceEntity instanceEntity = bpmInstanceDao == null
                ? null
                : bpmInstanceDao.selectById(taskEntity.getInstanceId());
        if (instanceEntity != null && bpmTaskFormContextService != null) {
            detailVO.setFormContext(
                    bpmTaskFormContextService.buildForEmployeeTask(taskEntity, instanceEntity)
            );
        }
        if (instanceEntity != null && instanceEntity.getApprovalSubjectSnapshotId() != null
                && bpmApprovalSubjectViewService != null) {
            detailVO.setApprovalSubjectContext(
                    bpmApprovalSubjectViewService.buildForTask(taskEntity, instanceEntity)
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

    private void attachTaskActions(List<BpmTaskVO> tasks) {
        if (tasks == null || tasks.isEmpty() || bpmTaskActionPolicy == null) {
            return;
        }
        Map<Long, BpmTaskEntity> entities = bpmTaskDao.selectBatchIds(
                        tasks.stream().map(BpmTaskVO::getTaskId).filter(Objects::nonNull).toList()
                ).stream()
                .collect(java.util.stream.Collectors.toMap(BpmTaskEntity::getTaskId, item -> item));
        for (BpmTaskVO task : tasks) {
            BpmTaskEntity entity = entities.get(task.getTaskId());
            if (entity == null) {
                continue;
            }
            BpmTaskActionPolicy.TaskActions actions = bpmTaskActionPolicy.describe(entity);
            task.setTaskKind(actions.taskKind());
            task.setAvailableActions(actions.availableActions().stream().map(Enum::name).toList());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> approve(BpmTaskApproveForm approveForm) {
        ResponseDTO<String> versionResponse = requireTaskVersion(approveForm.getTaskId(), approveForm.getTaskVersion());
        if (versionResponse != null) {
            return versionResponse;
        }
        ResponseDTO<String> approvalStageResponse = executeApprovalStageMemberActionIfPresent(
                approveForm.getTaskId(),
                "APPROVE",
                approveForm.getCommentText(),
                approveForm.getRequestId(),
                approveForm.getFormDataVersion(),
                approveForm.getFormDataPatchJson(),
                approveForm.getActionAttachmentsJson()
        );
        if (approvalStageResponse != null) {
            return approvalStageResponse;
        }
        ResponseDTO<String> policyResponse = requireAllowed(approveForm.getTaskId(), BpmTaskAction.APPROVE);
        if (policyResponse != null) {
            return policyResponse;
        }
        ResponseDTO<String> taskKindResponse = rejectApprovalActionForHandleTask(
                approveForm.getTaskId(),
                "审批通过"
        );
        if (taskKindResponse != null) {
            return taskKindResponse;
        }
        return completeTask(
                approveForm.getTaskId(),
                approveForm.getCommentText(),
                BpmTaskResultEnum.APPROVED,
                "APPROVED",
                approveForm.getCopyEmployeeIds(),
                BpmCopyTypeEnum.MANUAL_APPROVE_COPY,
                approveForm.getFormDataVersion(),
                approveForm.getFormDataPatchJson()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> reject(BpmTaskRejectForm rejectForm) {
        ResponseDTO<String> versionResponse = requireTaskVersion(rejectForm.getTaskId(), rejectForm.getTaskVersion());
        if (versionResponse != null) {
            return versionResponse;
        }
        ResponseDTO<String> approvalStageResponse = executeApprovalStageMemberActionIfPresent(
                rejectForm.getTaskId(),
                "REJECT",
                rejectForm.getCommentText(),
                rejectForm.getRequestId(),
                rejectForm.getWorkingDataVersion(),
                rejectForm.getWorkingDataPatchJson(),
                rejectForm.getActionAttachmentsJson()
        );
        if (approvalStageResponse != null) {
            return approvalStageResponse;
        }
        ResponseDTO<String> policyResponse = requireAllowed(rejectForm.getTaskId(), BpmTaskAction.REJECT);
        if (policyResponse != null) {
            return policyResponse;
        }
        ResponseDTO<String> taskKindResponse = rejectApprovalActionForHandleTask(
                rejectForm.getTaskId(),
                "审批拒绝"
        );
        if (taskKindResponse != null) {
            return taskKindResponse;
        }
        return completeTask(
                rejectForm.getTaskId(),
                rejectForm.getCommentText(),
                BpmTaskResultEnum.REJECTED,
                "REJECTED",
                rejectForm.getCopyEmployeeIds(),
                BpmCopyTypeEnum.MANUAL_REJECT_COPY,
                null,
                null
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> returnToInitiator(BpmTaskReturnForm returnForm) {
        BpmTaskEntity taskEntity = bpmTaskDao.selectById(returnForm.getTaskId());
        if (taskEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        ResponseDTO<String> versionResponse = requireTaskVersion(taskEntity, returnForm.getTaskVersion());
        if (versionResponse != null) {
            return versionResponse;
        }
        ResponseDTO<String> approvalStageResponse = executeApprovalStageMemberActionIfPresent(
                returnForm.getTaskId(),
                "RETURN",
                returnForm.getCommentText(),
                returnForm.getRequestId(),
                returnForm.getWorkingDataVersion(),
                returnForm.getWorkingDataPatchJson(),
                returnForm.getActionAttachmentsJson()
        );
        if (approvalStageResponse != null) {
            return approvalStageResponse;
        }
        ResponseDTO<String> policyResponse = requireAllowed(taskEntity, BpmTaskAction.RETURN);
        if (policyResponse != null) {
            return policyResponse;
        }
        if (taskEntity.getApprovalGroupId() != null) {
            return handleApprovalGroupAction(
                    taskEntity,
                    BpmApprovalMemberAction.RETURN,
                    returnForm.getCommentText(),
                    returnForm.getCopyEmployeeIds(),
                    BpmCopyTypeEnum.MANUAL_RETURN_COPY,
                    null,
                    null
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
        updateTaskEntity.setTaskVersion(nextTaskVersion(taskEntity));
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
        ResponseDTO<String> stageMemberResponse = rejectGenericActionForApprovalStageMember(taskEntity);
        if (stageMemberResponse != null) {
            return stageMemberResponse;
        }
        ResponseDTO<String> policyResponse = requireAllowed(taskEntity, BpmTaskAction.TRANSFER);
        if (policyResponse != null) {
            return policyResponse;
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
        ResponseDTO<String> stageMemberResponse = rejectGenericActionForApprovalStageMember(taskEntity);
        if (stageMemberResponse != null) {
            return stageMemberResponse;
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
        ResponseDTO<String> stageMemberResponse = rejectGenericActionForApprovalStageMember(taskEntity);
        if (stageMemberResponse != null) {
            return stageMemberResponse;
        }
        ResponseDTO<String> policyResponse = requireAllowed(taskEntity, BpmTaskAction.ADD_SIGN);
        if (policyResponse != null) {
            return policyResponse;
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
        ResponseDTO<String> stageMemberResponse = rejectGenericActionForApprovalStageMember(taskEntity);
        if (stageMemberResponse != null) {
            return stageMemberResponse;
        }
        ResponseDTO<String> policyResponse = requireAllowed(taskEntity, BpmTaskAction.REDUCE_SIGN);
        if (policyResponse != null) {
            return policyResponse;
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
        ResponseDTO<String> stageMemberResponse = rejectGenericActionForApprovalStageMember(taskEntity);
        if (stageMemberResponse != null) {
            return stageMemberResponse;
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
        ResponseDTO<String> stageMemberResponse = rejectGenericActionForApprovalStageMember(taskEntity);
        if (stageMemberResponse != null) {
            return stageMemberResponse;
        }
        if (requireCurrentAssignee) {
            ResponseDTO<String> policyResponse = requireAllowed(taskEntity, BpmTaskAction.DELEGATE);
            if (policyResponse != null) {
                return policyResponse;
            }
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
            BpmCopyTypeEnum copyTypeEnum,
            Long formDataVersion,
            String formDataPatchJson
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
                    copyTypeEnum,
                    formDataVersion,
                    formDataPatchJson
            );
        }
        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot employeeSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        if (!BpmTaskStateEnum.PENDING.equalsValue(taskEntity.getTaskState())
                || !Objects.equals(taskEntity.getAssigneeEmployeeId(), employeeId)) {
            return ResponseDTO.userErrorParam("只能处理自己的待办任务");
        }
        if (hasFormMutation(formDataVersion, formDataPatchJson)) {
            taskEntity = bpmTaskDao.selectByIdForUpdate(taskId);
            BpmInstanceEntity instanceEntity = bpmInstanceDao.selectByIdForUpdate(taskEntity.getInstanceId());
            if (instanceEntity == null) {
                return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
            }
            ResponseDTO<BpmFormDataMutationService.MutationResult> mutationResponse =
                    bpmFormDataMutationService.applyTaskApprovePatch(
                            taskEntity,
                            instanceEntity,
                            employeeSnapshot,
                            formDataVersion,
                            formDataPatchJson
                    );
            if (!Boolean.TRUE.equals(mutationResponse.getOk())) {
                return ResponseDTO.userErrorParam(mutationResponse.getMsg());
            }
        }
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
        updateTaskEntity.setTaskVersion(nextTaskVersion(taskEntity));
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

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> completeHandleTask(BpmTaskCompleteForm completeForm) {
        BpmTaskEntity task = bpmTaskDao.selectById(completeForm.getTaskId());
        if (task == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        ResponseDTO<String> versionResponse = requireTaskVersion(task, completeForm.getTaskVersion());
        if (versionResponse != null) {
            return versionResponse;
        }
        ResponseDTO<String> policyResponse = requireAllowed(task, BpmTaskAction.COMPLETE);
        if (policyResponse != null) {
            return policyResponse;
        }
        if (!isHandleTask(task)) {
            return ResponseDTO.userErrorParam("只有办理任务可以使用办理完成动作");
        }
        return completeTask(
                completeForm.getTaskId(),
                completeForm.getCommentText(),
                BpmTaskResultEnum.HANDLED,
                "HANDLE_COMPLETED",
                List.of(),
                BpmCopyTypeEnum.DESIGN_NODE_COPY,
                null,
                null
        );
    }

    /**
     * 执行发布时已冻结的系统超时动作；调用方必须位于运行时命令边界内。
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> executeSystemTimeoutAction(Long taskId, String action, String commentText, Long adminEmployeeId) {
        BpmTaskEntity taskLookup = bpmTaskDao.selectById(taskId);
        if (taskLookup == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (!"AUTO_APPROVE".equals(action) && !"AUTO_REJECT".equals(action) && !"ASSIGN_ADMIN".equals(action)) {
            return ResponseDTO.userErrorParam("不支持的系统超时动作");
        }
        if (!BpmTaskStateEnum.PENDING.equalsValue(taskLookup.getTaskState())) {
            return ResponseDTO.ok("任务已由其他动作处理");
        }
        BpmEmployeeSnapshot systemActor = new BpmEmployeeSnapshot(
                taskLookup.getAssigneeEmployeeId(),
                "系统自动动作",
                taskLookup.getAssigneeDepartmentIdSnapshot(),
                taskLookup.getAssigneeDepartmentNameSnapshot(),
                null,
                null
        );
        if ("ASSIGN_ADMIN".equals(action)) {
            if (adminEmployeeId == null || adminEmployeeId <= 0) {
                return ResponseDTO.userErrorParam("SLA 转管理员缺少目标员工");
            }
            BpmTaskEntity task = bpmTaskDao.selectByIdForUpdate(taskId);
            if (task == null || !BpmTaskStateEnum.PENDING.equalsValue(task.getTaskState())) {
                return ResponseDTO.ok("任务已由其他动作处理");
            }
            BpmEmployeeSnapshot target = bpmOrgIdentityGateway.requireEmployee(adminEmployeeId);
            flowableTaskGateway.transfer(task.getEngineTaskId(), adminEmployeeId);
            LocalDateTime now = LocalDateTime.now();
            BpmTaskEntity update = new BpmTaskEntity();
            update.setTaskId(taskId);
            update.setAssigneeEmployeeId(target.employeeId());
            update.setAssigneeNameSnapshot(target.actualName());
            update.setAssigneeDepartmentIdSnapshot(target.departmentId());
            update.setAssigneeDepartmentNameSnapshot(target.departmentName());
            update.setLastActionAt(now);
            bpmTaskDao.updateById(update);
            bpmTaskActionLogDao.insert(buildActionLog(
                    task, systemActor, "SYSTEM_SLA_ASSIGNED_ADMIN", commentText,
                    task.getAssigneeEmployeeId(), target.employeeId(), now
            ));
            return ResponseDTO.ok();
        }
        BpmTaskResultEnum result = "AUTO_REJECT".equals(action)
                ? BpmTaskResultEnum.REJECTED
                : BpmTaskResultEnum.APPROVED;
        if (taskLookup.getApprovalGroupId() != null) {
            BpmApprovalGroupActionResult actionResult = bpmApprovalGroupService.handleMemberAction(
                    taskId,
                    result == BpmTaskResultEnum.REJECTED
                            ? BpmApprovalMemberAction.REJECT
                            : BpmApprovalMemberAction.APPROVE,
                    systemActor,
                    commentText
            );
            if (!actionResult.processed()) {
                return ResponseDTO.ok("任务已由其他动作处理");
            }
            int activeTaskCount = bpmTaskProjectionService.syncActiveTasksForInstance(taskLookup.getInstanceId());
            if (actionResult.finishInstanceResultState() == BpmInstanceResultStateEnum.REJECTED) {
                closeOtherPendingTasks(taskLookup.getInstanceId(), taskId, BpmTaskResultEnum.REJECTED, LocalDateTime.now());
                finishInstance(taskLookup.getInstanceId(), BpmInstanceResultStateEnum.REJECTED);
            } else if (actionResult.finishInstanceResultState() == BpmInstanceResultStateEnum.APPROVED
                    && activeTaskCount == 0) {
                finishInstance(taskLookup.getInstanceId(), BpmInstanceResultStateEnum.APPROVED);
            }
            return ResponseDTO.ok();
        }

        BpmTaskEntity task = bpmTaskDao.selectByIdForUpdate(taskId);
        if (task == null || !BpmTaskStateEnum.PENDING.equalsValue(task.getTaskState())) {
            return ResponseDTO.ok("任务已由其他动作处理");
        }
        if (result == BpmTaskResultEnum.REJECTED) {
            flowableProcessInstanceGateway.cancel(task.getEngineProcessInstanceId(), "SLA 超时自动拒绝");
        } else {
            flowableTaskGateway.complete(task.getEngineTaskId());
        }
        LocalDateTime now = LocalDateTime.now();
        BpmTaskEntity update = new BpmTaskEntity();
        update.setTaskId(taskId);
        update.setTaskState(BpmTaskStateEnum.COMPLETED.getValue());
        update.setTaskResult(result.getValue());
        update.setCompletedAt(now);
        update.setLastActionAt(now);
        bpmTaskDao.updateById(update);
        bpmTaskActionLogDao.insert(buildActionLog(
                task,
                systemActor,
                result == BpmTaskResultEnum.REJECTED ? "SYSTEM_AUTO_REJECTED" : "SYSTEM_AUTO_APPROVED",
                commentText,
                task.getAssigneeEmployeeId(),
                task.getAssigneeEmployeeId(),
                now
        ));
        int activeTaskCount = bpmTaskProjectionService.syncActiveTasksForInstance(task.getInstanceId());
        if (result == BpmTaskResultEnum.REJECTED) {
            closeOtherPendingTasks(task.getInstanceId(), taskId, BpmTaskResultEnum.REJECTED, now);
            finishInstance(task.getInstanceId(), BpmInstanceResultStateEnum.REJECTED);
        } else if (activeTaskCount == 0) {
            finishInstance(task.getInstanceId(), BpmInstanceResultStateEnum.APPROVED);
        }
        return ResponseDTO.ok();
    }

    private ResponseDTO<String> rejectApprovalActionForHandleTask(Long taskId, String actionLabel) {
        BpmTaskEntity task = bpmTaskDao.selectById(taskId);
        if (task == null) {
            return null;
        }
        if (isHandleTask(task)) {
            return ResponseDTO.userErrorParam("办理任务不支持" + actionLabel + "，请使用办理完成动作");
        }
        return null;
    }

    private ResponseDTO<String> executeApprovalStageMemberActionIfPresent(
            Long taskId,
            String action,
            String commentText,
            String requestId,
            Long expectedWorkingDataVersion,
            String workingDataPatchJson,
            String attachmentsJson
    ) {
        BpmTaskEntity task = bpmTaskDao.selectById(taskId);
        if (task == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (task.getApprovalStageId() == null && task.getApprovalStageMemberId() == null) {
            return null;
        }
        return bpmApprovalStageCommandService.execute(
                taskId, action, commentText, requestId,
                expectedWorkingDataVersion, workingDataPatchJson, attachmentsJson
        );
    }

    private ResponseDTO<String> rejectGenericActionForApprovalStageMember(BpmTaskEntity task) {
        if (task.getApprovalStageId() == null && task.getApprovalStageMemberId() == null) {
            return null;
        }
        return ResponseDTO.userErrorParam("M2 审批阶段成员不支持通用高级动作，请使用受控阶段治理命令");
    }

    private ResponseDTO<String> requireAllowed(Long taskId, BpmTaskAction action) {
        BpmTaskEntity task = bpmTaskDao.selectById(taskId);
        if (task == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return requireAllowed(task, action);
    }

    private ResponseDTO<String> requireTaskVersion(Long taskId, Long expectedTaskVersion) {
        BpmTaskEntity task = bpmTaskDao.selectById(taskId);
        if (task == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return requireTaskVersion(task, expectedTaskVersion);
    }

    private ResponseDTO<String> requireTaskVersion(BpmTaskEntity task, Long expectedTaskVersion) {
        if (expectedTaskVersion == null) {
            return null;
        }
        long actual = task.getTaskVersion() == null ? 1L : task.getTaskVersion();
        return expectedTaskVersion.longValue() == actual
                ? null
                : ResponseDTO.userErrorParam("TASK_VERSION_CONFLICT：任务已被其他动作更新，请刷新后重试");
    }

    private Long nextTaskVersion(BpmTaskEntity task) {
        return (task.getTaskVersion() == null ? 1L : task.getTaskVersion()) + 1L;
    }

    private ResponseDTO<String> requireAllowed(BpmTaskEntity task, BpmTaskAction action) {
        if (bpmTaskActionPolicy == null) {
            return null;
        }
        try {
            bpmTaskActionPolicy.requireAllowed(task, action);
            return null;
        } catch (IllegalArgumentException ex) {
            return ResponseDTO.userErrorParam(ex.getMessage());
        }
    }

    private boolean isHandleTask(BpmTaskEntity task) {
        if (task.getDefinitionNodeId() == null) {
            return false;
        }
        BpmDefinitionNodeEntity node = bpmDefinitionNodeDao.selectById(task.getDefinitionNodeId());
        return node != null && "HANDLE_TASK".equals(node.getNodeType());
    }

    private ResponseDTO<String> handleApprovalGroupAction(
            BpmTaskEntity taskEntity,
            BpmApprovalMemberAction action,
            String commentText,
            Collection<Long> copyEmployeeIds,
            BpmCopyTypeEnum copyTypeEnum,
            Long formDataVersion,
            String formDataPatchJson
    ) {
        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot actorSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        BpmApprovalGroupActionResult actionResult;
        try {
            actionResult = bpmApprovalGroupService.handleMemberAction(
                    taskEntity.getTaskId(),
                    action,
                    actorSnapshot,
                    commentText,
                    hasFormMutation(formDataVersion, formDataPatchJson)
                            ? () -> applyLockedGroupFormMutation(
                            taskEntity,
                            actorSnapshot,
                            formDataVersion,
                            formDataPatchJson
                    )
                            : null
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

    private ResponseDTO<String> applyLockedGroupFormMutation(
            BpmTaskEntity taskEntity,
            BpmEmployeeSnapshot actorSnapshot,
            Long formDataVersion,
            String formDataPatchJson
    ) {
        BpmInstanceEntity instanceEntity = bpmInstanceDao.selectByIdForUpdate(taskEntity.getInstanceId());
        if (instanceEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        ResponseDTO<BpmFormDataMutationService.MutationResult> response =
                bpmFormDataMutationService.applyTaskApprovePatch(
                        taskEntity,
                        instanceEntity,
                        actorSnapshot,
                        formDataVersion,
                        formDataPatchJson
                );
        return Boolean.TRUE.equals(response.getOk())
                ? ResponseDTO.ok()
                : ResponseDTO.userErrorParam(response.getMsg());
    }

    private boolean hasFormMutation(Long formDataVersion, String formDataPatchJson) {
        return formDataVersion != null || StringUtils.hasText(formDataPatchJson);
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
        if (BpmInstanceResultStateEnum.REJECTED == resultStateEnum) {
            bpmSubProcessService.propagateChildRejection(instanceId, "子流程审批拒绝");
        }
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
        event.setFinalFormDataVersion(
                instanceEntity.getFormDataVersion() == null ? 1L : instanceEntity.getFormDataVersion()
        );
        event.setFinalFormDataJson(
                StringUtils.hasText(instanceEntity.getCurrentFormDataSnapshotJson())
                        ? instanceEntity.getCurrentFormDataSnapshotJson()
                        : "{}"
        );
        event.setFormDataLastModifiedAt(instanceEntity.getUpdateTime());
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
