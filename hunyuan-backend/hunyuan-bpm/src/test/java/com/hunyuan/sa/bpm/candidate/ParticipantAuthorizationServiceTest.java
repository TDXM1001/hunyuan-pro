package com.hunyuan.sa.bpm.candidate;

import com.hunyuan.sa.bpm.module.candidate.domain.model.ActorSnapshot;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalMemberFact;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalMemberState;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalStageFact;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalStageState;
import com.hunyuan.sa.bpm.module.candidate.service.ParticipantAuthorizationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParticipantAuthorizationServiceTest {

    private final ParticipantAuthorizationService authorization = new ParticipantAuthorizationService();

    @Test
    void authorizeShouldRejectActorWhoIsNotFrozenCurrentHandler() {
        boolean authorized = authorization.authorize(
                new ActorSnapshot(1L, 201L, true),
                new ApprovalStageFact("stage-1", 1L, ApprovalStageState.ACTIVE),
                new ApprovalMemberFact("member-1", 1, 1L, 200L, ApprovalMemberState.ACTIVE),
                "APPROVE"
        );

        assertThat(authorized).isFalse();
    }
}
