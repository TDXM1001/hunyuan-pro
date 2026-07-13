package com.hunyuan.sa.bpm.module.definition.service;

import com.hunyuan.sa.bpm.engine.graph.GraphNode;
import com.hunyuan.sa.bpm.engine.graph.GraphNodeType;
import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.integration.service.BpmConnectorRegistryService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class M5GraphPublicationResolver {
    private final GraphDefinitionVersionDao graphDefinitionVersionDao;
    private final BpmConnectorRegistryService bpmConnectorRegistryService;

    public M5GraphPublicationResolver(
            GraphDefinitionVersionDao graphDefinitionVersionDao,
            BpmConnectorRegistryService bpmConnectorRegistryService
    ) {
        this.graphDefinitionVersionDao = graphDefinitionVersionDao;
        this.bpmConnectorRegistryService = bpmConnectorRegistryService;
    }

    public ResolvedGraph resolve(HunyuanProcessDefinitionGraph graph) {
        List<GraphNode> nodes = new ArrayList<>();
        Map<String, Object> advancedDependencies = new LinkedHashMap<>();
        for (GraphNode node : graph.nodes()) {
            Map<String, Object> properties = new LinkedHashMap<>(node.properties());
            if (node.type() == GraphNodeType.EXTERNAL_TRIGGER) {
                String connectorKey = String.valueOf(properties.get("connectorKey"));
                Integer connectorVersion = integer(properties.get("connectorVersion"));
                String operationKey = String.valueOf(properties.get("operationKey"));
                if (connectorVersion == null || connectorVersion <= 0) {
                    throw new GraphPublicationDependencyException("连接器版本必须冻结为正整数：" + node.nodeId());
                }
                var connector = bpmConnectorRegistryService.requireOperation(connectorKey, connectorVersion, operationKey);
                advancedDependencies.put(node.nodeId(), Map.of(
                        "connectorKey", connectorKey,
                        "connectorVersion", connectorVersion,
                        "connectorDefinitionId", connector.definition().getConnectorDefinitionId(),
                        "operationKey", operationKey
                ));
            } else if (node.type() == GraphNodeType.SUB_PROCESS) {
                Long versionId = longValue(properties.get("calledDefinitionVersionId"));
                GraphDefinitionVersionEntity child = graphDefinitionVersionDao.selectById(versionId);
                if (child == null || !"ACTIVE".equals(child.getLifecycleState())
                        || child.getEngineProcessDefinitionId() == null) {
                    throw new GraphPublicationDependencyException("子流程定义版本不存在、未启用或未部署：" + versionId);
                }
                String calledProcessKey = String.valueOf(properties.get("calledProcessKey"));
                if (!calledProcessKey.equals(child.getProcessKey())) {
                    throw new GraphPublicationDependencyException("子流程 key 与冻结版本不一致：" + node.nodeId());
                }
                properties.put("calledEngineProcessDefinitionId", child.getEngineProcessDefinitionId());
                advancedDependencies.put(node.nodeId(), Map.of(
                        "calledDefinitionVersionId", versionId,
                        "calledProcessKey", child.getProcessKey(),
                        "engineProcessDefinitionId", child.getEngineProcessDefinitionId(),
                        "semanticHash", child.getSemanticHash()
                ));
            }
            nodes.add(new GraphNode(node.nodeId(), node.scopeId(), node.type(), node.name(), properties, node.layout()));
        }
        return new ResolvedGraph(
                new HunyuanProcessDefinitionGraph(graph.schemaVersion(), graph.rootScopeId(), graph.scopes(), nodes, graph.edges(), graph.policies()),
                Map.copyOf(advancedDependencies)
        );
    }

    private Integer integer(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    public record ResolvedGraph(HunyuanProcessDefinitionGraph graph, Map<String, Object> dependencies) {
    }
}
