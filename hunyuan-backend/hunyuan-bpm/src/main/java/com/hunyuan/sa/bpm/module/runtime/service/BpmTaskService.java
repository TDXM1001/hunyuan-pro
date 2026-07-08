package com.hunyuan.sa.bpm.module.runtime.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartPageUtil;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmCopyTypeEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceResultStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskResultEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableTaskGateway;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskApproveForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskRejectForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskReturnForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskTransferForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

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
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Resource
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    @Resource
    private BpmTaskProjectionService bpmTaskProjectionService;

    @Resource
    private BpmInstanceCopyService bpmInstanceCopyService;

    public ResponseDTO<PageResult<BpmTaskVO>> queryAdminPage(BpmTaskQueryForm queryForm) {
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<BpmTaskVO> list = bpmTaskDao.queryPage(page, queryForm);
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
        return ResponseDTO.ok(detailVO);
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
        BpmInstanceEntity instanceEntity = bpmInstanceDao.selectById(taskEntity.getInstanceId());
        if (instanceEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot employeeSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        flowableTaskGateway.complete(taskEntity.getEngineTaskId());

        LocalDateTime now = LocalDateTime.now();
        BpmTaskEntity updateTaskEntity = new BpmTaskEntity();
        updateTaskEntity.setTaskId(taskEntity.getTaskId());
        updateTaskEntity.setTaskState(BpmTaskStateEnum.COMPLETED.getValue());
        updateTaskEntity.setTaskResult(BpmTaskResultEnum.RETURNED.getValue());
        updateTaskEntity.setCompletedAt(now);
        updateTaskEntity.setLastActionAt(now);
        bpmTaskDao.updateById(updateTaskEntity);

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
                "TRANSFERRED",
                transferForm.getCommentText(),
                taskEntity.getAssigneeEmployeeId(),
                targetSnapshot.employeeId(),
                now
        ));
        return ResponseDTO.ok();
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
        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot employeeSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        flowableTaskGateway.complete(taskEntity.getEngineTaskId());

        LocalDateTime now = LocalDateTime.now();
        BpmTaskEntity updateTaskEntity = new BpmTaskEntity();
        updateTaskEntity.setTaskId(taskEntity.getTaskId());
        updateTaskEntity.setTaskState(BpmTaskStateEnum.COMPLETED.getValue());
        updateTaskEntity.setTaskResult(resultEnum.getValue());
        updateTaskEntity.setCompletedAt(now);
        updateTaskEntity.setLastActionAt(now);
        bpmTaskDao.updateById(updateTaskEntity);

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
