package com.hunyuan.sa.bpm.engine.ast;

import java.util.Map;

/**
 * 非阻塞设计时抄送节点。
 */
public record CopyTaskNode(
        String nodeKey,
        String name,
        String candidateResolverType,
        Map<String, Object> configuration
) implements ProcessNode {
    public CopyTaskNode {
        configuration = configuration == null ? Map.of() : Map.copyOf(configuration);
    }

    @Override
    public ProcessNodeType type() {
        return ProcessNodeType.COPY_TASK;
    }
}
