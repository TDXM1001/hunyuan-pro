package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.engine.route.BpmRouteExpressionRegistry;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.RoutingDataSnapshot;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmApprovalRuntimeDataService;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmRouteDecisionDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmRouteDecisionEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmRouteConditionEvaluator;
import com.hunyuan.sa.bpm.module.runtime.service.BpmRouteDecisionCommand;
import com.hunyuan.sa.bpm.module.runtime.service.BpmRouteDecisionResult;
import com.hunyuan.sa.bpm.module.runtime.service.BpmRouteDecisionService;
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

class BpmRouteDecisionServiceTest {

    private BpmInstanceDao instanceDao;
    private BpmDefinitionNodeDao definitionNodeDao;
    private BpmRouteDecisionDao routeDecisionDao;
    private BpmRouteDecisionService service;

    @BeforeEach
    void setUp() {
        instanceDao = Mockito.mock(BpmInstanceDao.class);
        definitionNodeDao = Mockito.mock(BpmDefinitionNodeDao.class);
        routeDecisionDao = Mockito.mock(BpmRouteDecisionDao.class);
        service = new BpmRouteDecisionService();
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "bpmDefinitionNodeDao", definitionNodeDao);
        setField(service, "bpmRouteDecisionDao", routeDecisionDao);
        setField(service, "conditionEvaluator", new BpmRouteConditionEvaluator(new BpmRouteExpressionRegistry(List.of())));
    }

    @Test
    void evaluateAndRecordShouldPersistMatchedBranchWithInputVersion() {
        when(instanceDao.selectByIdForUpdate(81L)).thenReturn(instance());
        when(definitionNodeDao.selectOne(any())).thenReturn(routeNode());
        when(routeDecisionDao.selectByGenerationAndNode(81L, "engine-1", "amount_route"))
                .thenReturn(null);
        when(routeDecisionDao.insert(any(BpmRouteDecisionEntity.class))).thenAnswer(invocation -> {
            invocation.<BpmRouteDecisionEntity>getArgument(0).setRouteDecisionId(901L);
            return 1;
        });

        BpmRouteDecisionResult result = service.evaluateAndRecord(
                new BpmRouteDecisionCommand(81L, "engine-1", "amount_route")
        );

        assertThat(result.routeDecisionId()).isEqualTo(901L);
        assertThat(result.matchedBranchKeys()).containsExactly("large");
        assertThat(result.defaultBranchUsed()).isFalse();
        assertThat(result.inputFormDataVersion()).isEqualTo(3L);
        ArgumentCaptor<BpmRouteDecisionEntity> captor = ArgumentCaptor.forClass(BpmRouteDecisionEntity.class);
        verify(routeDecisionDao).insert(captor.capture());
        assertThat(captor.getValue().getEngineProcessInstanceId()).isEqualTo("engine-1");
        assertThat(captor.getValue().getMatchedBranchKeysJson()).isEqualTo("[\"large\"]");
    }

    @Test
    void evaluateAndRecordShouldReuseExistingDecisionInSameEngineGeneration() {
        BpmRouteDecisionEntity existing = new BpmRouteDecisionEntity();
        existing.setRouteDecisionId(902L);
        existing.setInputFormDataVersion(3L);
        existing.setMatchedBranchKeysJson("[\"large\"]");
        existing.setDefaultBranchUsed(false);
        when(routeDecisionDao.selectByGenerationAndNode(81L, "engine-1", "amount_route"))
                .thenReturn(existing);

        BpmRouteDecisionResult result = service.evaluateAndRecord(
                new BpmRouteDecisionCommand(81L, "engine-1", "amount_route")
        );

        assertThat(result.routeDecisionId()).isEqualTo(902L);
        assertThat(result.matchedBranchKeys()).containsExactly("large");
        verify(instanceDao, never()).selectByIdForUpdate(any());
        verify(routeDecisionDao, never()).insert(any(BpmRouteDecisionEntity.class));
    }

    @Test
    void evaluateAndRecordShouldUseFrozenRoutingFactsForM3Instance() {
        BpmInstanceEntity instance = instance();
        instance.setCurrentFormDataSnapshotJson("{\"amount\":100}");
        instance.setRoutingFactSnapshotId(51L);
        when(instanceDao.selectByIdForUpdate(81L)).thenReturn(instance);
        when(definitionNodeDao.selectOne(any())).thenReturn(routeNode());
        when(routeDecisionDao.selectByGenerationAndNode(81L, "engine-1", "amount_route"))
                .thenReturn(null);
        when(routeDecisionDao.insert(any(BpmRouteDecisionEntity.class))).thenAnswer(invocation -> {
            invocation.<BpmRouteDecisionEntity>getArgument(0).setRouteDecisionId(903L);
            return 1;
        });
        BpmApprovalRuntimeDataService runtimeDataService = Mockito.mock(BpmApprovalRuntimeDataService.class);
        when(runtimeDataService.routingData(51L))
                .thenReturn(new RoutingDataSnapshot(7L, Map.of("amount", 9000)));
        setField(service, "bpmApprovalRuntimeDataService", runtimeDataService);

        BpmRouteDecisionResult result = service.evaluateAndRecord(
                new BpmRouteDecisionCommand(81L, "engine-1", "amount_route")
        );

        assertThat(result.matchedBranchKeys()).containsExactly("large");
        assertThat(result.inputFormDataVersion()).isEqualTo(7L);
    }

    @Test
    void evaluateAndRecordShouldUseFrozenGraphRouteProperties() {
        BpmInstanceEntity instance = instance();
        instance.setDefinitionId(null);
        instance.setDefinitionSource("GRAPH");
        instance.setGraphDefinitionVersionId(41L);
        when(instanceDao.selectByIdForUpdate(81L)).thenReturn(instance);
        when(routeDecisionDao.selectByGenerationAndNode(81L, "engine-1", "amount_route"))
                .thenReturn(null);
        when(routeDecisionDao.insert(any(BpmRouteDecisionEntity.class))).thenAnswer(invocation -> {
            invocation.<BpmRouteDecisionEntity>getArgument(0).setRouteDecisionId(904L);
            return 1;
        });
        GraphDefinitionVersionDao graphVersionDao = Mockito.mock(GraphDefinitionVersionDao.class);
        GraphDefinitionVersionEntity graphVersion = new GraphDefinitionVersionEntity();
        graphVersion.setGraphDefinitionVersionId(41L);
        graphVersion.setGraphSnapshotJson("""
                {"schemaVersion":1,"rootScopeId":"scope_root","scopes":[],"nodes":[
                  {"nodeId":"amount_route","scopeId":"scope_root","type":"CONDITION","name":"金额路由","properties":{"gatewayMode":"SPLIT"},"layout":{}}
                ],"edges":[
                  {"edgeId":"small","scopeId":"scope_root","sourceNodeId":"amount_route","targetNodeId":"end","sourcePort":"small","properties":{"routeCondition":{"sourceType":"FORM_FIELD","fieldKey":"amount","valueType":"NUMBER","operator":"LTE","compareValue":5000}}},
                  {"edgeId":"large","scopeId":"scope_root","sourceNodeId":"amount_route","targetNodeId":"end","sourcePort":"large","properties":{"routeCondition":{"sourceType":"FORM_FIELD","fieldKey":"amount","valueType":"NUMBER","operator":"GT","compareValue":5000}}},
                  {"edgeId":"default","scopeId":"scope_root","sourceNodeId":"amount_route","targetNodeId":"end","sourcePort":"default","properties":{}}
                ],"metadata":{}}
                """);
        when(graphVersionDao.selectById(41L)).thenReturn(graphVersion);
        setField(service, "graphDefinitionVersionDao", graphVersionDao);

        BpmRouteDecisionResult result = service.evaluateAndRecord(
                new BpmRouteDecisionCommand(81L, "engine-1", "amount_route")
        );

        assertThat(result.matchedBranchKeys()).containsExactly("large");
        ArgumentCaptor<BpmRouteDecisionEntity> captor = ArgumentCaptor.forClass(BpmRouteDecisionEntity.class);
        verify(routeDecisionDao).insert(captor.capture());
        assertThat(captor.getValue().getGraphDefinitionVersionId()).isEqualTo(41L);
        assertThat(captor.getValue().getDefinitionNodeId()).isNull();
    }

    private BpmInstanceEntity instance() {
        BpmInstanceEntity entity = new BpmInstanceEntity();
        entity.setInstanceId(81L);
        entity.setDefinitionId(18L);
        entity.setFormDataVersion(3L);
        entity.setCurrentFormDataSnapshotJson("{\"amount\":6000}");
        return entity;
    }

    private BpmDefinitionNodeEntity routeNode() {
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(301L);
        node.setDefinitionId(18L);
        node.setNodeKey("amount_route");
        node.setNodeType("EXCLUSIVE_BRANCH");
        node.setCompiledNodeSnapshotJson("""
                {"nodeKey":"amount_route","type":"EXCLUSIVE_BRANCH","branches":[
                  {"branchKey":"small","condition":{"sourceType":"FORM_FIELD","fieldKey":"amount","valueType":"NUMBER","operator":"LTE","compareValue":5000},"nodes":[]},
                  {"branchKey":"large","condition":{"sourceType":"FORM_FIELD","fieldKey":"amount","valueType":"NUMBER","operator":"GT","compareValue":5000},"nodes":[]},
                  {"branchKey":"default","isDefault":true,"nodes":[]}
                ]}
                """);
        return node;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
