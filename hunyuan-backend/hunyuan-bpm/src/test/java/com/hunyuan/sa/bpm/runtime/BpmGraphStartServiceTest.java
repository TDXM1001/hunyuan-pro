package com.hunyuan.sa.bpm.runtime;

import com.alibaba.fastjson.JSON;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.serialnumber.service.SerialNumberService;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.candidate.domain.model.StartDecision;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.ApprovalRuntimeBinding;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmApprovalRuntimeDataService;
import com.hunyuan.sa.bpm.module.candidate.service.StartVisibilityPolicyEvaluator;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceStartForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmRuntimeStartDraftVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmStartableDefinitionVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmGraphStartServiceTest {

    private BpmInstanceService service;
    private GraphDefinitionVersionDao graphDefinitionVersionDao;
    private BpmInstanceDao instanceDao;
    private FlowableProcessInstanceGateway processInstanceGateway;
    private StartVisibilityPolicyEvaluator startVisibilityPolicyEvaluator;
    private BpmApprovalRuntimeDataService approvalRuntimeDataService;

    @BeforeEach
    void setUp() {
        service = new BpmInstanceService();
        graphDefinitionVersionDao = Mockito.mock(GraphDefinitionVersionDao.class);
        instanceDao = Mockito.mock(BpmInstanceDao.class);
        processInstanceGateway = Mockito.mock(FlowableProcessInstanceGateway.class);
        startVisibilityPolicyEvaluator = Mockito.mock(StartVisibilityPolicyEvaluator.class);
        approvalRuntimeDataService = Mockito.mock(BpmApprovalRuntimeDataService.class);
        setField(service, "graphDefinitionVersionDao", graphDefinitionVersionDao);
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "flowableProcessInstanceGateway", processInstanceGateway);
        setField(service, "bpmCurrentActorProvider", currentActorProvider());
        setField(service, "bpmOrgIdentityGateway", identityGateway());
        setField(service, "serialNumberService", serialNumberService());
        setField(service, "bpmTaskProjectionService", Mockito.mock(BpmTaskProjectionService.class));
        setField(service, "startVisibilityPolicyEvaluator", startVisibilityPolicyEvaluator);
        setField(service, "bpmApprovalRuntimeDataService", approvalRuntimeDataService);
    }

    @Test
    void graphStartShouldUseExactActiveVersionAndPersistGraphOrigin() {
        GraphDefinitionVersionEntity graphVersion = graphVersion();
        when(graphDefinitionVersionDao.selectById(41L)).thenReturn(graphVersion);
        when(startVisibilityPolicyEvaluator.evaluateStart(eq(1), any(), any()))
                .thenReturn(new StartDecision(true, "EMPLOYEE_IDS", "命中发起范围"));
        when(approvalRuntimeDataService.prepareForStart(101L, graphVersion)).thenReturn(new ApprovalRuntimeBinding(
                101L, 201L, 301L, 22L, "HUNYUAN", "EXPENSE", "EXP-1001",
                "图形费用申请", "费用申请", "{}", 1L
        ));
        when(instanceDao.insert(any(BpmInstanceEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, BpmInstanceEntity.class).setInstanceId(9L);
            return 1;
        });
        when(processInstanceGateway.start(eq("graph-expense:1:100"), eq(9L), eq(100L), eq("{}"), eq(Map.of())))
                .thenReturn("process-1001");

        BpmInstanceStartForm form = new BpmInstanceStartForm();
        form.setGraphDefinitionVersionId(41L);
        form.setApprovalSubjectSnapshotId(101L);
        form.setFormDataJson("{}");

        ResponseDTO<Long> response = service.startInstance(form);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(9L);
        ArgumentCaptor<BpmInstanceEntity> instanceCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(instanceDao).insert(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getDefinitionId()).isNull();
        assertThat(instanceCaptor.getValue().getGraphDefinitionVersionId()).isEqualTo(41L);
        assertThat(instanceCaptor.getValue().getDefinitionSource()).isEqualTo("GRAPH");
        verify(graphDefinitionVersionDao, never()).selectLatestByDraftId(any());
    }

    @Test
    void graphStartDraftShouldReadOnlyTheRequestedActiveVersion() {
        when(graphDefinitionVersionDao.selectById(41L)).thenReturn(graphVersion());
        when(startVisibilityPolicyEvaluator.evaluateStart(eq(1), any(), any()))
                .thenReturn(new StartDecision(true, "EMPLOYEE_IDS", "命中发起范围"));

        ResponseDTO<BpmRuntimeStartDraftVO> response = service.getGraphStartDraft(41L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getGraphDefinitionVersionId()).isEqualTo(41L);
        assertThat(response.getData().getDefinitionSource()).isEqualTo("GRAPH");
        assertThat(response.getData().getDefinitionName()).isEqualTo("图形费用申请");
        verify(graphDefinitionVersionDao, never()).selectLatestByDraftId(any());
    }

    @Test
    void startableDefinitionsShouldIncludeOnlyGraphVersionsAllowedByFrozenStartPolicy() {
        GraphDefinitionVersionEntity allowed = graphVersion();
        GraphDefinitionVersionEntity denied = graphVersion();
        denied.setGraphDefinitionVersionId(42L);
        denied.setProcessKey("restricted-graph");
        denied.setProcessNameSnapshot("受限图形流程");
        when(graphDefinitionVersionDao.selectActiveStartableList()).thenReturn(List.of(allowed, denied));
        when(startVisibilityPolicyEvaluator.evaluateStart(eq(1), any(), any()))
                .thenReturn(
                        new StartDecision(true, "EMPLOYEE_IDS", "命中发起范围"),
                        new StartDecision(false, "EMPLOYEE_IDS", "不在发起范围")
                );

        ResponseDTO<List<BpmStartableDefinitionVO>> response = service.queryStartableDefinitions();

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).singleElement().satisfies(definition -> {
            assertThat(definition.getDefinitionSource()).isEqualTo("GRAPH");
            assertThat(definition.getGraphDefinitionVersionId()).isEqualTo(41L);
            assertThat(definition.getDefinitionId()).isNull();
            assertThat(definition.getDefinitionName()).isEqualTo("图形费用申请");
        });
    }

    private GraphDefinitionVersionEntity graphVersion() {
        GraphDefinitionVersionEntity entity = new GraphDefinitionVersionEntity();
        entity.setGraphDefinitionVersionId(41L);
        entity.setProcessKey("expense-graph");
        entity.setProcessNameSnapshot("图形费用申请");
        entity.setCategoryIdSnapshot(7L);
        entity.setCategoryNameSnapshot("财务流程");
        entity.setDefinitionVersion(3);
        entity.setLifecycleState("ACTIVE");
        entity.setEngineProcessDefinitionId("graph-expense:1:100");
        entity.setDependencyVersionsJson(JSON.toJSONString(Map.of(
                "startVisibilityPolicy", Map.of(
                        "policyKey", "employee-start",
                        "policyVersion", 1,
                        "schemaVersion", 1,
                        "policyVersionId", 13L,
                        "canonicalPayload", "{\"startScope\":{\"type\":\"EMPLOYEE_IDS\",\"employeeIds\":[100]},\"visibilityScope\":{\"type\":\"EMPLOYEE_IDS\",\"employeeIds\":[100]}}",
                        "digest", "d".repeat(64)
                )
        )));
        return entity;
    }

    private BpmCurrentActorProvider currentActorProvider() {
        BpmCurrentActorProvider provider = Mockito.mock(BpmCurrentActorProvider.class);
        when(provider.requireCurrentEmployeeId()).thenReturn(100L);
        return provider;
    }

    private BpmOrgIdentityGateway identityGateway() {
        BpmOrgIdentityGateway gateway = Mockito.mock(BpmOrgIdentityGateway.class);
        when(gateway.requireEmployee(100L)).thenReturn(new BpmEmployeeSnapshot(100L, "张三", 7L, "财务部", null, null));
        return gateway;
    }

    private SerialNumberService serialNumberService() {
        SerialNumberService serialNumberService = Mockito.mock(SerialNumberService.class);
        when(serialNumberService.generate(any())).thenReturn("SN-2026-0100");
        return serialNumberService;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
