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
import com.hunyuan.sa.bpm.module.runtime.dao.BpmFormDataChangeDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmFormDataChangeEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceStartForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmStartableDefinitionVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskAssignmentResolver;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskProjectionService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmRuntimeFormDataValidator;
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

    private BpmFormDataChangeDao formDataChangeDao;

    @BeforeEach
    void setUp() {
        service = new BpmInstanceService();
        definitionDao = Mockito.mock(BpmDefinitionDao.class);
        definitionNodeDao = Mockito.mock(BpmDefinitionNodeDao.class);
        instanceDao = Mockito.mock(BpmInstanceDao.class);
        processInstanceGateway = Mockito.mock(FlowableProcessInstanceGateway.class);
        formDataChangeDao = Mockito.mock(BpmFormDataChangeDao.class);

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
        setField(service, "bpmRuntimeFormDataValidator", new BpmRuntimeFormDataValidator());
        setField(service, "bpmFormDataChangeDao", formDataChangeDao);
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
        definitionEntity.setFormSchemaSnapshotJson(
                "{\"fields\":[{\"field\":\"amount\",\"type\":\"number\"}]}"
        );

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

        ArgumentCaptor<BpmInstanceEntity> instanceCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(instanceDao).insert(instanceCaptor.capture());
        assertThat(instanceCaptor.getValue().getFormDataVersion()).isEqualTo(1L);
        ArgumentCaptor<BpmFormDataChangeEntity> changeCaptor = ArgumentCaptor.forClass(BpmFormDataChangeEntity.class);
        verify(formDataChangeDao).insert(changeCaptor.capture());
        assertThat(changeCaptor.getValue().getChangeSource()).isEqualTo("INSTANCE_STARTED");
        assertThat(changeCaptor.getValue().getBeforeVersion()).isZero();
        assertThat(changeCaptor.getValue().getAfterVersion()).isEqualTo(1L);

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(processInstanceGateway).start(
                Mockito.eq("leave:1:1000"),
                Mockito.eq(100L),
                Mockito.eq("{\"amount\":100}"),
                variablesCaptor.capture()
        );
        assertThat(variablesCaptor.getValue()).containsEntry("assignee_task_self", "100");
    }

    @Test
    void startInstanceShouldPassEmployeeSelectedFromStartFormDataToFlowable() {
        BpmDefinitionEntity definitionEntity = new BpmDefinitionEntity();
        definitionEntity.setDefinitionId(1L);
        definitionEntity.setEngineProcessDefinitionId("expense:1:1000");
        definitionEntity.setDefinitionKey("expense");
        definitionEntity.setDefinitionVersion(1);
        definitionEntity.setCategoryIdSnapshot(7L);
        definitionEntity.setCategoryNameSnapshot("费用流程");
        definitionEntity.setInstanceNoRuleIdSnapshot(1);
        definitionEntity.setLifecycleState(1);
        definitionEntity.setStartState(1);

        BpmDefinitionNodeEntity nodeEntity = new BpmDefinitionNodeEntity();
        nodeEntity.setNodeKey("task_selected");
        nodeEntity.setNodeType("userTask");
        nodeEntity.setNodeNameSnapshot("发起时选择审批");
        nodeEntity.setAuthoredRuleSnapshotJson(
                "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
        );

        when(definitionDao.selectById(1L)).thenReturn(definitionEntity);
        when(definitionNodeDao.selectList(any())).thenReturn(List.of(nodeEntity));
        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(identityGateway().requireEmployee(100L)).thenReturn(new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null));
        when(identityGateway().requireEmployee(301L)).thenReturn(new BpmEmployeeSnapshot(301L, "审批人A", 8L, "财务部", null, null));
        when(serialNumberService().generate(any())).thenReturn("SN-2026-0004");
        when(processInstanceGateway.start("expense:1:1000", 100L, "{\"amount\":100,\"approverEmployeeId\":301}", Map.of("assignee_task_selected", "301")))
                .thenReturn("process-1003");
        when(instanceDao.insert(any(BpmInstanceEntity.class))).thenAnswer(invocation -> {
            BpmInstanceEntity entity = invocation.getArgument(0);
            entity.setInstanceId(11L);
            return 1;
        });

        BpmInstanceStartForm form = new BpmInstanceStartForm();
        form.setDefinitionId(1L);
        form.setFormDataJson("{\"amount\":100,\"approverEmployeeId\":301}");
        form.setTitle("费用申请");

        ResponseDTO<Long> response = service.startInstance(form);

        assertThat(response.getOk()).isTrue();

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(processInstanceGateway).start(
                Mockito.eq("expense:1:1000"),
                Mockito.eq(100L),
                Mockito.eq("{\"amount\":100,\"approverEmployeeId\":301}"),
                variablesCaptor.capture()
        );
        assertThat(variablesCaptor.getValue()).containsEntry("assignee_task_selected", "301");
    }

    @Test
    void startInstanceShouldRejectMissingSelectedEmployeeBeforeFlowableStart() {
        BpmDefinitionEntity definitionEntity = new BpmDefinitionEntity();
        definitionEntity.setDefinitionId(1L);
        definitionEntity.setEngineProcessDefinitionId("expense:1:1000");
        definitionEntity.setDefinitionKey("expense");
        definitionEntity.setDefinitionVersion(1);
        definitionEntity.setCategoryIdSnapshot(7L);
        definitionEntity.setCategoryNameSnapshot("费用流程");
        definitionEntity.setInstanceNoRuleIdSnapshot(1);
        definitionEntity.setLifecycleState(1);
        definitionEntity.setStartState(1);

        BpmDefinitionNodeEntity nodeEntity = new BpmDefinitionNodeEntity();
        nodeEntity.setNodeKey("task_selected");
        nodeEntity.setNodeType("userTask");
        nodeEntity.setNodeNameSnapshot("发起时选择审批");
        nodeEntity.setAuthoredRuleSnapshotJson(
                "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
        );

        when(definitionDao.selectById(1L)).thenReturn(definitionEntity);
        when(definitionNodeDao.selectList(any())).thenReturn(List.of(nodeEntity));
        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(identityGateway().requireEmployee(100L)).thenReturn(new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null));
        when(identityGateway().requireEmployee(301L)).thenThrow(new IllegalArgumentException("员工不存在"));
        when(serialNumberService().generate(any())).thenReturn("SN-2026-0005");

        BpmInstanceStartForm form = new BpmInstanceStartForm();
        form.setDefinitionId(1L);
        form.setFormDataJson("{\"amount\":100,\"approverEmployeeId\":301}");
        form.setTitle("费用申请");

        ResponseDTO<Long> response = service.startInstance(form);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("员工不存在");
        verify(processInstanceGateway, never()).start(any(), any(), any(), any());
        verify(instanceDao, never()).insert(any(BpmInstanceEntity.class));
    }

    @Test
    void startInstanceShouldPassSequentialApprovalAssignmentsToFlowable() {
        BpmDefinitionEntity definitionEntity = new BpmDefinitionEntity();
        definitionEntity.setDefinitionId(1L);
        definitionEntity.setEngineProcessDefinitionId("expense:1:1000");
        definitionEntity.setDefinitionKey("expense");
        definitionEntity.setDefinitionVersion(1);
        definitionEntity.setCategoryIdSnapshot(7L);
        definitionEntity.setCategoryNameSnapshot("费用流程");
        definitionEntity.setInstanceNoRuleIdSnapshot(1);
        definitionEntity.setLifecycleState(1);
        definitionEntity.setStartState(1);

        BpmDefinitionNodeEntity firstNode = new BpmDefinitionNodeEntity();
        firstNode.setNodeKey("task_finance_1");
        firstNode.setNodeType("userTask");
        firstNode.setNodeNameSnapshot("财务复核（1/2）");
        firstNode.setAuthoredRuleSnapshotJson(
                "{\"nodeKey\":\"task_finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[301,302]}"
        );
        firstNode.setCompiledNodeSnapshotJson(
                "{\"nodeKey\":\"task_finance_1\",\"name\":\"财务复核（1/2）\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":301,\"authoredNodeKey\":\"task_finance\",\"authoredNodeName\":\"财务复核\",\"sequentialIndex\":1,\"sequentialTotal\":2}"
        );
        firstNode.setSortOrder(1);

        BpmDefinitionNodeEntity secondNode = new BpmDefinitionNodeEntity();
        secondNode.setNodeKey("task_finance_2");
        secondNode.setNodeType("userTask");
        secondNode.setNodeNameSnapshot("财务复核（2/2）");
        secondNode.setAuthoredRuleSnapshotJson(
                "{\"nodeKey\":\"task_finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[301,302]}"
        );
        secondNode.setCompiledNodeSnapshotJson(
                "{\"nodeKey\":\"task_finance_2\",\"name\":\"财务复核（2/2）\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":302,\"authoredNodeKey\":\"task_finance\",\"authoredNodeName\":\"财务复核\",\"sequentialIndex\":2,\"sequentialTotal\":2}"
        );
        secondNode.setSortOrder(2);

        when(definitionDao.selectById(1L)).thenReturn(definitionEntity);
        when(definitionNodeDao.selectList(any())).thenReturn(List.of(firstNode, secondNode));
        when(currentActorProvider().requireCurrentEmployeeId()).thenReturn(100L);
        when(identityGateway().requireEmployee(100L)).thenReturn(new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null));
        when(serialNumberService().generate(any())).thenReturn("SN-2026-0006");
        when(processInstanceGateway.start(
                "expense:1:1000",
                100L,
                "{\"amount\":100}",
                Map.of("assignee_task_finance_1", "301", "assignee_task_finance_2", "302")
        )).thenReturn("process-1004");
        when(instanceDao.insert(any(BpmInstanceEntity.class))).thenAnswer(invocation -> {
            BpmInstanceEntity entity = invocation.getArgument(0);
            entity.setInstanceId(12L);
            return 1;
        });

        BpmInstanceStartForm form = new BpmInstanceStartForm();
        form.setDefinitionId(1L);
        form.setFormDataJson("{\"amount\":100}");
        form.setTitle("费用申请");

        ResponseDTO<Long> response = service.startInstance(form);

        assertThat(response.getOk()).isTrue();

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(processInstanceGateway).start(
                Mockito.eq("expense:1:1000"),
                Mockito.eq(100L),
                Mockito.eq("{\"amount\":100}"),
                variablesCaptor.capture()
        );
        assertThat(variablesCaptor.getValue())
                .containsEntry("assignee_task_finance_1", "301")
                .containsEntry("assignee_task_finance_2", "302");
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
