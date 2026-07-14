package com.hunyuan.sa.bpm.module.candidate.domain.visual;

import java.util.List;

public record PolicyVisualCompilation(
        String canonicalPayload,
        String digest,
        String businessSummary,
        String calculatedRiskLevel,
        List<PolicyValidationFinding> findings
) {

    public boolean valid() {
        return findings.stream().noneMatch(finding -> "BLOCKING".equals(finding.severity()));
    }
}
