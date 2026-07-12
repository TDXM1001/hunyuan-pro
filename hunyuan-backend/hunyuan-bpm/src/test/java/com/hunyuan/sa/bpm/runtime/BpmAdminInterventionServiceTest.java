package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
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
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmAdminInstanceCancelForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmAdminTaskTransferForm;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalGroupService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTimeEventService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmExternalWaitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmAdminInterventionServiceTest {

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

        setField(bpmInstanceService, "bpmInstanceDao", bpmInstanceDao);
        setField(bpmInstanceService, "bpmTaskDao", bpmTaskDao);
        setField(bpmInstanceService, "bpmTaskActionLogDao", bpmTaskActionLogDao);
        setField(bpmInstanceService, "flowableProcessInstanceGateway", Mockito.mock(FlowableProcessInstanceGateway.class));
        setField(bpmInstanceService, "bpmCurrentActorProvider", Mockito.mock(BpmCurrentActorProvider.class));
        setField(bpmInstanceService, "bpmOrgIdentityGateway", Mockito.mock(BpmOrgIdentityGateway.class));
        setField(bpmInstanceService, "bpmTaskProjectionService", Mockito.mock(BpmTaskProjectionService.class));
        setField(bpmInstanceService, "bpmApprovalGroupService", Mockito.mock(BpmApprovalGroupService.class));
        setField(bpmInstanceService, "bpmTimeEventService", Mockito.mock(BpmTimeEventService.class));
        setField(bpmInstanceService, "bpmExternalWaitService", Mockito.mock(BpmExternalWaitService.class));

        setField(bpmTaskService, "bpmTaskDao", bpmTaskDao);
        setField(bpmTaskService, "bpmTaskActionLogDao", bpmTaskActionLogDao);
        setField(bpmTaskService, "flowableTaskGateway", Mockito.mock(FlowableTaskGateway.class));
        setField(bpmTaskService, "bpmCurrentActorProvider", Mockito.mock(BpmCurrentActorProvider.class));
        setField(bpmTaskService, "bpmOrgIdentityGateway", Mockito.mock(BpmOrgIdentityGateway.class));
    }

    @Test
    void adminCancelShouldTerminateEngineCloseTasksAndWriteAdminActionLog() {
        BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
        instanceEntity.setInstanceId(8L);
        instanceEntity.setDefinitionId(2L);
        instanceEntity.setEngineProcessInstanceId("process-8");
        instanceEntity.setRunState(BpmInstanceRunStateEnum.RUNNING.getValue());

        BpmTaskEntity taskEntity = new BpmTaskEntity();
        taskEntity.setTaskId(11L);
        taskEntity.setInstanceId(8L);
        taskEntity.setTaskState(BpmTaskStateEnum.PENDING.getValue());

        when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
        when(instanceCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(900L);
        when(instanceIdentityGateway().requireEmployee(900L)).thenReturn(
                new BpmEmployeeSnapshot(900L, "管理员", 1L, "平台部", null, null)
        );
        when(bpmTaskDao.selectList(any())).thenReturn(List.of(taskEntity));

        BpmAdminInstanceCancelForm form = new BpmAdminInstanceCancelForm();
        form.setInstanceId(8L);
        form.setCancelReason("录入错误");

        ResponseDTO<String> response = bpmInstanceService.adminCancel(form);

        assertThat(response.getOk()).isTrue();
        verify(processInstanceGateway()).cancel("process-8", "录入错误");

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
                .isEqualTo(BpmInstanceResultStateEnum.CANCELLED_BY_ADMIN.getValue());
        assertThat(instanceCaptor.getValue().getActiveTaskCount()).isEqualTo(0);
        assertThat(instanceCaptor.getValue().getCurrentNodeSummaryJson()).isNull();
        assertThat(instanceCaptor.getValue().getCancelByEmployeeId()).isEqualTo(900L);
        assertThat(instanceCaptor.getValue().getCancelByNameSnapshot()).isEqualTo("管理员");
        assertThat(instanceCaptor.getValue().getCancelReason()).isEqualTo("录入错误");
        assertThat(instanceCaptor.getValue().getFinishedAt()).isNotNull();
        assertThat(instanceCaptor.getValue().getCancelledAt()).isNotNull();

        ArgumentCaptor<BpmTaskActionLogEntity> logCaptor = ArgumentCaptor.forClass(BpmTaskActionLogEntity.class);
        verify(bpmTaskActionLogDao).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("ADMIN_INSTANCE_CANCELLED");
        assertThat(logCaptor.getValue().getActorEmployeeId()).isEqualTo(900L);
        assertThat(logCaptor.getValue().getActorNameSnapshot()).isEqualTo("管理员");
        assertThat(logCaptor.getValue().getCommentText()).isEqualTo("录入错误");
    }

    @Test
    void adminTransferShouldReassignTaskAndWriteAdminActionLog() {
        BpmTaskEntity taskEntity = new BpmTaskEntity();
        taskEntity.setTaskId(1L);
        taskEntity.setInstanceId(8L);
        taskEntity.setDefinitionId(2L);
        taskEntity.setDefinitionNodeId(5L);
        taskEntity.setEngineTaskId("task-1");
        taskEntity.setTaskState(BpmTaskStateEnum.PENDING.getValue());
        taskEntity.setAssigneeEmployeeId(100L);

        when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
        when(taskCurrentActorProvider().requireCurrentEmployeeId()).thenReturn(900L);
        when(taskIdentityGateway().requireEmployee(900L)).thenReturn(
                new BpmEmployeeSnapshot(900L, "管理员", 1L, "平台部", null, null)
        );
        when(taskIdentityGateway().requireEmployee(200L)).thenReturn(
                new BpmEmployeeSnapshot(200L, "李四", 9L, "财务部", null, null)
        );

        BpmAdminTaskTransferForm form = new BpmAdminTaskTransferForm();
        form.setTaskId(1L);
        form.setTargetEmployeeId(200L);
        form.setReason("部门负责人调整");

        ResponseDTO<String> response = bpmTaskService.adminTransfer(form);

        assertThat(response.getOk()).isTrue();
        verify(taskGateway()).transfer("task-1", 200L);

        ArgumentCaptor<BpmTaskEntity> taskCaptor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(bpmTaskDao).updateById(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getAssigneeEmployeeId()).isEqualTo(200L);
        assertThat(taskCaptor.getValue().getAssigneeNameSnapshot()).isEqualTo("李四");
        assertThat(taskCaptor.getValue().getAssigneeDepartmentIdSnapshot()).isEqualTo(9L);
        assertThat(taskCaptor.getValue().getAssigneeDepartmentNameSnapshot()).isEqualTo("财务部");
        assertThat(taskCaptor.getValue().getRuntimeAssignmentSnapshotJson()).contains("\"assigneeEmployeeId\":200");
        assertThat(taskCaptor.getValue().getTaskState()).isNull();

        ArgumentCaptor<BpmTaskActionLogEntity> logCaptor = ArgumentCaptor.forClass(BpmTaskActionLogEntity.class);
        verify(bpmTaskActionLogDao).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("ADMIN_TRANSFERRED");
        assertThat(logCaptor.getValue().getActorEmployeeId()).isEqualTo(900L);
        assertThat(logCaptor.getValue().getActorNameSnapshot()).isEqualTo("管理员");
        assertThat(logCaptor.getValue().getFromAssigneeEmployeeId()).isEqualTo(100L);
        assertThat(logCaptor.getValue().getToAssigneeEmployeeId()).isEqualTo(200L);
        assertThat(logCaptor.getValue().getCommentText()).isEqualTo("部门负责人调整");
    }

    @Test
    void resyncProjectionShouldUseRuntimeProjectionService() {
        BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
        instanceEntity.setInstanceId(8L);
        instanceEntity.setEngineProcessInstanceId("process-8");
        when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
        when(taskProjectionService().syncActiveTasksForInstance(8L)).thenReturn(2);

        ResponseDTO<String> response = bpmInstanceService.resyncProjection(8L);

        assertThat(response.getOk()).isTrue();
        verify(taskProjectionService()).syncActiveTasksForInstance(8L);
    }

    private FlowableProcessInstanceGateway processInstanceGateway() {
        return (FlowableProcessInstanceGateway) getFieldValue(bpmInstanceService, "flowableProcessInstanceGateway");
    }

    private BpmTaskProjectionService taskProjectionService() {
        return (BpmTaskProjectionService) getFieldValue(bpmInstanceService, "bpmTaskProjectionService");
    }

    private BpmCurrentActorProvider instanceCurrentActorProvider() {
        return (BpmCurrentActorProvider) getFieldValue(bpmInstanceService, "bpmCurrentActorProvider");
    }

    private BpmOrgIdentityGateway instanceIdentityGateway() {
        return (BpmOrgIdentityGateway) getFieldValue(bpmInstanceService, "bpmOrgIdentityGateway");
    }

    private FlowableTaskGateway taskGateway() {
        return (FlowableTaskGateway) getFieldValue(bpmTaskService, "flowableTaskGateway");
    }

    private BpmCurrentActorProvider taskCurrentActorProvider() {
        return (BpmCurrentActorProvider) getFieldValue(bpmTaskService, "bpmCurrentActorProvider");
    }

    private BpmOrgIdentityGateway taskIdentityGateway() {
        return (BpmOrgIdentityGateway) getFieldValue(bpmTaskService, "bpmOrgIdentityGateway");
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
