package com.hunyuan.sa.bpm.candidate;

import com.hunyuan.sa.bpm.module.candidate.dao.BpmCandidatePolicyVersionDao;
import com.hunyuan.sa.bpm.module.candidate.dao.BpmApprovalPolicyVersionDao;
import com.hunyuan.sa.bpm.module.candidate.dao.BpmStartVisibilityPolicyVersionDao;
import com.hunyuan.sa.bpm.module.candidate.domain.entity.BpmCandidatePolicyVersionEntity;
import com.hunyuan.sa.bpm.module.candidate.domain.entity.BpmStartVisibilityPolicyVersionEntity;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyCatalogVersion;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyDraftCommand;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyLifecycleCommand;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyPublicationLease;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyReference;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyValidationResult;
import com.hunyuan.sa.bpm.module.candidate.domain.vo.BpmPolicyBusinessDetailVO;
import com.hunyuan.sa.bpm.module.candidate.domain.vo.BpmPolicyCatalogSummaryVO;
import com.hunyuan.sa.bpm.module.candidate.service.BpmPolicyCatalogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.*;

class BpmPolicyCatalogServiceTest {

    @Test
    void saveShouldUpdateOnlyDraftWithExpectedRevision() {
        BpmCandidatePolicyVersionDao candidateDao = Mockito.mock(BpmCandidatePolicyVersionDao.class);
        when(candidateDao.saveDraftVisual(
                eq("finance-reviewer"), eq(2), eq(3L), any(), any(), any(), any(), any(), any()
        )).thenReturn(1);
        BpmPolicyCatalogService service = new BpmPolicyCatalogService(
                candidateDao,
                Mockito.mock(BpmApprovalPolicyVersionDao.class),
                Mockito.mock(BpmStartVisibilityPolicyVersionDao.class)
        );

        BpmPolicyBusinessDetailVO saved = service.saveVisualDraft(
                new PolicyReference(PolicyType.CANDIDATE, "finance-reviewer", 2),
                3L,
                PolicyVisualFixtures.roleCandidate("finance-reviewer", "费用审批人", 8L, "财务经理"),
                1L
        );

        assertThat(saved.catalogRevision()).isEqualTo(4L);
        assertThat(saved.businessSummary()).contains("财务经理");
        verify(candidateDao).saveDraftVisual(
                eq("finance-reviewer"), eq(2), eq(3L), eq("费用审批人"), any(),
                any(), any(), eq("LOW"), any()
        );
    }

    @Test
    void staleSaveMustFailWithoutOverwriting() {
        BpmCandidatePolicyVersionDao candidateDao = Mockito.mock(BpmCandidatePolicyVersionDao.class);
        when(candidateDao.saveDraftVisual(anyString(), anyInt(), eq(2L), any(), any(), any(), any(), any(), any()))
                .thenReturn(0);
        BpmPolicyCatalogService service = new BpmPolicyCatalogService(
                candidateDao,
                Mockito.mock(BpmApprovalPolicyVersionDao.class),
                Mockito.mock(BpmStartVisibilityPolicyVersionDao.class)
        );

        assertThatThrownBy(() -> service.saveVisualDraft(
                new PolicyReference(PolicyType.CANDIDATE, "finance-reviewer", 2),
                2L,
                PolicyVisualFixtures.roleCandidate("finance-reviewer", "费用审批人", 8L, "财务经理"),
                1L
        )).hasMessageContaining("CATALOG_REVISION_CONFLICT");
    }

