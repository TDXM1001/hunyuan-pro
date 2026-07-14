package com.hunyuan.sa.bpm.candidate;

import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.candidate.domain.vo.BpmPolicySimulationMemberVO;
import com.hunyuan.sa.bpm.module.candidate.domain.vo.BpmPolicySimulationVO;
import com.hunyuan.sa.bpm.module.candidate.service.BpmPolicySimulationService;
import com.hunyuan.sa.bpm.module.candidate.service.CandidateResolutionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BpmPolicySimulationServiceTest {

    @Test
    void simulateCandidateShouldUseRuntimeResolverAndReturnBusinessNames() {
        BpmOrgIdentityGateway gateway = mock(BpmOrgIdentityGateway.class);
        when(gateway.listEmployeeIdsByRoleId(8L)).thenReturn(List.of(2L));
        when(gateway.requireEmployee(1L)).thenReturn(employee(1L, "管理员"));
        when(gateway.requireEmployee(2L)).thenReturn(employee(2L, "胡克"));
        BpmPolicySimulationService service = new BpmPolicySimulationService(
                new CandidateResolutionService(gateway), gateway);

        BpmPolicySimulationVO result = service.simulate(
                PolicySimulationFixtures.roleCandidateWithStarter(8L, 1L), 99L);

        assertThat(result.resolvedMembers())
                .extracting(BpmPolicySimulationMemberVO::employeeName)
                .containsExactly("胡克");
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void disabledRoleMustReturnBlockingFindingWithBusinessName() {
        BpmOrgIdentityGateway gateway = mock(BpmOrgIdentityGateway.class);
        when(gateway.requireEmployee(1L)).thenReturn(employee(1L, "管理员"));
        when(gateway.listEmployeeIdsByRoleId(8L)).thenThrow(new IllegalArgumentException("角色已禁用"));
        BpmPolicySimulationService service = new BpmPolicySimulationService(
                new CandidateResolutionService(gateway), gateway);

        BpmPolicySimulationVO result = service.simulate(
                PolicySimulationFixtures.roleCandidateWithStarter(8L, 1L), 99L);

        assertThat(result.findings()).anySatisfy(finding -> {
            assertThat(finding.fieldPath()).isEqualTo("candidate.identityReference");
            assertThat(finding.message()).contains("财务经理", "已禁用");
        });
    }

    private BpmEmployeeSnapshot employee(long id, String name) {
        return new BpmEmployeeSnapshot(id, name, 10L, "研发部", null, null);
    }
}
