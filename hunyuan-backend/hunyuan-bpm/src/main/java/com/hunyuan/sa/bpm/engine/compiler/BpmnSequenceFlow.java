package com.hunyuan.sa.bpm.engine.compiler;

/**
 * 编译期间使用的受控 BPMN 连线。
 */
public record BpmnSequenceFlow(
        String flowId,
        String sourceRef,
        String targetRef,
        String conditionExpression
) {
}
