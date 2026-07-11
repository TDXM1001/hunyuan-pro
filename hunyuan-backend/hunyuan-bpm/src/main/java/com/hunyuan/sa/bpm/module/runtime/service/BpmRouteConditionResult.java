package com.hunyuan.sa.bpm.module.runtime.service;

/**
 * 单条路由条件的计算结果。
 */
public record BpmRouteConditionResult(
        boolean matched,
        String reasonCode,
        String reasonText
) {
}
