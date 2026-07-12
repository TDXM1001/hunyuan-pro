package com.hunyuan.sa.bpm.engine.graph;

import java.util.Map;

/**
 * 作用域内的有向作者连线。
 */
public record GraphEdge(
        String edgeId,
        String scopeId,
        String sourceNodeId,
        String targetNodeId,
        String sourcePort,
        Map<String, Object> properties
) {
    public GraphEdge {
        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }
}
