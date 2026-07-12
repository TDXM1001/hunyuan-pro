package com.hunyuan.sa.bpm.candidate;

import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.candidate.domain.model.InstanceAccessDecision;
import com.hunyuan.sa.bpm.module.candidate.domain.model.StartDecision;
import com.hunyuan.sa.bpm.module.candidate.domain.model.StartVisibilityEvaluationContext;
import com.hunyuan.sa.bpm.module.candidate.service.StartVisibilityPolicyEvaluator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StartVisibilityPolicyEvaluatorTest {

    @Test
    void startShouldDefaultDenyWhenFrozenScopeDoesNotMatchActor() {
        BpmOrgIdentityGateway identityGateway = mock(BpmOrgIdentityGateway.class);
        BpmEmployeeSnapshot actor = employee(20L, 5L);
        when(identityGateway.requireEmployee(20L)).thenReturn(actor);
        StartVisibilityPolicyEvaluator evaluator = new StartVisibilityPolicyEvaluator(identityGateway);

        StartDecision decision = evaluator.evaluateStart(employeeOnlyPolicy(10L), context(actor));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).contains("未命中");
    }

    @Test
    void startShouldAllowRoleScopeUsingServerSideIdentityGateway() {
        BpmOrgIdentityGateway identityGateway = mock(BpmOrgIdentityGateway.class);
        BpmEmployeeSnapshot actor = employee(20L, 5L);
        when(identityGateway.requireEmployee(20L)).thenReturn(actor);
        when(identityGateway.listEmployeeIdsByRoleId(8L)).thenReturn(java.util.List.of(20L));
        StartVisibilityPolicyEvaluator evaluator = new StartVisibilityPolicyEvaluator(identityGateway);

        StartDecision decision = evaluator.evaluateStart(roleOnlyPolicy(8L), context(actor));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.matchedRule()).contains("ROLE_IDS");
    }

    @Test
    void visibilityShouldKeepParticipantAccessSeparateFromPolicyScope() {
        BpmOrgIdentityGateway identityGateway = mock(BpmOrgIdentityGateway.class);
        BpmEmployeeSnapshot actor = employee(20L, 5L);
        when(identityGateway.requireEmployee(20L)).thenReturn(actor);
        StartVisibilityPolicyEvaluator evaluator = new StartVisibilityPolicyEvaluator(identityGateway);

        InstanceAccessDecision participantDecision = evaluator.evaluateInstanceAccess(
                employeeOnlyPolicy(10L),
                context(actor, Set.of(20L), false)
        );
        InstanceAccessDecision unrelatedDecision = evaluator.evaluateInstanceAccess(
                employeeOnlyPolicy(10L),
                context(actor, Set.of(30L), false)
        );

        assertThat(participantDecision.allowed()).isTrue();
        assertThat(participantDecision.reason()).contains("参与者");
        assertThat(unrelatedDecision.allowed()).isFalse();
    }

    private StartVisibilityEvaluationContext context(BpmEmployeeSnapshot actor) {
        return context(actor, Set.of(), false);
    }

    private StartVisibilityEvaluationContext context(
            BpmEmployeeSnapshot actor,
            Set<Long> participantEmployeeIds,
            boolean explicitlyAuthorized
    ) {
        return new StartVisibilityEvaluationContext(1L, actor, participantEmployeeIds, explicitlyAuthorized);
    }

    private String employeeOnlyPolicy(long employeeId) {
        return "{\"startScope\":{\"type\":\"EMPLOYEE_IDS\",\"employeeIds\":[" + employeeId + "]},"
                + "\"visibilityScope\":{\"type\":\"EMPLOYEE_IDS\",\"employeeIds\":[" + employeeId + "]}}";
    }

    private String roleOnlyPolicy(long roleId) {
        return "{\"startScope\":{\"type\":\"ROLE_IDS\",\"roleIds\":[" + roleId + "]},"
                + "\"visibilityScope\":{\"type\":\"ROLE_IDS\",\"roleIds\":[" + roleId + "]}}";
    }

    private BpmEmployeeSnapshot employee(long employeeId, long departmentId) {
        return new BpmEmployeeSnapshot(employeeId, "员工" + employeeId, departmentId, "财务部", null, null);
    }
}
