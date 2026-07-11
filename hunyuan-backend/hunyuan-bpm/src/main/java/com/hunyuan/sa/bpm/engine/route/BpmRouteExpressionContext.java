package com.hunyuan.sa.bpm.engine.route;

import java.util.Map;

/**
 * 登记表达式只能读取 Hunyuan 提供的受控上下文。
 */
public record BpmRouteExpressionContext(
        Long instanceId,
        Long formDataVersion,
        Map<String, Object> formData,
        Map<String, Object> instanceContext
) {
}