    @Test
    void businessResponsesMustNotExposeCanonicalPayloadOrDigest() {
        assertThat(BpmPolicyCatalogSummaryVO.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("canonicalPayload", "policyJson", "digest");
        assertThat(BpmPolicyBusinessDetailVO.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("canonicalPayload", "policyJson", "digest");
    }

    @Test
    void springConstructorShouldDeclareInjectionPointWhenClassHasMultipleConstructors() throws Exception {
        assertThat(BpmPolicyCatalogService.class.getConstructor(
                BpmCandidatePolicyVersionDao.class,
                BpmApprovalPolicyVersionDao.class,
                BpmStartVisibilityPolicyVersionDao.class
        ).isAnnotationPresent(Autowired.class)).isTrue();
    }

    @Test
    void freezeShouldReturnCanonicalPayloadForActiveCandidatePolicy() {
        BpmCandidatePolicyVersionDao candidateDao = Mockito.mock(BpmCandidatePolicyVersionDao.class);
        BpmCandidatePolicyVersionEntity entity = candidatePolicy("ACTIVE", "{\"resolverType\":\"ROLE\",\"resolverParameters\":{\"roleId\":8},\"resolutionPhase\":\"ACTIVATE\",\"emptyCandidatePolicy\":\"BLOCK\",\"selfApprovalPolicy\":\"BLOCK\"}");
        when(candidateDao.selectByPolicyKeyAndVersionForUpdate("finance-manager", 2)).thenReturn(entity);
        BpmPolicyCatalogService service = new BpmPolicyCatalogService(
                candidateDao,
                Mockito.mock(BpmApprovalPolicyVersionDao.class),
                Mockito.mock(BpmStartVisibilityPolicyVersionDao.class)
        );

        PolicyPublicationLease lease = service.freezeForPublication(
                new PolicyReference(PolicyType.CANDIDATE, "finance-manager", 2),
                "publish-1"
        );

        assertThat(lease.canonicalPayload()).isEqualTo("{\"emptyCandidatePolicy\":\"BLOCK\",\"resolutionPhase\":\"ACTIVATE\",\"resolverParameters\":{\"roleId\":8},\"resolverType\":\"ROLE\",\"selfApprovalPolicy\":\"BLOCK\"}");
        assertThat(lease.digest()).hasSize(64);
        assertThat(lease.policyVersionId()).isEqualTo(31L);
        verify(candidateDao).selectByPolicyKeyAndVersionForUpdate("finance-manager", 2);
    }

    @Test
    void freezeShouldRejectRetiredCandidatePolicy() {
        BpmCandidatePolicyVersionDao candidateDao = Mockito.mock(BpmCandidatePolicyVersionDao.class);
        when(candidateDao.selectByPolicyKeyAndVersionForUpdate("finance-manager", 2))
                .thenReturn(candidatePolicy("RETIRED", "{\"resolverType\":\"ROLE\"}"));
        BpmPolicyCatalogService service = new BpmPolicyCatalogService(
                candidateDao,
                Mockito.mock(BpmApprovalPolicyVersionDao.class),
                Mockito.mock(BpmStartVisibilityPolicyVersionDao.class)
        );

        assertThatThrownBy(() -> service.freezeForPublication(
                new PolicyReference(PolicyType.CANDIDATE, "finance-manager", 2),
                "publish-2"
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未启用");
    }

    @Test
    void freezeShouldRejectCandidatePolicyWithUnregisteredResolverType() {
        BpmCandidatePolicyVersionDao candidateDao = Mockito.mock(BpmCandidatePolicyVersionDao.class);
        when(candidateDao.selectByPolicyKeyAndVersionForUpdate("finance-manager", 2)).thenReturn(candidatePolicy(
                "ACTIVE",
                "{\"resolverType\":\"SCRIPT\",\"resolverParameters\":{},\"resolutionPhase\":\"ACTIVATE\",\"emptyCandidatePolicy\":\"BLOCK\",\"selfApprovalPolicy\":\"BLOCK\"}"
        ));
        BpmPolicyCatalogService service = new BpmPolicyCatalogService(
                candidateDao,
                Mockito.mock(BpmApprovalPolicyVersionDao.class),
                Mockito.mock(BpmStartVisibilityPolicyVersionDao.class)
        );

        assertThatThrownBy(() -> service.freezeForPublication(
                new PolicyReference(PolicyType.CANDIDATE, "finance-manager", 2),
                "publish-invalid"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activateShouldRequireExpectedCatalogRevisionAndReturnImmutableVersion() {
        BpmCandidatePolicyVersionDao candidateDao = Mockito.mock(BpmCandidatePolicyVersionDao.class);
        BpmCandidatePolicyVersionEntity draft = candidatePolicy(
                "DRAFT",
                "{\"resolverType\":\"ROLE\",\"resolverParameters\":{\"roleId\":8},\"resolutionPhase\":\"ACTIVATE\",\"emptyCandidatePolicy\":\"BLOCK\",\"selfApprovalPolicy\":\"BLOCK\"}"
        );
        draft.setCatalogRevision(4L);
        when(candidateDao.selectByPolicyKeyAndVersionForUpdate("finance-manager", 2)).thenReturn(draft);
        when(candidateDao.update(ArgumentMatchers.isNull(), ArgumentMatchers.any())).thenReturn(1);
        BpmPolicyCatalogService service = new BpmPolicyCatalogService(
                candidateDao,
                Mockito.mock(BpmApprovalPolicyVersionDao.class),
                Mockito.mock(BpmStartVisibilityPolicyVersionDao.class)
        );

        PolicyCatalogVersion activated = service.activate(new PolicyLifecycleCommand(
                new PolicyReference(PolicyType.CANDIDATE, "finance-manager", 2),
                4L,
                99L
        ));

        assertThat(activated.lifecycleState()).isEqualTo("ACTIVE");
        assertThat(activated.catalogRevision()).isEqualTo(5L);
        assertThat(activated.digest()).hasSize(64);
    }

    @Test
    void ordinaryActivationShouldRejectHighRiskCandidatePolicy() {
        BpmCandidatePolicyVersionDao candidateDao = Mockito.mock(BpmCandidatePolicyVersionDao.class);
        BpmCandidatePolicyVersionEntity draft = candidatePolicy(
                "DRAFT",
                "{\"resolverType\":\"EMPLOYEE\",\"resolverParameters\":{\"employeeIds\":[20]},"
                        + "\"resolutionPhase\":\"ACTIVATE\",\"emptyCandidatePolicy\":\"AUTO_APPROVE\","
                        + "\"selfApprovalPolicy\":\"BLOCK\",\"riskLevel\":\"HIGH\"}"
        );
        draft.setCatalogRevision(0L);
        draft.setCreatedByEmployeeId(90L);
        when(candidateDao.selectByPolicyKeyAndVersionForUpdate("finance-manager", 2)).thenReturn(draft);
        BpmPolicyCatalogService service = new BpmPolicyCatalogService(
                candidateDao,
                Mockito.mock(BpmApprovalPolicyVersionDao.class),
                Mockito.mock(BpmStartVisibilityPolicyVersionDao.class)
        );

        assertThatThrownBy(() -> service.activate(new PolicyLifecycleCommand(
                new PolicyReference(PolicyType.CANDIDATE, "finance-manager", 2),
                0L,
                99L
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("高风险");

        verify(candidateDao, Mockito.never()).update(ArgumentMatchers.isNull(), ArgumentMatchers.any());
    }

    @Test
    void visualActivationShouldUseThePersistedServerRiskAssessment() {
        BpmStartVisibilityPolicyVersionDao visibilityDao = Mockito.mock(BpmStartVisibilityPolicyVersionDao.class);
        BpmStartVisibilityPolicyVersionEntity draft = new BpmStartVisibilityPolicyVersionEntity();
        draft.setStartVisibilityPolicyVersionId(41L);
        draft.setPolicyKey("admin-start");
        draft.setPolicyVersion(1);
        draft.setLifecycleState("DRAFT");
        draft.setSchemaVersion(2);
        draft.setPolicyJson("{\"schemaVersion\":2,\"startScope\":{\"employeeIds\":[1],\"type\":\"EMPLOYEE_IDS\"},\"visibilityScope\":{\"type\":\"ALL\"}}");
        draft.setCatalogRevision(1L);
        draft.setCalculatedRiskLevel("LOW");
        draft.setCreatedByEmployeeId(1L);
        when(visibilityDao.selectByPolicyKeyAndVersionForUpdate("admin-start", 1)).thenReturn(draft);
        when(visibilityDao.update(ArgumentMatchers.isNull(), ArgumentMatchers.any())).thenReturn(1);
        BpmPolicyCatalogService service = new BpmPolicyCatalogService(
                Mockito.mock(BpmCandidatePolicyVersionDao.class),
                Mockito.mock(BpmApprovalPolicyVersionDao.class),
                visibilityDao
        );

        PolicyCatalogVersion activated = service.activate(new PolicyLifecycleCommand(
                new PolicyReference(PolicyType.START_VISIBILITY, "admin-start", 1),
                1L,
                1L
        ));

        assertThat(activated.lifecycleState()).isEqualTo("ACTIVE");
        assertThat(activated.catalogRevision()).isEqualTo(2L);
    }

    @Test
    void highRiskActivationShouldRequireAConfirmerDifferentFromTheCreator() {
        BpmCandidatePolicyVersionDao candidateDao = Mockito.mock(BpmCandidatePolicyVersionDao.class);
        BpmCandidatePolicyVersionEntity draft = candidatePolicy(
                "DRAFT",
                "{\"resolverType\":\"EMPLOYEE\",\"resolverParameters\":{\"employeeIds\":[20]},"
                        + "\"resolutionPhase\":\"ACTIVATE\",\"emptyCandidatePolicy\":\"AUTO_APPROVE\","
                        + "\"selfApprovalPolicy\":\"BLOCK\",\"riskLevel\":\"HIGH\"}"
        );
        draft.setCatalogRevision(0L);
        draft.setCreatedByEmployeeId(99L);
        when(candidateDao.selectByPolicyKeyAndVersionForUpdate("finance-manager", 2)).thenReturn(draft);
        BpmPolicyCatalogService service = new BpmPolicyCatalogService(
                candidateDao,
                Mockito.mock(BpmApprovalPolicyVersionDao.class),
                Mockito.mock(BpmStartVisibilityPolicyVersionDao.class)
        );

        assertThatThrownBy(() -> service.activateHighRisk(new PolicyLifecycleCommand(
                new PolicyReference(PolicyType.CANDIDATE, "finance-manager", 2), 0L, 99L
        ), "确认自动终态风险"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("创建人");

        verify(candidateDao, Mockito.never()).update(ArgumentMatchers.isNull(), ArgumentMatchers.any());
    }

    @Test
    void highRiskActivationShouldPersistAnIndependentConfirmation() {
        BpmCandidatePolicyVersionDao candidateDao = Mockito.mock(BpmCandidatePolicyVersionDao.class);
        BpmCandidatePolicyVersionEntity draft = candidatePolicy(
                "DRAFT",
                "{\"resolverType\":\"EMPLOYEE\",\"resolverParameters\":{\"employeeIds\":[20]},"
                        + "\"resolutionPhase\":\"ACTIVATE\",\"emptyCandidatePolicy\":\"AUTO_REJECT\","
                        + "\"selfApprovalPolicy\":\"BLOCK\",\"riskLevel\":\"HIGH\"}"
        );
        draft.setCatalogRevision(2L);
        draft.setCreatedByEmployeeId(90L);
        when(candidateDao.selectByPolicyKeyAndVersionForUpdate("finance-manager", 2)).thenReturn(draft);
        when(candidateDao.update(ArgumentMatchers.isNull(), ArgumentMatchers.any())).thenReturn(1);
        BpmPolicyCatalogService service = new BpmPolicyCatalogService(
                candidateDao,
                Mockito.mock(BpmApprovalPolicyVersionDao.class),
                Mockito.mock(BpmStartVisibilityPolicyVersionDao.class)
        );

        PolicyCatalogVersion activated = service.activateHighRisk(new PolicyLifecycleCommand(
                new PolicyReference(PolicyType.CANDIDATE, "finance-manager", 2), 2L, 99L
        ), "已核对无候选自动拒绝影响");

        assertThat(activated.lifecycleState()).isEqualTo("ACTIVE");
        assertThat(activated.catalogRevision()).isEqualTo(3L);
    }

    @Test
    void retireShouldRejectWhenCatalogRevisionHasMoved() {
        BpmCandidatePolicyVersionDao candidateDao = Mockito.mock(BpmCandidatePolicyVersionDao.class);
        BpmCandidatePolicyVersionEntity active = candidatePolicy(
                "ACTIVE",
                "{\"resolverType\":\"ROLE\",\"resolverParameters\":{\"roleId\":8},\"resolutionPhase\":\"ACTIVATE\",\"emptyCandidatePolicy\":\"BLOCK\",\"selfApprovalPolicy\":\"BLOCK\"}"
        );
        active.setCatalogRevision(8L);
        when(candidateDao.selectByPolicyKeyAndVersionForUpdate("finance-manager", 2)).thenReturn(active);
        when(candidateDao.update(ArgumentMatchers.isNull(), ArgumentMatchers.any())).thenReturn(0);
        BpmPolicyCatalogService service = new BpmPolicyCatalogService(
                candidateDao,
                Mockito.mock(BpmApprovalPolicyVersionDao.class),
                Mockito.mock(BpmStartVisibilityPolicyVersionDao.class)
        );

        assertThatThrownBy(() -> service.retire(new PolicyLifecycleCommand(
                new PolicyReference(PolicyType.CANDIDATE, "finance-manager", 2),
                8L,
                99L
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("版本已变更");
    }

    @Test
    void createDraftShouldGenerateNewVersionAndNeverModifyExistingActiveVersion() {
        BpmCandidatePolicyVersionDao candidateDao = Mockito.mock(BpmCandidatePolicyVersionDao.class);
        when(candidateDao.selectMaxPolicyVersionByPolicyKey("finance-manager")).thenReturn(2);
        BpmPolicyCatalogService service = new BpmPolicyCatalogService(
                candidateDao,
                Mockito.mock(BpmApprovalPolicyVersionDao.class),
                Mockito.mock(BpmStartVisibilityPolicyVersionDao.class)
        );

        PolicyCatalogVersion draft = service.createDraft(new PolicyDraftCommand(
                PolicyType.CANDIDATE,
                "finance-manager",
                1,
                "{\"resolverType\":\"ROLE\",\"resolverParameters\":{\"roleId\":8},\"resolutionPhase\":\"ACTIVATE\",\"emptyCandidatePolicy\":\"BLOCK\",\"selfApprovalPolicy\":\"BLOCK\"}",
                99L
        ));

        assertThat(draft.reference().policyVersion()).isEqualTo(3);
        assertThat(draft.lifecycleState()).isEqualTo("DRAFT");
        assertThat(draft.catalogRevision()).isZero();
        verify(candidateDao).insert(ArgumentMatchers.<BpmCandidatePolicyVersionEntity>argThat(entity ->
                "DRAFT".equals(entity.getLifecycleState())
                        && Integer.valueOf(3).equals(entity.getPolicyVersion())
                        && Long.valueOf(99L).equals(entity.getCreatedByEmployeeId())
        ));
    }

    @Test
    void listShouldReturnCanonicalVersionsForTheRequestedPolicyType() {
        BpmCandidatePolicyVersionDao candidateDao = Mockito.mock(BpmCandidatePolicyVersionDao.class);
        BpmCandidatePolicyVersionEntity active = candidatePolicy(
                "ACTIVE",
                "{\"resolverType\":\"ROLE\",\"resolverParameters\":{\"roleId\":8},\"resolutionPhase\":\"ACTIVATE\",\"emptyCandidatePolicy\":\"BLOCK\",\"selfApprovalPolicy\":\"BLOCK\"}"
        );
        active.setSchemaVersion(1);
        active.setCatalogRevision(6L);
        when(candidateDao.selectList(ArgumentMatchers.any())).thenReturn(List.of(active));
        BpmPolicyCatalogService service = new BpmPolicyCatalogService(
                candidateDao,
                Mockito.mock(BpmApprovalPolicyVersionDao.class),
                Mockito.mock(BpmStartVisibilityPolicyVersionDao.class)
        );

        List<PolicyCatalogVersion> versions = service.list(PolicyType.CANDIDATE, "finance-manager", "ACTIVE");

        assertThat(versions).singleElement().satisfies(version -> {
            assertThat(version.reference()).isEqualTo(new PolicyReference(PolicyType.CANDIDATE, "finance-manager", 2));
            assertThat(version.lifecycleState()).isEqualTo("ACTIVE");
            assertThat(version.catalogRevision()).isEqualTo(6L);
            assertThat(version.canonicalPayload()).contains("resolverType");
        });
    }

    @Test
    void listShouldIsolateInvalidLegacyVersionsWithoutExposingThemAsBindablePolicies() {
        BpmCandidatePolicyVersionDao candidateDao = Mockito.mock(BpmCandidatePolicyVersionDao.class);
        BpmCandidatePolicyVersionEntity legacy = candidatePolicy(
                "ACTIVE",
                "{\"resolverType\":\"EMPLOYEE\",\"employeeId\":1,\"purpose\":\"M1 local acceptance\"}"
        );
        legacy.setPolicyKey("m1_acceptance_policy");
        legacy.setPolicyVersion(1);
        legacy.setSchemaVersion(1);
        BpmCandidatePolicyVersionEntity valid = candidatePolicy(
                "ACTIVE",
                "{\"resolverType\":\"EMPLOYEE\",\"resolverParameters\":{\"employeeIds\":[1]},\"resolutionPhase\":\"ACTIVATE\",\"emptyCandidatePolicy\":\"BLOCK\",\"selfApprovalPolicy\":\"BLOCK\"}"
        );
        valid.setPolicyKey("employee-one");
        valid.setPolicyVersion(1);
        valid.setSchemaVersion(1);
        when(candidateDao.selectList(ArgumentMatchers.any())).thenReturn(List.of(legacy, valid));
        BpmPolicyCatalogService service = new BpmPolicyCatalogService(
                candidateDao,
                Mockito.mock(BpmApprovalPolicyVersionDao.class),
                Mockito.mock(BpmStartVisibilityPolicyVersionDao.class)
        );

        List<PolicyCatalogVersion> versions = service.list(PolicyType.CANDIDATE, null, null);

        assertThat(versions).singleElement().satisfies(version ->
                assertThat(version.reference()).isEqualTo(new PolicyReference(PolicyType.CANDIDATE, "employee-one", 1))
        );
    }

    @Test
    void validateShouldReturnCanonicalPayloadWithoutCreatingAPolicyVersion() {
        BpmCandidatePolicyVersionDao candidateDao = Mockito.mock(BpmCandidatePolicyVersionDao.class);
        BpmPolicyCatalogService service = new BpmPolicyCatalogService(
                candidateDao,
                Mockito.mock(BpmApprovalPolicyVersionDao.class),
                Mockito.mock(BpmStartVisibilityPolicyVersionDao.class)
        );

        PolicyValidationResult result = service.validate(
                PolicyType.CANDIDATE,
                1,
                "{\"resolverType\":\"ROLE\",\"resolverParameters\":{\"roleId\":8},\"resolutionPhase\":\"ACTIVATE\",\"emptyCandidatePolicy\":\"BLOCK\",\"selfApprovalPolicy\":\"BLOCK\"}"
        );

        assertThat(result.canonicalPayload()).contains("resolverType");
        assertThat(result.digest()).hasSize(64);
        verify(candidateDao, Mockito.never()).insert(ArgumentMatchers.<BpmCandidatePolicyVersionEntity>any());
    }

    private BpmCandidatePolicyVersionEntity candidatePolicy(String lifecycleState, String policyJson) {
        BpmCandidatePolicyVersionEntity entity = new BpmCandidatePolicyVersionEntity();
        entity.setCandidatePolicyVersionId(31L);
        entity.setPolicyKey("finance-manager");
        entity.setPolicyVersion(2);
        entity.setLifecycleState(lifecycleState);
        entity.setPolicyJson(policyJson);
        return entity;
    }
}
