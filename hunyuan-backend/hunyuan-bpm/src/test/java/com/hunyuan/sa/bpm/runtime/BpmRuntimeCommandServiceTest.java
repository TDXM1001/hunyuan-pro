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
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionDao;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceResubmitForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceStartForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskApproveForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskRejectForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskReturnForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskTransferForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmRuntimeStartDraftVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskAssignmentResolver;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalGroupService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceCopyService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @BeforeEach
    void setUp() {
        bpmInstanceService = new BpmInstanceService();
        bpmTaskService = new BpmTaskService();
        bpmInstanceDao = Mockito.mock(BpmInstanceDao.class);
        bpmTaskDao = Mockito.mock(BpmTaskDao.class);
        bpmTaskActionLogDao = Mockito.mock(BpmTaskActionLogDao.class);
        bpmInstanceCopyService = Mockito.mock(BpmInstanceCopyService.class);
        bpmBusinessProcessApi = Mockito.mock(BpmBusinessProcessApi.class);

        setField(bpmInstanceService, "bpmDefinitionDao", Mockito.mock(BpmDefinitionDao.class));
        setField(bpmInstanceService, "bpmDefinitionNodeDao", Mockito.mock(BpmDefinitionNodeDao.class));
        setField(bpmInstanceService, "bpmInstanceDao", bpmInstanceDao);
        setField(bpmInstanceService, "bpmTaskDao", bpmTaskDao);
        setField(bpmInstanceService, "bpmTaskActionLogDao", bpmTaskActionLogDao);
        setField(bpmInstanceService, "flowableProcessInstanceGateway", Mockito.mock(FlowableProcessInstanceGateway.class));
        setField(bpmInstanceService, "bpmCurrentActorProvider", Mockito.mock(BpmCurrentActorProvider.class));
        setField(bpmInstanceService, "bpmOrgIdentityGateway", Mockito.mock(BpmOrgIdentityGateway.class));
        setField(bpmInstanceService, "serialNumberService", Mockito.mock(SerialNumberService.class));
        setField(bpmInstanceService, "bpmTaskAssignmentResolver", Mockito.mock(BpmTaskAssignmentResolver.class));
        setField(bpmInstanceService, "bpmTaskProjectionService", Mockito.mock(BpmTaskProjectionService.class));
        setField(bpmInstanceService, "bpmApprovalGroupService", Mockito.mock(BpmApprovalGroupService.class));

        setField(bpmTaskService, "bpmTaskDao", bpmTaskDao);
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
        when(bpmInstanceCopyService.createManualCopies(any(), any(), any(), any())).thenReturn(ResponseDTO.ok());
    }

    @Test
    void startInstanceShouldKeepInitialAndCurrentFormSnapshotsSeparated() {
        BpmDefinitionEntity definitionEntity = new BpmDefinitionEntity();
        definitionEntity.setDefinitionId(1L);
        definitionEntity.setEngineProcessDefinitionId("leave:1:1000");
        definitionEntity.setDefinitionKey("leave");
        definitionEntity.setDefinitionVersion(1);
        definitionEntity.setCategoryIdSnapshot(7L);
        definitionEntity.setCategoryNameSnapshot("人事流程");
        definitionEntity.setInstanceNoRuleIdSnapshot(1);
        definitionEntity.setLifecycleState(1);
        definitionEntity.setStartState(1);

        when(definitionDao().selectById(1L)).thenReturn(definitionEntity);
        when(instanceCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(instanceIdentityGateway().requireEmployee(100L)).thenReturn(new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null));
        when(serialNumberService().generate(any())).thenReturn("SN-2026-0001");
        when(taskAssignmentResolver().resolve(any(), any(BpmEmployeeSnapshot.class))).thenReturn(Map.of());
        when(processInstanceGateway().start("leave:1:1000", 100L, "{\"amount\":100}", Map.of())).thenReturn("process-1000");
        when(bpmInstanceDao.insert(any(BpmInstanceEntity.class))).thenAnswer(invocation -> {
            BpmInstanceEntity entity = invocation.getArgument(0);
            entity.setInstanceId(8L);
            return 1;
        });

        BpmInstanceStartForm form = new BpmInstanceStartForm();
        form.setDefinitionId(1L);
        form.setFormDataJson("{\"amount\":100}");
        form.setTitle("请假申请");

        ResponseDTO<Long> response = bpmInstanceService.startInstance(form);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(8L);

        ArgumentCaptor<BpmInstanceEntity> instanceCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(bpmInstanceDao).insert(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getInitialFormDataSnapshotJson()).isEqualTo("{\"amount\":100}");
        assertThat(instanceCaptor.getValue().getCurrentFormDataSnapshotJson()).isEqualTo("{\"amount\":100}");
        assertThat(instanceCaptor.getValue().getRunState()).isEqualTo(BpmInstanceRunStateEnum.RUNNING.getValue());
        assertThat(instanceCaptor.getValue().getInstanceNo()).isEqualTo("SN-2026-0001");
        verify(taskProjectionService()).syncActiveTasksForInstance(8L);
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
        when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(10L);
        when(taskIdentityGateway().requireEmployee(10L)).thenReturn(new BpmEmployeeSnapshot(10L, "王主管", 7L, "人事部", null, null));

        BpmTaskReturnForm form = new BpmTaskReturnForm();
        form.setTaskId(1L);
        form.setCommentText("请补齐附件");

        ResponseDTO<String> response = bpmTaskService.returnToInitiator(form);

        assertThat(response.getOk()).isTrue();
        verify(taskGateway()).complete("task-1");

        ArgumentCaptor<BpmTaskEntity> taskCaptor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(bpmTaskDao).updateById(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getTaskState()).isEqualTo(BpmTaskStateEnum.COMPLETED.getValue());
        assertThat(taskCaptor.getValue().getTaskResult()).isEqualTo(BpmTaskResultEnum.RETURNED.getValue());

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
    void getResubmitDraftShouldExposeCurrentSnapshotForWaitResubmitInstance() {
        BpmDefinitionEntity definitionEntity = new BpmDefinitionEntity();
        definitionEntity.setDefinitionId(2L);
        definitionEntity.setDefinitionName("请假流程");
        definitionEntity.setFormNameSnapshot("请假表单");
        definitionEntity.setFormSchemaSnapshotJson("{\"type\":\"object\"}");
        definitionEntity.setLifecycleState(1);
        definitionEntity.setStartState(1);

        BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
        instanceEntity.setInstanceId(8L);
        instanceEntity.setDefinitionId(2L);
        instanceEntity.setStartEmployeeId(100L);
        instanceEntity.setRunState(BpmInstanceRunStateEnum.WAIT_RESUBMIT.getValue());
        instanceEntity.setTitle("请假申请");
        instanceEntity.setSummary("原始摘要");
        instanceEntity.setCurrentFormDataSnapshotJson("{\"amount\":200}");

        when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
        when(definitionDao().selectById(2L)).thenReturn(definitionEntity);
        when(instanceCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);

        ResponseDTO<BpmRuntimeStartDraftVO> response = bpmInstanceService.getResubmitDraft(8L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getDefinitionId()).isEqualTo(2L);
        assertThat(response.getData().getDefinitionName()).isEqualTo("请假流程");
        assertThat(response.getData().getFormNameSnapshot()).isEqualTo("请假表单");
        assertThat(response.getData().getFormSchemaSnapshotJson()).isEqualTo("{\"type\":\"object\"}");
        assertThat(response.getData().getFormDataJson()).isEqualTo("{\"amount\":200}");
        assertThat(response.getData().getSourceInstanceId()).isEqualTo(8L);
        assertThat(response.getData().getSummary()).isEqualTo("原始摘要");
        assertThat(response.getData().getTitle()).isEqualTo("请假申请");
    }

    @Test
    void resubmitMyInstanceShouldReusePlatformInstanceButStartNewEngineRun() {
        BpmDefinitionEntity definitionEntity = new BpmDefinitionEntity();
        definitionEntity.setDefinitionId(2L);
        definitionEntity.setDefinitionKey("leave");
        definitionEntity.setDefinitionName("请假流程");
        definitionEntity.setDefinitionVersion(2);
        definitionEntity.setCategoryIdSnapshot(7L);
        definitionEntity.setCategoryNameSnapshot("人事流程");
        definitionEntity.setLifecycleState(1);
        definitionEntity.setStartState(1);
        definitionEntity.setEngineProcessDefinitionId("leave:2:2000");

        BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
        instanceEntity.setInstanceId(8L);
        instanceEntity.setDefinitionId(2L);
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
        instanceEntity.setRunState(BpmInstanceRunStateEnum.WAIT_RESUBMIT.getValue());

        when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
        when(definitionDao().selectById(2L)).thenReturn(definitionEntity);
        when(instanceCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(instanceIdentityGateway().requireEmployee(100L)).thenReturn(
                new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null)
        );
        when(definitionNodeDao().selectList(any())).thenReturn(java.util.List.of());
        when(taskAssignmentResolver().resolve(any(), any(BpmEmployeeSnapshot.class))).thenReturn(Map.of());
        when(processInstanceGateway().start("leave:2:2000", 100L, "{\"amount\":200}", Map.of()))
                .thenReturn("process-2000");

        BpmInstanceResubmitForm form = new BpmInstanceResubmitForm();
        form.setInstanceId(8L);
        form.setFormDataJson("{\"amount\":200}");
        form.setSummary("修改后重提");
        form.setTitle("请假申请-重提");

        ResponseDTO<Long> response = bpmInstanceService.resubmitMyInstance(form);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(8L);
        verify(processInstanceGateway()).start("leave:2:2000", 100L, "{\"amount\":200}", Map.of());
        verify(taskProjectionService()).syncActiveTasksForInstance(8L);

        ArgumentCaptor<BpmInstanceEntity> instanceCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(bpmInstanceDao).updateById(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getInstanceId()).isEqualTo(8L);
        assertThat(instanceCaptor.getValue().getEngineProcessDefinitionId()).isEqualTo("leave:2:2000");
        assertThat(instanceCaptor.getValue().getEngineProcessInstanceId()).isEqualTo("process-2000");
        assertThat(instanceCaptor.getValue().getDefinitionVersionSnapshot()).isEqualTo(2);
        assertThat(instanceCaptor.getValue().getCurrentFormDataSnapshotJson()).isEqualTo("{\"amount\":200}");
        assertThat(instanceCaptor.getValue().getInitialFormDataSnapshotJson()).isNull();
        assertThat(instanceCaptor.getValue().getRunState()).isEqualTo(BpmInstanceRunStateEnum.RUNNING.getValue());
        assertThat(instanceCaptor.getValue().getSummary()).isEqualTo("修改后重提");
        assertThat(instanceCaptor.getValue().getTitle()).isEqualTo("请假申请-重提");

        ArgumentCaptor<BpmTaskActionLogEntity> logCaptor = ArgumentCaptor.forClass(BpmTaskActionLogEntity.class);
        verify(bpmTaskActionLogDao).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("RESUBMITTED");
    }

    @Test
    void resubmitMyInstanceShouldResolveSelectedEmployeeFromLatestFormData() {
        BpmDefinitionEntity definitionEntity = new BpmDefinitionEntity();
        definitionEntity.setDefinitionId(2L);
        definitionEntity.setDefinitionKey("expense");
        definitionEntity.setDefinitionName("费用流程");
        definitionEntity.setDefinitionVersion(2);
        definitionEntity.setCategoryIdSnapshot(7L);
        definitionEntity.setCategoryNameSnapshot("费用流程");
        definitionEntity.setLifecycleState(1);
        definitionEntity.setStartState(1);
        definitionEntity.setEngineProcessDefinitionId("expense:2:2000");

        BpmDefinitionNodeEntity nodeEntity = new BpmDefinitionNodeEntity();
        nodeEntity.setNodeKey("task_selected");
        nodeEntity.setNodeType("userTask");
        nodeEntity.setNodeNameSnapshot("发起时选择审批");
        nodeEntity.setAuthoredRuleSnapshotJson(
                "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
        );

        BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
        instanceEntity.setInstanceId(8L);
        instanceEntity.setDefinitionId(2L);
        instanceEntity.setEngineProcessDefinitionId("expense:1:1000");
        instanceEntity.setEngineProcessInstanceId("process-1000");
        instanceEntity.setDefinitionKeySnapshot("expense");
        instanceEntity.setDefinitionVersionSnapshot(1);
        instanceEntity.setCategoryIdSnapshot(7L);
        instanceEntity.setCategoryNameSnapshot("费用流程");
        instanceEntity.setTitle("费用申请");
        instanceEntity.setStartEmployeeId(100L);
        instanceEntity.setInitialFormDataSnapshotJson("{\"amount\":100,\"approverEmployeeId\":301}");
        instanceEntity.setCurrentFormDataSnapshotJson("{\"amount\":100,\"approverEmployeeId\":301}");
        instanceEntity.setRunState(BpmInstanceRunStateEnum.WAIT_RESUBMIT.getValue());

        BpmTaskAssignmentResolver realResolver = new BpmTaskAssignmentResolver();
        setField(realResolver, "bpmOrgIdentityGateway", instanceIdentityGateway());
        setField(bpmInstanceService, "bpmTaskAssignmentResolver", realResolver);

        when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
        when(definitionDao().selectById(2L)).thenReturn(definitionEntity);
        when(definitionNodeDao().selectList(any())).thenReturn(List.of(nodeEntity));
        when(instanceCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(instanceIdentityGateway().requireEmployee(100L)).thenReturn(
                new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null)
        );
        when(instanceIdentityGateway().requireEmployee(302L)).thenReturn(
                new BpmEmployeeSnapshot(302L, "审批人B", 8L, "财务部", null, null)
        );
        when(processInstanceGateway().start("expense:2:2000", 100L, "{\"amount\":200,\"approverEmployeeId\":302}", Map.of("assignee_task_selected", "302")))
                .thenReturn("process-2000");

        BpmInstanceResubmitForm form = new BpmInstanceResubmitForm();
        form.setInstanceId(8L);
        form.setFormDataJson("{\"amount\":200,\"approverEmployeeId\":302}");
        form.setTitle("费用申请-重提");

        ResponseDTO<Long> response = bpmInstanceService.resubmitMyInstance(form);

        assertThat(response.getOk()).isTrue();

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(processInstanceGateway()).start(
                Mockito.eq("expense:2:2000"),
                Mockito.eq(100L),
                Mockito.eq("{\"amount\":200,\"approverEmployeeId\":302}"),
                variablesCaptor.capture()
        );
        assertThat(variablesCaptor.getValue()).containsEntry("assignee_task_selected", "302");
    }

    @SuppressWarnings("unchecked")
    private BpmDefinitionDao definitionDao() {
        return (BpmDefinitionDao) getFieldValue(bpmInstanceService, "bpmDefinitionDao");
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
    private BpmDefinitionNodeDao definitionNodeDao() {
        return (BpmDefinitionNodeDao) getFieldValue(bpmInstanceService, "bpmDefinitionNodeDao");
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
    private BpmTaskAssignmentResolver taskAssignmentResolver() {
        return (BpmTaskAssignmentResolver) getFieldValue(bpmInstanceService, "bpmTaskAssignmentResolver");
    }

    @SuppressWarnings("unchecked")
    private BpmTaskProjectionService taskServiceProjectionService() {
        return (BpmTaskProjectionService) getFieldValue(bpmTaskService, "bpmTaskProjectionService");
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
