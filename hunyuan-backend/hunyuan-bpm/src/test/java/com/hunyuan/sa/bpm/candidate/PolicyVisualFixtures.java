package com.hunyuan.sa.bpm.candidate;

import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.BpmPolicyVisualDraft;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.CandidatePolicyVisualDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.PolicyIdentityReference;

final class PolicyVisualFixtures {

    private PolicyVisualFixtures() {
    }

    static BpmPolicyVisualDraft roleCandidate(
            String policyKey,
            String policyName,
            long roleId,
            String roleName
    ) {
        return new BpmPolicyVisualDraft(
                PolicyType.CANDIDATE,
                policyKey,
                policyName,
                null,
                2,
                0L,
                new CandidatePolicyVisualDocument(
                        "ROLE",
                        new PolicyIdentityReference("ROLE", roleId, roleName, null),
                        "ACTIVATE",
                        "SELECTION_ORDER",
                        "BLOCK",
                        "BLOCK",
                        null,
                        "LOW"
                ),
                null,
                null
        );
    }

    static BpmPolicyVisualDraft autoApproveFallbackWithClientRisk(String clientRisk) {
        return new BpmPolicyVisualDraft(
                PolicyType.CANDIDATE,
                "auto-approve-fallback",
                "无人时自动通过",
                null,
                2,
                0L,
                new CandidatePolicyVisualDocument(
                        "START_EMPLOYEE",
                        new PolicyIdentityReference("START_EMPLOYEE", null, "发起人", null),
                        "ACTIVATE",
                        "SELECTION_ORDER",
                        "AUTO_APPROVE",
                        "BLOCK",
                        null,
                        clientRisk
                ),
                null,
                null
        );
    }
}
