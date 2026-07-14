package com.hunyuan.sa.bpm.module.candidate.domain.visual;

import java.util.List;

public record PolicyBusinessValidationResult(
        boolean valid,
        String calculatedRiskLevel,
        String businessSummary,
        List<PolicyValidationFinding> findings,
        String canonicalPayload,
        String digest
) {
}
