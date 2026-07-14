package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.serialnumber.service.SerialNumberService;
import com.hunyuan.sa.bpm.api.business.BpmBusinessProcessApi;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessResultEvent;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmCopyTypeEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceResultStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskResultEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.engine.internal.FlowableTaskGateway;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.WorkingDataMutationCommand;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.WorkingDataMutationResult;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmApprovalDataMutationService;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmFormDataChangeDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmFormDataChangeEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceResubmitForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceStartForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskApproveForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskRejectForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskReturnForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskTransferForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmRuntimeStartDraftVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalGroupService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceCopyService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTimeEventService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmExternalWaitService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmSubProcessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmRuntimeCommandServiceTest {

    private BpmInstanceService bpmInstanceService;

    private BpmTaskService bpmTaskService;

    private BpmInstanceDao bpmInstanceDao;

    private BpmTaskDao bpmTaskDao;

    private BpmTaskActionLogDao bpmTaskActionLogDao;

    private BpmInstanceCopyService bpmInstanceCopyService;

    private BpmBusinessProcessApi bpmBusinessProcessApi;

    private BpmFormDataChangeDao bpmFormDataChangeDao;

    private BpmApprovalDataMutationService bpmApprovalDataMutationService;

    @BeforeEach
    void setUp() {
        bpmInstanceService = new BpmInstanceService();
        bpmTaskService = new BpmTaskService();
        bpmInstanceDao = Mockito.mock(BpmInstanceDao.class);
        bpmTaskDao = Mockito.mock(BpmTaskDao.class);
        bpmTaskActionLogDao = Mockito.mock(BpmTaskActionLogDao.class);
        bpmInstanceCopyService = Mockito.mock(BpmInstanceCopyService.class);
        bpmBusinessProcessApi = Mockito.mock(BpmBusinessProcessApi.class);
        bpmFormDataChangeDao = Mockito.mock(BpmFormDataChangeDao.class);
        bpmApprovalDataMutationService = Mockito.mock(BpmApprovalDataMutationService.class);

        setField(bpmInstanceService, "graphDefinitionVersionDao", Mockito.mock(GraphDefinitionVersionDao.class));
        setField(bpmInstanceService, "bpmInstanceDao", bpmInstanceDao);
        setField(bpmInstanceService, "bpmTaskDao", bpmTaskDao);
        setField(bpmInstanceService, "bpmTaskActionLogDao", bpmTaskActionLogDao);
        setField(bpmInstanceService, "flowableProcessInstanceGateway", Mockito.mock(FlowableProcessInstanceGateway.class));
        setField(bpmInstanceService, "bpmCurrentActorProvider", Mockito.mock(BpmCurrentActorProvider.class));
        setField(bpmInstanceService, "bpmOrgIdentityGateway", Mockito.mock(BpmOrgIdentityGateway.class));
        setField(bpmInstanceService, "serialNumberService", Mockito.mock(SerialNumberService.class));
        setField(bpmInstanceService, "bpmTaskProjectionService", Mockito.mock(BpmTaskProjectionService.class));
        setField(bpmInstanceService, "bpmApprovalGroupService", Mockito.mock(BpmApprovalGroupService.class));
        setField(bpmInstanceService, "bpmFormDataChangeDao", bpmFormDataChangeDao);
        setField(bpmInstanceService, "bpmApprovalDataMutationService", bpmApprovalDataMutationService);
        setField(bpmInstanceService, "bpmTimeEventService", Mockito.mock(BpmTimeEventService.class));
        setField(bpmInstanceService, "bpmExternalWaitService", Mockito.mock(BpmExternalWaitService.class));
        setField(bpmInstanceService, "bpmSubProcessService", Mockito.mock(BpmSubProcessService.class));

        setField(bpmTaskService, "bpmTaskDao", bpmTaskDao);
        setField(bpmTaskService, "bpmDefinitionNodeDao", Mockito.mock(BpmDefinitionNodeDao.class));
        setField(bpmTaskService, "bpmInstanceDao", bpmInstanceDao);
        setField(bpmTaskService, "bpmTaskActionLogDao", bpmTaskActionLogDao);
        setField(bpmTaskService, "flowableTaskGateway", Mockito.mock(FlowableTaskGateway.class));
        setField(bpmTaskService, "flowableProcessInstanceGateway", processInstanceGateway());
        setField(bpmTaskService, "bpmCurrentActorProvider", Mockito.mock(BpmCurrentActorProvider.class));
        setField(bpmTaskService, "bpmOrgIdentityGateway", Mockito.mock(BpmOrgIdentityGateway.class));
        setField(bpmTaskService, "bpmTaskProjectionService", Mockito.mock(BpmTaskProjectionService.class));
        setField(bpmTaskService, "bpmInstanceCopyService", bpmInstanceCopyService);
        setField(bpmTaskService, "bpmBusinessProcessApi", bpmBusinessProcessApi);
        setField(bpmTaskService, "bpmApprovalGroupService", Mockito.mock(BpmApprovalGroupService.class));
        setField(bpmTaskService, "bpmSubProcessService", Mockito.mock(BpmSubProcessService.class));
        when(bpmInstanceCopyService.createManualCopies(any(), any(), any(), any())).thenReturn(ResponseDTO.ok());
    }

    @Test
    void returnToInitiatorShouldMoveInstanceToWaitResubmitInsteadOfRejectingIt() {
        BpmTaskEntity taskEntity = new BpmTaskEntity();
        taskEntity.setTaskId(1L);
        taskEntity.setInstanceId(8L);
        taskEntity.setDefinitionId(2L);
        taskEntity.setDefinitionNodeId(5L);
        taskEntity.setEngineTaskId("task-1");
        taskEntity.setEngineProcessInstanceId("process-8");
        taskEntity.setTaskState(BpmTaskStateEnum.PENDING.getValue());
        taskEntity.setAssigneeEmployeeId(10L);

        BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
        instanceEntity.setInstanceId(8L);
        instanceEntity.setRunState(BpmInstanceRunStateEnum.RUNNING.getValue());

        when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
        when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
        BpmTaskEntity otherPendingAddSignTask = new BpmTaskEntity();
        otherPendingAddSignTask.setTaskId(2L);
        otherPendingAddSignTask.setInstanceId(8L);
        otherPendingAddSignTask.setTaskState(BpmTaskStateEnum.PENDING.getValue());
        otherPendingAddSignTask.setRuntimeAssignmentSnapshotJson(
                "{\"addSign\":true,\"sourceTaskId\":1}"
        );
        when(bpmTaskDao.selectList(any())).thenReturn(List.of(otherPendingAddSignTask));
        when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
        when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));

        BpmTaskReturnForm form = new BpmTaskReturnForm();
        form.setTaskId(1L);
        form.setCommentText("请补齐附件");

        ResponseDTO<String> response = bpmTaskService.returnToInitiator(form);

        assertThat(response.getOk()).isTrue();
        verify(processInstanceGateway()).cancel("process-8", "审批退回发起人");
        verify(taskGateway(), never()).complete("task-1");

        ArgumentCaptor<BpmTaskEntity> taskCaptor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(bpmTaskDao, Mockito.times(2)).updateById(taskCaptor.capture());
        assertThat(taskCaptor.getAllValues()).anySatisfy(update -> {
            assertThat(update.getTaskId()).isEqualTo(1L);
            assertThat(update.getTaskState()).isEqualTo(BpmTaskStateEnum.COMPLETED.getValue());
            assertThat(update.getTaskResult()).isEqualTo(BpmTaskResultEnum.RETURNED.getValue());
        });
        verify(bpmTaskDao).updateById(argThat((BpmTaskEntity update) ->
                update.getTaskId().equals(otherPendingAddSignTask.getTaskId())
                        && update.getTaskState().equals(BpmTaskStateEnum.CANCELLED.getValue())
                        && update.getTaskResult().equals(BpmTaskResultEnum.RETURNED.getValue())));

        ArgumentCaptor<BpmInstanceEntity> instanceCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(bpmInstanceDao).updateById(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getInstanceId()).isEqualTo(8L);
        assertThat(instanceCaptor.getValue().getRunState()).isEqualTo(BpmInstanceRunStateEnum.WAIT_RESUBMIT.getValue());
        assertThat(instanceCaptor.getValue().getResultState()).isNull();
        assertThat(instanceCaptor.getValue().getActiveTaskCount()).isEqualTo(0);

        ArgumentCaptor<BpmTaskActionLogEntity> logCaptor = ArgumentCaptor.forClass(BpmTaskActionLogEntity.class);
        verify(bpmTaskActionLogDao).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("RETURNED_TO_INITIATOR");
        assertThat(logCaptor.getValue().getCommentText()).isEqualTo("请补齐附件");
    }

    @Test
    void transferShouldWriteActionLogAndReassignTask() {
        BpmTaskEntity taskEntity = new BpmTaskEntity();
        taskEntity.setTaskId(1L);
        taskEntity.setInstanceId(8L);
        taskEntity.setDefinitionId(2L);
        taskEntity.setDefinitionNodeId(5L);
        taskEntity.setEngineTaskId("task-1");
        taskEntity.setEngineProcessInstanceId("process-8");
        taskEntity.setTaskState(BpmTaskStateEnum.PENDING.getValue());
        taskEntity.setAssigneeEmployeeId(10L);

        when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
        when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
        when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));
        when(taskIdentityGateway().requireEmployee(22L)).thenReturn(new BpmEmployeeSnapshot(22L, "李四", 9L, "财务部", null, null));

        BpmTaskTransferForm form = new BpmTaskTransferForm();
        form.setTaskId(1L);
        form.setToEmployeeId(22L);
        form.setCommentText("转给财务复核");

        ResponseDTO<String> response = bpmTaskService.transfer(form);

        assertThat(response.getOk()).isTrue();
        verify(taskGateway()).transfer("task-1", 22L);

        ArgumentCaptor<BpmTaskEntity> taskCaptor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(bpmTaskDao).updateById(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getAssigneeEmployeeId()).isEqualTo(22L);
        assertThat(taskCaptor.getValue().getAssigneeNameSnapshot()).isEqualTo("李四");
        assertThat(taskCaptor.getValue().getTaskState()).isNull();
        assertThat(taskCaptor.getValue().getTaskResult()).isNull();
        assertThat(taskCaptor.getValue().getRuntimeAssignmentSnapshotJson()).contains("\"assigneeEmployeeId\":22");

        ArgumentCaptor<BpmTaskActionLogEntity> logCaptor = ArgumentCaptor.forClass(BpmTaskActionLogEntity.class);
        verify(bpmTaskActionLogDao).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("TRANSFERRED");
        assertThat(logCaptor.getValue().getFromAssigneeEmployeeId()).isEqualTo(10L);
        assertThat(logCaptor.getValue().getToAssigneeEmployeeId()).isEqualTo(22L);
    }

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

    @Test
    void rejectShouldFinishInstanceAsRejectedWhenNoActiveTaskRemains() {
        BpmTaskEntity taskEntity = new BpmTaskEntity();
        taskEntity.setTaskId(1L);
        taskEntity.setInstanceId(8L);
        taskEntity.setDefinitionId(2L);
        taskEntity.setDefinitionNodeId(5L);
        taskEntity.setEngineTaskId("task-1");
        taskEntity.setEngineProcessInstanceId("process-8");
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
        verify(processInstanceGateway()).cancel("process-8", "审批驳回");
        verify(taskGateway(), never()).complete("task-1");
        ArgumentCaptor<BpmInstanceEntity> instanceCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(bpmInstanceDao).updateById(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getInstanceId()).isEqualTo(8L);
        assertThat(instanceCaptor.getValue().getRunState()).isEqualTo(BpmInstanceRunStateEnum.FINISHED.getValue());
        assertThat(instanceCaptor.getValue().getResultState()).isEqualTo(BpmInstanceResultStateEnum.REJECTED.getValue());
    }

    @Test
    void approveShouldPublishBusinessResultEventWhenLastActiveTaskCompletes() {
        BpmTaskEntity taskEntity = buildPendingTask();
        BpmInstanceEntity instanceEntity = buildBusinessInstance();

        when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
        when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
        when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
        when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));
        when(taskServiceProjectionService().syncActiveTasksForInstance(8L)).thenReturn(0);

        BpmTaskApproveForm form = new BpmTaskApproveForm();
        form.setTaskId(1L);
        form.setCommentText("同意");

        ResponseDTO<String> response = bpmTaskService.approve(form);

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmBusinessResultEvent> eventCaptor = ArgumentCaptor.forClass(BpmBusinessResultEvent.class);
        verify(bpmBusinessProcessApi).publishResultEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo("RESULT:8:1");
        assertThat(eventCaptor.getValue().getInstanceId()).isEqualTo(8L);
        assertThat(eventCaptor.getValue().getBusinessType()).isEqualTo("sample_expense");
        assertThat(eventCaptor.getValue().getBusinessId()).isEqualTo(1001L);
        assertThat(eventCaptor.getValue().getResultState()).isEqualTo(BpmInstanceResultStateEnum.APPROVED.getValue());
        assertThat(eventCaptor.getValue().getPayloadJson()).isNull();
        assertThat(eventCaptor.getValue().getFinalFormDataVersion()).isEqualTo(4L);
        assertThat(eventCaptor.getValue().getFinalFormDataJson()).contains("\"approvedAmount\":98");
        assertThat(eventCaptor.getValue().getFormDataLastModifiedAt()).isNotNull();
        assertThat(eventCaptor.getValue().getOccurredAt()).isNotNull();
    }

    @Test
    void rejectShouldPublishBusinessResultEventWhenInstanceFinishes() {
        BpmTaskEntity taskEntity = buildPendingTask();
        BpmInstanceEntity instanceEntity = buildBusinessInstance();

        when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
        when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
        when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
        when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));
        when(taskServiceProjectionService().syncActiveTasksForInstance(8L)).thenReturn(0);

        BpmTaskRejectForm form = new BpmTaskRejectForm();
        form.setTaskId(1L);
        form.setCommentText("不同意");

        ResponseDTO<String> response = bpmTaskService.reject(form);

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmBusinessResultEvent> eventCaptor = ArgumentCaptor.forClass(BpmBusinessResultEvent.class);
        verify(bpmBusinessProcessApi).publishResultEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo("RESULT:8:2");
        assertThat(eventCaptor.getValue().getResultState()).isEqualTo(BpmInstanceResultStateEnum.REJECTED.getValue());
    }

    @Test
    void finishInstanceShouldNotPublishBusinessResultEventWhenBusinessKeyIsMissing() {
        BpmTaskEntity taskEntity = buildPendingTask();
        BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
        instanceEntity.setInstanceId(8L);

        when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
        when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
        when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
        when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));
        when(taskServiceProjectionService().syncActiveTasksForInstance(8L)).thenReturn(0);

        BpmTaskApproveForm form = new BpmTaskApproveForm();
        form.setTaskId(1L);
        form.setCommentText("同意");

        ResponseDTO<String> response = bpmTaskService.approve(form);

        assertThat(response.getOk()).isTrue();
        verify(bpmBusinessProcessApi, never()).publishResultEvent(any());
    }

    @Test
    void approveShouldCreateManualCopiesWhenCopyEmployeesProvided() {
        BpmTaskEntity taskEntity = buildPendingTask();

        when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
        when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
        when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));
        when(taskServiceProjectionService().syncActiveTasksForInstance(8L)).thenReturn(1);

        BpmTaskApproveForm form = new BpmTaskApproveForm();
        form.setTaskId(1L);
        form.setCommentText("同意");
        form.setCopyEmployeeIds(java.util.List.of(22L, 23L));

        ResponseDTO<String> response = bpmTaskService.approve(form);

        assertThat(response.getOk()).isTrue();
        verify(bpmInstanceCopyService).createManualCopies(
                taskEntity,
                java.util.List.of(22L, 23L),
                "同意",
                BpmCopyTypeEnum.MANUAL_APPROVE_COPY
        );
    }

    @Test
    void rejectShouldCreateManualCopiesWhenCopyEmployeesProvided() {
        BpmTaskEntity taskEntity = buildPendingTask();

        when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
        when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
        when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));
        when(taskServiceProjectionService().syncActiveTasksForInstance(8L)).thenReturn(0);

        BpmTaskRejectForm form = new BpmTaskRejectForm();
        form.setTaskId(1L);
        form.setCommentText("不同意");
        form.setCopyEmployeeIds(java.util.List.of(22L));

        ResponseDTO<String> response = bpmTaskService.reject(form);

        assertThat(response.getOk()).isTrue();
        verify(bpmInstanceCopyService).createManualCopies(
                taskEntity,
                java.util.List.of(22L),
                "不同意",
                BpmCopyTypeEnum.MANUAL_REJECT_COPY
        );
    }

    @Test
    void returnShouldCreateManualCopiesWhenCopyEmployeesProvided() {
        BpmTaskEntity taskEntity = buildPendingTask();
        BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
        instanceEntity.setInstanceId(8L);
        instanceEntity.setRunState(BpmInstanceRunStateEnum.RUNNING.getValue());

        when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
        when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
        when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
        when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));

        BpmTaskReturnForm form = new BpmTaskReturnForm();
        form.setTaskId(1L);
        form.setCommentText("请补材料");
        form.setCopyEmployeeIds(java.util.List.of(23L));

        ResponseDTO<String> response = bpmTaskService.returnToInitiator(form);

        assertThat(response.getOk()).isTrue();
        verify(bpmInstanceCopyService).createManualCopies(
                taskEntity,
                java.util.List.of(23L),
                "请补材料",
                BpmCopyTypeEnum.MANUAL_RETURN_COPY
        );
    }

    @Test
    void cancelMyInstanceShouldTerminateEngineAndClosePendingTasks() {
        BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
        instanceEntity.setInstanceId(8L);
        instanceEntity.setDefinitionId(2L);
        instanceEntity.setEngineProcessInstanceId("process-8");
        instanceEntity.setRunState(BpmInstanceRunStateEnum.RUNNING.getValue());
        instanceEntity.setStartEmployeeId(100L);

        BpmTaskEntity taskEntity = new BpmTaskEntity();
        taskEntity.setTaskId(11L);
        taskEntity.setInstanceId(8L);
        taskEntity.setTaskState(BpmTaskStateEnum.PENDING.getValue());

        when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
        when(instanceCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(instanceIdentityGateway().requireEmployee(100L)).thenReturn(
                new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null)
        );
        when(bpmTaskDao.selectList(any())).thenReturn(java.util.List.of(taskEntity));

        com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceCancelForm form =
                new com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceCancelForm();
        form.setInstanceId(8L);
        form.setCancelReason("发起人撤销");

        ResponseDTO<String> response = bpmInstanceService.cancelMyInstance(form);

        assertThat(response.getOk()).isTrue();
        verify((BpmSubProcessService) getFieldValue(bpmInstanceService, "bpmSubProcessService"))
                .cancelChildren(8L, "发起人撤销");
        verify(processInstanceGateway()).cancel("process-8", "发起人撤销");

        ArgumentCaptor<BpmTaskEntity> taskCaptor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(bpmTaskDao).updateById(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getTaskId()).isEqualTo(11L);
        assertThat(taskCaptor.getValue().getTaskState()).isEqualTo(BpmTaskStateEnum.CANCELLED.getValue());
        assertThat(taskCaptor.getValue().getTaskResult()).isEqualTo(BpmTaskResultEnum.INSTANCE_CANCELLED.getValue());

        ArgumentCaptor<BpmInstanceEntity> instanceCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(bpmInstanceDao).updateById(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getInstanceId()).isEqualTo(8L);
        assertThat(instanceCaptor.getValue().getRunState()).isEqualTo(BpmInstanceRunStateEnum.CANCELLED.getValue());
        assertThat(instanceCaptor.getValue().getResultState())
                .isEqualTo(BpmInstanceResultStateEnum.CANCELLED_BY_START_USER.getValue());
        assertThat(instanceCaptor.getValue().getFinishedAt()).isNotNull();
        assertThat(instanceCaptor.getValue().getCancelledAt()).isNotNull();

        ArgumentCaptor<BpmTaskActionLogEntity> logCaptor = ArgumentCaptor.forClass(BpmTaskActionLogEntity.class);
        verify(bpmTaskActionLogDao).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("INSTANCE_CANCELLED");
        assertThat(logCaptor.getValue().getCommentText()).isEqualTo("发起人撤销");
    }

    @Test
    void getResubmitDraftShouldExposeGraphVersionAndCurrentSnapshot() {
        GraphDefinitionVersionEntity graphVersion = activeGraphVersion();

        BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
        instanceEntity.setInstanceId(8L);
        instanceEntity.setGraphDefinitionVersionId(20L);
        instanceEntity.setStartEmployeeId(100L);
        instanceEntity.setRunState(BpmInstanceRunStateEnum.WAIT_RESUBMIT.getValue());
        instanceEntity.setTitle("请假申请");
        instanceEntity.setSummary("原始摘要");
        instanceEntity.setCurrentFormDataSnapshotJson("{\"amount\":200}");
        instanceEntity.setFormDataVersion(3L);

        when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
        when(graphVersionDao().selectById(20L)).thenReturn(graphVersion);
        when(instanceCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);

        ResponseDTO<BpmRuntimeStartDraftVO> response = bpmInstanceService.getResubmitDraft(8L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getGraphDefinitionVersionId()).isEqualTo(20L);
        assertThat(response.getData().getDefinitionSource()).isEqualTo("GRAPH");
        assertThat(response.getData().getDefinitionName()).isEqualTo("请假流程");
        assertThat(response.getData().getFormDataJson()).isEqualTo("{\"amount\":200}");
        assertThat(response.getData().getFormDataVersion()).isEqualTo(3L);
        assertThat(response.getData().getSourceInstanceId()).isEqualTo(8L);
        assertThat(response.getData().getSummary()).isEqualTo("原始摘要");
        assertThat(response.getData().getTitle()).isEqualTo("请假申请");
    }

    @Test
    void resubmitMyGraphInstanceShouldReusePlatformInstanceAndFrozenGraphVersion() {
        GraphDefinitionVersionEntity graphVersion = activeGraphVersion();

        BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
        instanceEntity.setInstanceId(8L);
        instanceEntity.setGraphDefinitionVersionId(20L);
        instanceEntity.setEngineProcessDefinitionId("leave:1:1000");
        instanceEntity.setEngineProcessInstanceId("process-1000");
        instanceEntity.setDefinitionKeySnapshot("leave");
        instanceEntity.setDefinitionVersionSnapshot(1);
        instanceEntity.setCategoryIdSnapshot(7L);
        instanceEntity.setCategoryNameSnapshot("人事流程");
        instanceEntity.setTitle("请假申请");
        instanceEntity.setSummary("原始摘要");
        instanceEntity.setStartEmployeeId(100L);
        instanceEntity.setStartEmployeeNameSnapshot("张三");
        instanceEntity.setStartDepartmentIdSnapshot(7L);
        instanceEntity.setStartDepartmentNameSnapshot("人事部");
        instanceEntity.setInitialFormDataSnapshotJson("{\"amount\":100}");
        instanceEntity.setCurrentFormDataSnapshotJson("{\"amount\":100}");
        instanceEntity.setFormDataVersion(3L);
        instanceEntity.setRunState(BpmInstanceRunStateEnum.WAIT_RESUBMIT.getValue());

        when(bpmInstanceDao.selectByIdForUpdate(8L)).thenReturn(instanceEntity);
        when(graphVersionDao().selectById(20L)).thenReturn(graphVersion);
        when(instanceCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(instanceIdentityGateway().requireEmployee(100L)).thenReturn(
                new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null)
        );
        when(processInstanceGateway().start("leave:2:2000", 8L, 100L, "{\"amount\":200}", Map.of()))
                .thenReturn("process-2000");

        BpmInstanceResubmitForm form = new BpmInstanceResubmitForm();
        form.setInstanceId(8L);
        form.setFormDataVersion(3L);
        form.setFormDataJson("{\"amount\":200}");
        form.setSummary("修改后重提");
        form.setTitle("请假申请-重提");

        ResponseDTO<Long> response = bpmInstanceService.resubmitMyInstance(form);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(8L);
        verify(processInstanceGateway()).start("leave:2:2000", 8L, 100L, "{\"amount\":200}", Map.of());
        verify(taskProjectionService()).syncActiveTasksForInstance(8L);

        ArgumentCaptor<BpmInstanceEntity> instanceCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(bpmInstanceDao, Mockito.times(2)).updateById(instanceCaptor.capture());
        BpmInstanceEntity stateUpdate = instanceCaptor.getAllValues().get(0);
        BpmInstanceEntity engineUpdate = instanceCaptor.getAllValues().get(1);
        assertThat(stateUpdate.getEngineProcessDefinitionId()).isEqualTo("leave:2:2000");
        assertThat(stateUpdate.getDefinitionVersionSnapshot()).isEqualTo(2);
        assertThat(stateUpdate.getCurrentFormDataSnapshotJson()).isEqualTo("{\"amount\":200}");
        assertThat(stateUpdate.getFormDataVersion()).isEqualTo(4L);
        assertThat(stateUpdate.getRunState()).isEqualTo(BpmInstanceRunStateEnum.RUNNING.getValue());
        assertThat(stateUpdate.getSummary()).isEqualTo("修改后重提");
        assertThat(stateUpdate.getTitle()).isEqualTo("请假申请-重提");
        assertThat(engineUpdate.getEngineProcessInstanceId()).isEqualTo("process-2000");

        ArgumentCaptor<BpmTaskActionLogEntity> logCaptor = ArgumentCaptor.forClass(BpmTaskActionLogEntity.class);
        verify(bpmTaskActionLogDao).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("RESUBMITTED");
        ArgumentCaptor<BpmFormDataChangeEntity> changeCaptor = ArgumentCaptor.forClass(BpmFormDataChangeEntity.class);
        verify(bpmFormDataChangeDao).insert(changeCaptor.capture());
        assertThat(changeCaptor.getValue().getChangeSource()).isEqualTo("INSTANCE_RESUBMITTED");
        assertThat(changeCaptor.getValue().getBeforeVersion()).isEqualTo(3L);
        assertThat(changeCaptor.getValue().getAfterVersion()).isEqualTo(4L);
    }

    @Test
    void resubmitGraphApprovalInstanceShouldCreateWorkingDataVersionAndEvidence() {
        GraphDefinitionVersionEntity graphVersion = activeGraphVersion();

        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(8L);
        instance.setGraphDefinitionVersionId(20L);
        instance.setStartEmployeeId(100L);
        instance.setApprovalSubjectSnapshotId(101L);
        instance.setProcessWorkingDataId(301L);
        instance.setCurrentFormDataSnapshotJson("{\"amount\":100}");
        instance.setFormDataVersion(3L);
        instance.setRunState(BpmInstanceRunStateEnum.WAIT_RESUBMIT.getValue());

        when(bpmInstanceDao.selectByIdForUpdate(8L)).thenReturn(instance);
        when(graphVersionDao().selectById(20L)).thenReturn(graphVersion);
        when(instanceCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(instanceIdentityGateway().requireEmployee(100L)).thenReturn(
                new BpmEmployeeSnapshot(100L, "张三", 7L, "财务部", null, null)
        );
        when(bpmApprovalDataMutationService.update(any())).thenReturn(
                new WorkingDataMutationResult(302L, 901L, 4L, "{\"amount\":200}")
        );
        when(processInstanceGateway().start("leave:2:2000", 8L, 100L, "{\"amount\":200}", Map.of()))
                .thenReturn("process-2000");

        BpmInstanceResubmitForm form = new BpmInstanceResubmitForm();
        form.setInstanceId(8L);
        form.setFormDataVersion(3L);
        form.setFormDataJson("{\"amount\":200}");
        form.setTitle("费用申请-重提");

        ResponseDTO<Long> response = bpmInstanceService.resubmitMyInstance(form);

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<WorkingDataMutationCommand> mutationCaptor =
                ArgumentCaptor.forClass(WorkingDataMutationCommand.class);
        verify(bpmApprovalDataMutationService).update(mutationCaptor.capture());
        assertThat(mutationCaptor.getValue()).satisfies(command -> {
            assertThat(command.approvalSubjectSnapshotId()).isEqualTo(101L);
            assertThat(command.expectedDataVersion()).isEqualTo(3L);
            assertThat(command.patchJson()).isEqualTo("{\"amount\":200}");
            assertThat(command.actionType()).isEqualTo("RESUBMIT");
        });
        ArgumentCaptor<BpmInstanceEntity> updateCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(bpmInstanceDao, Mockito.times(2)).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getAllValues().get(0).getProcessWorkingDataId()).isEqualTo(302L);
        assertThat(updateCaptor.getAllValues().get(0).getFormDataVersion()).isEqualTo(4L);
        verify(bpmFormDataChangeDao, never()).insert(any(BpmFormDataChangeEntity.class));
    }

    @SuppressWarnings("unchecked")
    private GraphDefinitionVersionDao graphVersionDao() {
        return (GraphDefinitionVersionDao) getFieldValue(bpmInstanceService, "graphDefinitionVersionDao");
    }

    @SuppressWarnings("unchecked")
    private FlowableProcessInstanceGateway processInstanceGateway() {
        return (FlowableProcessInstanceGateway) getFieldValue(bpmInstanceService, "flowableProcessInstanceGateway");
    }

    @SuppressWarnings("unchecked")
    private BpmCurrentActorProvider instanceCurrentActorProvider() {
        return (BpmCurrentActorProvider) getFieldValue(bpmInstanceService, "bpmCurrentActorProvider");
    }

    @SuppressWarnings("unchecked")
    private BpmOrgIdentityGateway instanceIdentityGateway() {
        return (BpmOrgIdentityGateway) getFieldValue(bpmInstanceService, "bpmOrgIdentityGateway");
    }

    @SuppressWarnings("unchecked")
    private BpmCurrentActorProvider taskCurrentActorProvider() {
        return (BpmCurrentActorProvider) getFieldValue(bpmTaskService, "bpmCurrentActorProvider");
    }

    @SuppressWarnings("unchecked")
    private BpmOrgIdentityGateway taskIdentityGateway() {
        return (BpmOrgIdentityGateway) getFieldValue(bpmTaskService, "bpmOrgIdentityGateway");
    }

    @SuppressWarnings("unchecked")
    private SerialNumberService serialNumberService() {
        return (SerialNumberService) getFieldValue(bpmInstanceService, "serialNumberService");
    }

    @SuppressWarnings("unchecked")
    private FlowableTaskGateway taskGateway() {
        return (FlowableTaskGateway) getFieldValue(bpmTaskService, "flowableTaskGateway");
    }

    @SuppressWarnings("unchecked")
    private BpmTaskProjectionService taskProjectionService() {
        return (BpmTaskProjectionService) getFieldValue(bpmInstanceService, "bpmTaskProjectionService");
    }

    @SuppressWarnings("unchecked")
    private BpmTaskProjectionService taskServiceProjectionService() {
        return (BpmTaskProjectionService) getFieldValue(bpmTaskService, "bpmTaskProjectionService");
    }

    private GraphDefinitionVersionEntity activeGraphVersion() {
        GraphDefinitionVersionEntity graphVersion = new GraphDefinitionVersionEntity();
        graphVersion.setGraphDefinitionVersionId(20L);
        graphVersion.setProcessKey("leave");
        graphVersion.setProcessNameSnapshot("请假流程");
        graphVersion.setDefinitionVersion(2);
        graphVersion.setCategoryIdSnapshot(7L);
        graphVersion.setCategoryNameSnapshot("人事流程");
        graphVersion.setLifecycleState("ACTIVE");
        graphVersion.setEngineProcessDefinitionId("leave:2:2000");
        return graphVersion;
    }

    private Object getFieldValue(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("读取测试字段失败: " + fieldName, ex);
        }
    }

    private BpmTaskEntity buildPendingTask() {
        BpmTaskEntity taskEntity = new BpmTaskEntity();
        taskEntity.setTaskId(1L);
        taskEntity.setInstanceId(8L);
        taskEntity.setDefinitionId(2L);
        taskEntity.setDefinitionNodeId(5L);
        taskEntity.setEngineTaskId("task-1");
        taskEntity.setEngineProcessInstanceId("process-8");
        taskEntity.setTaskKey("manager_approve");
        taskEntity.setTaskName("经理审批");
        taskEntity.setTaskState(BpmTaskStateEnum.PENDING.getValue());
        taskEntity.setAssigneeEmployeeId(10L);
        return taskEntity;
    }

    private BpmInstanceEntity buildBusinessInstance() {
        BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
        instanceEntity.setInstanceId(8L);
        instanceEntity.setBusinessType("sample_expense");
        instanceEntity.setBusinessId(1001L);
        instanceEntity.setCurrentFormDataSnapshotJson("{\"requestedAmount\":100,\"approvedAmount\":98}");
        instanceEntity.setFormDataVersion(4L);
        instanceEntity.setUpdateTime(LocalDateTime.of(2026, 7, 11, 15, 0));
        return instanceEntity;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("设置测试字段失败: " + fieldName, ex);
        }
    }
}
