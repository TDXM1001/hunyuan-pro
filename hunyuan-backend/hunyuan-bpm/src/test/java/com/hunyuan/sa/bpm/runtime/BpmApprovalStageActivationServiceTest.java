package com.hunyuan.sa.bpm.runtime;

import com.alibaba.fastjson.JSON;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ResolvedCandidateMember;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ResolvedCandidateSnapshot;
import com.hunyuan.sa.bpm.module.candidate.service.CandidateResolutionService;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionElementMappingDao;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionElementMappingEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageActivationService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmApprovalStageActivationServiceTest {

    @Test
    void activateShouldUsePublishedCompiledMappingInsteadOfDecodingTheActivityId() {
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        GraphDefinitionVersionDao versionDao = Mockito.mock(GraphDefinitionVersionDao.class);
        GraphDefinitionElementMappingDao mappingDao = Mockito.mock(GraphDefinitionElementMappingDao.class);
        BpmApprovalStageDao stageDao = Mockito.mock(BpmApprovalStageDao.class);
        CandidateResolutionService candidateResolutionService = Mockito.mock(CandidateResolutionService.class);
        BpmApprovalStageService stageService = Mockito.mock(BpmApprovalStageService.class);
        BpmTaskProjectionService taskProjectionService = Mockito.mock(BpmTaskProjectionService.class);
        BpmApprovalStageActivationService service = new BpmApprovalStageActivationService(
                instanceDao,
                versionDao,
                mappingDao,
                stageDao,
                candidateResolutionService,
                stageService,
                taskProjectionService
        );

        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(81L);
        instance.setDefinitionId(9L);
        instance.setStartEmployeeId(7L);
        instance.setStartEmployeeNameSnapshot("申请人");
        instance.setStartDepartmentIdSnapshot(3L);
        instance.setStartDepartmentNameSnapshot("财务部");
        when(instanceDao.selectByIdForUpdate(81L)).thenReturn(instance);

        GraphDefinitionVersionEntity version = new GraphDefinitionVersionEntity();
        version.setGraphDefinitionVersionId(41L);
        version.setDependencyVersionsJson(JSON.toJSONString(Map.of(
                "candidatePolicies", Map.of("finance-review:chief", policy("candidate", 31L,
                        "{\"resolverType\":\"EMPLOYEE\",\"resolverParameters\":{\"employeeIds\":[20]}}")),
                "approvalPolicies", Map.of("finance-review:chief", policy("approval", 51L,
                        "{\"completionMode\":\"SINGLE\",\"ratioPercent\":100,\"rejectionRule\":\"IMMEDIATE\",\"allowedActions\":[\"APPROVE\",\"REJECT\",\"RETURN\"]}"))
        )));
        when(versionDao.selectById(41L)).thenReturn(version);

        GraphDefinitionElementMappingEntity mapping = new GraphDefinitionElementMappingEntity();
        mapping.setGraphDefinitionVersionId(41L);
        mapping.setAuthoredElementId("finance-review:chief");
        mapping.setAuthoredElementKind("NODE");
        mapping.setCompiledElementId("graph_stage_finance_review_chief");
        mapping.setCompiledElementType("receiveTask");
        when(mappingDao.selectByGraphDefinitionVersionIdAndCompiledElementId(41L, "graph_stage_finance_review_chief"))
                .thenReturn(mapping);
        when(stageDao.selectByStageInvocationId("execution-92")).thenReturn(null);
        when(stageDao.selectNextGeneration(81L, "finance-review:chief")).thenReturn(0);
        when(candidateResolutionService.resolve(any(), any())).thenReturn(new ResolvedCandidateSnapshot(
                List.of(new ResolvedCandidateMember(20L, 20L, "审批人", 3L, "财务部")),
                List.of()
        ));
        BpmApprovalStageEntity stage = new BpmApprovalStageEntity();
        stage.setApprovalStageId(71L);
        when(stageService.open(any())).thenReturn(stage);

        service.activate(new BpmApprovalStageActivationService.ActivateApprovalStageCommand(
                81L,
                41L,
                "graph_stage_finance_review_chief",
                "process-91",
                "execution-92"
        ));

        ArgumentCaptor<BpmApprovalStageService.OpenApprovalStageCommand> command =
                ArgumentCaptor.forClass(BpmApprovalStageService.OpenApprovalStageCommand.class);
        verify(stageService).open(command.capture());
        assertThat(command.getValue().authoredNodeId()).isEqualTo("finance-review:chief");
        assertThat(command.getValue().definitionVersionId()).isEqualTo(41L);
        assertThat(command.getValue().stageInvocationId()).isEqualTo("execution-92");
        assertThat(command.getValue().engineProcessInstanceId()).isEqualTo("process-91");
        assertThat(command.getValue().engineExecutionId()).isEqualTo("execution-92");
        verify(taskProjectionService).projectActiveApprovalStageMembers(stage);
    }

    @Test
    void activateShouldSerializeDifferentExecutionsBeforeAllocatingTheirGenerations() {
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        GraphDefinitionVersionDao versionDao = Mockito.mock(GraphDefinitionVersionDao.class);
        GraphDefinitionElementMappingDao mappingDao = Mockito.mock(GraphDefinitionElementMappingDao.class);
        BpmApprovalStageDao stageDao = Mockito.mock(BpmApprovalStageDao.class);
        CandidateResolutionService candidateResolutionService = Mockito.mock(CandidateResolutionService.class);
        BpmApprovalStageService stageService = Mockito.mock(BpmApprovalStageService.class);
        BpmTaskProjectionService taskProjectionService = Mockito.mock(BpmTaskProjectionService.class);
        BpmApprovalStageActivationService service = new BpmApprovalStageActivationService(
                instanceDao, versionDao, mappingDao, stageDao, candidateResolutionService, stageService, taskProjectionService
        );

        BpmInstanceEntity instance = instance();
        when(instanceDao.selectByIdForUpdate(81L)).thenReturn(instance);
        when(versionDao.selectById(41L)).thenReturn(versionWithPolicies("IMMEDIATE"));
        when(mappingDao.selectByGraphDefinitionVersionIdAndCompiledElementId(41L, "graph_stage_finance_review_chief"))
                .thenReturn(mapping());
        when(stageDao.selectByStageInvocationId("execution-92")).thenReturn(null);
        when(stageDao.selectByStageInvocationId("execution-93")).thenReturn(null);
        when(stageDao.selectNextGeneration(81L, "finance-review:chief")).thenReturn(0, 1);
        when(candidateResolutionService.resolve(any(), any())).thenReturn(snapshot());
        BpmApprovalStageEntity first = new BpmApprovalStageEntity();
        first.setApprovalStageId(71L);
        BpmApprovalStageEntity second = new BpmApprovalStageEntity();
        second.setApprovalStageId(72L);
        when(stageService.open(any())).thenReturn(first, second);

        service.activate(command("execution-92"));
        service.activate(command("execution-93"));

        ArgumentCaptor<BpmApprovalStageService.OpenApprovalStageCommand> commands =
                ArgumentCaptor.forClass(BpmApprovalStageService.OpenApprovalStageCommand.class);
        verify(stageService, Mockito.times(2)).open(commands.capture());
        assertThat(commands.getAllValues())
                .extracting(BpmApprovalStageService.OpenApprovalStageCommand::generation)
                .containsExactly(0, 1);
        InOrder order = inOrder(instanceDao, stageDao);
        order.verify(instanceDao).selectByIdForUpdate(81L);
        order.verify(stageDao).selectByStageInvocationId("execution-92");
        order.verify(instanceDao).selectByIdForUpdate(81L);
        order.verify(stageDao).selectByStageInvocationId("execution-93");
    }

    @Test
    void activateShouldRejectFrozenApprovalPolicyWithoutRejectionRuleBeforeOpeningStage() {
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        GraphDefinitionVersionDao versionDao = Mockito.mock(GraphDefinitionVersionDao.class);
        GraphDefinitionElementMappingDao mappingDao = Mockito.mock(GraphDefinitionElementMappingDao.class);
        BpmApprovalStageDao stageDao = Mockito.mock(BpmApprovalStageDao.class);
        CandidateResolutionService candidateResolutionService = Mockito.mock(CandidateResolutionService.class);
        BpmApprovalStageService stageService = Mockito.mock(BpmApprovalStageService.class);
        BpmTaskProjectionService taskProjectionService = Mockito.mock(BpmTaskProjectionService.class);
        BpmApprovalStageActivationService service = new BpmApprovalStageActivationService(
                instanceDao, versionDao, mappingDao, stageDao, candidateResolutionService, stageService, taskProjectionService
        );
        when(instanceDao.selectByIdForUpdate(81L)).thenReturn(instance());
        when(versionDao.selectById(41L)).thenReturn(versionWithPolicies(null));
        when(mappingDao.selectByGraphDefinitionVersionIdAndCompiledElementId(41L, "graph_stage_finance_review_chief"))
                .thenReturn(mapping());
        when(stageDao.selectByStageInvocationId("execution-92")).thenReturn(null);

        assertThatThrownBy(() -> service.activate(command("execution-92")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rejectionRule");
    }

    private BpmInstanceEntity instance() {
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(81L);
        instance.setDefinitionId(9L);
        instance.setStartEmployeeId(7L);
        instance.setStartEmployeeNameSnapshot("申请人");
        instance.setStartDepartmentIdSnapshot(3L);
        instance.setStartDepartmentNameSnapshot("财务部");
        return instance;
    }

    private GraphDefinitionVersionEntity versionWithPolicies(String rejectionRule) {
        GraphDefinitionVersionEntity version = new GraphDefinitionVersionEntity();
        version.setGraphDefinitionVersionId(41L);
        String approvalPayload = rejectionRule == null
                ? "{\"completionMode\":\"SINGLE\",\"ratioPercent\":100,\"allowedActions\":[\"APPROVE\",\"REJECT\",\"RETURN\"]}"
                : "{\"completionMode\":\"SINGLE\",\"ratioPercent\":100,\"rejectionRule\":\""
                + rejectionRule + "\",\"allowedActions\":[\"APPROVE\",\"REJECT\",\"RETURN\"]}";
        version.setDependencyVersionsJson(JSON.toJSONString(Map.of(
                "candidatePolicies", Map.of("finance-review:chief", policy("candidate", 31L,
                        "{\"resolverType\":\"EMPLOYEE\",\"resolverParameters\":{\"employeeIds\":[20]}}")),
                "approvalPolicies", Map.of("finance-review:chief", policy("approval", 51L, approvalPayload))
        )));
        return version;
    }

    private GraphDefinitionElementMappingEntity mapping() {
        GraphDefinitionElementMappingEntity mapping = new GraphDefinitionElementMappingEntity();
        mapping.setGraphDefinitionVersionId(41L);
        mapping.setAuthoredElementId("finance-review:chief");
        mapping.setAuthoredElementKind("NODE");
        mapping.setCompiledElementId("graph_stage_finance_review_chief");
        mapping.setCompiledElementType("receiveTask");
        return mapping;
    }

    private ResolvedCandidateSnapshot snapshot() {
        return new ResolvedCandidateSnapshot(
                List.of(new ResolvedCandidateMember(20L, 20L, "审批人", 3L, "财务部")),
                List.of()
        );
    }

    private BpmApprovalStageActivationService.ActivateApprovalStageCommand command(String executionId) {
        return new BpmApprovalStageActivationService.ActivateApprovalStageCommand(
                81L, 41L, "graph_stage_finance_review_chief", "process-91", executionId
        );
    }

    private Map<String, Object> policy(String key, Long versionId, String canonicalPayload) {
        return Map.of(
                "policyKey", key,
                "policyVersion", 1,
                "policyVersionId", versionId,
                "schemaVersion", 1,
                "canonicalPayload", canonicalPayload,
                "digest", key + "-digest"
        );
    }
}
