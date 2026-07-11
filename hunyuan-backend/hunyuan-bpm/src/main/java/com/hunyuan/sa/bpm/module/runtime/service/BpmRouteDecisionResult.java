package com.hunyuan.sa.bpm.module.runtime.service;

import java.util.List;

/**
 * 可直接转换为受控 Flowable 分支变量的路由结果。
 */
public record BpmRouteDecisionResult(
        Long routeDecisionId,
        List<String> matchedBranchKeys,
        boolean defaultBranchUsed,
        long inputFormDataVersion
) {
    public BpmRouteDecisionResult {
        matchedBranchKeys = List.copyOf(matchedBranchKeys);
    }
}
