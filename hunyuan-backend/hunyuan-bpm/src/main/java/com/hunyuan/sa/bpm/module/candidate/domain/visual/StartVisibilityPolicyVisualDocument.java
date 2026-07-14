package com.hunyuan.sa.bpm.module.candidate.domain.visual;

public record StartVisibilityPolicyVisualDocument(
        PolicyScopeVisualDocument startScope,
        PolicyScopeVisualDocument visibilityScope
) {
}
