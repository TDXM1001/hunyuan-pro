package com.hunyuan.sa.bpm.module.candidate.domain.visual;

public record PolicyIdentityReference(
        String kind,
        Long stableId,
        String displayName,
        String factKey
) {
}
