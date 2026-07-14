package com.hunyuan.sa.bpm.module.candidate.domain.vo;

import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyReference;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.BpmPolicyVisualDraft;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.PolicyValidationFinding;

import java.util.List;

public record BpmPolicyBusinessDetailVO(
        PolicyReference reference, String lifecycleState, Integer schemaVersion, Long catalogRevision,
        String policyName, String description, String businessSummary, String calculatedRiskLevel,
        BpmPolicyVisualDraft configuration, List<PolicyValidationFinding> findings
) {
}
