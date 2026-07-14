package com.hunyuan.sa.bpm.module.candidate.service;

import com.alibaba.fastjson.JSON;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.ApprovalPolicyVisualDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.BpmPolicyVisualDraft;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.CandidatePolicyVisualDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.PolicyIdentityReference;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.PolicyRiskAssessment;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.PolicyScopeVisualDocument;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.PolicyValidationFinding;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.PolicyVisualCompilation;
import com.hunyuan.sa.bpm.module.candidate.domain.visual.StartVisibilityPolicyVisualDocument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PolicyVisualDocumentMapper {

    private final PolicyCanonicalizer canonicalizer;
    private final PolicyDocumentValidator validator;
    private final PolicyBusinessSummaryService summaryService;
    private final PolicyRiskAssessmentService riskService;

    public PolicyVisualDocumentMapper(
            PolicyCanonicalizer canonicalizer,
            PolicyDocumentValidator validator,
            PolicyBusinessSummaryService summaryService,
            PolicyRiskAssessmentService riskService
    ) {
        this.canonicalizer = canonicalizer;
        this.validator = validator;
        this.summaryService = summaryService;
        this.riskService = riskService;
    }

    public PolicyVisualCompilation compile(BpmPolicyVisualDraft draft) {
        List<PolicyValidationFinding> findings = new ArrayList<>();
        Map<String, Object> document = new LinkedHashMap<>();
        String summary = "配置不完整，暂时无法生成摘要。";
        PolicyRiskAssessment risk = riskService.assess(draft);
        try {
            requireDraft(draft);
            document.put("schemaVersion", 2);
            switch (draft.type()) {
                case CANDIDATE -> mapCandidate(document, draft.candidate());
                case APPROVAL -> mapApproval(document, draft.approval());
                case START_VISIBILITY -> mapStartVisibility(document, draft.startVisibility());
            }
            summary = summaryService.summarize(draft);
        } catch (RuntimeException ex) {
            findings.add(finding(ex));
        }

        String canonicalPayload = canonicalizer.canonicalize(JSON.toJSONString(document));
        String digest = canonicalizer.sha256(canonicalPayload);
        if (findings.isEmpty()) {
            try {
                validator.validate(draft.type(), 2, canonicalPayload);
            } catch (IllegalArgumentException ex) {
                findings.add(finding(ex));
            }
        }
        return new PolicyVisualCompilation(
                canonicalPayload,
                digest,
                summary,
                risk.level(),
                List.copyOf(findings)
        );
    }

    private void requireDraft(BpmPolicyVisualDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("规则草稿不能为空");
        }
        if (draft.schemaVersion() == null || draft.schemaVersion() != 2) {
            throw new IllegalArgumentException("可视化规则必须使用 Schema v2");
        }
        draft.requireMatchingDocument();
    }

    private void mapCandidate(Map<String, Object> target, CandidatePolicyVisualDocument source) {
        target.put("resolverType", source.resolverType());
        target.put("resolverParameters", resolverParameters(source.resolverType(), source.identityReference()));
        target.put("resolutionPhase", text(source.resolutionPhase(), "ACTIVATE"));
        target.put("memberOrder", text(source.memberOrder(), "SELECTION_ORDER"));
        target.put("duplicateRule", "KEEP_FIRST");
        target.put("emptyCandidatePolicy", text(source.emptyCandidatePolicy(), "BLOCK"));
        target.put("selfApprovalPolicy", text(source.selfApprovalPolicy(), "BLOCK"));
        if (source.fallbackIdentityReference() != null) {
            target.put("fallbackIdentityReference", identityReference(source.fallbackIdentityReference()));
        }
    }

    private Map<String, Object> resolverParameters(String resolverType, PolicyIdentityReference identity) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (identity == null) {
            return parameters;
        }
        switch (text(resolverType, "")) {
            case "EMPLOYEE" -> parameters.put("employeeIds", List.of(identity.stableId()));
            case "ROLE" -> parameters.put("roleId", identity.stableId());
            case "DEPARTMENT_MANAGER" -> parameters.put("departmentId", identity.stableId());
            case "POST" -> parameters.put("positionId", identity.stableId());
            case "USER_GROUP" -> parameters.put("userGroupId", identity.stableId());
            case "ROUTING_FACT_EMPLOYEE" -> parameters.put("factKey", identity.factKey());
            default -> {
                // 发起人及其部门负责人从运行时快照解析，不接受客户端 ID。
            }
        }
        return parameters;
    }

    private Map<String, Object> identityReference(PolicyIdentityReference source) {
        Map<String, Object> reference = new LinkedHashMap<>();
        reference.put("kind", source.kind());
        if (source.stableId() != null) {
            reference.put("stableId", source.stableId());
        }
        if (source.factKey() != null && !source.factKey().isBlank()) {
            reference.put("factKey", source.factKey());
        }
        return reference;
    }

    private void mapApproval(Map<String, Object> target, ApprovalPolicyVisualDocument source) {
        target.put("completionMode", source.completionMode());
        if (source.ratioPercent() != null) {
            target.put("ratioPercent", source.ratioPercent());
        }
        target.put("rejectionRule", source.rejectionRule());
        target.put("returnRule", source.returnRule());
        target.put("terminationRule", "CANCEL_REMAINING_MEMBERS");
        target.put("allowedActions", source.allowedActions());
    }

    private void mapStartVisibility(Map<String, Object> target, StartVisibilityPolicyVisualDocument source) {
        target.put("startScope", scope(source.startScope()));
        target.put("visibilityScope", scope(source.visibilityScope()));
    }

    private Map<String, Object> scope(PolicyScopeVisualDocument source) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", source.type());
        if (source.identities() != null && !source.identities().isEmpty()) {
            String idField = switch (source.type()) {
                case "EMPLOYEE_IDS" -> "employeeIds";
                case "ROLE_IDS" -> "roleIds";
                case "DEPARTMENT_IDS" -> "departmentIds";
                default -> null;
            };
            if (idField != null) {
                result.put(idField, source.identities().stream().map(PolicyIdentityReference::stableId).toList());
            }
        }
        if (source.scopes() != null && !source.scopes().isEmpty()) {
            result.put("scopes", source.scopes().stream().map(this::scope).toList());
        }
        return result;
    }

    private PolicyValidationFinding finding(RuntimeException ex) {
        String message = ex.getMessage() == null ? "规则配置不合法" : ex.getMessage();
        String path = fieldPath(message);
        return new PolicyValidationFinding(
                "POLICY_VISUAL_INVALID",
                "BLOCKING",
                path,
                message,
                "请检查该字段并重新校验"
        );
    }

    private String fieldPath(String message) {
        if (message.contains("fallbackIdentityReference")) {
            return "candidate.emptyCandidatePolicy";
        }
        if (message.contains("ratioPercent")) {
            return "approval.ratioPercent";
        }
        if (message.contains("allowedActions") || message.contains("动作")) {
            return "approval.allowedActions";
        }
        if (message.contains("selfApprovalPolicy") || message.contains("自审")) {
            return "candidate.selfApprovalPolicy";
        }
        if (message.contains("resolver") || message.contains("employeeIds") || message.contains("roleId")) {
            return "candidate.identityReference";
        }
        return "document";
    }

    private String text(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
