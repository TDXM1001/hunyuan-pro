package com.hunyuan.sa.bpm.engine.ast;

import java.util.Map;

/**
 * 受控延迟节点，时间值必须在发布校验阶段完成类型检查。
 */
public record DelayNode(
        String nodeKey,
        String name,
        String mode,
        String value,
        String timezone,
        String overduePolicy,
        Map<String, Object> configuration
) implements ProcessNode {

    public DelayNode {
        configuration = configuration == null ? Map.of() : Map.copyOf(configuration);
    }

    @Override
    public ProcessNodeType type() {
        return ProcessNodeType.DELAY;
    }
}
