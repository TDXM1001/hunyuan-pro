package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.serialnumber.service.SerialNumberService;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.ApprovalRuntimeBinding;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmApprovalRuntimeDataService;
import com.hunyuan.sa.bpm.module.candidate.domain.model.StartDecision;
import com.hunyuan.sa.bpm.module.candidate.service.StartVisibilityPolicyEvaluator;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceStartForm;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmInstanceServiceM3StartTest {

    @Test
    void graphStartShouldUseFrozenM3BindingInsteadOfClientBusinessData() {
        BpmInstanceService service = new BpmInstanceService();
        GraphDefinitionVersionDao versionDao = Mockito.mock(GraphDefinitionVersionDao.class);
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        BpmApprovalRuntimeDataService approvalDataService = Mockito.mock(BpmApprovalRuntimeDataService.class);
        FlowableProcessInstanceGateway flowable = Mockito.mock(FlowableProcessInstanceGateway.class);
        BpmTaskProjectionService taskProjection = Mockito.mock(BpmTaskProjectionService.class);
        BpmCurrentActorProvider actorProvider = Mockito.mock(BpmCurrentActorProvider.class);
        BpmOrgIdentityGateway identityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        StartVisibilityPolicyEvaluator visibilityEvaluator = Mockito.mock(StartVisibilityPolicyEvaluator.class);
        SerialNumberService serialNumberService = Mockito.mock(SerialNumberService.class);
        setField(service, "graphDefinitionVersionDao", versionDao);
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "bpmApprovalRuntimeDataService", approvalDataService);
        setField(service, "flowableProcessInstanceGateway", flowable);
        setField(service, "bpmTaskProjectionService", taskProjection);
        setField(service, "bpmCurrentActorProvider", actorProvider);
        setField(service, "bpmOrgIdentityGateway", identityGateway);
        setField(service, "startVisibilityPolicyEvaluator", visibilityEvaluator);
        setField(service, "serialNumberService", serialNumberService);

        GraphDefinitionVersionEntity graph = graphVersion();
        when(versionDao.selectById(41L)).thenReturn(graph);
        when(actorProvider.requireCurrentEmployeeId()).thenReturn(20L);
        when(identityGateway.requireEmployee(20L)).thenReturn(new BpmEmployeeSnapshot(
                20L, "申请人", 3L, "研发部", null, null
        ));
        when(visibilityEvaluator.evaluateStart(anyInt(), anyString(), any()))
                .thenReturn(new StartDecision(true, "ALL", "允许发起"));
        when(serialNumberService.generate(any())).thenReturn("BPM-0001");
        when(approvalDataService.prepareForStart(101L, graph)).thenReturn(new ApprovalRuntimeBinding(
                101L, 201L, 301L, 22L, "HUNYUAN", "GENERIC_APPLICATION", "REQ-2026-0001",
                "设备采购申请", "申请采购研发设备", "{\"approvedAmount\":12000.50}", 1L
        ));
        when(instanceDao.insert(any(BpmInstanceEntity.class))).thenAnswer(invocation -> {
            ((BpmInstanceEntity) invocation.getArgument(0)).setInstanceId(81L);
            return 1;
        });
        when(flowable.start(anyString(), any(), any(), anyString(), any(Map.class)))
                .thenReturn("engine-81");

        BpmInstanceStartForm form = new BpmInstanceStartForm();
        form.setGraphDefinitionVersionId(41L);
        form.setApprovalSubjectSnapshotId(101L);
        form.setTitle("客户端伪造标题");
        form.setBusinessKey("CLIENT-KEY");
        form.setFormDataJson("{\"financeApprover\":999}");
        ResponseDTO<Long> response = service.startInstance(form);

        ArgumentCaptor<BpmInstanceEntity> instance = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(instanceDao).insert(instance.capture());
        assertThat(response.getData()).isEqualTo(81L);
        assertThat(instance.getValue()).satisfies(value -> {
            assertThat(value.getApprovalSubjectSnapshotId()).isEqualTo(101L);
            assertThat(value.getRoutingFactSnapshotId()).isEqualTo(201L);
            assertThat(value.getProcessWorkingDataId()).isEqualTo(301L);
            assertThat(value.getTitle()).isEqualTo("设备采购申请");
            assertThat(value.getBusinessKey()).isEqualTo("REQ-2026-0001");
            assertThat(value.getCurrentFormDataSnapshotJson()).contains("approvedAmount");
        });
        verify(flowable).start(
                "graph:1:41", 81L, 20L, "{\"approvedAmount\":12000.50}", Map.of()
        );
    }

    private GraphDefinitionVersionEntity graphVersion() {
        GraphDefinitionVersionEntity graph = new GraphDefinitionVersionEntity();
        graph.setGraphDefinitionVersionId(41L);
        graph.setProcessKey("generic-approval");
        graph.setProcessNameSnapshot("通用审批");
        graph.setDefinitionVersion(1);
        graph.setLifecycleState("ACTIVE");
        graph.setEngineProcessDefinitionId("graph:1:41");
        graph.setCategoryIdSnapshot(3L);
        graph.setCategoryNameSnapshot("通用申请");
        graph.setDependencyVersionsJson("""
                {"businessContract":{"contractVersionId":22},"startVisibilityPolicy":{"policyVersionId":11,"schemaVersion":1,"canonicalPayload":"{\\"scopeType\\":\\"ALL\\"}","digest":"abc"}}
                """);
        return graph;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
