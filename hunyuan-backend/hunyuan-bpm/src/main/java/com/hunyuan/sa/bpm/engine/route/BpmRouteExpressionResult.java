package com.hunyuan.sa.bpm.engine.route;

/**
 * 登记表达式的布尔结果与可审计原因。
 */
public record BpmRouteExpressionResult(
        boolean matched,
        String reasonCode,
        String reasonText
) {
}
