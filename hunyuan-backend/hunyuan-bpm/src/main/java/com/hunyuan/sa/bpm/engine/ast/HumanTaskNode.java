package com.hunyuan.sa.bpm.engine.ast;

import java.util.Map;

/**
 * 审批或办理人工任务。
 */
public record HumanTaskNode(
        String nodeKey,
        String name,
        ProcessNodeType type,
        String approvalMode,
        String candidateResolverType,
        Map<String, Object> configuration
) implements ProcessNode {
    public HumanTaskNode {
        configuration = configuration == null ? Map.of() : Map.copyOf(configuration);
    }
}
