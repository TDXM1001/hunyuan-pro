package com.hunyuan.sa.bpm.candidate;

import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.ApprovalPolicyVisualDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.BpmPolicyVisualDraft;
import com.hunyuan.sa.bpm.module.candidate.service.PolicyBusinessSummaryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyBusinessSummaryServiceTest {

    private final PolicyBusinessSummaryService service = new PolicyBusinessSummaryService();

    @Test
    void ratioApprovalShouldExplainThresholdRejectionAndActions() {
        BpmPolicyVisualDraft draft = new BpmPolicyVisualDraft(
                PolicyType.APPROVAL,
                "two-thirds",
                "三分之二通过",
                null,
                2,
                0L,
                null,
                new ApprovalPolicyVisualDocument(
                        "RATIO",
                        67,
                        "WHEN_APPROVAL_UNREACHABLE",
                        "RETURN_INITIATOR",
                        List.of("APPROVE", "REJECT", "RETURN")
                ),
                null
        );

        assertThat(service.summarize(draft)).isEqualTo(
                "至少 67% 的审批人通过后完成；无法达到通过比例时拒绝；允许通过、拒绝和退回发起人。");
    }
}
