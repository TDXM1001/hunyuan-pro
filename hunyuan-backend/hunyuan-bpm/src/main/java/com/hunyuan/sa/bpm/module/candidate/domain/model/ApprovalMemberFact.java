package com.hunyuan.sa.bpm.module.candidate.domain.model;

public record ApprovalMemberFact(
        String memberId,
        int memberOrder,
        Long sourceEmployeeId,
        Long currentEmployeeId,
        ApprovalMemberState state
) {
}
