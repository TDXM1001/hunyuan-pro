package com.hunyuan.sa.bpm.module.candidate.domain.visual;

import java.util.List;

public record ApprovalPolicyVisualDocument(
        String completionMode,
        Integer ratioPercent,
        String rejectionRule,
        String returnRule,
        List<String> allowedActions
) {
}
