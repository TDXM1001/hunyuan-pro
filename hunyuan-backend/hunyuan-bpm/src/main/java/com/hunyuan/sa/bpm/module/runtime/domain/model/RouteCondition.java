package com.hunyuan.sa.bpm.module.runtime.domain.model;

import java.util.Map;

/**
 * 类型化路由条件或后端登记表达式引用。
 */
public record RouteCondition(
        String sourceType,
        String fieldKey,
        String valueType,
        String operator,
        Object compareValue,
        String expressionKey,
        Integer expressionVersion,
        Map<String, Object> parameters
) {
    public RouteCondition {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
