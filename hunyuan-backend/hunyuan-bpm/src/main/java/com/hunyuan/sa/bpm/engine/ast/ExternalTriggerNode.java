package com.hunyuan.sa.bpm.engine.ast;

import java.util.Map;

/**
 * 登记连接器触发节点，模型只保存目录键和字段映射。
 */
public record ExternalTriggerNode(
        String nodeKey,
        String name,
        String connectorKey,
        String operationKey,
        Map<String, Object> requestMapping,
        Map<String, Object> responseMapping,
        String waitMode,
        Map<String, Object> timeoutPolicy,
        Map<String, Object> configuration
) implements ProcessNode {

    public ExternalTriggerNode {
        requestMapping = requestMapping == null ? Map.of() : Map.copyOf(requestMapping);
        responseMapping = responseMapping == null ? Map.of() : Map.copyOf(responseMapping);
        timeoutPolicy = timeoutPolicy == null ? Map.of() : Map.copyOf(timeoutPolicy);
        configuration = configuration == null ? Map.of() : Map.copyOf(configuration);
    }

    @Override
    public ProcessNodeType type() {
        return ProcessNodeType.EXTERNAL_TRIGGER;
    }
}
