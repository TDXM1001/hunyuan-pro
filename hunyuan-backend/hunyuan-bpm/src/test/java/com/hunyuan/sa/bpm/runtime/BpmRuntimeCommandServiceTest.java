package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.serialnumber.service.SerialNumberService;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceResultStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskResultEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.engine.internal.FlowableTaskGateway;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionDao;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceStartForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskApproveForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskRejectForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskReturnForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskTransferForm;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskAssignmentResolver;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmRuntimeCommandServiceTest {

    private BpmInstanceService bpmInstanceService;

    private BpmTaskService bpmTaskService;

    private BpmInstanceDao bpmInstanceDao;

    private BpmTaskDao bpmTaskDao;

    private BpmTaskActionLogDao bpmTaskActionLogDao;

    @BeforeEach
    void setUp() {
        bpmInstanceService = new BpmInstanceService();
        bpmTaskService = new BpmTaskService();
        bpmInstanceDao = Mockito.mock(BpmInstanceDao.class);
        bpmTaskDao = Mockito.mock(BpmTaskDao.class);
        bpmTaskActionLogDao = Mockito.mock(BpmTaskActionLogDao.class);

        setField(bpmInstanceService, "bpmDefinitionDao", Mockito.mock(BpmDefinitionDao.class));
        setField(bpmInstanceService, "bpmDefinitionNodeDao", Mockito.mock(BpmDefinitionNodeDao.class));
        setField(bpmInstanceService, "bpmInstanceDao", bpmInstanceDao);
        setField(bpmInstanceService, "flowableProcessInstanceGateway", Mockito.mock(FlowableProcessInstanceGateway.class));
        setField(bpmInstanceService, "bpmCurrentActorProvider", Mockito.mock(BpmCurrentActorProvider.class));
        setField(bpmInstanceService, "bpmOrgIdentityGateway", Mockito.mock(BpmOrgIdentityGateway.class));
        setField(bpmInstanceService, "serialNumberService", Mockito.mock(SerialNumberService.class));
        setField(bpmInstanceService, "bpmTaskAssignmentResolver", Mockito.mock(BpmTaskAssignmentResolver.class));
        setField(bpmInstanceService, "bpmTaskProjectionService", Mockito.mock(BpmTaskProjectionService.class));

        setField(bpmTaskService, "bpmTaskDao", bpmTaskDao);
        setField(bpmTaskService, "bpmInstanceDao", bpmInstanceDao);
        setField(bpmTaskService, "bpmTaskActionLogDao", bpmTaskActionLogDao);
        setField(bpmTaskService, "flowableTaskGateway", Mockito.mock(FlowableTaskGateway.class));
        setField(bpmTaskService, "bpmCurrentActorProvider", Mockito.mock(BpmCurrentActorProvider.class));
        setField(bpmTaskService, "bpmOrgIdentityGateway", Mockito.mock(BpmOrgIdentityGateway.class));
        setField(bpmTaskService, "bpmTaskProjectionService", Mockito.mock(BpmTaskProjectionService.class));
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
        when(taskAssignmentResolver().resolve(any(), any())).thenReturn(Map.of());
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
        ArgumentCaptor<BpmInstanceEntity> instanceCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(bpmInstanceDao).updateById(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getInstanceId()).isEqualTo(8L);
        assertThat(instanceCaptor.getValue().getRunState()).isEqualTo(BpmInstanceRunStateEnum.FINISHED.getValue());
        assertThat(instanceCaptor.getValue().getResultState()).isEqualTo(BpmInstanceResultStateEnum.REJECTED.getValue());
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
