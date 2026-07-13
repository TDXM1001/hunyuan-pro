package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceCopyDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmRouteDecisionDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmRouteDecisionEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmRuntimeGraphVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmRuntimeGraphService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmGraphRuntimeMetadataService;
import com.hunyuan.sa.bpm.engine.graph.GraphNodeType;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BpmRuntimeGraphServiceTest {

    @Test
    void buildShouldUseFactsForStateAndSanitizeEmployeeReason() {
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        BpmDefinitionNodeDao nodeDao = Mockito.mock(BpmDefinitionNodeDao.class);
        BpmTaskDao taskDao = Mockito.mock(BpmTaskDao.class);
        BpmInstanceCopyDao copyDao = Mockito.mock(BpmInstanceCopyDao.class);
        BpmRouteDecisionDao routeDao = Mockito.mock(BpmRouteDecisionDao.class);
        BpmRuntimeGraphService service = new BpmRuntimeGraphService();
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "bpmDefinitionNodeDao", nodeDao);
        setField(service, "bpmTaskDao", taskDao);
        setField(service, "bpmInstanceCopyDao", copyDao);
        setField(service, "bpmRouteDecisionDao", routeDao);

        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(81L);
        instance.setDefinitionId(18L);
        when(instanceDao.selectById(81L)).thenReturn(instance);
        when(nodeDao.selectList(any())).thenReturn(List.of(
                node(301L, "amount_route", "EXCLUSIVE_BRANCH", "{\"branches\":[{\"branchKey\":\"small\"},{\"branchKey\":\"large\"}]}"),
                node(302L, "small_review", "USER_TASK", "{\"branchPath\":[\"small\"]}"),
                node(303L, "large_review", "USER_TASK", "{\"branchPath\":[\"large\"]}")
        ));
        BpmTaskEntity activeTask = new BpmTaskEntity();
        activeTask.setDefinitionNodeId(303L);
        activeTask.setTaskState(1);
        when(taskDao.selectList(any())).thenReturn(List.of(activeTask));
        when(copyDao.selectList(any())).thenReturn(List.of());
        BpmRouteDecisionEntity decision = new BpmRouteDecisionEntity();
        decision.setRouteDecisionId(901L);
        decision.setInstanceId(81L);
        decision.setDefinitionNodeId(301L);
        decision.setRouteNodeKey("amount_route");
        decision.setMatchedBranchKeysJson("[\"large\"]");
        decision.setReasonSnapshotJson("{\"fieldKey\":\"approvedAmount\",\"value\":6000,\"internalCode\":\"SECRET\"}");
        decision.setInputFormDataVersion(3L);
        decision.setEvaluatedAt(LocalDateTime.now());
        when(routeDao.queryByInstanceId(81L)).thenReturn(List.of(decision));

        BpmRuntimeGraphVO admin = service.build(81L, false);
        BpmRuntimeGraphVO employee = service.build(81L, true);

        assertThat(admin.getNodes()).filteredOn(node -> "small_review".equals(node.getNodeKey()))
                .extracting(BpmRuntimeGraphVO.Node::getState).containsExactly("SKIPPED");
        assertThat(admin.getNodes()).filteredOn(node -> "large_review".equals(node.getNodeKey()))
                .extracting(BpmRuntimeGraphVO.Node::getState).containsExactly("ACTIVE");
        assertThat(admin.getRouteDecisions().get(0).getReasonSnapshotJson())
                .contains("approvedAmount").contains("6000");
        assertThat(employee.getRouteDecisions().get(0).getReasonSnapshotJson())
                .contains("金额条件已满足").doesNotContain("6000").doesNotContain("internalCode");
    }

    @Test
    void buildShouldUseFrozenGraphNodesAndAuthoredTaskKeys() {
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        BpmDefinitionNodeDao nodeDao = Mockito.mock(BpmDefinitionNodeDao.class);
        BpmTaskDao taskDao = Mockito.mock(BpmTaskDao.class);
        BpmInstanceCopyDao copyDao = Mockito.mock(BpmInstanceCopyDao.class);
        BpmRouteDecisionDao routeDao = Mockito.mock(BpmRouteDecisionDao.class);
        BpmGraphRuntimeMetadataService metadataService = Mockito.mock(BpmGraphRuntimeMetadataService.class);
        BpmRuntimeGraphService service = new BpmRuntimeGraphService();
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "bpmDefinitionNodeDao", nodeDao);
        setField(service, "bpmTaskDao", taskDao);
        setField(service, "bpmInstanceCopyDao", copyDao);
        setField(service, "bpmRouteDecisionDao", routeDao);
        setField(service, "bpmGraphRuntimeMetadataService", metadataService);

        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(82L);
        instance.setDefinitionSource("GRAPH");
        instance.setGraphDefinitionVersionId(41L);
        when(instanceDao.selectById(82L)).thenReturn(instance);
        when(metadataService.listNodes(41L)).thenReturn(List.of(
                graphNode("start", "开始", GraphNodeType.START),
                graphNode("archive_handle", "归档办理", GraphNodeType.HANDLE),
                graphNode("end", "结束", GraphNodeType.END)
        ));
        BpmTaskEntity activeTask = new BpmTaskEntity();
        activeTask.setTaskKey("archive_handle");
        activeTask.setTaskState(1);
        when(taskDao.selectList(any())).thenReturn(List.of(activeTask));
        when(copyDao.selectList(any())).thenReturn(List.of());
        when(routeDao.queryByInstanceId(82L)).thenReturn(List.of());

        BpmRuntimeGraphVO graph = service.build(82L, true);

        assertThat(graph.getGraphDefinitionVersionId()).isEqualTo(41L);
        assertThat(graph.getNodes()).extracting(BpmRuntimeGraphVO.Node::getNodeKey)
                .containsExactly("start", "archive_handle", "end");
        assertThat(graph.getNodes()).filteredOn(node -> "archive_handle".equals(node.getNodeKey()))
                .extracting(BpmRuntimeGraphVO.Node::getState).containsExactly("ACTIVE");
        assertThat(graph.getNodes()).filteredOn(node -> "archive_handle".equals(node.getNodeKey()))
                .extracting(BpmRuntimeGraphVO.Node::getNodeType).containsExactly("HANDLE");
    }

    private BpmGraphRuntimeMetadataService.GraphNodeMetadata graphNode(
            String id,
            String name,
            GraphNodeType type
    ) {
        return new BpmGraphRuntimeMetadataService.GraphNodeMetadata(
                id, "scope_root", name, type, new JSONObject(true)
        );
    }

    private BpmDefinitionNodeEntity node(Long id, String key, String type, String snapshot) {
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(id);
        node.setDefinitionId(18L);
        node.setNodeKey(key);
        node.setNodeNameSnapshot(key);
        node.setNodeType(type);
        node.setCompiledNodeSnapshotJson(snapshot);
        return node;
    }

    private void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
