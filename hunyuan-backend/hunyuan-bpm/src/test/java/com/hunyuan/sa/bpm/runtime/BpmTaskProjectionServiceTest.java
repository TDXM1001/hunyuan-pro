package com.hunyuan.sa.bpm.runtime;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmTaskStateEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableActiveTaskSnapshot;
import com.hunyuan.sa.bpm.engine.internal.FlowableTaskGateway;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmNotificationCommand;
import com.hunyuan.sa.bpm.module.runtime.service.BpmNotificationListenerService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalGroupService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmGraphRuntimeMetadataService;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.engine.graph.GraphNodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
class BpmTaskProjectionServiceTest {

    private BpmTaskProjectionService service;
    private BpmInstanceDao bpmInstanceDao;
    private BpmTaskDao bpmTaskDao;
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;
    private FlowableTaskGateway flowableTaskGateway;
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;
    private BpmNotificationListenerService bpmNotificationListenerService;
    private BpmApprovalGroupService bpmApprovalGroupService;

    @BeforeEach
    void setUp() {
        service = new BpmTaskProjectionService();
        bpmInstanceDao = Mockito.mock(BpmInstanceDao.class);
        bpmTaskDao = Mockito.mock(BpmTaskDao.class);
        bpmDefinitionNodeDao = Mockito.mock(BpmDefinitionNodeDao.class);
        flowableTaskGateway = Mockito.mock(FlowableTaskGateway.class);
        bpmOrgIdentityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        bpmNotificationListenerService = Mockito.mock(BpmNotificationListenerService.class);
        bpmApprovalGroupService = Mockito.mock(BpmApprovalGroupService.class);
        setField(service, "bpmInstanceDao", bpmInstanceDao);
        setField(service, "bpmTaskDao", bpmTaskDao);
        setField(service, "bpmDefinitionNodeDao", bpmDefinitionNodeDao);
        setField(service, "flowableTaskGateway", flowableTaskGateway);
        setField(service, "bpmOrgIdentityGateway", bpmOrgIdentityGateway);
        setField(service, "bpmNotificationListenerService", bpmNotificationListenerService);
        setField(service, "bpmApprovalGroupService", bpmApprovalGroupService);
    }

    @Test
    void syncActiveTasksShouldInsertMissingTaskProjectionAndUpdateActiveCount() {
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(8L);
        instance.setDefinitionId(2L);
        instance.setEngineProcessInstanceId("process-1");
        instance.setInstanceNo("SN-2026-0001");
        instance.setTitle("请假申请");
        instance.setStartEmployeeId(100L);
        instance.setStartEmployeeNameSnapshot("张三");
        instance.setCategoryIdSnapshot(7L);
        instance.setCategoryNameSnapshot("人事流程");

        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(5L);
        node.setNodeKey("approve_1");

        when(bpmInstanceDao.selectById(8L)).thenReturn(instance);
        when(flowableTaskGateway.queryActiveTasksByProcessInstanceId("process-1")).thenReturn(List.of(
                new FlowableActiveTaskSnapshot("task-1", "execution-1", "process-1", "approve_1", "一级审批", 22L)
        ));
        when(bpmTaskDao.selectOne(any(Wrapper.class))).thenReturn(null);
        when(bpmDefinitionNodeDao.selectOne(any(Wrapper.class))).thenReturn(node);
        when(bpmOrgIdentityGateway.requireEmployee(22L)).thenReturn(new BpmEmployeeSnapshot(22L, "李四", 9L, "财务部", null, null));

        int activeCount = service.syncActiveTasksForInstance(8L);

        assertThat(activeCount).isEqualTo(1);
        ArgumentCaptor<BpmTaskEntity> taskCaptor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(bpmTaskDao).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getEngineTaskId()).isEqualTo("task-1");
        assertThat(taskCaptor.getValue().getTaskKey()).isEqualTo("approve_1");
        assertThat(taskCaptor.getValue().getTaskState()).isEqualTo(BpmTaskStateEnum.PENDING.getValue());
        assertThat(taskCaptor.getValue().getAssigneeEmployeeId()).isEqualTo(22L);
        assertThat(taskCaptor.getValue().getAssigneeNameSnapshot()).isEqualTo("李四");

