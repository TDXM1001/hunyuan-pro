package com.hunyuan.sa.bpm.module.candidate.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * M2 策略仅允许受控、类型化的 JSON 结构，拒绝表达式和未登记的扩展字段。
 */
public class PolicyDocumentValidator {

    private static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Set<String> CANDIDATE_RESOLVERS = Set.of(
            "EMPLOYEE", "ROLE", "DEPARTMENT_MANAGER", "START_EMPLOYEE",
            "START_DEPARTMENT_MANAGER", "ROUTING_FACT_EMPLOYEE", "POST",
            "USER_GROUP", "MANAGEMENT_CHAIN"
    );
    private static final Set<String> RESOLUTION_PHASES = Set.of("PUBLISH", "START", "ACTIVATE");
    private static final Set<String> EMPTY_CANDIDATE_POLICIES = Set.of(
            "BLOCK", "ASSIGN_NAMED_EMPLOYEE", "ASSIGN_NAMED_ROLE", "AUTO_APPROVE", "AUTO_REJECT"
    );
    private static final Set<String> SELF_APPROVAL_POLICIES = Set.of(
            "ALLOW", "SKIP_SELF", "ASSIGN_DEPARTMENT_MANAGER", "BLOCK"
    );
    private static final Set<String> COMPLETION_MODES = Set.of("SINGLE", "SEQUENTIAL", "ALL", "ANY", "RATIO");
    private static final Set<String> REJECTION_RULES = Set.of("IMMEDIATE", "WHEN_APPROVAL_UNREACHABLE");
    private static final Set<String> RETURN_RULES = Set.of("RETURN_INITIATOR", "RETURN_ANCESTOR", "END_REJECTED");
    private static final Set<String> TERMINATION_RULES = Set.of("CANCEL_REMAINING_MEMBERS");
    private static final Set<String> RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");
    private static final Set<String> SCOPE_TYPES = Set.of(
            "ALL", "EMPLOYEE_IDS", "ROLE_IDS", "DEPARTMENT_IDS", "ANY_OF", "ALL_OF"
    );

    public void validate(PolicyType policyType, Integer schemaVersion, String policyJson) {
        if (policyType == null) {
            throw new IllegalArgumentException("策略类型不能为空");
        }
        if (schemaVersion == null || schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("不支持的策略 schema 版本：" + schemaVersion);
        }
        JSONObject document;
        try {
            document = JSON.parseObject(policyJson);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("策略 JSON 不合法", ex);
        }
        if (document == null) {
            throw new IllegalArgumentException("策略 JSON 必须为对象");
        }
        switch (policyType) {
            case CANDIDATE -> validateCandidate(document);
            case APPROVAL -> validateApproval(document);
            case START_VISIBILITY -> validateStartVisibility(document);
        }
    }

    private void validateCandidate(JSONObject document) {
        rejectUnknownFields(document, Set.of(
                "resolverType", "resolverParameters", "resolutionPhase", "memberOrder", "duplicateRule",
                "emptyCandidatePolicy", "selfApprovalPolicy", "fallbackIdentityReference", "riskLevel"
        ), "候选策略");
        String resolverType = requiredEnum(document, "resolverType", CANDIDATE_RESOLVERS, "候选解析器类型");
        JSONObject parameters = requiredObject(document, "resolverParameters", "候选策略 resolverParameters 必须为对象");
        optionalEnum(document, "resolutionPhase", RESOLUTION_PHASES, "候选解析时机");
        optionalEnum(document, "emptyCandidatePolicy", EMPTY_CANDIDATE_POLICIES, "空候选策略");
        optionalEnum(document, "selfApprovalPolicy", SELF_APPROVAL_POLICIES, "自审策略");
        optionalEnum(document, "riskLevel", RISK_LEVELS, "风险级别");
        switch (resolverType) {
            case "EMPLOYEE" -> requiredPositiveIdArray(parameters, "employeeIds", "EMPLOYEE 候选策略缺少有效 employeeIds");
            case "ROLE" -> requiredPositiveId(parameters, "roleId", "ROLE 候选策略缺少有效 roleId");
            case "DEPARTMENT_MANAGER" -> requiredPositiveId(parameters, "departmentId", "DEPARTMENT_MANAGER 候选策略缺少有效 departmentId");
            case "ROUTING_FACT_EMPLOYEE" -> requiredText(parameters, "factKey", "ROUTING_FACT_EMPLOYEE 候选策略缺少 factKey");
            case "POST" -> requiredPositiveId(parameters, "positionId", "POST 候选策略缺少有效 positionId");
            case "USER_GROUP" -> requiredPositiveId(parameters, "userGroupId", "USER_GROUP 候选策略缺少有效 userGroupId");
            case "MANAGEMENT_CHAIN" -> {
                optionalEnum(parameters, "chainType", Set.of("DEPARTMENT_MANAGER_CHAIN", "EMPLOYEE_REPORTING_CHAIN"), "管理链类型");
                Integer maxDepth = parameters.getInteger("maxDepth");
                if (maxDepth != null && (maxDepth < 1 || maxDepth > 20)) {
                    throw new IllegalArgumentException("MANAGEMENT_CHAIN maxDepth 必须在 1 到 20 之间");
                }
            }
            default -> {
                // START_EMPLOYEE 与 START_DEPARTMENT_MANAGER 只消费服务端发起人快照。
            }
        }
    }

