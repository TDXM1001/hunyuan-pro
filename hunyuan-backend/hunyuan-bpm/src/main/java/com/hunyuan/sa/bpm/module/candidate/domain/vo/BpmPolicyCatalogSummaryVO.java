package com.hunyuan.sa.bpm.module.candidate.domain.vo;

import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyReference;

public record BpmPolicyCatalogSummaryVO(
        PolicyReference reference, String policyName, String description, String lifecycleState,
        Integer schemaVersion, Long catalogRevision, String businessSummary,
        String calculatedRiskLevel, long referenceCount
) {
}
