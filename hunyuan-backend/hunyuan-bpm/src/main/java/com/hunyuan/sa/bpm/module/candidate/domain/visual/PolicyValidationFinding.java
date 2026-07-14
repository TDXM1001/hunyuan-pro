package com.hunyuan.sa.bpm.module.candidate.domain.visual;

public record PolicyValidationFinding(
        String code,
        String severity,
        String fieldPath,
        String message,
        String suggestion
) {
}
