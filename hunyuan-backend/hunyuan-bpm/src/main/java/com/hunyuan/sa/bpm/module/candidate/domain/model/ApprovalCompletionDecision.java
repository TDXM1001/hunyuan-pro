package com.hunyuan.sa.bpm.module.candidate.domain.model;

import java.util.List;

public record ApprovalCompletionDecision(
        ApprovalStageState stageState,
        List<MemberUpdate> memberUpdates,
        EngineEffect engineEffect
) {

    public ApprovalCompletionDecision {
        memberUpdates = List.copyOf(memberUpdates);
    }
}
