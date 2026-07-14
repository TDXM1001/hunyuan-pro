package com.hunyuan.sa.bpm.candidate;

import com.hunyuan.sa.bpm.module.candidate.domain.visual.BpmPolicyVisualDraft;
import com.hunyuan.sa.bpm.module.candidate.service.PolicyRiskAssessmentService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyRiskAssessmentServiceTest {

    private final PolicyRiskAssessmentService riskService = new PolicyRiskAssessmentService();

    @Test
    void clientRiskMustBeIgnoredAndAutoApproveMustCalculateHighRisk() {
        BpmPolicyVisualDraft draft = PolicyVisualFixtures.autoApproveFallbackWithClientRisk("LOW");

        assertThat(riskService.assess(draft).level()).isEqualTo("HIGH");
        assertThat(riskService.assess(draft).reasons()).contains("无人审批时自动通过");
    }

    @Test
    void blockingFallbackAndSelfApprovalShouldRemainLowRisk() {
        BpmPolicyVisualDraft draft = PolicyVisualFixtures.roleCandidate(
                "finance-reviewer", "费用审批人", 8L, "财务经理");

        assertThat(riskService.assess(draft).level()).isEqualTo("LOW");
        assertThat(riskService.assess(draft).reasons()).isEmpty();
    }
}
