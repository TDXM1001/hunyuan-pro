package com.hunyuan.sa.bpm.module.candidate.domain.vo;

import com.hunyuan.sa.bpm.module.candidate.domain.visual.PolicyValidationFinding;

import java.util.List;

public record BpmPolicySimulationVO(
        List<BpmPolicySimulationMemberVO> resolvedMembers,
        List<String> diagnostics,
        String automaticOutcome,
        String businessSummary,
        List<PolicyValidationFinding> findings
) {
}
