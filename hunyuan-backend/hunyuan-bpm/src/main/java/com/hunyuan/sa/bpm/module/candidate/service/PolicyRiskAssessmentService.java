package com.hunyuan.sa.bpm.module.candidate.service;

import com.hunyuan.sa.bpm.module.candidate.domain.visual.BpmPolicyVisualDraft;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.CandidatePolicyVisualDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.PolicyRiskAssessment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PolicyRiskAssessmentService {

    public PolicyRiskAssessment assess(BpmPolicyVisualDraft draft) {
        List<String> reasons = new ArrayList<>();
        if (draft == null || draft.type() == null) {
            return new PolicyRiskAssessment("HIGH", List.of("规则类型缺失"));
        }
        switch (draft.type()) {
            case CANDIDATE -> assessCandidate(draft.candidate(), reasons);
            case START_VISIBILITY -> {
                if (draft.startVisibility() != null
                        && "ALL".equals(draft.startVisibility().startScope().type())) {
                    reasons.add("允许全部员工发起");
                }
            }
            case APPROVAL -> {
                // 审批比例和动作本身不提高风险，非法组合由语义校验阻断。
            }
        }
        String level = reasons.isEmpty() ? "LOW" : reasons.stream().anyMatch(this::isHighRisk)
                ? "HIGH" : "MEDIUM";
        return new PolicyRiskAssessment(level, List.copyOf(reasons));
    }

    private void assessCandidate(CandidatePolicyVisualDocument document, List<String> reasons) {
        if (document == null) {
            reasons.add("审批人规则缺失");
            return;
        }
        if ("ALLOW".equals(document.selfApprovalPolicy())) {
            reasons.add("允许发起人自审");
        }
        if ("AUTO_APPROVE".equals(document.emptyCandidatePolicy())) {
            reasons.add("无人审批时自动通过");
        } else if ("AUTO_REJECT".equals(document.emptyCandidatePolicy())) {
            reasons.add("无人审批时自动拒绝");
        } else if (Set.of("ASSIGN_NAMED_EMPLOYEE", "ASSIGN_NAMED_ROLE")
                .contains(document.emptyCandidatePolicy())) {
            reasons.add("无人审批时转交指定对象");
        }
    }

    private boolean isHighRisk(String reason) {
        return reason.contains("自动通过") || reason.contains("自审") || reason.contains("全部员工");
    }
}
