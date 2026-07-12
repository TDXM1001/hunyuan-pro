package com.hunyuan.sa.bpm.module.candidate.domain.model;

public record MemberAvailabilityDecision(
        String memberId,
        ApprovalMemberState memberState,
        ApprovalStageState stageState
) {
}
