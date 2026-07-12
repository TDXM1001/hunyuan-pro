package com.hunyuan.sa.bpm.module.candidate.domain.model;

import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;

import java.time.LocalDateTime;

/**
 * 解析候选成员时允许使用的已冻结事实。
 */
public record CandidateResolutionContext(
        Long tenantId,
        Long definitionVersionId,
        String authoredNodeId,
        String stageInvocationId,
        BpmEmployeeSnapshot startEmployee,
        RoutingFactView routingFactView,
        LocalDateTime resolvedAt
) {

    public CandidateResolutionContext(
            Long tenantId,
            Long definitionVersionId,
            String authoredNodeId,
            String stageInvocationId,
            BpmEmployeeSnapshot startEmployee,
            LocalDateTime resolvedAt
    ) {
        this(tenantId, definitionVersionId, authoredNodeId, stageInvocationId, startEmployee, null, resolvedAt);
    }
}