        ArgumentCaptor<BpmInstanceEntity> instanceCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(bpmInstanceDao).updateById(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getInstanceId()).isEqualTo(8L);
        assertThat(instanceCaptor.getValue().getActiveTaskCount()).isEqualTo(1);
        assertThat(instanceCaptor.getValue().getCurrentNodeSummaryJson()).contains("approve_1");
    }

    @Test
    void syncActiveTasksShouldDispatchNotificationWhenNewTaskHasMessageListener() {
        BpmInstanceEntity instance = buildInstance();
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(5L);
        node.setNodeKey("approve_1");
        node.setCompiledNodeSnapshotJson("{\"listeners\":[{\"channel\":\"MESSAGE\"}]}");
        when(bpmInstanceDao.selectById(8L)).thenReturn(instance);
        when(flowableTaskGateway.queryActiveTasksByProcessInstanceId("process-1")).thenReturn(List.of(
                new FlowableActiveTaskSnapshot("task-1", "execution-1", "process-1", "approve_1", "一级审批", 22L)
        ));
        when(bpmTaskDao.selectOne(any(Wrapper.class))).thenReturn(null);
        when(bpmDefinitionNodeDao.selectOne(any(Wrapper.class))).thenReturn(node);
        when(bpmOrgIdentityGateway.requireEmployee(22L)).thenReturn(new BpmEmployeeSnapshot(22L, "李四", 9L, "财务部", "13800000000", "lisi@example.com"));
        when(bpmTaskDao.insert(any(BpmTaskEntity.class))).thenAnswer(invocation -> {
            BpmTaskEntity task = invocation.getArgument(0);
            task.setTaskId(11L);
            return 1;
        });

        service.syncActiveTasksForInstance(8L);

        ArgumentCaptor<BpmNotificationCommand> commandCaptor = ArgumentCaptor.forClass(BpmNotificationCommand.class);
        verify(bpmNotificationListenerService).dispatch(commandCaptor.capture());
        assertThat(commandCaptor.getValue().safeChannels()).containsExactly("MESSAGE");
        assertThat(commandCaptor.getValue().instanceId()).isEqualTo(8L);
        assertThat(commandCaptor.getValue().taskId()).isEqualTo(11L);
        assertThat(commandCaptor.getValue().definitionNodeId()).isEqualTo(5L);
        assertThat(commandCaptor.getValue().eventKey()).isEqualTo("TASK_CREATED");
        assertThat(commandCaptor.getValue().receiverEmployeeId()).isEqualTo(22L);
        assertThat(commandCaptor.getValue().title()).isEqualTo("流程待办提醒");
        assertThat(commandCaptor.getValue().subject()).isEqualTo("流程待办提醒");
        assertThat(commandCaptor.getValue().content()).isEqualTo("你有一个新的流程待办：请假申请");
    }

    @Test
    void syncActiveTasksShouldIncludeApprovalGroupProgressInParallelNotification() {
        BpmInstanceEntity instance = buildInstance();
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(5L);
        node.setNodeKey("finance_review_1");
        node.setCompiledNodeSnapshotJson("""
                {
                  "approvalMode":"parallelAll",
                  "approvalGroupKey":"finance_review",
                  "approvalGroupName":"财务会签",
                  "parallelIndex":1,
                  "parallelTotal":3,
                  "listeners":[{"channel":"MESSAGE"}]
                }
                """);
        when(bpmInstanceDao.selectById(8L)).thenReturn(instance);
        when(flowableTaskGateway.queryActiveTasksByProcessInstanceId("process-1")).thenReturn(List.of(
                new FlowableActiveTaskSnapshot(
                        "task-1",
                        "execution-1",
                        "process-1",
                        "finance_review_1",
                        "财务会签-审批人甲",
                        101L
                )
        ));
        when(bpmTaskDao.selectOne(any(Wrapper.class))).thenReturn(null);
        when(bpmDefinitionNodeDao.selectOne(any(Wrapper.class))).thenReturn(node);
        when(bpmOrgIdentityGateway.requireEmployee(101L)).thenReturn(
                new BpmEmployeeSnapshot(101L, "审批人甲", 9L, "财务部", null, null)
        );
        when(bpmApprovalGroupService.assignApprovalGroup(any(), any(), any())).thenReturn(21L);
        when(bpmTaskDao.insert(any(BpmTaskEntity.class))).thenAnswer(invocation -> {
            BpmTaskEntity task = invocation.getArgument(0);
            task.setTaskId(11L);
            return 1;
        });

        service.syncActiveTasksForInstance(8L);

        ArgumentCaptor<BpmNotificationCommand> commandCaptor = ArgumentCaptor.forClass(BpmNotificationCommand.class);
        verify(bpmNotificationListenerService).dispatch(commandCaptor.capture());
        assertThat(commandCaptor.getValue().title()).isEqualTo("流程会签待办提醒");
        assertThat(commandCaptor.getValue().subject()).isEqualTo("流程会签待办提醒");
        assertThat(commandCaptor.getValue().content())
                .isEqualTo("你有一个新的流程待办：请假申请；审批组：财务会签，成员 1/3");
    }

    @Test
    void syncActiveTasksShouldNotDispatchNotificationWhenTaskAlreadyExists() {
        BpmInstanceEntity instance = buildInstance();
        BpmTaskEntity existingTask = new BpmTaskEntity();
        existingTask.setTaskId(11L);
        when(bpmInstanceDao.selectById(8L)).thenReturn(instance);
        when(flowableTaskGateway.queryActiveTasksByProcessInstanceId("process-1")).thenReturn(List.of(
                new FlowableActiveTaskSnapshot("task-1", "execution-1", "process-1", "approve_1", "一级审批", 22L)
        ));
        when(bpmTaskDao.selectOne(any(Wrapper.class))).thenReturn(existingTask);

        service.syncActiveTasksForInstance(8L);

        verify(bpmNotificationListenerService, never()).dispatch(any(BpmNotificationCommand.class));
    }

    @Test
    void syncShouldAttachGroupToExistingSequentialTaskWhenGroupIdIsMissing() {
        BpmInstanceEntity instance = buildInstance();
        BpmTaskEntity existing = new BpmTaskEntity();
        existing.setTaskId(22L);
        existing.setDefinitionNodeId(12L);
        existing.setEngineTaskId("engine-task-2");
        existing.setEngineProcessInstanceId("process-1");
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(12L);
        node.setNodeKey("finance_review_2");
        node.setCompiledNodeSnapshotJson("{\"approvalMode\":\"sequential\","
                + "\"approvalGroupKey\":\"finance_review\","
                + "\"approvalGroupName\":\"财务复核\","
                + "\"sequentialIndex\":2,\"sequentialTotal\":3}");
        when(bpmInstanceDao.selectById(8L)).thenReturn(instance);
        when(flowableTaskGateway.queryActiveTasksByProcessInstanceId("process-1")).thenReturn(List.of(
                new FlowableActiveTaskSnapshot(
                        "engine-task-2", "execution-2", "process-1", "finance_review_2", "财务复核", 102L
                )
        ));
        when(bpmTaskDao.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(bpmDefinitionNodeDao.selectOne(any(Wrapper.class))).thenReturn(node);
        when(bpmApprovalGroupService.assignApprovalGroup(any(), same(node), same(existing)))
                .thenReturn(31L);

        service.syncActiveTasksForInstance(8L);

        verify(bpmTaskDao).updateById(argThat((BpmTaskEntity update) ->
                update.getTaskId().equals(existing.getTaskId())
                        && update.getApprovalGroupId().equals(31L)));
    }

    @Test
    void syncActiveTasksShouldNotDispatchNotificationWhenAssigneeIsMissing() {
        BpmInstanceEntity instance = buildInstance();
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(5L);
        node.setNodeKey("approve_1");
        node.setCompiledNodeSnapshotJson("{\"listeners\":[{\"channel\":\"MESSAGE\"}]}");
        when(bpmInstanceDao.selectById(8L)).thenReturn(instance);
        when(flowableTaskGateway.queryActiveTasksByProcessInstanceId("process-1")).thenReturn(List.of(
                new FlowableActiveTaskSnapshot("task-1", "execution-1", "process-1", "approve_1", "一级审批", null)
        ));
        when(bpmTaskDao.selectOne(any(Wrapper.class))).thenReturn(null);
        when(bpmDefinitionNodeDao.selectOne(any(Wrapper.class))).thenReturn(node);

        service.syncActiveTasksForInstance(8L);

        verify(bpmNotificationListenerService, never()).dispatch(any(BpmNotificationCommand.class));
    }

    @Test
    void syncActiveTasksShouldAssignSameApprovalGroupToParallelMembers() {
        BpmInstanceEntity instance = buildInstance();
        BpmDefinitionNodeEntity firstNode = new BpmDefinitionNodeEntity();
        firstNode.setDefinitionNodeId(5L);
        firstNode.setNodeKey("finance_review_1");
        firstNode.setCompiledNodeSnapshotJson("""
                {"approvalMode":"parallelAll","approvalGroupKey":"finance_review","approvalGroupName":"财务会签","parallelIndex":1,"parallelTotal":2}
                """);
        BpmDefinitionNodeEntity secondNode = new BpmDefinitionNodeEntity();
        secondNode.setDefinitionNodeId(6L);
        secondNode.setNodeKey("finance_review_2");
        secondNode.setCompiledNodeSnapshotJson("""
                {"approvalMode":"parallelAll","approvalGroupKey":"finance_review","approvalGroupName":"财务会签","parallelIndex":2,"parallelTotal":2}
                """);
        when(bpmInstanceDao.selectById(8L)).thenReturn(instance);
        when(flowableTaskGateway.queryActiveTasksByProcessInstanceId("process-1")).thenReturn(List.of(
                new FlowableActiveTaskSnapshot("task-1", "execution-1", "process-1", "finance_review_1", "财务会签-审批人甲", 101L),
                new FlowableActiveTaskSnapshot("task-2", "execution-2", "process-1", "finance_review_2", "财务会签-审批人乙", 102L)
        ));
        when(bpmTaskDao.selectOne(any(Wrapper.class))).thenReturn(null);
        when(bpmDefinitionNodeDao.selectOne(any(Wrapper.class))).thenReturn(firstNode, secondNode);
        when(bpmOrgIdentityGateway.requireEmployee(101L)).thenReturn(
                new BpmEmployeeSnapshot(101L, "审批人甲", 9L, "财务部", null, null)
        );
        when(bpmOrgIdentityGateway.requireEmployee(102L)).thenReturn(
                new BpmEmployeeSnapshot(102L, "审批人乙", 9L, "财务部", null, null)
        );
        when(bpmApprovalGroupService.assignApprovalGroup(any(), any(), any())).thenReturn(21L);

        service.syncActiveTasksForInstance(8L);

        ArgumentCaptor<BpmTaskEntity> taskCaptor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(bpmTaskDao, Mockito.times(2)).insert(taskCaptor.capture());
        assertThat(taskCaptor.getAllValues())
                .extracting(BpmTaskEntity::getApprovalGroupId)
                .containsExactly(21L, 21L);
    }

    @Test
    void syncActiveTasksShouldKeepOrdinaryTaskApprovalGroupNull() {
        BpmInstanceEntity instance = buildInstance();
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(5L);
        node.setNodeKey("approve_1");
        node.setCompiledNodeSnapshotJson("{\"approvalMode\":\"single\"}");
        when(bpmInstanceDao.selectById(8L)).thenReturn(instance);
        when(flowableTaskGateway.queryActiveTasksByProcessInstanceId("process-1")).thenReturn(List.of(
                new FlowableActiveTaskSnapshot("task-1", "execution-1", "process-1", "approve_1", "一级审批", 22L)
        ));
        when(bpmTaskDao.selectOne(any(Wrapper.class))).thenReturn(null);
        when(bpmDefinitionNodeDao.selectOne(any(Wrapper.class))).thenReturn(node);
        when(bpmOrgIdentityGateway.requireEmployee(22L)).thenReturn(
                new BpmEmployeeSnapshot(22L, "李四", 9L, "财务部", null, null)
        );

        service.syncActiveTasksForInstance(8L);

        ArgumentCaptor<BpmTaskEntity> taskCaptor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(bpmTaskDao).insert(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getApprovalGroupId()).isNull();
        verify(bpmApprovalGroupService, never()).assignApprovalGroup(any(), any(), any());
    }

    @Test
    void syncGraphHandleTaskShouldPersistAuthoredNodeIdentity() {
        BpmInstanceEntity instance = buildInstance();
        instance.setDefinitionId(null);
        instance.setDefinitionSource("GRAPH");
        instance.setGraphDefinitionVersionId(41L);
        BpmGraphRuntimeMetadataService metadataService = Mockito.mock(BpmGraphRuntimeMetadataService.class);
        setField(service, "bpmGraphRuntimeMetadataService", metadataService);
        when(metadataService.resolveCompiledNode(41L, "graph_node_archive_handle"))
                .thenReturn(new BpmGraphRuntimeMetadataService.GraphNodeMetadata(
                        "archive_handle", "scope_root", "归档办理", GraphNodeType.HANDLE, new JSONObject(true)
                ));
        when(bpmInstanceDao.selectById(8L)).thenReturn(instance);
        when(flowableTaskGateway.queryActiveTasksByProcessInstanceId("process-1")).thenReturn(List.of(
                new FlowableActiveTaskSnapshot(
                        "task-graph-1", "execution-graph-1", "process-1",
                        "graph_node_archive_handle", "归档办理", 22L
                )
        ));
        when(bpmTaskDao.selectOne(any(Wrapper.class))).thenReturn(null);
        when(bpmOrgIdentityGateway.requireEmployee(22L)).thenReturn(
                new BpmEmployeeSnapshot(22L, "李四", 9L, "财务部", null, null)
        );

        service.syncActiveTasksForInstance(8L);

        ArgumentCaptor<BpmTaskEntity> captor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(bpmTaskDao).insert(captor.capture());
        assertThat(captor.getValue().getDefinitionSource()).isEqualTo("GRAPH");
        assertThat(captor.getValue().getGraphDefinitionVersionId()).isEqualTo(41L);
        assertThat(captor.getValue().getTaskKey()).isEqualTo("archive_handle");
        assertThat(captor.getValue().getTaskName()).isEqualTo("归档办理");
        assertThat(captor.getValue().getDefinitionNodeId()).isNull();
    }

    private BpmInstanceEntity buildInstance() {
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(8L);
        instance.setDefinitionId(2L);
        instance.setEngineProcessInstanceId("process-1");
        instance.setInstanceNo("SN-2026-0001");
        instance.setTitle("请假申请");
        instance.setStartEmployeeId(100L);
        instance.setStartEmployeeNameSnapshot("张三");
        instance.setCategoryIdSnapshot(7L);
        instance.setCategoryNameSnapshot("人事流程");
        return instance;
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
