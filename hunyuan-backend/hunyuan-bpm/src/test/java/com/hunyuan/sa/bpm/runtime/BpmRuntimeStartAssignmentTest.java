package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.serialnumber.service.SerialNumberService;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionDao;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceStartForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmStartableDefinitionVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskAssignmentResolver;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmRuntimeStartAssignmentTest {

    private BpmInstanceService service;

    private BpmDefinitionDao definitionDao;

    private BpmDefinitionNodeDao definitionNodeDao;

    private BpmInstanceDao instanceDao;

    private FlowableProcessInstanceGateway processInstanceGateway;

    @BeforeEach
    void setUp() {
        service = new BpmInstanceService();
        definitionDao = Mockito.mock(BpmDefinitionDao.class);
        definitionNodeDao = Mockito.mock(BpmDefinitionNodeDao.class);
        instanceDao = Mockito.mock(BpmInstanceDao.class);
        processInstanceGateway = Mockito.mock(FlowableProcessInstanceGateway.class);

        setField(service, "bpmDefinitionDao", definitionDao);
        setField(service, "bpmDefinitionNodeDao", definitionNodeDao);
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "flowableProcessInstanceGateway", processInstanceGateway);
        setField(service, "bpmCurrentActorProvider", Mockito.mock(BpmCurrentActorProvider.class));
        setField(service, "bpmOrgIdentityGateway", Mockito.mock(BpmOrgIdentityGateway.class));
        setField(service, "serialNumberService", Mockito.mock(SerialNumberService.class));
        BpmTaskAssignmentResolver assignmentResolver = new BpmTaskAssignmentResolver();
        setField(assignmentResolver, "bpmOrgIdentityGateway", identityGateway());
        setField(service, "bpmTaskAssignmentResolver", assignmentResolver);
        setField(service, "bpmTaskProjectionService", Mockito.mock(BpmTaskProjectionService.class));
    }

    @Test
    void queryStartableDefinitionsShouldHideDefinitionOutsideEmployeeStartScope() {
        BpmStartableDefinitionVO definition = new BpmStartableDefinitionVO();
        definition.setDefinitionId(100L);
        definition.setDefinitionName("费用申请");
        definition.setStartScopeJson("{\"type\":\"EMPLOYEE\",\"employeeIds\":[200]}");

        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(definitionDao.queryStartableList(100L)).thenReturn(List.of(definition));

        ResponseDTO<List<BpmStartableDefinitionVO>> response = service.queryStartableDefinitions();

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEmpty();
    }

    @Test
    void startInstanceShouldRejectDefinitionOutsideEmployeeStartScope() {
        BpmDefinitionEntity definitionEntity = new BpmDefinitionEntity();
        definitionEntity.setDefinitionId(1L);
        definitionEntity.setLifecycleState(1);
        definitionEntity.setStartState(1);
        definitionEntity.setStartScopeJson("{\"type\":\"EMPLOYEE\",\"employeeIds\":[200]}");

        when(definitionDao.selectById(1L)).thenReturn(definitionEntity);
        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);

        BpmInstanceStartForm form = new BpmInstanceStartForm();
        form.setDefinitionId(1L);
        form.setFormDataJson("{\"amount\":100}");

        ResponseDTO<Long> response = service.startInstance(form);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("可发起范围");
        verify(processInstanceGateway, never()).start(any(), any(), any(), any());
    }

    @Test
    void startInstanceShouldPassResolvedNodeAssignmentsToFlowable() {
        BpmDefinitionEntity definitionEntity = new BpmDefinitionEntity();
        definitionEntity.setDefinitionId(1L);
        definitionEntity.setEngineProcessDefinitionId("leave:1:1000");
        definitionEntity.setDefinitionKey("leave");
        definitionEntity.setDefinitionVersion(1);
        definitionEntity.setCategoryIdSnapshot(7L);
        definitionEntity.setCategoryNameSnapshot("人事流程");
        definitionEntity.setInstanceNoRuleIdSnapshot(1);
        definitionEntity.setLifecycleState(1);
        definitionEntity.setStartState(1);

        BpmDefinitionNodeEntity nodeEntity = new BpmDefinitionNodeEntity();
        nodeEntity.setNodeKey("task_manager");
        nodeEntity.setNodeType("userTask");
        nodeEntity.setNodeNameSnapshot("部门主管审批");
        nodeEntity.setAuthoredRuleSnapshotJson(
                "{\"nodeKey\":\"task_manager\",\"name\":\"部门主管审批\",\"type\":\"userTask\",\"candidateResolverType\":\"DEPARTMENT_MANAGER\"}"
        );

        when(definitionDao.selectById(1L)).thenReturn(definitionEntity);
        when(definitionNodeDao.selectList(any())).thenReturn(List.of(nodeEntity));
        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(identityGateway().requireEmployee(100L)).thenReturn(new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null));
        when(identityGateway().resolveDepartmentManagerEmployeeId(7L)).thenReturn(200L);
        when(serialNumberService().generate(any())).thenReturn("SN-2026-0002");
        when(processInstanceGateway.start("leave:1:1000", 100L, "{\"amount\":100}", Map.of("assignee_task_manager", "200")))
                .thenReturn("process-1001");
        when(instanceDao.insert(any(BpmInstanceEntity.class))).thenAnswer(invocation -> {
            BpmInstanceEntity entity = invocation.getArgument(0);
            entity.setInstanceId(9L);
            return 1;
        });

        BpmInstanceStartForm form = new BpmInstanceStartForm();
        form.setDefinitionId(1L);
        form.setFormDataJson("{\"amount\":100}");
        form.setTitle("请假申请");

        ResponseDTO<Long> response = service.startInstance(form);

        assertThat(response.getOk()).isTrue();

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(processInstanceGateway).start(
                Mockito.eq("leave:1:1000"),
                Mockito.eq(100L),
                Mockito.eq("{\"amount\":100}"),
                variablesCaptor.capture()
        );
        assertThat(variablesCaptor.getValue()).containsEntry("assignee_task_manager", "200");
    }

    @Test
    void startInstanceShouldPassStartEmployeeAssignmentToFlowable() {
        BpmDefinitionEntity definitionEntity = new BpmDefinitionEntity();
        definitionEntity.setDefinitionId(1L);
        definitionEntity.setEngineProcessDefinitionId("leave:1:1000");
        definitionEntity.setDefinitionKey("leave");
        definitionEntity.setDefinitionVersion(1);
        definitionEntity.setCategoryIdSnapshot(7L);
        definitionEntity.setCategoryNameSnapshot("人事流程");
        definitionEntity.setInstanceNoRuleIdSnapshot(1);
        definitionEntity.setLifecycleState(1);
        definitionEntity.setStartState(1);

        BpmDefinitionNodeEntity nodeEntity = new BpmDefinitionNodeEntity();
        nodeEntity.setNodeKey("task_self");
        nodeEntity.setNodeType("userTask");
        nodeEntity.setNodeNameSnapshot("发起人自审");
        nodeEntity.setAuthoredRuleSnapshotJson(
                "{\"nodeKey\":\"task_self\",\"name\":\"发起人自审\",\"type\":\"userTask\",\"candidateResolverType\":\"START_EMPLOYEE\"}"
        );

        when(definitionDao.selectById(1L)).thenReturn(definitionEntity);
        when(definitionNodeDao.selectList(any())).thenReturn(List.of(nodeEntity));
        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(identityGateway().requireEmployee(100L)).thenReturn(new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null));
        when(serialNumberService().generate(any())).thenReturn("SN-2026-0003");
        when(processInstanceGateway.start("leave:1:1000", 100L, "{\"amount\":100}", Map.of("assignee_task_self", "100")))
                .thenReturn("process-1002");
        when(instanceDao.insert(any(BpmInstanceEntity.class))).thenAnswer(invocation -> {
            BpmInstanceEntity entity = invocation.getArgument(0);
            entity.setInstanceId(10L);
            return 1;
        });

        BpmInstanceStartForm form = new BpmInstanceStartForm();
        form.setDefinitionId(1L);
        form.setFormDataJson("{\"amount\":100}");
        form.setTitle("请假申请");

        ResponseDTO<Long> response = service.startInstance(form);

        assertThat(response.getOk()).isTrue();

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(processInstanceGateway).start(
                Mockito.eq("leave:1:1000"),
                Mockito.eq(100L),
                Mockito.eq("{\"amount\":100}"),
                variablesCaptor.capture()
        );
        assertThat(variablesCaptor.getValue()).containsEntry("assignee_task_self", "100");
    }

    @SuppressWarnings("unchecked")
    private BpmCurrentActorProvider currentActorProvider() {
        return (BpmCurrentActorProvider) getFieldValue("bpmCurrentActorProvider");
    }

    @SuppressWarnings("unchecked")
    private BpmOrgIdentityGateway identityGateway() {
        return (BpmOrgIdentityGateway) getFieldValue("bpmOrgIdentityGateway");
    }

    @SuppressWarnings("unchecked")
    private SerialNumberService serialNumberService() {
        return (SerialNumberService) getFieldValue("serialNumberService");
    }

    private Object getFieldValue(String fieldName) {
        try {
            Field field = service.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(service);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("读取测试字段失败: " + fieldName, ex);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("设置测试字段失败: " + fieldName, ex);
        }
    }
}