    private void validateApproval(JSONObject document) {
        rejectUnknownFields(document, Set.of(
                "completionMode", "ratioPercent", "rejectionRule", "returnRule", "terminationRule",
                "allowedActions", "riskLevel"
        ), "审批策略");
        String completionMode = requiredEnum(document, "completionMode", COMPLETION_MODES, "审批完成模式");
        Integer ratioPercent = document.getInteger("ratioPercent");
        if ("RATIO".equals(completionMode) && (ratioPercent == null || ratioPercent < 1 || ratioPercent > 100)) {
            throw new IllegalArgumentException("RATIO 审批策略的 ratioPercent 必须在 1 到 100 之间");
        }
        if (ratioPercent != null && (ratioPercent < 1 || ratioPercent > 100)) {
            throw new IllegalArgumentException("ratioPercent 必须在 1 到 100 之间");
        }
        String rejectionRule = requiredEnum(document, "rejectionRule", REJECTION_RULES, "拒绝规则");
        if (("SINGLE".equals(completionMode) || "SEQUENTIAL".equals(completionMode) || "ALL".equals(completionMode))
                && !"IMMEDIATE".equals(rejectionRule)) {
            throw new IllegalArgumentException(completionMode + " 审批策略只允许 IMMEDIATE 拒绝规则");
        }
        optionalEnum(document, "returnRule", RETURN_RULES, "退回规则");
        optionalEnum(document, "terminationRule", TERMINATION_RULES, "成员终止规则");
        optionalEnum(document, "riskLevel", RISK_LEVELS, "风险级别");
        JSONArray allowedActions = document.getJSONArray("allowedActions");
        if (allowedActions == null || allowedActions.isEmpty()) {
            throw new IllegalArgumentException("审批策略 allowedActions 不能为空");
        }
        for (Object action : allowedActions) {
            if (!(action instanceof String text) || !Set.of("APPROVE", "REJECT", "RETURN").contains(text)) {
                throw new IllegalArgumentException("审批策略 allowedActions 包含不支持的动作");
            }
        }
    }

    private void validateStartVisibility(JSONObject document) {
        rejectUnknownFields(document, Set.of("startScope", "visibilityScope", "riskLevel"), "发起可见范围策略");
        validateScope(requiredObject(document, "startScope", "发起范围 startScope 必须为对象"), "startScope");
        validateScope(requiredObject(document, "visibilityScope", "可见范围 visibilityScope 必须为对象"), "visibilityScope");
        optionalEnum(document, "riskLevel", RISK_LEVELS, "风险级别");
    }

    private void validateScope(JSONObject scope, String fieldName) {
        rejectUnknownFields(scope, Set.of("type", "employeeIds", "roleIds", "departmentIds", "scopes"), fieldName);
        String type = requiredEnum(scope, "type", SCOPE_TYPES, "范围类型");
        switch (type) {
            case "EMPLOYEE_IDS" -> requiredPositiveIdArray(scope, "employeeIds", fieldName + " 缺少有效 employeeIds");
            case "ROLE_IDS" -> requiredPositiveIdArray(scope, "roleIds", fieldName + " 缺少有效 roleIds");
            case "DEPARTMENT_IDS" -> requiredPositiveIdArray(scope, "departmentIds", fieldName + " 缺少有效 departmentIds");
            case "ANY_OF", "ALL_OF" -> {
                JSONArray scopes = scope.getJSONArray("scopes");
                if (scopes == null || scopes.isEmpty()) {
                    throw new IllegalArgumentException(fieldName + " 的组合范围不能为空");
                }
                for (Object nested : scopes) {
                    if (!(nested instanceof JSONObject nestedScope)) {
                        throw new IllegalArgumentException(fieldName + " 的组合范围必须为对象");
                    }
                    validateScope(nestedScope, fieldName + ".scopes");
                }
            }
            default -> {
                // ALL 不接受任意表达式或附加查询条件。
            }
        }
    }

    private JSONObject requiredObject(JSONObject document, String fieldName, String errorMessage) {
        JSONObject value = document.getJSONObject(fieldName);
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    private void requiredPositiveId(JSONObject document, String fieldName, String errorMessage) {
        Long value = document.getLong(fieldName);
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void requiredPositiveIdArray(JSONObject document, String fieldName, String errorMessage) {
        JSONArray values = document.getJSONArray(fieldName);
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        for (Object value : values) {
            if (!(value instanceof Number number) || number.longValue() <= 0) {
                throw new IllegalArgumentException(errorMessage);
            }
        }
    }

    private String requiredEnum(JSONObject document, String fieldName, Set<String> allowed, String label) {
        String value = requiredText(document, fieldName, label + "不能为空");
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException(label + "不合法：" + value);
        }
        return value;
    }

    private void optionalEnum(JSONObject document, String fieldName, Set<String> allowed, String label) {
        String value = document.getString(fieldName);
        if (value != null && !allowed.contains(value)) {
            throw new IllegalArgumentException(label + "不合法：" + value);
        }
    }

    private String requiredText(JSONObject document, String fieldName, String errorMessage) {
        String value = document.getString(fieldName);
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    private void rejectUnknownFields(JSONObject document, Set<String> allowed, String label) {
        for (String fieldName : document.keySet()) {
            if (!allowed.contains(fieldName)) {
                throw new IllegalArgumentException(label + "包含未登记字段：" + fieldName);
            }
        }
    }
}
