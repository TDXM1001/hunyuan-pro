package com.hunyuan.sa.bpm.module.candidate.domain.visual;

import java.util.List;

public record PolicyScopeVisualDocument(
        String type,
        List<PolicyIdentityReference> identities,
        List<PolicyScopeVisualDocument> scopes
) {
}
