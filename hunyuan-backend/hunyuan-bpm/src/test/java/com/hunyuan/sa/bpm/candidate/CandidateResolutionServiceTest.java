package com.hunyuan.sa.bpm.candidate;

import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.candidate.domain.model.CandidateResolutionContext;
import com.hunyuan.sa.bpm.module.candidate.domain.model.CandidateAutomaticOutcome;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyPublicationLease;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyReference;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ResolvedCandidateMember;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ResolvedCandidateSnapshot;
import com.hunyuan.sa.bpm.module.candidate.domain.model.RoutingFactView;
import com.hunyuan.sa.bpm.module.candidate.service.CandidateResolutionService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CandidateResolutionServiceTest {

    @Test
    void roleResolutionShouldFilterInvalidEmployeesDeduplicateAndSort() {
        BpmOrgIdentityGateway identityGateway = mock(BpmOrgIdentityGateway.class);
        when(identityGateway.listEmployeeIdsByRoleId(8L)).thenReturn(List.of(30L, 20L, 30L));
        when(identityGateway.requireEmployee(20L)).thenReturn(employee(20L));
        when(identityGateway.requireEmployee(30L)).thenThrow(new IllegalArgumentException("员工已停用"));
        CandidateResolutionService resolver = new CandidateResolutionService(identityGateway);

        ResolvedCandidateSnapshot snapshot = resolver.resolve(rolePolicy(8L), context());

        assertThat(snapshot.members())
                .extracting(ResolvedCandidateMember::sourceEmployeeId)
                .containsExactly(20L);
        assertThat(snapshot.diagnostics())
                .anySatisfy(diagnostic -> assertThat(diagnostic.code()).isEqualTo("IDENTITY_UNAVAILABLE"));
    }

    @Test
    void employeeResolutionShouldFreezeExplicitEmployeesInStableOrder() {
        BpmOrgIdentityGateway identityGateway = mock(BpmOrgIdentityGateway.class);
        when(identityGateway.requireEmployee(20L)).thenReturn(employee(20L));
        when(identityGateway.requireEmployee(30L)).thenReturn(employee(30L));
        CandidateResolutionService resolver = new CandidateResolutionService(identityGateway);

        ResolvedCandidateSnapshot snapshot = resolver.resolve(employeePolicy(30L, 20L, 30L), context());

        assertThat(snapshot.members())
                .extracting(ResolvedCandidateMember::sourceEmployeeId)
                .containsExactly(20L, 30L);
    }

    @Test
    void startDepartmentManagerResolutionShouldUseConfirmedStartDepartment() {
        BpmOrgIdentityGateway identityGateway = mock(BpmOrgIdentityGateway.class);
        when(identityGateway.resolveDepartmentManagerEmployeeId(5L)).thenReturn(40L);
        when(identityGateway.requireEmployee(40L)).thenReturn(employee(40L));
        CandidateResolutionService resolver = new CandidateResolutionService(identityGateway);

        ResolvedCandidateSnapshot snapshot = resolver.resolve(startDepartmentManagerPolicy(), context());

        assertThat(snapshot.members())
                .extracting(ResolvedCandidateMember::sourceEmployeeId)
                .containsExactly(40L);
    }

    @Test
    void routingFactResolutionShouldUseOnlyWhitelistedEmployeeFact() {
        BpmOrgIdentityGateway identityGateway = mock(BpmOrgIdentityGateway.class);
        when(identityGateway.requireEmployee(50L)).thenReturn(employee(50L));
        CandidateResolutionService resolver = new CandidateResolutionService(identityGateway);
        CandidateResolutionContext context = new CandidateResolutionContext(
                1L,
                101L,
                "manager-review",
                "stage-1",
                employee(10L),
                new RoutingFactView("expense-v2", "route-v1", Set.of("financeApprover"), Map.of("financeApprover", 50L)),
                LocalDateTime.of(2026, 7, 12, 17, 30)
        );

        ResolvedCandidateSnapshot snapshot = resolver.resolve(routingFactPolicy("financeApprover"), context);

        assertThat(snapshot.members())
                .extracting(ResolvedCandidateMember::sourceEmployeeId)
                .containsExactly(50L);
    }

    @Test
    void postResolutionShouldUseActiveEmployeesFromOrganizationGateway() {
        BpmOrgIdentityGateway identityGateway = mock(BpmOrgIdentityGateway.class);
        when(identityGateway.listActiveEmployeeIdsByPositionId(8L)).thenReturn(List.of(30L, 20L));
        when(identityGateway.requireEmployee(20L)).thenReturn(employee(20L));
        when(identityGateway.requireEmployee(30L)).thenReturn(employee(30L));
        CandidateResolutionService resolver = new CandidateResolutionService(identityGateway);

        ResolvedCandidateSnapshot snapshot = resolver.resolve(postPolicy(8L), context());

        assertThat(snapshot.members())
                .extracting(ResolvedCandidateMember::sourceEmployeeId)
                .containsExactly(20L, 30L);
    }

    @Test
    void unavailableOrganizationSourceShouldFailClosedInsteadOfFallingBackToRole() {
        CandidateResolutionService resolver = new CandidateResolutionService(mock(BpmOrgIdentityGateway.class));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> resolver.resolve(userGroupPolicy(8L), context()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("失败关闭");
    }

    @Test
    void userGroupResolutionShouldUseRegisteredOrganizationGroupMembers() {
        BpmOrgIdentityGateway identityGateway = mock(BpmOrgIdentityGateway.class);
        when(identityGateway.listActiveEmployeeIdsByUserGroupId(8L)).thenReturn(List.of(30L, 20L, 30L));
        when(identityGateway.requireEmployee(20L)).thenReturn(employee(20L));
        when(identityGateway.requireEmployee(30L)).thenReturn(employee(30L));
        CandidateResolutionService resolver = new CandidateResolutionService(identityGateway);

        ResolvedCandidateSnapshot snapshot = resolver.resolve(userGroupPolicy(8L), context());

        assertThat(snapshot.members())
                .extracting(ResolvedCandidateMember::sourceEmployeeId)
                .containsExactly(20L, 30L);
    }

    @Test
    void departmentManagementChainShouldUseRegisteredNearestToFarthestManagers() {
        BpmOrgIdentityGateway identityGateway = mock(BpmOrgIdentityGateway.class);
        when(identityGateway.listDepartmentManagerChain(8L, 3)).thenReturn(List.of(47L, 44L, 1L));
        when(identityGateway.requireEmployee(1L)).thenReturn(employee(1L));
        when(identityGateway.requireEmployee(44L)).thenReturn(employee(44L));
        when(identityGateway.requireEmployee(47L)).thenReturn(employee(47L));
        CandidateResolutionService resolver = new CandidateResolutionService(identityGateway);

        ResolvedCandidateSnapshot snapshot = resolver.resolve(departmentManagementChainPolicy(8L, 3), context());

        assertThat(snapshot.members())
                .extracting(ResolvedCandidateMember::sourceEmployeeId)
                .containsExactly(47L, 44L, 1L);
    }

    @Test
    void employeeReportingChainShouldUseRegisteredManagerRelations() {
        BpmOrgIdentityGateway identityGateway = mock(BpmOrgIdentityGateway.class);
        when(identityGateway.listEmployeeReportingManagerChain(20L, 2)).thenReturn(List.of(30L, 40L));
        when(identityGateway.requireEmployee(30L)).thenReturn(employee(30L));
        when(identityGateway.requireEmployee(40L)).thenReturn(employee(40L));
        CandidateResolutionService resolver = new CandidateResolutionService(identityGateway);

        ResolvedCandidateSnapshot snapshot = resolver.resolve(employeeReportingChainPolicy(20L, 2), context());

        assertThat(snapshot.members())
                .extracting(ResolvedCandidateMember::sourceEmployeeId)
                .containsExactly(30L, 40L);
    }

    @Test
    void selfApprovalBlockShouldFailBeforeOpeningAStage() {
        BpmOrgIdentityGateway identityGateway = mock(BpmOrgIdentityGateway.class);
        when(identityGateway.requireEmployee(10L)).thenReturn(employee(10L));
        CandidateResolutionService resolver = new CandidateResolutionService(identityGateway);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> resolver.resolve(
                        candidatePolicy("EMPLOYEE", "{\"employeeIds\":[10]}", "BLOCK", "BLOCK"),
                        context()
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("自审");
    }

    @Test
    void skipSelfShouldRemoveStarterAndKeepOtherFrozenMembers() {
        BpmOrgIdentityGateway identityGateway = mock(BpmOrgIdentityGateway.class);
        when(identityGateway.requireEmployee(10L)).thenReturn(employee(10L));
        when(identityGateway.requireEmployee(20L)).thenReturn(employee(20L));
        CandidateResolutionService resolver = new CandidateResolutionService(identityGateway);

        ResolvedCandidateSnapshot snapshot = resolver.resolve(
                candidatePolicy("EMPLOYEE", "{\"employeeIds\":[10,20]}", "BLOCK", "SKIP_SELF"),
                context()
        );

        assertThat(snapshot.members())
                .extracting(ResolvedCandidateMember::sourceEmployeeId)
                .containsExactly(20L);
    }

    @Test
    void emptyCandidateAutoApproveShouldReturnExplicitAutomaticOutcomeWithoutMembers() {
        CandidateResolutionService resolver = new CandidateResolutionService(mock(BpmOrgIdentityGateway.class));

        ResolvedCandidateSnapshot snapshot = resolver.resolve(
                candidatePolicy("EMPLOYEE", "{\"employeeIds\":[999]}", "AUTO_APPROVE", "BLOCK"),
                context()
        );

        assertThat(snapshot.members()).isEmpty();
        assertThat(snapshot.automaticOutcome()).isEqualTo(CandidateAutomaticOutcome.AUTO_APPROVE);
    }

    private PolicyPublicationLease rolePolicy(long roleId) {
        PolicyReference reference = new PolicyReference(PolicyType.CANDIDATE, "finance-manager", 2);
        return new PolicyPublicationLease(
                reference,
                31L,
                1,
                "{\"resolverType\":\"ROLE\",\"resolverParameters\":{\"roleId\":" + roleId + "}}",
                "digest",
                "publish-1"
        );
    }

    private PolicyPublicationLease employeePolicy(long... employeeIds) {
        String joinedIds = java.util.Arrays.stream(employeeIds)
                .mapToObj(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        return policy("{\"resolverType\":\"EMPLOYEE\",\"resolverParameters\":{\"employeeIds\":[" + joinedIds + "]}}");
    }

    private PolicyPublicationLease startDepartmentManagerPolicy() {
        return policy("{\"resolverType\":\"START_DEPARTMENT_MANAGER\",\"resolverParameters\":{}}");
    }

    private PolicyPublicationLease routingFactPolicy(String factKey) {
        return policy("{\"resolverType\":\"ROUTING_FACT_EMPLOYEE\",\"resolverParameters\":{\"factKey\":\"" + factKey + "\"}}");
    }

    private PolicyPublicationLease postPolicy(long positionId) {
        return policy("{\"resolverType\":\"POST\",\"resolverParameters\":{\"positionId\":" + positionId + "}}");
    }

    private PolicyPublicationLease userGroupPolicy(long userGroupId) {
        return policy("{\"resolverType\":\"USER_GROUP\",\"resolverParameters\":{\"userGroupId\":" + userGroupId + "}}");
    }

    private PolicyPublicationLease departmentManagementChainPolicy(long departmentId, int maxDepth) {
        return policy("{\"resolverType\":\"MANAGEMENT_CHAIN\",\"resolverParameters\":{" +
                "\"chainType\":\"DEPARTMENT_MANAGER_CHAIN\"," +
                "\"seedIdentityReference\":{\"kind\":\"DEPARTMENT\",\"stableId\":" + departmentId + "}," +
                "\"maxDepth\":" + maxDepth + "}}");
    }

    private PolicyPublicationLease employeeReportingChainPolicy(long employeeId, int maxDepth) {
        return policy("{\"resolverType\":\"MANAGEMENT_CHAIN\",\"resolverParameters\":{" +
                "\"chainType\":\"EMPLOYEE_REPORTING_CHAIN\"," +
                "\"seedIdentityReference\":{\"kind\":\"EMPLOYEE\",\"stableId\":" + employeeId + "}," +
                "\"maxDepth\":" + maxDepth + "}}");
    }

    private PolicyPublicationLease candidatePolicy(
            String resolverType,
            String resolverParameters,
            String emptyCandidatePolicy,
            String selfApprovalPolicy
    ) {
        return policy("{\"resolverType\":\"" + resolverType + "\",\"resolverParameters\":"
                + resolverParameters + ",\"emptyCandidatePolicy\":\"" + emptyCandidatePolicy
                + "\",\"selfApprovalPolicy\":\"" + selfApprovalPolicy + "\"}");
    }

    private PolicyPublicationLease policy(String payload) {
        return new PolicyPublicationLease(
                new PolicyReference(PolicyType.CANDIDATE, "finance-manager", 2),
                31L,
                1,
                payload,
                "digest",
                "publish-1"
        );
    }

    private CandidateResolutionContext context() {
        return new CandidateResolutionContext(
                1L,
                101L,
                "manager-review",
                "stage-1",
                employee(10L),
                LocalDateTime.of(2026, 7, 12, 17, 30)
        );
    }

    private BpmEmployeeSnapshot employee(long employeeId) {
        return new BpmEmployeeSnapshot(employeeId, "员工" + employeeId, 5L, "财务部", null, null);
    }
}
