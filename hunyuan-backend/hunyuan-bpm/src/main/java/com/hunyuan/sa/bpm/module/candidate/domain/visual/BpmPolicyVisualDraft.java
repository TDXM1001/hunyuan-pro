package com.hunyuan.sa.bpm.module.candidate.domain.visual;

import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;

import java.util.Objects;

public record BpmPolicyVisualDraft(
        PolicyType type,
        String policyKey,
        String policyName,
        String description,
        Integer schemaVersion,
        Long catalogRevision,
        CandidatePolicyVisualDocument candidate,
        ApprovalPolicyVisualDocument approval,
        StartVisibilityPolicyVisualDocument startVisibility
) {

    public Object requireMatchingDocument() {
        return switch (Objects.requireNonNull(type, "规则类型不能为空")) {
            case CANDIDATE -> Objects.requireNonNull(candidate, "审批人规则不能为空");
            case APPROVAL -> Objects.requireNonNull(approval, "审批方式规则不能为空");
            case START_VISIBILITY -> Objects.requireNonNull(startVisibility, "发起范围规则不能为空");
        };
    }

    public BpmPolicyVisualDraft withCandidate(CandidatePolicyVisualDocument document) {
        return new BpmPolicyVisualDraft(
                type, policyKey, policyName, description, schemaVersion, catalogRevision,
                document, approval, startVisibility
        );
    }
}
