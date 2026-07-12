package com.hunyuan.sa.bpm.engine.graph;

import java.util.Map;

/**
 * 流程图作者节点；layout 只用于显示，不参与语义版本。
 */
public record GraphNode(
        String nodeId,
        String scopeId,
        GraphNodeType type,
        String name,
        Map<String, Object> properties,
        Map<String, Object> layout
) {
    public GraphNode {
        properties = properties == null ? Map.of() : Map.copyOf(properties);
        layout = layout == null ? Map.of() : Map.copyOf(layout);
    }
}
