package com.hunyuan.sa.bpm.module.candidate.domain.model;

import java.util.Set;

public record ApprovalPolicyDocument(
        ApprovalCompletionMode completionMode,
        int ratioPercent,
        String rejectionRule,
        Set<String> allowedActions
) {

    private static final Set<String> SUPPORTED_ACTIONS = Set.of("APPROVE", "REJECT", "RETURN");

    public ApprovalPolicyDocument {
        if (completionMode == null || ratioPercent < 1 || ratioPercent > 100
                || allowedActions == null || allowedActions.isEmpty()
                || !SUPPORTED_ACTIONS.containsAll(allowedActions)) {
            throw new IllegalArgumentException("审批完成策略不合法");
        }
        allowedActions = Set.copyOf(allowedActions);
    }

    public ApprovalPolicyDocument(
            ApprovalCompletionMode completionMode,
            int ratioPercent,
            String rejectionRule
    ) {
        this(completionMode, ratioPercent, rejectionRule, SUPPORTED_ACTIONS);
    }

    public boolean permitsAction(String action) {
        return allowedActions.contains(action);
    }
}
