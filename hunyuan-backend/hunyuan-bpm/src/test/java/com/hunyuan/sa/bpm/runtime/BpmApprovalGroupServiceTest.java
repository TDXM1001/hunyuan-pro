package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.common.enumeration.BpmApprovalGroupCloseReasonEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmApprovalGroupStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskResultEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.engine.internal.FlowableTaskGateway;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalGroupDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalGroupEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmApprovalGroupDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskActionLogVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalGroupActionResult;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalGroupService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalMemberAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmApprovalGroupServiceTest {

    private BpmApprovalGroupService service;
    private BpmApprovalGroupDao groupDao;
    private BpmTaskDao taskDao;
    private BpmInstanceDao instanceDao;
    private BpmTaskActionLogDao actionLogDao;
    private FlowableTaskGateway taskGateway;
    private FlowableProcessInstanceGateway processGateway;

    @BeforeEach
    void setUp() {
        service = new BpmApprovalGroupService();
        groupDao = Mockito.mock(BpmApprovalGroupDao.class);
        taskDao = Mockito.mock(BpmTaskDao.class);
        instanceDao = Mockito.mock(BpmInstanceDao.class);
        actionLogDao = Mockito.mock(BpmTaskActionLogDao.class);
        taskGateway = Mockito.mock(FlowableTaskGateway.class);
        processGateway = Mockito.mock(FlowableProcessInstanceGateway.class);
        setField(service, "bpmApprovalGroupDao", groupDao);
        setField(service, "bpmTaskDao", taskDao);
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "bpmTaskActionLogDao", actionLogDao);
        setField(service, "flowableTaskGateway", taskGateway);
        setField(service, "flowableProcessInstanceGateway", processGateway);
    }

    @Test
    void assignApprovalGroupShouldReuseSameEngineInstanceAndGroupKey() {
        BpmInstanceEntity instance = buildInstance("process-8");
        BpmDefinitionNodeEntity node = buildParallelNode("finance_review", "财务会签", 1, 2);
        BpmTaskEntity task = buildMemberTask(11L, 101L);
        BpmApprovalGroupEntity existingGroup = buildPendingGroup(21L, 2);
        when(groupDao.selectByEngineProcessInstanceIdAndGroupKey("process-8", "finance_review"))
                .thenReturn(existingGroup);

        Long groupId = service.assignApprovalGroup(instance, node, task);

        assertThat(groupId).isEqualTo(21L);
        verify(groupDao, never()).insert(any(BpmApprovalGroupEntity.class));
    }

    @Test
    void assignApprovalGroupShouldCreateDifferentGroupsForDifferentEngineInstances() {
        BpmInstanceEntity firstInstance = buildInstance("process-8");
        BpmInstanceEntity secondInstance = buildInstance("process-9");
        secondInstance.setInstanceId(9L);
        BpmDefinitionNodeEntity node = buildParallelNode("finance_review", "财务会签", 1, 2);
        when(groupDao.selectByEngineProcessInstanceIdAndGroupKey(any(), any())).thenReturn(null);
        when(groupDao.insert(any(BpmApprovalGroupEntity.class))).thenAnswer(invocation -> {
            BpmApprovalGroupEntity group = invocation.getArgument(0);
            group.setApprovalGroupId("process-8".equals(group.getEngineProcessInstanceId()) ? 21L : 22L);
            return 1;
        });

        Long firstGroupId = service.assignApprovalGroup(firstInstance, node, buildMemberTask(11L, 101L));
        Long secondGroupId = service.assignApprovalGroup(secondInstance, node, buildMemberTask(12L, 102L));

        assertThat(firstGroupId).isEqualTo(21L);
        assertThat(secondGroupId).isEqualTo(22L);
    }

    @Test
    void approveShouldKeepGroupPendingBeforeLastMember() {
        BpmApprovalGroupEntity group = buildPendingGroup(21L, 2);
        BpmTaskEntity currentTask = buildMemberTask(11L, 101L);
        BpmTaskEntity siblingTask = buildMemberTask(12L, 102L);
        prepareLockedAction(group, currentTask, List.of(currentTask, siblingTask));

        BpmApprovalGroupActionResult result = service.handleMemberAction(
                11L,
                BpmApprovalMemberAction.APPROVE,
                employee(101L, "审批人甲"),
                "同意"
        );

        assertThat(result.processed()).isTrue();
        assertThat(result.groupState()).isEqualTo(BpmApprovalGroupStateEnum.PENDING);
        verify(taskGateway).complete("task-11");
        verify(processGateway, never()).cancel(any(), any());
        ArgumentCaptor<BpmApprovalGroupEntity> groupCaptor = ArgumentCaptor.forClass(BpmApprovalGroupEntity.class);
        verify(groupDao).updateById(groupCaptor.capture());
        assertThat(groupCaptor.getValue().getProcessedMemberCount()).isEqualTo(1);
        assertThat(groupCaptor.getValue().getApprovedMemberCount()).isEqualTo(1);
        assertThat(groupCaptor.getValue().getGroupState()).isEqualTo(BpmApprovalGroupStateEnum.PENDING.name());
    }

    @Test
    void approveShouldCloseGroupOnlyForLastMember() {
        BpmApprovalGroupEntity group = buildPendingGroup(21L, 2);
        group.setProcessedMemberCount(1);
        group.setApprovedMemberCount(1);
        BpmTaskEntity approvedTask = buildMemberTask(11L, 101L);
        approvedTask.setTaskState(BpmTaskStateEnum.COMPLETED.getValue());
        approvedTask.setTaskResult(BpmTaskResultEnum.APPROVED.getValue());
        BpmTaskEntity currentTask = buildMemberTask(12L, 102L);
        prepareLockedAction(group, currentTask, List.of(approvedTask, currentTask));

        BpmApprovalGroupActionResult result = service.handleMemberAction(
                12L,
                BpmApprovalMemberAction.APPROVE,
                employee(102L, "审批人乙"),
                "同意"
        );

        assertThat(result.groupState()).isEqualTo(BpmApprovalGroupStateEnum.APPROVED);
        verify(taskGateway).complete("task-12");
        ArgumentCaptor<BpmApprovalGroupEntity> groupCaptor = ArgumentCaptor.forClass(BpmApprovalGroupEntity.class);
        verify(groupDao).updateById(groupCaptor.capture());
        assertThat(groupCaptor.getValue().getGroupState()).isEqualTo(BpmApprovalGroupStateEnum.APPROVED.name());
        assertThat(groupCaptor.getValue().getCloseReason()).isEqualTo("ALL_APPROVED");
        assertThat(groupCaptor.getValue().getClosedAt()).isNotNull();
    }

    @Test
    void rejectShouldCancelEngineAndPendingSiblingOnce() {
        BpmApprovalGroupEntity group = buildPendingGroup(21L, 2);
        BpmTaskEntity currentTask = buildMemberTask(11L, 101L);
        BpmTaskEntity siblingTask = buildMemberTask(12L, 102L);
        prepareLockedAction(group, currentTask, List.of(currentTask, siblingTask));

        BpmApprovalGroupActionResult result = service.handleMemberAction(
                11L,
                BpmApprovalMemberAction.REJECT,
                employee(101L, "审批人甲"),
                "拒绝"
        );

        assertThat(result.groupState()).isEqualTo(BpmApprovalGroupStateEnum.REJECTED);
        assertThat(result.shouldCancelEngineProcess()).isTrue();
        verify(processGateway).cancel("process-8", "并行会签成员拒绝");
        verify(taskGateway, never()).complete(any());
        ArgumentCaptor<BpmTaskEntity> taskCaptor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(taskDao, Mockito.times(2)).updateById(taskCaptor.capture());
        assertThat(taskCaptor.getAllValues()).anySatisfy(task -> {
            assertThat(task.getTaskId()).isEqualTo(12L);
            assertThat(task.getTaskState()).isEqualTo(BpmTaskStateEnum.CANCELLED.getValue());
        });
    }

    @Test
    void returnShouldCancelEngineAndMoveInstanceToWaitResubmit() {
        BpmApprovalGroupEntity group = buildPendingGroup(21L, 2);
        BpmTaskEntity currentTask = buildMemberTask(11L, 101L);
        BpmTaskEntity siblingTask = buildMemberTask(12L, 102L);
        prepareLockedAction(group, currentTask, List.of(currentTask, siblingTask));

        BpmApprovalGroupActionResult result = service.handleMemberAction(
                11L,
                BpmApprovalMemberAction.RETURN,
                employee(101L, "审批人甲"),
                "补充材料"
        );

        assertThat(result.groupState()).isEqualTo(BpmApprovalGroupStateEnum.RETURNED);
        assertThat(result.waitResubmit()).isTrue();
        verify(processGateway).cancel("process-8", "并行会签成员退回发起人");
        ArgumentCaptor<BpmInstanceEntity> instanceCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(instanceDao).updateById(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getRunState()).isEqualTo(2);
        assertThat(instanceCaptor.getValue().getActiveTaskCount()).isZero();
    }

    @Test
    void repeatedActionShouldNotDriveFlowableTwice() {
        BpmApprovalGroupEntity closedGroup = buildPendingGroup(21L, 2);
        closedGroup.setGroupState(BpmApprovalGroupStateEnum.APPROVED.name());
        BpmTaskEntity task = buildMemberTask(11L, 101L);
        when(taskDao.selectById(11L)).thenReturn(task);
        when(groupDao.selectByIdForUpdate(21L)).thenReturn(closedGroup);

        BpmApprovalGroupActionResult result = service.handleMemberAction(
                11L,
                BpmApprovalMemberAction.APPROVE,
                employee(101L, "审批人甲"),
                "重复提交"
        );

        assertThat(result.processed()).isFalse();
        verify(taskGateway, never()).complete(any());
        verify(processGateway, never()).cancel(any(), any());
        verify(taskDao, never()).selectByIdForUpdate(any());
    }

    @Test
    void handleMemberActionShouldLockGroupBeforeTask() {
        BpmApprovalGroupEntity group = buildPendingGroup(21L, 2);
        BpmTaskEntity currentTask = buildMemberTask(11L, 101L);
        prepareLockedAction(group, currentTask, List.of(currentTask, buildMemberTask(12L, 102L)));

        service.handleMemberAction(
                11L,
                BpmApprovalMemberAction.APPROVE,
                employee(101L, "审批人甲"),
                "同意"
        );

        InOrder inOrder = Mockito.inOrder(groupDao, taskDao);
        inOrder.verify(groupDao).selectByIdForUpdate(21L);
        inOrder.verify(taskDao).selectByIdForUpdate(11L);
        inOrder.verify(taskDao).selectPendingByApprovalGroupIdForUpdate(21L);
    }

    @Test
    void closePendingGroupsShouldCancelEveryPendingMember() {
        BpmApprovalGroupEntity group = buildPendingGroup(21L, 2);
        BpmTaskEntity firstTask = buildMemberTask(11L, 101L);
        BpmTaskEntity secondTask = buildMemberTask(12L, 102L);
        when(groupDao.selectPendingByInstanceIdForUpdate(8L)).thenReturn(List.of(group));
        when(taskDao.selectPendingByApprovalGroupIdForUpdate(21L)).thenReturn(List.of(firstTask, secondTask));

        service.closePendingGroupsForInstance(
                8L,
                BpmApprovalGroupCloseReasonEnum.INSTANCE_RECALLED,
                BpmTaskResultEnum.RECALLED,
                LocalDateTime.of(2026, 7, 10, 12, 0)
        );

        verify(taskDao, Mockito.times(2)).updateById(any(BpmTaskEntity.class));
        ArgumentCaptor<BpmApprovalGroupEntity> groupCaptor = ArgumentCaptor.forClass(BpmApprovalGroupEntity.class);
        verify(groupDao).updateById(groupCaptor.capture());
        assertThat(groupCaptor.getValue().getGroupState()).isEqualTo(BpmApprovalGroupStateEnum.CANCELLED.name());
        assertThat(groupCaptor.getValue().getCloseReason()).isEqualTo("INSTANCE_RECALLED");
    }

    @Test
    void getDetailShouldReturnStructuredMembersAndLastAction() {
        BpmApprovalGroupEntity group = buildPendingGroup(21L, 2);
        BpmTaskEntity firstTask = buildMemberTask(11L, 101L);
        firstTask.setTaskName("财务会签-审批人甲");
        firstTask.setAssigneeNameSnapshot("审批人甲");
        BpmTaskEntity secondTask = buildMemberTask(12L, 102L);
        secondTask.setTaskName("财务会签-审批人乙");
        secondTask.setAssigneeNameSnapshot("审批人乙");
        BpmTaskActionLogVO firstAction = new BpmTaskActionLogVO();
        firstAction.setTaskId(11L);
        firstAction.setActionType("PARALLEL_MEMBER_APPROVED");
        firstAction.setCommentText("同意");
        when(groupDao.selectById(21L)).thenReturn(group);
        when(taskDao.selectByApprovalGroupIds(List.of(21L))).thenReturn(List.of(firstTask, secondTask));
        when(actionLogDao.queryByInstanceId(8L)).thenReturn(List.of(firstAction));

        BpmApprovalGroupDetailVO detail = service.getDetailById(21L);

        assertThat(detail.getApprovalGroupName()).isEqualTo("财务会签");
        assertThat(detail.getMembers()).hasSize(2);
        assertThat(detail.getMembers().get(0).getMemberIndex()).isEqualTo(1);
        assertThat(detail.getMembers().get(0).getAssigneeNameSnapshot()).isEqualTo("审批人甲");
        assertThat(detail.getMembers().get(0).getLastAction().getCommentText()).isEqualTo("同意");
        assertThat(detail.getMembers().get(1).getMemberIndex()).isEqualTo(2);
        assertThat(detail.getMembers().get(1).getLastAction()).isNull();
    }

    private void prepareLockedAction(
            BpmApprovalGroupEntity group,
            BpmTaskEntity currentTask,
            List<BpmTaskEntity> groupTasks
    ) {
        when(taskDao.selectById(currentTask.getTaskId())).thenReturn(currentTask);
        when(groupDao.selectByIdForUpdate(group.getApprovalGroupId())).thenReturn(group);
        when(taskDao.selectByIdForUpdate(currentTask.getTaskId())).thenReturn(currentTask);
        when(taskDao.selectPendingByApprovalGroupIdForUpdate(group.getApprovalGroupId())).thenReturn(groupTasks);
    }

    private BpmInstanceEntity buildInstance(String engineProcessInstanceId) {
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(8L);
        instance.setDefinitionId(2L);
        instance.setEngineProcessInstanceId(engineProcessInstanceId);
        return instance;
    }

    private BpmDefinitionNodeEntity buildParallelNode(
            String groupKey,
            String groupName,
            int parallelIndex,
            int parallelTotal
    ) {
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(5L);
        node.setCompiledNodeSnapshotJson("""
                {"approvalMode":"parallelAll","approvalGroupKey":"%s","approvalGroupName":"%s","parallelIndex":%d,"parallelTotal":%d}
                """.formatted(groupKey, groupName, parallelIndex, parallelTotal));
        return node;
    }

    private BpmApprovalGroupEntity buildPendingGroup(Long groupId, int total) {
        BpmApprovalGroupEntity group = new BpmApprovalGroupEntity();
        group.setApprovalGroupId(groupId);
        group.setInstanceId(8L);
        group.setDefinitionId(2L);
        group.setEngineProcessInstanceId("process-8");
        group.setApprovalGroupKey("finance_review");
        group.setApprovalGroupName("财务会签");
        group.setApprovalMode("parallelAll");
        group.setGroupState(BpmApprovalGroupStateEnum.PENDING.name());
        group.setTotalMemberCount(total);
        group.setProcessedMemberCount(0);
        group.setApprovedMemberCount(0);
        group.setRejectedMemberCount(0);
        return group;
    }

    private BpmTaskEntity buildMemberTask(Long taskId, Long assigneeEmployeeId) {
        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(taskId);
        task.setInstanceId(8L);
        task.setDefinitionId(2L);
        task.setDefinitionNodeId(5L);
        task.setApprovalGroupId(21L);
        task.setEngineTaskId("task-" + taskId);
        task.setEngineProcessInstanceId("process-8");
        task.setTaskState(BpmTaskStateEnum.PENDING.getValue());
        task.setAssigneeEmployeeId(assigneeEmployeeId);
        return task;
    }

    private BpmEmployeeSnapshot employee(Long employeeId, String name) {
        return new BpmEmployeeSnapshot(employeeId, name, 7L, "财务部", null, null);
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
}
