package com.hunyuan.sa.bpm.candidate;

import com.hunyuan.sa.bpm.module.candidate.domain.form.BpmPolicySimulationForm;

final class PolicySimulationFixtures {
    private PolicySimulationFixtures() {
    }

    static BpmPolicySimulationForm roleCandidateWithStarter(long roleId, long starterId) {
        BpmPolicySimulationForm form = new BpmPolicySimulationForm();
        form.setDraft(PolicyVisualFixtures.roleCandidate(
                "finance-reviewer", "费用审批人", roleId, "财务经理"));
        form.setStarterEmployeeId(starterId);
        return form;
    }
}
