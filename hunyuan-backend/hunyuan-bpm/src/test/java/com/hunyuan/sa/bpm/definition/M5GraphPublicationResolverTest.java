package com.hunyuan.sa.bpm.definition;

import com.hunyuan.sa.bpm.engine.graph.*;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.definition.service.M5GraphPublicationResolver;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmConnectorDefinitionEntity;
import com.hunyuan.sa.bpm.module.integration.service.BpmConnectorRegistryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class M5GraphPublicationResolverTest {
    @Test
    void resolveShouldFreezeConnectorAndChildEngineDefinition() {
        GraphDefinitionVersionDao versionDao = Mockito.mock(GraphDefinitionVersionDao.class);
        BpmConnectorRegistryService registry = Mockito.mock(BpmConnectorRegistryService.class);
        GraphDefinitionVersionEntity child = new GraphDefinitionVersionEntity();
        child.setGraphDefinitionVersionId(42L);
        child.setProcessKey("expense_archive");
        child.setLifecycleState("ACTIVE");
        child.setEngineProcessDefinitionId("expense_archive:7:dep-42");
        child.setSemanticHash("child-hash");
        when(versionDao.selectById(42L)).thenReturn(child);
        BpmConnectorDefinitionEntity connector = new BpmConnectorDefinitionEntity();
        connector.setConnectorDefinitionId(9L);
        when(registry.requireOperation("finance", 3, "createExpense")).thenReturn(
                new BpmConnectorRegistryService.RegisteredConnector(connector,
                        new BpmConnectorRegistryService.RegisteredOperation("createExpense", "/expense", "POST", true)));
        M5GraphPublicationResolver resolver = new M5GraphPublicationResolver(versionDao, registry);
        HunyuanProcessDefinitionGraph graph = new HunyuanProcessDefinitionGraph(
                1, "scope", List.of(new GraphScope("scope", null, "主")),
                List.of(
                        new GraphNode("external", "scope", GraphNodeType.EXTERNAL_TRIGGER, "调用",
                                Map.of("connectorKey", "finance", "connectorVersion", 3, "operationKey", "createExpense"), Map.of()),
                        new GraphNode("child", "scope", GraphNodeType.SUB_PROCESS, "子流程",
                                Map.of("calledProcessKey", "expense_archive", "calledDefinitionVersionId", 42), Map.of())
                ), List.of(), Map.of());

        var resolved = resolver.resolve(graph);

        assertThat(resolved.graph().nodes().get(1).properties().get("calledEngineProcessDefinitionId"))
                .isEqualTo("expense_archive:7:dep-42");
        assertThat(resolved.dependencies()).containsKeys("external", "child");
    }
}
