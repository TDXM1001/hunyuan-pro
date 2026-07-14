package com.hunyuan.sa.bpm.module.candidate.service;

import com.hunyuan.sa.bpm.module.candidate.domain.visual.ApprovalPolicyVisualDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.BpmPolicyVisualDraft;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.CandidatePolicyVisualDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.PolicyIdentityReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PolicyBusinessSummaryService {

    private static final Map<String, String> ACTION_LABELS = Map.of(
            "APPROVE", "通过", "REJECT", "拒绝", "RETURN", "退回发起人"
    );

    public String summarize(BpmPolicyVisualDraft draft) {
        draft.requireMatchingDocument();
        return switch (draft.type()) {
            case CANDIDATE -> summarizeCandidate(draft.candidate());
            case APPROVAL -> summarizeApproval(draft.approval());
            case START_VISIBILITY -> summarizeScope(draft);
        };
    }

    private String summarizeCandidate(CandidatePolicyVisualDocument document) {
        String source = identityLabel(document.identityReference(), document.resolverType());
        String selfApproval = switch (text(document.selfApprovalPolicy(), "BLOCK")) {
            case "ALLOW" -> "允许发起人自审";
            case "SKIP_SELF" -> "发起人匹配时跳过本人";
            case "ASSIGN_DEPARTMENT_MANAGER" -> "发起人匹配时转其部门负责人";
            default -> "发起人不能自审";
        };
        String empty = switch (text(document.emptyCandidatePolicy(), "BLOCK")) {
            case "AUTO_APPROVE" -> "无人可处理时自动通过";
            case "AUTO_REJECT" -> "无人可处理时自动拒绝";
            case "ASSIGN_NAMED_EMPLOYEE", "ASSIGN_NAMED_ROLE" ->
                    "无人可处理时转交“" + identityLabel(document.fallbackIdentityReference(), "") + "”";
            default -> "无人可处理时阻断流程";
        };
        return "任务到达时，由" + source + "审批；" + selfApproval + "；" + empty + "。";
    }

    private String summarizeApproval(ApprovalPolicyVisualDocument document) {
        String completion = switch (text(document.completionMode(), "SINGLE")) {
            case "SEQUENTIAL" -> "审批人依次通过后完成";
            case "ALL" -> "全部审批人通过后完成";
            case "ANY" -> "任意一名审批人通过后完成";
            case "RATIO" -> "至少 " + document.ratioPercent() + "% 的审批人通过后完成";
            default -> "一名审批人处理后完成";
        };
        String rejection = "WHEN_APPROVAL_UNREACHABLE".equals(document.rejectionRule())
                ? "无法达到通过比例时拒绝" : "任一审批人拒绝时立即结束";
        List<String> actions = new ArrayList<>();
        if (document.allowedActions() != null) {
            document.allowedActions().stream().map(ACTION_LABELS::get).filter(label -> label != null)
                    .forEach(actions::add);
        }
        return completion + "；" + rejection + "；允许" + joinActions(actions) + "。";
    }

    private String summarizeScope(BpmPolicyVisualDraft draft) {
        return "按已配置的人员范围控制发起和查看权限。";
    }

    private String identityLabel(PolicyIdentityReference reference, String resolverType) {
        if (reference != null && reference.displayName() != null && !reference.displayName().isBlank()) {
            return switch (text(reference.kind(), resolverType)) {
                case "ROLE" -> "“" + reference.displayName() + "”角色成员";
                case "EMPLOYEE" -> "员工“" + reference.displayName() + "”";
                default -> "“" + reference.displayName() + "”";
            };
        }
        return switch (text(resolverType, "")) {
            case "START_EMPLOYEE" -> "发起人本人";
            case "START_DEPARTMENT_MANAGER" -> "发起人的部门负责人";
            default -> "已配置人员";
        };
    }

    private String text(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String joinActions(List<String> actions) {
        if (actions.size() < 2) {
            return String.join("", actions);
        }
        return String.join("、", actions.subList(0, actions.size() - 1))
                + "和" + actions.get(actions.size() - 1);
    }
}
