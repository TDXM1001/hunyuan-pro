package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskAddSignForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskApproveForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskCompleteForm;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskResultEnum;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.engine.internal.FlowableTaskGateway;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceCopyService;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskReduceSignForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmApprovalGroupSummaryVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskFormContextVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalGroupService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskFormContextService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskService;
import com.hunyuan.sa.bpm.module.approvaldata.domain.vo.BpmApprovalSubjectContextVO;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmApprovalSubjectViewService;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class BpmTaskServiceTest {

    private BpmTaskService service;
    private BpmTaskDao taskDao;
    private BpmApprovalGroupService approvalGroupService;
    private BpmInstanceDao instanceDao;
    private BpmTaskFormContextService taskFormContextService;
    private BpmCurrentActorProvider currentActorProvider;
    private BpmDefinitionNodeDao definitionNodeDao;
    private BpmApprovalSubjectViewService approvalSubjectViewService;

    @BeforeEach
    void setUp() {
        service = new BpmTaskService();
        taskDao = Mockito.mock(BpmTaskDao.class);
        approvalGroupService = Mockito.mock(BpmApprovalGroupService.class);
        instanceDao = Mockito.mock(BpmInstanceDao.class);
        taskFormContextService = Mockito.mock(BpmTaskFormContextService.class);
        currentActorProvider = Mockito.mock(BpmCurrentActorProvider.class);
        definitionNodeDao = Mockito.mock(BpmDefinitionNodeDao.class);
        approvalSubjectViewService = Mockito.mock(BpmApprovalSubjectViewService.class);
        setField(service, "bpmTaskDao", taskDao);
        setField(service, "bpmApprovalGroupService", approvalGroupService);
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "bpmTaskFormContextService", taskFormContextService);
        setField(service, "bpmCurrentActorProvider", currentActorProvider);
        setField(service, "bpmDefinitionNodeDao", definitionNodeDao);
        setField(service, "bpmApprovalSubjectViewService", approvalSubjectViewService);
        setField(service, "bpmTaskActionLogDao", Mockito.mock(BpmTaskActionLogDao.class));
    }

    @Test
    void queryAdminPageShouldBatchAttachApprovalGroupSummary() {
        BpmTaskVO task = new BpmTaskVO();
        task.setTaskId(11L);
        task.setApprovalGroupId(21L);
        BpmApprovalGroupSummaryVO summary = new BpmApprovalGroupSummaryVO();
        summary.setApprovalGroupId(21L);
        summary.setApprovalGroupName("财务会签");
        summary.setProcessedMemberCount(1);
        summary.setTotalMemberCount(3);
        when(approvalGroupService.mapSummariesById(List.of(21L))).thenReturn(Map.of(21L, summary));

        invokeAttachApprovalGroupSummaries(service, List.of(task));

        assertThat(task.getApprovalGroup()).isSameAs(summary);
    }

    @Test
    void addSignShouldRejectParallelAllMember() {
        BpmTaskEntity task = buildGroupMember();
        when(taskDao.selectById(11L)).thenReturn(task);
        when(approvalGroupService.isParallelAllGroup(21L)).thenReturn(true);
        BpmTaskAddSignForm form = new BpmTaskAddSignForm();
        form.setTaskId(11L);
        form.setTargetEmployeeId(102L);

        ResponseDTO<String> response = service.addSign(form);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).isEqualTo("并行全员会签成员不支持加签或减签");
    }

    @Test
    void reduceSignShouldRejectParallelAllMember() {
        BpmTaskEntity task = buildGroupMember();
        when(taskDao.selectById(11L)).thenReturn(task);
        when(approvalGroupService.isParallelAllGroup(21L)).thenReturn(true);
        BpmTaskReduceSignForm form = new BpmTaskReduceSignForm();
        form.setTaskId(11L);

        ResponseDTO<String> response = service.reduceSign(form);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).isEqualTo("并行全员会签成员不支持加签或减签");
    }

    @Test
    void getMyDetailShouldAttachEmployeeSafeFormContext() {
        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(11L);
        task.setInstanceId(31L);
        task.setAssigneeEmployeeId(9L);
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(31L);
        BpmTaskFormContextVO context = new BpmTaskFormContextVO();
        context.setDataVersion(3L);
        when(taskDao.selectById(11L)).thenReturn(task);
        when(currentActorProvider.requireCurrentEmployeeId()).thenReturn(9L);
        when(instanceDao.selectById(31L)).thenReturn(instance);
        when(taskFormContextService.buildForEmployeeTask(task, instance)).thenReturn(context);

        ResponseDTO<BpmTaskDetailVO> response = service.getMyDetail(11L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getFormContext()).isSameAs(context);
    }

    @Test
    void getMyDetailShouldAttachM3ApprovalSubjectContextForGraphTask() {
        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(11L);
        task.setInstanceId(31L);
        task.setAssigneeEmployeeId(9L);
        task.setGraphDefinitionVersionId(41L);
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(31L);
        instance.setApprovalSubjectSnapshotId(101L);
        BpmApprovalSubjectContextVO context = new BpmApprovalSubjectContextVO();
        context.setViewState("READY");
        when(taskDao.selectById(11L)).thenReturn(task);
        when(currentActorProvider.requireCurrentEmployeeId()).thenReturn(9L);
        when(instanceDao.selectById(31L)).thenReturn(instance);
        when(approvalSubjectViewService.buildForTask(task, instance)).thenReturn(context);

        ResponseDTO<BpmTaskDetailVO> response = service.getMyDetail(11L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getApprovalSubjectContext()).isSameAs(context);
        verify(approvalSubjectViewService).buildForTask(task, instance);
    }

    @Test
    void approveFormShouldCarryOptimisticVersionAndPatch() {
        BpmTaskApproveForm form = new BpmTaskApproveForm();
        form.setFormDataVersion(3L);
        form.setFormDataPatchJson("{\"approvedAmount\":98}");

        assertThat(form.getFormDataVersion()).isEqualTo(3L);
        assertThat(form.getFormDataPatchJson()).contains("approvedAmount");
    }

    @Test
    void approveShouldRejectHandleTask() {
        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(11L);
        task.setDefinitionNodeId(31L);
        when(taskDao.selectById(11L)).thenReturn(task);
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(31L);
        node.setNodeType("HANDLE_TASK");
        when(definitionNodeDao.selectById(31L)).thenReturn(node);
        BpmTaskApproveForm form = new BpmTaskApproveForm();
        form.setTaskId(11L);

        ResponseDTO<String> response = service.approve(form);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("办理任务").contains("审批通过");
    }

    @Test
    void completeHandleTaskShouldCompleteEngineTaskWithHandledResult() {
        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(11L);
        task.setDefinitionNodeId(31L);
        task.setInstanceId(41L);
        task.setDefinitionId(18L);
        task.setEngineTaskId("engine-task-11");
        task.setEngineProcessInstanceId("engine-41");
        task.setAssigneeEmployeeId(9L);
        task.setTaskState(1);
        when(taskDao.selectById(11L)).thenReturn(task);
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(31L);
        node.setNodeType("HANDLE_TASK");
        when(definitionNodeDao.selectById(31L)).thenReturn(node);
        when(currentActorProvider.requireCurrentEmployeeId()).thenReturn(9L);
        BpmOrgIdentityGateway identityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        when(identityGateway.requireEmployee(9L))
                .thenReturn(new BpmEmployeeSnapshot(9L, "办理人", 7L, "行政部", null, null));
        setField(service, "bpmOrgIdentityGateway", identityGateway);
        FlowableTaskGateway flowableTaskGateway = Mockito.mock(FlowableTaskGateway.class);
        setField(service, "flowableTaskGateway", flowableTaskGateway);
        BpmTaskProjectionService projectionService = Mockito.mock(BpmTaskProjectionService.class);
        when(projectionService.syncActiveTasksForInstance(41L)).thenReturn(1);
        setField(service, "bpmTaskProjectionService", projectionService);
        BpmInstanceCopyService copyService = Mockito.mock(BpmInstanceCopyService.class);
        when(copyService.createManualCopies(any(), any(), any(), any())).thenReturn(ResponseDTO.ok());
        setField(service, "bpmInstanceCopyService", copyService);
        BpmTaskCompleteForm form = new BpmTaskCompleteForm();
        form.setTaskId(11L);
        form.setCommentText("归档完成");

        ResponseDTO<String> response = service.completeHandleTask(form);

        assertThat(response.getOk()).isTrue();
        verify(flowableTaskGateway).complete("engine-task-11");
        ArgumentCaptor<BpmTaskEntity> captor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(taskDao).updateById(captor.capture());
        assertThat(captor.getValue().getTaskResult()).isEqualTo(BpmTaskResultEnum.HANDLED.getValue());
    }

    private BpmTaskEntity buildGroupMember() {
        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(11L);
        task.setApprovalGroupId(21L);
        task.setTaskState(1);
        return task;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("设置测试字段失败: " + fieldName, ex);
        }
    }

    private static void invokeAttachApprovalGroupSummaries(
            BpmTaskService target,
            List<BpmTaskVO> tasks
    ) {
        try {
            var method = BpmTaskService.class.getDeclaredMethod(
                    "attachApprovalGroupSummaries",
                    List.class
            );
            method.setAccessible(true);
            method.invoke(target, tasks);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("调用审批组摘要装配方法失败", ex);
        }
    }
}
