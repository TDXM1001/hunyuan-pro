package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskResultEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableTaskGateway;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskAddSignForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskDelegateForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskRecallForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskReduceSignForm;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalGroupService;
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

class BpmTaskAdvancedActionServiceTest {

    private BpmTaskService bpmTaskService;

    private BpmTaskDao bpmTaskDao;

    private BpmInstanceDao bpmInstanceDao;

    private BpmTaskActionLogDao bpmTaskActionLogDao;

    private BpmApprovalGroupService bpmApprovalGroupService;

    @BeforeEach
    void setUp() {
        bpmTaskService = new BpmTaskService();
        bpmTaskDao = Mockito.mock(BpmTaskDao.class);
        bpmInstanceDao = Mockito.mock(BpmInstanceDao.class);
        bpmTaskActionLogDao = Mockito.mock(BpmTaskActionLogDao.class);
        bpmApprovalGroupService = Mockito.mock(BpmApprovalGroupService.class);
        setField(bpmTaskService, "bpmTaskDao", bpmTaskDao);
        setField(bpmTaskService, "bpmInstanceDao", bpmInstanceDao);
        setField(bpmTaskService, "bpmTaskActionLogDao", bpmTaskActionLogDao);
        setField(bpmTaskService, "flowableTaskGateway", Mockito.mock(FlowableTaskGateway.class));
        setField(bpmTaskService, "flowableProcessInstanceGateway", Mockito.mock(FlowableProcessInstanceGateway.class));
        setField(bpmTaskService, "bpmApprovalGroupService", bpmApprovalGroupService);
        setField(bpmTaskService, "bpmCurrentActorProvider", Mockito.mock(BpmCurrentActorProvider.class));
        setField(bpmTaskService, "bpmOrgIdentityGateway", Mockito.mock(BpmOrgIdentityGateway.class));
    }

    @Test
    void delegateShouldChangeAssigneeAndWriteActionLog() {
        BpmTaskEntity taskEntity = buildPendingTask();
        when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(identityGateway().requireEmployee(100L)).thenReturn(new BpmEmployeeSnapshot(100L, "王主管", 7L, "业务部", null, null));
        when(identityGateway().requireEmployee(200L)).thenReturn(new BpmEmployeeSnapshot(200L, "李四", 9L, "财务部", null, null));

        BpmTaskDelegateForm form = new BpmTaskDelegateForm();
        form.setTaskId(1L);
        form.setTargetEmployeeId(200L);
        form.setReason("请协助处理");

        ResponseDTO<String> response = bpmTaskService.delegate(form);

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmTaskEntity> taskCaptor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(bpmTaskDao).updateById(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getAssigneeEmployeeId()).isEqualTo(200L);
        assertThat(taskCaptor.getValue().getAssigneeNameSnapshot()).isEqualTo("李四");

        ArgumentCaptor<BpmTaskActionLogEntity> logCaptor = ArgumentCaptor.forClass(BpmTaskActionLogEntity.class);
        verify(bpmTaskActionLogDao).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("DELEGATED");
        assertThat(logCaptor.getValue().getFromAssigneeEmployeeId()).isEqualTo(100L);
        assertThat(logCaptor.getValue().getToAssigneeEmployeeId()).isEqualTo(200L);
        assertThat(logCaptor.getValue().getCommentText()).isEqualTo("请协助处理");
    }

    @Test
    void addSignShouldCreateExtraPendingTaskProjectionAndWriteActionLog() {
        BpmTaskEntity taskEntity = buildPendingTask();
        when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(identityGateway().requireEmployee(100L)).thenReturn(new BpmEmployeeSnapshot(100L, "王主管", 7L, "业务部", null, null));
        when(identityGateway().requireEmployee(300L)).thenReturn(new BpmEmployeeSnapshot(300L, "赵六", 10L, "法务部", null, null));

        BpmTaskAddSignForm form = new BpmTaskAddSignForm();
        form.setTaskId(1L);
        form.setTargetEmployeeId(300L);
        form.setReason("请法务复核");

        ResponseDTO<String> response = bpmTaskService.addSign(form);

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmTaskEntity> taskCaptor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(bpmTaskDao).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getInstanceId()).isEqualTo(8L);
        assertThat(taskCaptor.getValue().getAssigneeEmployeeId()).isEqualTo(300L);
        assertThat(taskCaptor.getValue().getTaskState()).isEqualTo(BpmTaskStateEnum.PENDING.getValue());
        assertThat(taskCaptor.getValue().getRuntimeAssignmentSnapshotJson()).contains("\"addSign\":true");

