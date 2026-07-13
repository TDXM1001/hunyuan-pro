package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.engine.internal.FlowableTaskGateway;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmAdminTaskTransferForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskAddSignForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskApproveForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskDelegateForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskRejectForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskRecallForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskReduceSignForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskReturnForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskTransferForm;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageCommandService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalGroupService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceCopyService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskActionPolicy;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BpmTaskServiceM2ActionTest {

    private BpmTaskService service;
    private BpmTaskDao taskDao;
    private BpmInstanceDao instanceDao;
    private BpmApprovalStageCommandService approvalStageCommandService;
    private FlowableTaskGateway flowableTaskGateway;
    private FlowableProcessInstanceGateway flowableProcessInstanceGateway;

    @BeforeEach
    void setUp() {
        service = new BpmTaskService();
        taskDao = Mockito.mock(BpmTaskDao.class);
        instanceDao = Mockito.mock(BpmInstanceDao.class);
        approvalStageCommandService = Mockito.mock(BpmApprovalStageCommandService.class);
        flowableTaskGateway = Mockito.mock(FlowableTaskGateway.class);
        flowableProcessInstanceGateway = Mockito.mock(FlowableProcessInstanceGateway.class);
        BpmCurrentActorProvider currentActorProvider = Mockito.mock(BpmCurrentActorProvider.class);
        BpmOrgIdentityGateway identityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        BpmTaskProjectionService taskProjectionService = Mockito.mock(BpmTaskProjectionService.class);
        BpmInstanceCopyService instanceCopyService = Mockito.mock(BpmInstanceCopyService.class);
        BpmApprovalGroupService approvalGroupService = Mockito.mock(BpmApprovalGroupService.class);
        BpmDefinitionNodeDao definitionNodeDao = Mockito.mock(BpmDefinitionNodeDao.class);

        setField(service, "bpmTaskDao", taskDao);
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "bpmTaskActionLogDao", Mockito.mock(BpmTaskActionLogDao.class));
        setField(service, "flowableTaskGateway", flowableTaskGateway);
        setField(service, "flowableProcessInstanceGateway", flowableProcessInstanceGateway);
        setField(service, "bpmCurrentActorProvider", currentActorProvider);
        setField(service, "bpmOrgIdentityGateway", identityGateway);
        setField(service, "bpmTaskProjectionService", taskProjectionService);
        setField(service, "bpmInstanceCopyService", instanceCopyService);
        setField(service, "bpmApprovalGroupService", approvalGroupService);
        setField(service, "bpmApprovalStageCommandService", approvalStageCommandService);
        setField(service, "bpmTaskActionPolicy", new BpmTaskActionPolicy(definitionNodeDao));

        when(currentActorProvider.requireCurrentEmployeeId()).thenReturn(20L);
        when(identityGateway.requireEmployee(20L)).thenReturn(new BpmEmployeeSnapshot(
                20L, "approver", 7L, "finance", null, null
        ));
        when(identityGateway.requireEmployee(30L)).thenReturn(new BpmEmployeeSnapshot(
                30L, "target", 8L, "legal", null, null
        ));
        when(taskProjectionService.syncActiveTasksForInstance(8L)).thenReturn(1);
        when(instanceCopyService.createManualCopies(any(), any(), any(), any())).thenReturn(ResponseDTO.ok());
        when(taskDao.selectList(any())).thenReturn(List.of());
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(8L);
        instance.setStartEmployeeId(20L);
        instance.setRunState(BpmInstanceRunStateEnum.RUNNING.getValue());
        instance.setEngineProcessInstanceId("engine-process-8");
        when(instanceDao.selectById(8L)).thenReturn(instance);
    }

    @Test
    void approveM2MemberTaskShouldDelegateToStageCommandWithoutCompletingFlowableTask() {
        BpmTaskEntity task = m2MemberTask();
        when(taskDao.selectById(101L)).thenReturn(task);
        when(approvalStageCommandService.execute(101L, "APPROVE", "approved", "request-approve", null, null, null))
                .thenReturn(ResponseDTO.ok());

        BpmTaskApproveForm form = new BpmTaskApproveForm();
        form.setTaskId(101L);
        form.setCommentText("approved");
        form.setRequestId("request-approve");

        ResponseDTO<String> response = service.approve(form);

        assertThat(response.getOk()).isTrue();
        verify(approvalStageCommandService).execute(101L, "APPROVE", "approved", "request-approve", null, null, null);
        verifyNoInteractions(flowableTaskGateway, flowableProcessInstanceGateway);
    }

    @Test
    void completedM2TaskShouldReachReceiptReplayBeforeGenericTaskStatePolicy() {
        BpmTaskEntity task = m2MemberTask();
        task.setTaskState(BpmTaskStateEnum.COMPLETED.getValue());
        when(taskDao.selectById(101L)).thenReturn(task);
        when(approvalStageCommandService.execute(101L, "APPROVE", "approved", "request-replay", null, null, null))
                .thenReturn(ResponseDTO.ok());

        BpmTaskApproveForm form = new BpmTaskApproveForm();
        form.setTaskId(101L);
        form.setCommentText("approved");
        form.setRequestId("request-replay");

        assertThat(service.approve(form).getOk()).isTrue();
        verify(approvalStageCommandService).execute(101L, "APPROVE", "approved", "request-replay", null, null, null);
    }

    @Test
    void rejectM2MemberTaskShouldDelegateToStageCommandWithoutCancellingFlowableInstance() {
        BpmTaskEntity task = m2MemberTask();
        when(taskDao.selectById(101L)).thenReturn(task);
        when(approvalStageCommandService.execute(101L, "REJECT", "rejected", "request-reject", null, null, null))
                .thenReturn(ResponseDTO.ok());

        BpmTaskRejectForm form = new BpmTaskRejectForm();
        form.setTaskId(101L);
        form.setCommentText("rejected");
        form.setRequestId("request-reject");

        ResponseDTO<String> response = service.reject(form);

        assertThat(response.getOk()).isTrue();
        verify(approvalStageCommandService).execute(101L, "REJECT", "rejected", "request-reject", null, null, null);
        verifyNoInteractions(flowableTaskGateway, flowableProcessInstanceGateway);
    }

    @Test
    void returnM2MemberTaskShouldDelegateToStageCommandWithoutCancellingFlowableInstance() {
        BpmTaskEntity task = m2MemberTask();
        when(taskDao.selectById(101L)).thenReturn(task);
        when(approvalStageCommandService.execute(101L, "RETURN", "returned", "request-return", null, null, null))
                .thenReturn(ResponseDTO.ok());

        BpmTaskReturnForm form = new BpmTaskReturnForm();
        form.setTaskId(101L);
        form.setCommentText("returned");
        form.setRequestId("request-return");

        ResponseDTO<String> response = service.returnToInitiator(form);

        assertThat(response.getOk()).isTrue();
        verify(approvalStageCommandService).execute(101L, "RETURN", "returned", "request-return", null, null, null);
        verifyNoInteractions(flowableTaskGateway, flowableProcessInstanceGateway);
    }

    @Test
    void approveM2TaskWithApprovalStageIdOnlyShouldDelegateToStageCommand() {
        assertPartialStageLinkDelegates("APPROVE", true);
    }

    @Test
    void approveM2TaskWithApprovalStageMemberIdOnlyShouldDelegateToStageCommand() {
        assertPartialStageLinkDelegates("APPROVE", false);
    }

    @Test
    void rejectM2TaskWithApprovalStageIdOnlyShouldDelegateToStageCommand() {
        assertPartialStageLinkDelegates("REJECT", true);
    }

    @Test
    void rejectM2TaskWithApprovalStageMemberIdOnlyShouldDelegateToStageCommand() {
        assertPartialStageLinkDelegates("REJECT", false);
    }

    @Test
    void returnM2TaskWithApprovalStageIdOnlyShouldDelegateToStageCommand() {
        assertPartialStageLinkDelegates("RETURN", true);
    }

    @Test
    void returnM2TaskWithApprovalStageMemberIdOnlyShouldDelegateToStageCommand() {
        assertPartialStageLinkDelegates("RETURN", false);
    }

    @Test
    void approveLegacyTaskWithoutStageLinksShouldUseFlowableTaskPath() {
        BpmTaskEntity task = legacyTask();
        when(taskDao.selectById(101L)).thenReturn(task);

        ResponseDTO<String> response = invokeAction("APPROVE", 101L, "approved");

        assertThat(response.getOk()).isTrue();
        verifyNoInteractions(approvalStageCommandService, flowableProcessInstanceGateway);
        verify(flowableTaskGateway).complete("engine-task-101");
    }

    @Test
    void rejectLegacyTaskWithoutStageLinksShouldUseFlowableProcessPath() {
        BpmTaskEntity task = legacyTask();
        when(taskDao.selectById(101L)).thenReturn(task);

        ResponseDTO<String> response = invokeAction("REJECT", 101L, "rejected");

        assertThat(response.getOk()).isTrue();
        verifyNoInteractions(approvalStageCommandService, flowableTaskGateway);
        verify(flowableProcessInstanceGateway).cancel("engine-process-8", "审批驳回");
    }

    @Test
    void returnLegacyTaskWithoutStageLinksShouldUseFlowableProcessPath() {
        BpmTaskEntity task = legacyTask();
        when(taskDao.selectById(101L)).thenReturn(task);

        ResponseDTO<String> response = invokeAction("RETURN", 101L, "returned");

        assertThat(response.getOk()).isTrue();
        verifyNoInteractions(approvalStageCommandService, flowableTaskGateway);
        verify(flowableProcessInstanceGateway).cancel("engine-process-8", "审批退回发起人");
    }

    @Test
    void transferM2MemberTaskShouldRejectGenericTransferWithoutCallingFlowable() {
        when(taskDao.selectById(101L)).thenReturn(m2MemberTask());
        BpmTaskTransferForm form = new BpmTaskTransferForm();
        form.setTaskId(101L);
        form.setToEmployeeId(30L);

        assertM2AdvancedActionRejected(() -> service.transfer(form));
    }

    @Test
    void adminTransferM2MemberTaskShouldRejectGenericTransferWithoutCallingFlowable() {
        when(taskDao.selectById(101L)).thenReturn(m2MemberTask());
        BpmAdminTaskTransferForm form = new BpmAdminTaskTransferForm();
        form.setTaskId(101L);
        form.setTargetEmployeeId(30L);
        form.setReason("governance transfer");

        assertM2AdvancedActionRejected(() -> service.adminTransfer(form));
    }

    @Test
    void delegateM2MemberTaskShouldRejectGenericDelegationWithoutCallingFlowable() {
        when(taskDao.selectById(101L)).thenReturn(m2MemberTask());
        BpmTaskDelegateForm form = new BpmTaskDelegateForm();
        form.setTaskId(101L);
        form.setTargetEmployeeId(30L);

        assertM2AdvancedActionRejected(() -> service.delegate(form));
    }

    @Test
    void adminDelegateM2MemberTaskShouldRejectGenericDelegationWithoutCallingFlowable() {
        when(taskDao.selectById(101L)).thenReturn(m2MemberTask());
        BpmTaskDelegateForm form = new BpmTaskDelegateForm();
        form.setTaskId(101L);
        form.setTargetEmployeeId(30L);

        assertM2AdvancedActionRejected(() -> service.adminDelegate(form));
    }

    @Test
    void addSignM2MemberTaskShouldRejectGenericAddSignWithoutCreatingTask() {
        when(taskDao.selectById(101L)).thenReturn(m2MemberTask());
        BpmTaskAddSignForm form = new BpmTaskAddSignForm();
        form.setTaskId(101L);
        form.setTargetEmployeeId(30L);

        assertM2AdvancedActionRejected(() -> service.addSign(form));
    }

    @Test
    void reduceSignM2MemberTaskShouldRejectGenericReduceSignWithoutChangingTask() {
        BpmTaskEntity task = m2MemberTask();
        task.setRuntimeAssignmentSnapshotJson("{\"addSign\":true}");
        when(taskDao.selectById(101L)).thenReturn(task);
        BpmTaskReduceSignForm form = new BpmTaskReduceSignForm();
        form.setTaskId(101L);

        assertM2AdvancedActionRejected(() -> service.reduceSign(form));
    }

    @Test
    void recallM2MemberTaskShouldRejectGenericRecallWithoutCancellingStage() {
        when(taskDao.selectById(101L)).thenReturn(m2MemberTask());
        BpmTaskRecallForm form = new BpmTaskRecallForm();
        form.setTaskId(101L);

        assertM2AdvancedActionRejected(() -> service.recall(form));
        verifyNoInteractions(instanceDao);
    }

    private void assertPartialStageLinkDelegates(String action, boolean hasApprovalStageId) {
        BpmTaskEntity task = m2MemberTask();
        if (hasApprovalStageId) {
            task.setApprovalStageMemberId(null);
        } else {
            task.setApprovalStageId(null);
        }
        when(taskDao.selectById(101L)).thenReturn(task);
        String requestId = "request-" + action.toLowerCase();
        when(approvalStageCommandService.execute(101L, action, action.toLowerCase(), requestId, null, null, null))
                .thenReturn(ResponseDTO.ok());

        ResponseDTO<String> response = invokeAction(action, 101L, action.toLowerCase());

        assertThat(response.getOk()).isTrue();
        verify(approvalStageCommandService).execute(101L, action, action.toLowerCase(), requestId, null, null, null);
        verifyNoInteractions(flowableTaskGateway, flowableProcessInstanceGateway);
    }

    private ResponseDTO<String> invokeAction(String action, Long taskId, String commentText) {
        return switch (action) {
            case "APPROVE" -> {
                BpmTaskApproveForm form = new BpmTaskApproveForm();
                form.setTaskId(taskId);
                form.setCommentText(commentText);
                form.setRequestId("request-approve");
                yield service.approve(form);
            }
            case "REJECT" -> {
                BpmTaskRejectForm form = new BpmTaskRejectForm();
                form.setTaskId(taskId);
                form.setCommentText(commentText);
                form.setRequestId("request-reject");
                yield service.reject(form);
            }
            case "RETURN" -> {
                BpmTaskReturnForm form = new BpmTaskReturnForm();
                form.setTaskId(taskId);
                form.setCommentText(commentText);
                form.setRequestId("request-return");
                yield service.returnToInitiator(form);
            }
            default -> throw new IllegalArgumentException("unsupported action: " + action);
        };
    }

    private void assertM2AdvancedActionRejected(Supplier<ResponseDTO<String>> action) {
        ResponseDTO<String> response = action.get();

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("M2 审批阶段成员");
        verifyNoInteractions(approvalStageCommandService, flowableTaskGateway, flowableProcessInstanceGateway);
        verify(taskDao, never()).insert(any(BpmTaskEntity.class));
        verify(taskDao, never()).updateById(any(BpmTaskEntity.class));
    }

    private BpmTaskEntity m2MemberTask() {
        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(101L);
        task.setInstanceId(8L);
        task.setApprovalStageId(70L);
        task.setApprovalStageMemberId(700L);
        task.setAssigneeEmployeeId(20L);
        task.setTaskState(BpmTaskStateEnum.PENDING.getValue());
        return task;
    }

    private BpmTaskEntity legacyTask() {
        BpmTaskEntity task = m2MemberTask();
        task.setApprovalStageId(null);
        task.setApprovalStageMemberId(null);
        task.setEngineTaskId("engine-task-101");
        task.setEngineProcessInstanceId("engine-process-8");
        return task;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
