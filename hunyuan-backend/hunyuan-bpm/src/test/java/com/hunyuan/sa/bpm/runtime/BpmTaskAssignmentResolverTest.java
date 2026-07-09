package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskAssignmentContext;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskAssignmentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class BpmTaskAssignmentResolverTest {

    private BpmTaskAssignmentResolver resolver;

    private BpmOrgIdentityGateway identityGateway;

    @BeforeEach
    void setUp() {
        resolver = new BpmTaskAssignmentResolver();
        identityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        setField(resolver, "bpmOrgIdentityGateway", identityGateway);
    }

    @Test
    void resolveShouldUseStartEmployeeDepartmentManagerWhenNodeUsesDepartmentManager() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);
        when(identityGateway.resolveDepartmentManagerEmployeeId(7L)).thenReturn(200L);

        Map<String, Object> variables = resolver.resolve(
                List.of(buildNode(
                        "task_manager",
                        "{\"nodeKey\":\"task_manager\",\"name\":\"部门主管审批\",\"type\":\"userTask\",\"candidateResolverType\":\"DEPARTMENT_MANAGER\"}"
                )),
                startEmployee
        );

        assertThat(variables).containsEntry("assignee_task_manager", "200");
    }

    @Test
    void resolveShouldUseExplicitEmployeeIdWhenNodeUsesEmployeeStrategy() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        Map<String, Object> variables = resolver.resolve(
                List.of(buildNode(
                        "task_finance",
                        "{\"nodeKey\":\"task_finance\",\"name\":\"财务审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":301}"
                )),
                startEmployee
        );

        assertThat(variables).containsEntry("assignee_task_finance", "301");
    }

    @Test
    void resolveShouldPickStableRoleMemberWhenNodeUsesRoleStrategy() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);
        when(identityGateway.listEmployeeIdsByRoleId(9L)).thenReturn(List.of(42L, 15L, 18L));

        Map<String, Object> variables = resolver.resolve(
                List.of(buildNode(
                        "task_role",
                        "{\"nodeKey\":\"task_role\",\"name\":\"角色审批\",\"type\":\"userTask\",\"candidateResolverType\":\"ROLE\",\"roleId\":9}"
                )),
                startEmployee
        );

        assertThat(variables).containsEntry("assignee_task_role", "15");
    }

    @Test
    void resolveShouldUseStartEmployeeWhenNodeUsesStartEmployeeStrategy() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        Map<String, Object> variables = resolver.resolve(
                List.of(buildNode(
                        "task_self",
                        "{\"nodeKey\":\"task_self\",\"name\":\"发起人自审\",\"type\":\"userTask\",\"candidateResolverType\":\"START_EMPLOYEE\"}"
                )),
                startEmployee
        );

        assertThat(variables).containsEntry("assignee_task_self", "100");
    }

    @Test
    void resolveShouldRejectStartEmployeeStrategyWhenStartEmployeeMissing() {
        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_self",
                        "{\"nodeKey\":\"task_self\",\"name\":\"发起人自审\",\"type\":\"userTask\",\"candidateResolverType\":\"START_EMPLOYEE\"}"
                )),
                (BpmEmployeeSnapshot) null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【发起人自审】未找到发起人");
    }

    @Test
    void resolveShouldUseStartEmployeeDepartmentManagerWhenNodeUsesStartDepartmentManagerStrategy() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);
        when(identityGateway.resolveDepartmentManagerEmployeeId(7L)).thenReturn(200L);

        Map<String, Object> variables = resolver.resolve(
                List.of(buildNode(
                        "task_start_manager",
                        "{\"nodeKey\":\"task_start_manager\",\"name\":\"发起人部门主管审批\",\"type\":\"userTask\",\"candidateResolverType\":\"START_DEPARTMENT_MANAGER\"}"
                )),
                startEmployee
        );

        assertThat(variables).containsEntry("assignee_task_start_manager", "200");
    }

    @Test
    void resolveShouldRejectStartDepartmentManagerStrategyWhenStartDepartmentMissing() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", null, null, null, null);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_start_manager",
                        "{\"nodeKey\":\"task_start_manager\",\"name\":\"发起人部门主管审批\",\"type\":\"userTask\",\"candidateResolverType\":\"START_DEPARTMENT_MANAGER\"}"
                )),
                startEmployee
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【发起人部门主管审批】未找到发起人部门");
    }

    @Test
    void resolveShouldRejectStartDepartmentManagerStrategyWhenManagerMissing() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);
        when(identityGateway.resolveDepartmentManagerEmployeeId(7L)).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_start_manager",
                        "{\"nodeKey\":\"task_start_manager\",\"name\":\"发起人部门主管审批\",\"type\":\"userTask\",\"candidateResolverType\":\"START_DEPARTMENT_MANAGER\"}"
                )),
                startEmployee
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【发起人部门主管审批】未找到发起人部门主管");
    }

    @Test
    void resolveShouldUseEmployeeSelectedFromStartFormData() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        Map<String, Object> variables = resolver.resolve(
                List.of(buildNode(
                        "task_selected",
                        "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
                )),
                new BpmTaskAssignmentContext(startEmployee, "{\"approverEmployeeId\":301}")
        );

        assertThat(variables).containsEntry("assignee_task_selected", "301");
    }

    @Test
    void resolveShouldUseStringEmployeeSelectedFromStartFormData() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        Map<String, Object> variables = resolver.resolve(
                List.of(buildNode(
                        "task_selected",
                        "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
                )),
                new BpmTaskAssignmentContext(startEmployee, "{\"approverEmployeeId\":\"302\"}")
        );

        assertThat(variables).containsEntry("assignee_task_selected", "302");
    }

    @Test
    void resolveShouldRejectEmployeeSelectAtStartWhenFieldKeyMissing() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_selected",
                        "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\"}"
                )),
                new BpmTaskAssignmentContext(startEmployee, "{\"approverEmployeeId\":301}")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【发起时选择审批】未配置发起时自选审批人字段");
    }

    @Test
    void resolveShouldRejectEmployeeSelectAtStartWhenFormFieldMissing() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_selected",
                        "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
                )),
                new BpmTaskAssignmentContext(startEmployee, "{\"amount\":100}")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【发起时选择审批】未找到发起时自选审批人");
    }

    @Test
    void resolveShouldRejectEmployeeSelectAtStartWhenValueIsArray() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_selected",
                        "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
                )),
                new BpmTaskAssignmentContext(startEmployee, "{\"approverEmployeeId\":[301,302]}")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【发起时选择审批】发起时自选审批人无效");
    }

    @Test
    void resolveShouldRejectEmployeeSelectAtStartWhenValueIsCommaString() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_selected",
                        "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
                )),
                new BpmTaskAssignmentContext(startEmployee, "{\"approverEmployeeId\":\"301,302\"}")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【发起时选择审批】发起时自选审批人无效");
    }

    private BpmDefinitionNodeEntity buildNode(String nodeKey, String authoredRuleSnapshotJson) {
        BpmDefinitionNodeEntity entity = new BpmDefinitionNodeEntity();
        entity.setNodeKey(nodeKey);
        entity.setNodeType("userTask");
        entity.setNodeNameSnapshot(nodeKey);
        entity.setAuthoredRuleSnapshotJson(authoredRuleSnapshotJson);
        return entity;
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
