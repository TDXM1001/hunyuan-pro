package com.hunyuan.sa.bpm.module.candidate.domain.vo;

import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyReference;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.PolicyValidationFinding;

import java.util.List;

public record BpmPolicyTechnicalDetailVO(
        PolicyReference reference, Integer schemaVersion, String canonicalPayload,
        String digest, List<PolicyValidationFinding> diagnostics
) {
}
