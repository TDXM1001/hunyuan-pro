package com.hunyuan.sa.bpm.candidate;

import com.hunyuan.sa.bpm.module.candidate.domain.visual.BpmPolicyVisualDraft;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.PolicyVisualCompilation;
import com.hunyuan.sa.bpm.module.candidate.service.PolicyBusinessSummaryService;
import com.hunyuan.sa.bpm.module.candidate.service.PolicyCanonicalizer;
import com.hunyuan.sa.bpm.module.candidate.service.PolicyDocumentValidator;
import com.hunyuan.sa.bpm.module.candidate.service.PolicyRiskAssessmentService;
import com.hunyuan.sa.bpm.module.candidate.service.PolicyVisualDocumentMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyVisualDocumentMapperTest {

    private final PolicyBusinessSummaryService summaryService = new PolicyBusinessSummaryService();
    private final PolicyRiskAssessmentService riskService = new PolicyRiskAssessmentService();
    private final PolicyVisualDocumentMapper mapper = new PolicyVisualDocumentMapper(
            new PolicyCanonicalizer(),
            new PolicyDocumentValidator(),
            summaryService,
            riskService
    );

    @Test
    void candidateRoleRuleShouldCompileToCanonicalV2AndReadableSummary() {
        BpmPolicyVisualDraft draft = PolicyVisualFixtures.roleCandidate(
                "finance-reviewer", "费用审批人", 8L, "财务经理");

        PolicyVisualCompilation result = mapper.compile(draft);

        assertThat(result.canonicalPayload()).contains(
                "\"schemaVersion\":2", "\"resolverType\":\"ROLE\"", "\"roleId\":8");
        assertThat(result.businessSummary()).isEqualTo(
                "任务到达时，由“财务经理”角色成员审批；发起人不能自审；无人可处理时阻断流程。");
        assertThat(result.calculatedRiskLevel()).isEqualTo("LOW");
        assertThat(result.digest()).hasSize(64);
    }

    @Test
    void invalidNamedFallbackShouldReturnStableFieldPath() {
        BpmPolicyVisualDraft invalid = PolicyVisualFixtures.roleCandidate(
                "finance-reviewer", "费用审批人", 8L, "财务经理");
        invalid = invalid.withCandidate(invalid.candidate().withEmptyCandidatePolicy("ASSIGN_NAMED_EMPLOYEE"));

        PolicyVisualCompilation result = mapper.compile(invalid);

        assertThat(result.findings())
                .anySatisfy(finding -> {
                    assertThat(finding.severity()).isEqualTo("BLOCKING");
                    assertThat(finding.fieldPath()).isEqualTo("candidate.emptyCandidatePolicy");
                });
    }
}
