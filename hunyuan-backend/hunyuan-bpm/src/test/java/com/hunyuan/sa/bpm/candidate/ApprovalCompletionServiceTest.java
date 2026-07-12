package com.hunyuan.sa.bpm.candidate;

import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalCompletionDecision;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalCompletionMode;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalMemberAction;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalMemberFact;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalMemberState;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalPolicyDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalStageFact;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ApprovalStageState;
import com.hunyuan.sa.bpm.module.candidate.domain.model.EngineEffect;
import com.hunyuan.sa.bpm.module.candidate.domain.model.MemberUpdate;
import com.hunyuan.sa.bpm.module.candidate.service.ApprovalCompletionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApprovalCompletionServiceTest {

    private final ApprovalCompletionService completion = new ApprovalCompletionService();

    @Test
    void sequentialApprovalShouldActivateOnlyNextMemberAfterPreviousApproval() {
        ApprovalCompletionDecision decision = completion.decide(
                policy(ApprovalCompletionMode.SEQUENTIAL),
                stage(),
                List.of(member("first", 1, ApprovalMemberState.ACTIVE), member("second", 2, ApprovalMemberState.PLANNED)),
                new ApprovalMemberAction("first", "APPROVE")
        );

        assertThat(decision.memberUpdates()).extracting(MemberUpdate::state)
                .containsExactly(ApprovalMemberState.APPROVED, ApprovalMemberState.ACTIVE);
        assertThat(decision.stageState()).isEqualTo(ApprovalStageState.ACTIVE);
        assertThat(decision.engineEffect()).isEqualTo(EngineEffect.NONE);
    }

    @Test
    void singleApprovalShouldCompleteStageAfterOnlyMemberApproves() {
        ApprovalCompletionDecision decision = completion.decide(
                policy(ApprovalCompletionMode.SINGLE),
                stage(),
                List.of(member("only", 1, ApprovalMemberState.ACTIVE)),
                new ApprovalMemberAction("only", "APPROVE")
        );

        assertThat(decision.stageState()).isEqualTo(ApprovalStageState.APPROVED);
        assertThat(decision.engineEffect()).isEqualTo(EngineEffect.COMPLETE_ONCE);
    }

    @Test
    void allApprovalShouldWaitForRemainingActiveMember() {
        ApprovalCompletionDecision decision = completion.decide(
                policy(ApprovalCompletionMode.ALL),
                stage(),
                List.of(member("first", 1, ApprovalMemberState.ACTIVE), member("second", 2, ApprovalMemberState.ACTIVE)),
                new ApprovalMemberAction("first", "APPROVE")
        );

        assertThat(decision.stageState()).isEqualTo(ApprovalStageState.ACTIVE);
        assertThat(decision.engineEffect()).isEqualTo(EngineEffect.NONE);
    }

    @Test
    void anyApprovalShouldTerminateRemainingMembersAfterFirstApproval() {
        ApprovalCompletionDecision decision = completion.decide(
                policy(ApprovalCompletionMode.ANY),
                stage(),
                List.of(member("first", 1, ApprovalMemberState.ACTIVE), member("second", 2, ApprovalMemberState.ACTIVE)),
                new ApprovalMemberAction("first", "APPROVE")
        );

        assertThat(decision.stageState()).isEqualTo(ApprovalStageState.APPROVED);
        assertThat(decision.memberUpdates()).contains(
                new MemberUpdate("first", ApprovalMemberState.APPROVED),
                new MemberUpdate("second", ApprovalMemberState.TERMINATED)
        );
        assertThat(decision.engineEffect()).isEqualTo(EngineEffect.COMPLETE_ONCE);
    }

    @Test
    void ratioApprovalShouldUseFrozenMemberCountWhenThresholdReached() {
        ApprovalCompletionDecision decision = completion.decide(
                new ApprovalPolicyDocument(ApprovalCompletionMode.RATIO, 50, "IMMEDIATE"),
                stage(),
                List.of(
                        member("first", 1, ApprovalMemberState.APPROVED),
                        member("second", 2, ApprovalMemberState.ACTIVE),
                        member("third", 3, ApprovalMemberState.ACTIVE)
                ),
                new ApprovalMemberAction("second", "APPROVE")
        );

        assertThat(decision.stageState()).isEqualTo(ApprovalStageState.APPROVED);
        assertThat(decision.memberUpdates()).contains(
                new MemberUpdate("second", ApprovalMemberState.APPROVED),
                new MemberUpdate("third", ApprovalMemberState.TERMINATED)
        );
        assertThat(decision.engineEffect()).isEqualTo(EngineEffect.COMPLETE_ONCE);
    }

    @Test
    void singleApprovalShouldEnterExceptionPendingWhenOnlyMemberBecomesIneligible() {
        ApprovalCompletionDecision decision = completion.decide(
                policy(ApprovalCompletionMode.SINGLE),
                stage(),
                List.of(member("only", 1, ApprovalMemberState.ACTIVE)),
                new ApprovalMemberAction("only", "MARK_INELIGIBLE")
        );

        assertThat(decision.stageState()).isEqualTo(ApprovalStageState.EXCEPTION_PENDING);
        assertThat(decision.memberUpdates()).containsExactly(new MemberUpdate("only", ApprovalMemberState.INELIGIBLE));
        assertThat(decision.engineEffect()).isEqualTo(EngineEffect.NONE);
    }

    @Test
    void ratioShouldRejectWhenFrozenThresholdBecomesUnreachable() {
        ApprovalCompletionDecision decision = completion.decide(
                new ApprovalPolicyDocument(ApprovalCompletionMode.RATIO, 67, "WHEN_APPROVAL_UNREACHABLE"),
                stage(),
                List.of(
                        member("approved", 1, ApprovalMemberState.APPROVED),
                        member("rejected", 2, ApprovalMemberState.REJECTED),
                        member("unavailable", 3, ApprovalMemberState.ACTIVE)
                ),
                new ApprovalMemberAction("unavailable", "MARK_INELIGIBLE")
        );

        assertThat(decision.stageState()).isEqualTo(ApprovalStageState.REJECTED);
        assertThat(decision.engineEffect()).isEqualTo(EngineEffect.CLOSE_ONCE);
    }

    @Test
    void ratioRejectionShouldWaitUntilFrozenThresholdActuallyBecomesUnreachable() {
        ApprovalCompletionDecision decision = completion.decide(
                new ApprovalPolicyDocument(ApprovalCompletionMode.RATIO, 50, "WHEN_APPROVAL_UNREACHABLE"),
                stage(),
                List.of(
                        member("first", 1, ApprovalMemberState.ACTIVE),
                        member("second", 2, ApprovalMemberState.ACTIVE),
                        member("third", 3, ApprovalMemberState.ACTIVE)
                ),
                new ApprovalMemberAction("first", "REJECT")
        );

        assertThat(decision.stageState()).isEqualTo(ApprovalStageState.ACTIVE);
        assertThat(decision.memberUpdates()).containsExactly(new MemberUpdate("first", ApprovalMemberState.REJECTED));
        assertThat(decision.engineEffect()).isEqualTo(EngineEffect.NONE);
    }

    @Test
    void returnShouldCloseStageAndTerminateOtherPendingMembers() {
        ApprovalCompletionDecision decision = completion.decide(
                policy(ApprovalCompletionMode.ALL),
                stage(),
                List.of(member("first", 1, ApprovalMemberState.ACTIVE), member("second", 2, ApprovalMemberState.ACTIVE)),
                new ApprovalMemberAction("first", "RETURN")
        );

        assertThat(decision.stageState()).isEqualTo(ApprovalStageState.RETURNED);
        assertThat(decision.memberUpdates()).containsExactly(
                new MemberUpdate("first", ApprovalMemberState.RETURNED),
                new MemberUpdate("second", ApprovalMemberState.TERMINATED)
        );
        assertThat(decision.engineEffect()).isEqualTo(EngineEffect.CLOSE_ONCE);
    }

    private ApprovalPolicyDocument policy(ApprovalCompletionMode completionMode) {
        return new ApprovalPolicyDocument(completionMode, 100, "IMMEDIATE");
    }

    private ApprovalStageFact stage() {
        return new ApprovalStageFact("stage-1", 1L, ApprovalStageState.ACTIVE);
    }

    private ApprovalMemberFact member(String memberId, int order, ApprovalMemberState state) {
        return new ApprovalMemberFact(memberId, order, 1L, 100L + order, state);
    }
}
