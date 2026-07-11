package com.hunyuan.sa.bpm.module.runtime.service;

/**
 * Flowable 路由 delegate 传入的最小受控命令。
 */
public record BpmRouteDecisionCommand(
        Long instanceId,
        String engineProcessInstanceId,
        String routeNodeKey
) {
}
