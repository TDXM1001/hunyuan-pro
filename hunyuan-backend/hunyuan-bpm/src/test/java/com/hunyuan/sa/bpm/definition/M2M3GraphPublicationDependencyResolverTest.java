package com.hunyuan.sa.bpm.definition;

import com.hunyuan.sa.bpm.engine.graph.GraphEdge;
import com.hunyuan.sa.bpm.engine.graph.GraphNode;
import com.hunyuan.sa.bpm.engine.graph.GraphNodeType;
import com.hunyuan.sa.bpm.engine.graph.GraphScope;
import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;
import com.hunyuan.sa.bpm.module.candidate.dao.BpmCandidatePolicyVersionDao;
import com.hunyuan.sa.bpm.module.candidate.dao.BpmApprovalPolicyVersionDao;
import com.hunyuan.sa.bpm.module.candidate.dao.BpmStartVisibilityPolicyVersionDao;
import com.hunyuan.sa.bpm.module.candidate.domain.entity.BpmApprovalPolicyVersionEntity;
import com.hunyuan.sa.bpm.module.candidate.domain.entity.BpmCandidatePolicyVersionEntity;
import com.hunyuan.sa.bpm.module.candidate.domain.entity.BpmStartVisibilityPolicyVersionEntity;
import com.hunyuan.sa.bpm.module.candidate.service.BpmPolicyCatalogService;
import com.hunyuan.sa.bpm.module.definition.service.GraphPublicationDependencyException;
import com.hunyuan.sa.bpm.module.definition.service.GraphPublicationDependencySnapshot;
import com.hunyuan.sa.bpm.module.definition.service.M2M3GraphPublicationDependencyResolver;
import com.hunyuan.sa.bpm.module.businesscontract.dao.BpmBusinessContractVersionDao;
import com.hunyuan.sa.bpm.module.businesscontract.domain.entity.BpmBusinessContractVersionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class M2M3GraphPublicationDependencyResolverTest {

    private BpmCandidatePolicyVersionDao candidatePolicyVersionDao;
    private BpmApprovalPolicyVersionDao approvalPolicyVersionDao;
    private BpmStartVisibilityPolicyVersionDao startVisibilityPolicyVersionDao;
    private BpmBusinessContractVersionDao businessContractVersionDao;
    private M2M3GraphPublicationDependencyResolver resolver;

    @BeforeEach
    void setUp() {
        candidatePolicyVersionDao = mock(BpmCandidatePolicyVersionDao.class);
        approvalPolicyVersionDao = mock(BpmApprovalPolicyVersionDao.class);
        startVisibilityPolicyVersionDao = mock(BpmStartVisibilityPolicyVersionDao.class);
        businessContractVersionDao = mock(BpmBusinessContractVersionDao.class);
        resolver = new M2M3GraphPublicationDependencyResolver(
                new BpmPolicyCatalogService(candidatePolicyVersionDao, approvalPolicyVersionDao, startVisibilityPolicyVersionDao),
                businessContractVersionDao
        );
    }

    @Test
    void shouldBlockPublishWhenReferencedCandidatePolicyVersionDoesNotExist() {
        when(businessContractVersionDao.selectOne(any())).thenReturn(businessContract());
        when(startVisibilityPolicyVersionDao.selectByPolicyKeyAndVersionForUpdate("employee-start", 1))
                .thenReturn(startVisibilityPolicy());

        assertThatThrownBy(() -> resolver.resolve(graph()))
                .isInstanceOf(GraphPublicationDependencyException.class)
                .hasMessageContaining("finance-review")
                .hasMessageContaining("候选策略版本不存在");
    }

    @Test
    void shouldBlockPublishWhenReferencedBusinessContractVersionDoesNotExist() {
        when(candidatePolicyVersionDao.selectByPolicyKeyAndVersionForUpdate("finance-review", 3))
                .thenReturn(candidatePolicy());

        assertThatThrownBy(() -> resolver.resolve(graph()))
                .isInstanceOf(GraphPublicationDependencyException.class)
                .hasMessageContaining("expense")
                .hasMessageContaining("业务契约版本不存在");
    }

    @Test
    void shouldFreezeResolvedM2AndM3VersionsFromCatalogs() {
        when(candidatePolicyVersionDao.selectByPolicyKeyAndVersionForUpdate("finance-review", 3))
                .thenReturn(candidatePolicy());
        when(approvalPolicyVersionDao.selectByPolicyKeyAndVersionForUpdate("finance-completion", 1))
                .thenReturn(approvalPolicy());
        when(startVisibilityPolicyVersionDao.selectByPolicyKeyAndVersionForUpdate("employee-start", 1))
                .thenReturn(startVisibilityPolicy());
        when(businessContractVersionDao.selectOne(any())).thenReturn(businessContract());

        GraphPublicationDependencySnapshot snapshot = resolver.resolve(graph());

        assertThat(snapshot.toSnapshotMap()).containsEntry("businessContract", Map.of(
                "contractKey", "expense",
                "contractVersion", 2,
                "contractVersionId", 22L,
                "canonicalPayload", "{}"
        ));
        Map<String, Object> candidatePolicies = (Map<String, Object>) snapshot.toSnapshotMap().get("candidatePolicies");
        Map<String, Object> candidatePolicy = (Map<String, Object>) candidatePolicies.get("review");
        assertThat(candidatePolicy)
                .containsEntry("policyKey", "finance-review")
                .containsEntry("canonicalPayload", "{\"emptyCandidatePolicy\":\"BLOCK\",\"resolutionPhase\":\"ACTIVATE\",\"resolverParameters\":{\"roleId\":8},\"resolverType\":\"ROLE\",\"selfApprovalPolicy\":\"BLOCK\"}")
                .containsKey("digest");
        assertThat(snapshot.toSnapshotMap()).containsKeys("approvalPolicies", "startVisibilityPolicy");
    }

    private BpmCandidatePolicyVersionEntity candidatePolicy() {
        BpmCandidatePolicyVersionEntity entity = new BpmCandidatePolicyVersionEntity();
        entity.setCandidatePolicyVersionId(11L);
        entity.setPolicyKey("finance-review");
        entity.setPolicyVersion(3);
        entity.setLifecycleState("ACTIVE");
        entity.setPolicyJson("{\"resolverType\":\"ROLE\",\"resolverParameters\":{\"roleId\":8},\"resolutionPhase\":\"ACTIVATE\",\"emptyCandidatePolicy\":\"BLOCK\",\"selfApprovalPolicy\":\"BLOCK\"}");
        return entity;
    }

    private BpmApprovalPolicyVersionEntity approvalPolicy() {
        BpmApprovalPolicyVersionEntity entity = new BpmApprovalPolicyVersionEntity();
        entity.setApprovalPolicyVersionId(12L);
        entity.setPolicyKey("finance-completion");
        entity.setPolicyVersion(1);
        entity.setLifecycleState("ACTIVE");
        entity.setPolicyJson("{\"completionMode\":\"ALL\",\"ratioPercent\":100,\"rejectionRule\":\"IMMEDIATE\",\"allowedActions\":[\"APPROVE\",\"REJECT\",\"RETURN\"]}");
        return entity;
    }

    private BpmStartVisibilityPolicyVersionEntity startVisibilityPolicy() {
        BpmStartVisibilityPolicyVersionEntity entity = new BpmStartVisibilityPolicyVersionEntity();
        entity.setStartVisibilityPolicyVersionId(13L);
        entity.setPolicyKey("employee-start");
        entity.setPolicyVersion(1);
        entity.setLifecycleState("ACTIVE");
        entity.setPolicyJson("{\"startScope\":{\"type\":\"EMPLOYEE_IDS\",\"employeeIds\":[10]},\"visibilityScope\":{\"type\":\"EMPLOYEE_IDS\",\"employeeIds\":[10]}}");
        return entity;
    }

    private BpmBusinessContractVersionEntity businessContract() {
        BpmBusinessContractVersionEntity entity = new BpmBusinessContractVersionEntity();
        entity.setBusinessContractVersionId(22L);
        entity.setContractKey("expense");
        entity.setContractVersion(2);
        entity.setLifecycleState("ACTIVE");
        return entity;
    }

    private HunyuanProcessDefinitionGraph graph() {
        return new HunyuanProcessDefinitionGraph(
                1,
                "scope_root",
                List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(
                        new GraphNode("start", "scope_root", GraphNodeType.START, "开始", Map.of(), Map.of()),
                        new GraphNode("review", "scope_root", GraphNodeType.APPROVAL, "财务审批", Map.of(
                                "candidatePolicy", Map.of("policyKey", "finance-review", "policyVersion", 3),
                                "approvalPolicy", Map.of("policyKey", "finance-completion", "policyVersion", 1)
                        ), Map.of()),
                        new GraphNode("end", "scope_root", GraphNodeType.END, "结束", Map.of(), Map.of())
                ),
                List.of(
                        new GraphEdge("edge_start_review", "scope_root", "start", "review", "default", Map.of()),
                        new GraphEdge("edge_review_end", "scope_root", "review", "end", "default", Map.of())
                ),
                Map.of(
                        "businessContract", Map.of("contractKey", "expense", "contractVersion", 2),
                        "startVisibilityPolicy", Map.of("policyKey", "employee-start", "policyVersion", 1)
                )
        );
    }
}
