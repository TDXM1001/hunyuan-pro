package com.hunyuan.sa.bpm.definition;

import com.hunyuan.sa.bpm.engine.compiler.graph.GraphBpmnCompiler;
import com.hunyuan.sa.bpm.engine.graph.*;
import com.hunyuan.sa.bpm.engine.internal.GraphFlowableDeployment;
import com.hunyuan.sa.bpm.engine.internal.GraphFlowableDeploymentGateway;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionElementMappingDao;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionElementMappingEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.form.GraphDefinitionPublishCommand;
import com.hunyuan.sa.bpm.module.category.dao.BpmCategoryDao;
import com.hunyuan.sa.bpm.module.category.domain.entity.BpmCategoryEntity;
import com.hunyuan.sa.bpm.module.model.dao.BpmProcessDraftDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmProcessDraftEntity;
import com.hunyuan.sa.bpm.module.definition.service.GraphPublicationDependencyResolver;
import com.hunyuan.sa.bpm.module.definition.service.GraphPublicationDependencySnapshot;
import com.hunyuan.sa.bpm.module.definition.service.GraphDefinitionPublicationService;
import com.hunyuan.sa.bpm.module.definition.service.M5GraphPublicationResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GraphDefinitionPublicationServiceTest {

    private GraphDefinitionPublicationService service;
    private BpmProcessDraftDao draftDao;
    private GraphDefinitionVersionDao versionDao;
    private GraphDefinitionElementMappingDao mappingDao;
    private GraphFlowableDeploymentGateway deploymentGateway;
    private BpmCategoryDao categoryDao;

    @BeforeEach
    void setUp() {
        service = new GraphDefinitionPublicationService();
        draftDao = Mockito.mock(BpmProcessDraftDao.class);
        versionDao = Mockito.mock(GraphDefinitionVersionDao.class);
        mappingDao = Mockito.mock(GraphDefinitionElementMappingDao.class);
        deploymentGateway = Mockito.mock(GraphFlowableDeploymentGateway.class);
        categoryDao = Mockito.mock(BpmCategoryDao.class);
        setField(service, "bpmProcessDraftDao", draftDao);
        setField(service, "graphDefinitionVersionDao", versionDao);
        setField(service, "graphDefinitionElementMappingDao", mappingDao);
        setField(service, "graphFlowableDeploymentGateway", deploymentGateway);
        setField(service, "bpmCategoryDao", categoryDao);
        BpmCategoryEntity category = new BpmCategoryEntity();
        category.setCategoryId(7L);
        category.setCategoryName("通用流程");
        when(categoryDao.selectById(7L)).thenReturn(category);
        setField(
                service,
                "graphPublicationDependencyResolver",
                (GraphPublicationDependencyResolver) graph -> new GraphPublicationDependencySnapshot(Map.of())
        );
        M5GraphPublicationResolver m5Resolver = Mockito.mock(M5GraphPublicationResolver.class);
        when(m5Resolver.resolve(any())).thenAnswer(invocation ->
                new M5GraphPublicationResolver.ResolvedGraph(invocation.getArgument(0), Map.of()));
        setField(service, "m5GraphPublicationResolver", m5Resolver);
    }

    @Test
    void publicationServiceShouldExposeImmutableSnapshotPublicationContract() {
        assertThat(service.compilerVersion()).isEqualTo("graph-v1");
    }

    @Test
    void publishShouldFreezeGraphBpmnDependenciesAndMappings() {
        when(draftDao.selectById(1L)).thenReturn(draft());
        when(versionDao.selectMaxVersionByProcessKey("expense")).thenReturn(1);
        when(deploymentGateway.deploy(any(), any(), any())).thenReturn(new GraphFlowableDeployment("dep-1", "expense:2"));
        when(versionDao.insert(any(GraphDefinitionVersionEntity.class))).thenAnswer(invocation -> { invocation.<GraphDefinitionVersionEntity>getArgument(0).setGraphDefinitionVersionId(10L); return 1; });

        Long versionId = service.publish(new GraphDefinitionPublishCommand(1L, 7L));

        assertThat(versionId).isEqualTo(10L);
        verify(mappingDao, times(5)).insert(any(com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionElementMappingEntity.class));
        verify(deploymentGateway, never()).delete(any());
    }

    @Test
    void publishShouldDeleteFlowableDeploymentWhenMappingPersistenceFails() {
        when(draftDao.selectById(1L)).thenReturn(draft());
        when(versionDao.selectMaxVersionByProcessKey("expense")).thenReturn(null);
        GraphFlowableDeployment deployment = new GraphFlowableDeployment("dep-1", "expense:1");
        when(deploymentGateway.deploy(any(), any(), any())).thenReturn(deployment);
        when(versionDao.insert(any(GraphDefinitionVersionEntity.class))).thenAnswer(invocation -> { invocation.<GraphDefinitionVersionEntity>getArgument(0).setGraphDefinitionVersionId(10L); return 1; });
        doThrow(new IllegalStateException("db failed")).when(mappingDao).insert(any(com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionElementMappingEntity.class));

        assertThatThrownBy(() -> service.publish(new GraphDefinitionPublishCommand(1L, 7L)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("db failed");
        verify(deploymentGateway).delete(deployment);
    }

    @Test
    void publishShouldRejectDraftWithoutCategoryBeforeDeployingFlowable() {
        BpmProcessDraftEntity draft = draft();
        draft.setCategoryId(null);
        when(draftDao.selectById(1L)).thenReturn(draft);

        assertThatThrownBy(() -> service.publish(new GraphDefinitionPublishCommand(1L, 7L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("流程分类");

        verify(deploymentGateway, never()).deploy(any(), any(), any());
    }

    @Test
    void deactivateShouldSuspendEngineDefinitionBeforeMarkingGraphVersionInactive() {
        GraphDefinitionVersionEntity version = new GraphDefinitionVersionEntity();
        version.setGraphDefinitionVersionId(10L);
        version.setEngineProcessDefinitionId("expense:2");
        when(versionDao.selectById(10L)).thenReturn(version);
        when(versionDao.deactivate(10L)).thenReturn(1);

        service.deactivate(10L);

        verify(deploymentGateway).suspend("expense:2");
        verify(versionDao).deactivate(10L);
    }

    @Test
    void definitionDetailShouldExposeFrozenArtifactAndAuthoredCompiledMappings() throws Exception {
        GraphDefinitionVersionEntity version = new GraphDefinitionVersionEntity();
        version.setGraphDefinitionVersionId(10L);
        version.setProcessKey("expense");
        version.setDefinitionVersion(2);
        version.setLifecycleState("ACTIVE");
        version.setSemanticHash("semantic-hash");
        version.setCompiledBpmnXml("<definitions/>");
        version.setEngineProcessDefinitionId("expense:2");
        GraphDefinitionElementMappingEntity mapping = new GraphDefinitionElementMappingEntity();
        mapping.setAuthoredElementId("route_large");
        mapping.setAuthoredElementKind("EDGE");
        mapping.setCompiledElementId("graph_edge_route_large");
        mapping.setCompiledElementType("sequenceFlow");
        when(versionDao.selectById(10L)).thenReturn(version);
        when(mappingDao.selectList(any())).thenReturn(List.of(mapping));

        ResponseDTO<?> response = (ResponseDTO<?>) GraphDefinitionPublicationService.class
                .getMethod("getDefinitionDetail", Long.class)
                .invoke(service, 10L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).extracting(
                "graphDefinitionVersionId",
                "processKey",
                "definitionVersion",
                "compiledBpmnXml"
        ).containsExactly(10L, "expense", 2, "<definitions/>");
        assertThat(response.getData()).extracting("mappings").asInstanceOf(LIST)
                .singleElement()
                .extracting("authoredElementId", "authoredElementKind", "compiledElementId", "compiledElementType")
                .containsExactly("route_large", "EDGE", "graph_edge_route_large", "sequenceFlow");
    }

    @Test
    void latestDefinitionDetailShouldRestoreTheDraftsLatestPublishedVersion() {
        GraphDefinitionVersionEntity version = new GraphDefinitionVersionEntity();
        version.setGraphDefinitionVersionId(10L);
        version.setProcessKey("expense");
        version.setDefinitionVersion(2);
        version.setLifecycleState("ACTIVE");
        when(versionDao.selectLatestByDraftId(1L)).thenReturn(version);
        when(mappingDao.selectList(any())).thenReturn(List.of());

        ResponseDTO<?> response = service.getLatestDefinitionDetail(1L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).extracting("graphDefinitionVersionId", "definitionVersion")
                .containsExactly(10L, 2);
    }

    @Test
    void latestDefinitionDetailShouldKeepUnpublishedDraftsEmpty() {
        when(versionDao.selectLatestByDraftId(1L)).thenReturn(null);

        ResponseDTO<?> response = service.getLatestDefinitionDetail(1L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isNull();
    }

    private BpmProcessDraftEntity draft() {
        HunyuanProcessDefinitionGraph graph = new HunyuanProcessDefinitionGraph(1, "scope", List.of(new GraphScope("scope", null, "主")), List.of(
                new GraphNode("start", "scope", GraphNodeType.START, "开始", Map.of(), Map.of()),
                new GraphNode("review", "scope", GraphNodeType.APPROVAL, "审批", Map.of(), Map.of()),
                new GraphNode("end", "scope", GraphNodeType.END, "结束", Map.of(), Map.of())
        ), List.of(new GraphEdge("a", "scope", "start", "review", "default", Map.of()), new GraphEdge("b", "scope", "review", "end", "default", Map.of())), Map.of());
        BpmProcessDraftEntity entity = new BpmProcessDraftEntity(); entity.setDraftId(1L); entity.setProcessKey("expense"); entity.setProcessName("报销"); entity.setCategoryId(7L);
        entity.setGraphJson(new GraphCanonicalizer().canonicalize(graph)); entity.setLayoutJson("{}"); entity.setSemanticHash(new GraphCanonicalizer().semanticHash(graph)); return entity;
    }

    private void setField(Object target, String field, Object value) { try { var f = target.getClass().getDeclaredField(field); f.setAccessible(true); f.set(target, value); } catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); } }
}
