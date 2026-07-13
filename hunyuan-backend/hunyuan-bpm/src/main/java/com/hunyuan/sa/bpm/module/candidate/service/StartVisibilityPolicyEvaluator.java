package com.hunyuan.sa.bpm.module.candidate.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.candidate.domain.model.InstanceAccessDecision;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import com.hunyuan.sa.bpm.module.candidate.domain.model.StartDecision;
import com.hunyuan.sa.bpm.module.candidate.domain.model.StartVisibilityEvaluationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 解释定义版本内已冻结的发起/可见范围策略。未命中范围时始终失败关闭。
 */
@Service
public class StartVisibilityPolicyEvaluator {

    private final BpmOrgIdentityGateway identityGateway;
    private final PolicyDocumentValidator validator;

    @Autowired
    public StartVisibilityPolicyEvaluator(BpmOrgIdentityGateway identityGateway) {
        this(identityGateway, new PolicyDocumentValidator());
    }

    StartVisibilityPolicyEvaluator(BpmOrgIdentityGateway identityGateway, PolicyDocumentValidator validator) {
        this.identityGateway = identityGateway;
        this.validator = validator;
    }

    public StartDecision evaluateStart(String frozenPolicyPayload, StartVisibilityEvaluationContext context) {
        return evaluateStart(1, frozenPolicyPayload, context);
    }

    public StartDecision evaluateStart(
            Integer schemaVersion,
            String frozenPolicyPayload,
            StartVisibilityEvaluationContext context
    ) {
        JSONObject policy = readPolicy(schemaVersion, frozenPolicyPayload);
        BpmEmployeeSnapshot actor = requireCurrentActor(context);
        ScopeMatch match = evaluateScope(policy.getJSONObject("startScope"), actor);
        return match.matched()
                ? new StartDecision(true, match.rule(), "命中发起范围")
                : new StartDecision(false, null, "未命中发起范围");
    }

    public InstanceAccessDecision evaluateInstanceAccess(
            String frozenPolicyPayload,
            StartVisibilityEvaluationContext context
    ) {
        return evaluateInstanceAccess(1, frozenPolicyPayload, context);
    }

    public InstanceAccessDecision evaluateInstanceAccess(
            Integer schemaVersion,
            String frozenPolicyPayload,
            StartVisibilityEvaluationContext context
    ) {
        JSONObject policy = readPolicy(schemaVersion, frozenPolicyPayload);
        BpmEmployeeSnapshot actor = requireCurrentActor(context);
        if (context.explicitlyAuthorized()) {
            return new InstanceAccessDecision(true, "EXPLICIT", "命中明确实例授权");
        }
        if (context.participantEmployeeIds().contains(actor.employeeId())) {
            return new InstanceAccessDecision(true, "PARTICIPANT", "作为实例参与者可查看最小事实");
        }
        ScopeMatch match = evaluateScope(policy.getJSONObject("visibilityScope"), actor);
        return match.matched()
                ? new InstanceAccessDecision(true, match.rule(), "命中实例可见范围")
                : new InstanceAccessDecision(false, null, "未命中实例可见范围");
    }

    private JSONObject readPolicy(Integer schemaVersion, String frozenPolicyPayload) {
        validator.validate(PolicyType.START_VISIBILITY, schemaVersion, frozenPolicyPayload);
        JSONObject policy = JSON.parseObject(frozenPolicyPayload);
        if (policy == null) {
            throw new IllegalArgumentException("冻结发起可见范围策略不能为空");
        }
        return policy;
    }

    private BpmEmployeeSnapshot requireCurrentActor(StartVisibilityEvaluationContext context) {
        BpmEmployeeSnapshot current = identityGateway.requireEmployee(context.actor().employeeId());
        if (current == null || !Objects.equals(current.employeeId(), context.actor().employeeId())) {
            throw new IllegalStateException("当前员工身份校验失败");
        }
        return current;
    }

    private ScopeMatch evaluateScope(JSONObject scope, BpmEmployeeSnapshot actor) {
        String type = scope.getString("type");
        return switch (type) {
            case "ALL" -> new ScopeMatch(true, "ALL");
            case "EMPLOYEE_IDS" -> new ScopeMatch(containsId(scope.getJSONArray("employeeIds"), actor.employeeId()), "EMPLOYEE_IDS");
            case "DEPARTMENT_IDS" -> new ScopeMatch(containsId(scope.getJSONArray("departmentIds"), actor.departmentId()), "DEPARTMENT_IDS");
            case "ROLE_IDS" -> matchesRole(scope.getJSONArray("roleIds"), actor.employeeId());
            case "ANY_OF" -> matchesAny(scope.getJSONArray("scopes"), actor);
            case "ALL_OF" -> matchesAll(scope.getJSONArray("scopes"), actor);
            default -> throw new IllegalArgumentException("范围类型不合法：" + type);
        };
    }

    private ScopeMatch matchesRole(JSONArray roleIds, Long employeeId) {
        for (Object roleId : roleIds) {
            Long normalizedRoleId = ((Number) roleId).longValue();
            if (identityGateway.listEmployeeIdsByRoleId(normalizedRoleId).contains(employeeId)) {
                return new ScopeMatch(true, "ROLE_IDS:" + normalizedRoleId);
            }
        }
        return new ScopeMatch(false, "ROLE_IDS");
    }

    private ScopeMatch matchesAny(JSONArray scopes, BpmEmployeeSnapshot actor) {
        for (Object rawScope : scopes) {
            ScopeMatch nested = evaluateScope((JSONObject) rawScope, actor);
            if (nested.matched()) {
                return new ScopeMatch(true, "ANY_OF(" + nested.rule() + ")");
            }
        }
        return new ScopeMatch(false, "ANY_OF");
    }

    private ScopeMatch matchesAll(JSONArray scopes, BpmEmployeeSnapshot actor) {
        StringBuilder rules = new StringBuilder("ALL_OF(");
        for (int index = 0; index < scopes.size(); index++) {
            ScopeMatch nested = evaluateScope(scopes.getJSONObject(index), actor);
            if (!nested.matched()) {
                return new ScopeMatch(false, "ALL_OF");
            }
            if (index > 0) {
                rules.append(',');
            }
            rules.append(nested.rule());
        }
        return new ScopeMatch(true, rules.append(')').toString());
    }

    private boolean containsId(JSONArray ids, Long targetId) {
        if (targetId == null) {
            return false;
        }
        for (Object id : ids) {
            if (id instanceof Number number && number.longValue() == targetId) {
                return true;
            }
        }
        return false;
    }

    private record ScopeMatch(boolean matched, String rule) {
    }
}
