package com.hunyuan.sa.bpm.engine.graph;

import java.util.List;
import java.util.Map;

/**
 * M1 唯一的正式流程定义作者模型。
 */
public record HunyuanProcessDefinitionGraph(
        int schemaVersion,
        String rootScopeId,
        List<GraphScope> scopes,
        List<GraphNode> nodes,
        List<GraphEdge> edges,
        Map<String, Object> policies
) {
    public HunyuanProcessDefinitionGraph {
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
        policies = policies == null ? Map.of() : Map.copyOf(policies);
    }
}
