package com.hunyuan.sa.bpm.engine.route;

/**
 * 可公开给设计器的登记表达式说明。
 */
public record BpmRouteExpressionDescriptor(
        String expressionKey,
        int version,
        String name,
        String parameterSchemaJson
) {
}