        ArgumentCaptor<BpmTaskActionLogEntity> logCaptor = ArgumentCaptor.forClass(BpmTaskActionLogEntity.class);
        verify(bpmTaskActionLogDao).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("ADD_SIGNED");
        assertThat(logCaptor.getValue().getToAssigneeEmployeeId()).isEqualTo(300L);
    }

    @Test
    void reduceSignShouldCancelPendingAddSignTaskAndWriteActionLog() {
        BpmTaskEntity taskEntity = buildPendingTask();
        taskEntity.setTaskId(2L);
        taskEntity.setApprovalGroupId(31L);
        taskEntity.setAssigneeEmployeeId(300L);
        taskEntity.setRuntimeAssignmentSnapshotJson("{\"addSign\":true,\"sourceTaskId\":1}");
        when(bpmTaskDao.selectById(2L)).thenReturn(taskEntity);
        when(bpmApprovalGroupService.isParallelAllGroup(31L)).thenReturn(false);
        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(identityGateway().requireEmployee(100L)).thenReturn(new BpmEmployeeSnapshot(100L, "王主管", 7L, "业务部", null, null));

        BpmTaskReduceSignForm form = new BpmTaskReduceSignForm();
        form.setTaskId(2L);
        form.setReason("无需继续复核");

        ResponseDTO<String> response = bpmTaskService.reduceSign(form);

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmTaskEntity> taskCaptor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(bpmTaskDao).updateById(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getTaskId()).isEqualTo(2L);
        assertThat(taskCaptor.getValue().getTaskState()).isEqualTo(BpmTaskStateEnum.CANCELLED.getValue());
        assertThat(taskCaptor.getValue().getTaskResult()).isEqualTo(BpmTaskResultEnum.ADD_SIGN_REDUCED.getValue());

        ArgumentCaptor<BpmTaskActionLogEntity> logCaptor = ArgumentCaptor.forClass(BpmTaskActionLogEntity.class);
        verify(bpmTaskActionLogDao).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("REDUCE_SIGNED");
        assertThat(logCaptor.getValue().getCommentText()).isEqualTo("无需继续复核");
    }

    @Test
    void addSignShouldRemainAvailableForSequentialApprovalGroupMember() {
        BpmTaskEntity taskEntity = buildPendingTask();
        taskEntity.setApprovalGroupId(31L);
        when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
        when(bpmApprovalGroupService.isParallelAllGroup(31L)).thenReturn(false);
        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(identityGateway().requireEmployee(100L)).thenReturn(
                new BpmEmployeeSnapshot(100L, "王主管", 7L, "业务部", null, null));
        when(identityGateway().requireEmployee(300L)).thenReturn(
                new BpmEmployeeSnapshot(300L, "赵六", 10L, "法务部", null, null));

        BpmTaskAddSignForm form = new BpmTaskAddSignForm();
        form.setTaskId(1L);
        form.setTargetEmployeeId(300L);
        form.setReason("请法务复核");

        ResponseDTO<String> response = bpmTaskService.addSign(form);

        assertThat(response.getOk()).isTrue();
        verify(bpmTaskDao).insert(org.mockito.ArgumentMatchers.argThat(
                (BpmTaskEntity task) -> task.getApprovalGroupId() == null));
    }

    @Test
    void addAndReduceSignShouldRemainBlockedForParallelApprovalGroupMember() {
        BpmTaskEntity taskEntity = buildPendingTask();
        taskEntity.setApprovalGroupId(32L);
        when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
        when(bpmApprovalGroupService.isParallelAllGroup(32L)).thenReturn(true);

        BpmTaskAddSignForm addForm = new BpmTaskAddSignForm();
        addForm.setTaskId(1L);
        addForm.setTargetEmployeeId(300L);
        BpmTaskReduceSignForm reduceForm = new BpmTaskReduceSignForm();
        reduceForm.setTaskId(1L);

        assertThat(bpmTaskService.addSign(addForm).getMsg())
                .contains("并行全员会签成员不支持加签或减签");
        assertThat(bpmTaskService.reduceSign(reduceForm).getMsg())
                .contains("并行全员会签成员不支持加签或减签");
    }

    @Test
    void recallShouldMoveRunningInstanceToWaitResubmitForStartEmployee() {
        BpmTaskEntity taskEntity = buildPendingTask();
        BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
        instanceEntity.setInstanceId(8L);
        instanceEntity.setRunState(BpmInstanceRunStateEnum.RUNNING.getValue());
        instanceEntity.setStartEmployeeId(500L);
        when(bpmTaskDao.selectById(1L)).thenReturn(taskEntity);
        when(bpmInstanceDao.selectById(8L)).thenReturn(instanceEntity);
        when(bpmTaskDao.selectList(any())).thenReturn(List.of(taskEntity));
        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(500L);
        when(identityGateway().requireEmployee(500L)).thenReturn(new BpmEmployeeSnapshot(500L, "发起人", 6L, "市场部", null, null));

        BpmTaskRecallForm form = new BpmTaskRecallForm();
        form.setTaskId(1L);
        form.setReason("撤回修改");

        ResponseDTO<String> response = bpmTaskService.recall(form);

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmInstanceEntity> instanceCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(bpmInstanceDao).updateById(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getRunState()).isEqualTo(BpmInstanceRunStateEnum.WAIT_RESUBMIT.getValue());
        assertThat(instanceCaptor.getValue().getActiveTaskCount()).isEqualTo(0);

        ArgumentCaptor<BpmTaskActionLogEntity> logCaptor = ArgumentCaptor.forClass(BpmTaskActionLogEntity.class);
        verify(bpmTaskActionLogDao).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionType()).isEqualTo("RECALLED");
        assertThat(logCaptor.getValue().getCommentText()).isEqualTo("撤回修改");
    }

    private BpmTaskEntity buildPendingTask() {
        BpmTaskEntity taskEntity = new BpmTaskEntity();
        taskEntity.setTaskId(1L);
        taskEntity.setInstanceId(8L);
        taskEntity.setDefinitionId(2L);
        taskEntity.setDefinitionNodeId(5L);
        taskEntity.setEngineTaskId("task-1");
        taskEntity.setEngineExecutionId("execution-1");
        taskEntity.setEngineProcessInstanceId("process-8");
        taskEntity.setTaskKey("manager_approve");
        taskEntity.setTaskName("经理审批");
        taskEntity.setInstanceNo("SN-001");
        taskEntity.setInstanceTitle("费用申请");
        taskEntity.setStartEmployeeId(500L);
        taskEntity.setStartEmployeeNameSnapshot("发起人");
        taskEntity.setCategoryIdSnapshot(3L);
        taskEntity.setCategoryNameSnapshot("费用流程");
        taskEntity.setTaskState(BpmTaskStateEnum.PENDING.getValue());
        taskEntity.setAssigneeEmployeeId(100L);
        taskEntity.setAssigneeNameSnapshot("王主管");
        taskEntity.setAssigneeDepartmentIdSnapshot(7L);
        taskEntity.setAssigneeDepartmentNameSnapshot("业务部");
        return taskEntity;
    }

    private BpmCurrentActorProvider currentActorProvider() {
        return (BpmCurrentActorProvider) getFieldValue(bpmTaskService, "bpmCurrentActorProvider");
    }

    private BpmOrgIdentityGateway identityGateway() {
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
