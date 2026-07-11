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
import static org.mockito.Mockito.verify;
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
    void resolveShouldCreateAssigneeVariableForHandleTask() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);
        BpmDefinitionNodeEntity node = buildNode(
                "archive_handle",
                "{\"nodeKey\":\"archive_handle\",\"name\":\"归档办理\",\"type\":\"HANDLE_TASK\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":301}"
        );
        node.setNodeType("HANDLE_TASK");

        Map<String, Object> variables = resolver.resolve(List.of(node), startEmployee);

        assertThat(variables).containsEntry("assignee_archive_handle", "301");
    }

    @Test
    void resolveShouldPickStableRoleMemberWhenNodeUsesRoleStrategy() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);
        when(identityGateway.listEmployeeIdsByRoleId(9L)).thenReturn(List.of(42L, 15L, 18L));
        when(identityGateway.requireEmployee(15L))
                .thenReturn(new BpmEmployeeSnapshot(15L, "审批人A", 8L, "财务部", null, null));

        Map<String, Object> variables = resolver.resolve(
                List.of(buildNode(
                        "task_role",
                        "{\"nodeKey\":\"task_role\",\"name\":\"角色审批\",\"type\":\"userTask\",\"candidateResolverType\":\"ROLE\",\"roleId\":9}"
                )),
                startEmployee
        );

        assertThat(variables).containsEntry("assignee_task_role", "15");
        verify(identityGateway).requireEmployee(15L);
    }

    @Test
    void resolveShouldSkipUnavailableRoleMember() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);
        when(identityGateway.listEmployeeIdsByRoleId(9L)).thenReturn(List.of(42L, 15L, 18L));
        when(identityGateway.requireEmployee(15L)).thenThrow(new IllegalArgumentException("员工已禁用"));
        when(identityGateway.requireEmployee(18L))
                .thenReturn(new BpmEmployeeSnapshot(18L, "审批人B", 8L, "财务部", null, null));

        Map<String, Object> variables = resolver.resolve(
                List.of(buildNode(
                        "task_role",
                        "{\"nodeKey\":\"task_role\",\"name\":\"角色审批\",\"type\":\"userTask\",\"candidateResolverType\":\"ROLE\",\"roleId\":9}"
                )),
                startEmployee
        );

        assertThat(variables).containsEntry("assignee_task_role", "18");
        verify(identityGateway).requireEmployee(15L);
        verify(identityGateway).requireEmployee(18L);
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
    void resolveShouldRejectStartDepartmentManagerStrategyWhenManagerUnavailable() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);
        when(identityGateway.resolveDepartmentManagerEmployeeId(7L)).thenReturn(200L);
        when(identityGateway.requireEmployee(200L)).thenThrow(new IllegalArgumentException("员工已禁用"));

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_start_manager",
                        "{\"nodeKey\":\"task_start_manager\",\"name\":\"发起人部门主管审批\",\"type\":\"userTask\",\"candidateResolverType\":\"START_DEPARTMENT_MANAGER\"}"
                )),
                startEmployee
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("员工已禁用");

        verify(identityGateway).requireEmployee(200L);
    }

    @Test
    void resolveShouldUseEmployeeSelectedFromStartFormData() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);
        when(identityGateway.requireEmployee(301L)).thenReturn(new BpmEmployeeSnapshot(301L, "审批人A", 8L, "财务部", null, null));

        Map<String, Object> variables = resolver.resolve(
                List.of(buildNode(
                        "task_selected",
                        "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
                )),
                new BpmTaskAssignmentContext(startEmployee, "{\"approverEmployeeId\":301}")
        );

        assertThat(variables).containsEntry("assignee_task_selected", "301");
        verify(identityGateway).requireEmployee(301L);
    }

    @Test
    void resolveShouldUseStringEmployeeSelectedFromStartFormData() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);
        when(identityGateway.requireEmployee(302L)).thenReturn(new BpmEmployeeSnapshot(302L, "审批人B", 8L, "财务部", null, null));

        Map<String, Object> variables = resolver.resolve(
                List.of(buildNode(
                        "task_selected",
                        "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
                )),
                new BpmTaskAssignmentContext(startEmployee, "{\"approverEmployeeId\":\"302\"}")
        );

        assertThat(variables).containsEntry("assignee_task_selected", "302");
        verify(identityGateway).requireEmployee(302L);
    }

    @Test
    void resolveShouldRejectEmployeeSelectAtStartWhenEmployeeMissing() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);
        when(identityGateway.requireEmployee(301L)).thenThrow(new IllegalArgumentException("员工不存在"));

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_selected",
                        "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
                )),
                new BpmTaskAssignmentContext(startEmployee, "{\"approverEmployeeId\":301}")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("员工不存在");

        verify(identityGateway).requireEmployee(301L);
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

    @Test
    void resolveShouldRejectFractionalEmployeeSelectedFromStartFormData() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_selected",
                        "{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}"
                )),
                new BpmTaskAssignmentContext(startEmployee, "{\"approverEmployeeId\":301.9}")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【发起时选择审批】发起时自选审批人无效");
    }

    @Test
    void resolveShouldRejectFractionalFixedEmployeeId() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_finance",
                        "{\"nodeKey\":\"task_finance\",\"name\":\"财务审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":301.9}"
                )),
                startEmployee
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【财务审批】")
                .hasMessageContaining("指定员工");
    }

    @Test
    void resolveShouldRejectOverflowFixedEmployeeId() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        assertThatThrownBy(() -> resolver.resolve(
                List.of(buildNode(
                        "task_finance",
                        "{\"nodeKey\":\"task_finance\",\"name\":\"财务审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":9223372036854775808}"
                )),
                startEmployee
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审批节点【财务审批】未配置指定员工");
    }

    @Test
    void resolveShouldUseExpandedEmployeeIdsForSequentialApprovalNodes() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        Map<String, Object> variables = resolver.resolve(
                List.of(
                        buildSequentialNode("task_finance_1", "财务复核（1/2）", 301, 1, 2),
                        buildSequentialNode("task_finance_2", "财务复核（2/2）", 302, 2, 2)
                ),
                startEmployee
        );

        assertThat(variables)
                .containsEntry("assignee_task_finance_1", "301")
                .containsEntry("assignee_task_finance_2", "302");
    }

    @Test
    void resolveShouldRejectSequentialApprovalWhenExpandedEmployeeMissing() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);
        when(identityGateway.requireEmployee(302L)).thenThrow(new IllegalArgumentException("员工不存在"));

        assertThatThrownBy(() -> resolver.resolve(
                List.of(
                        buildSequentialNode("task_finance_1", "财务复核（1/2）", 301, 1, 2),
                        buildSequentialNode("task_finance_2", "财务复核（2/2）", 302, 2, 2)
                ),
                startEmployee
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("员工不存在");

        verify(identityGateway).requireEmployee(302L);
    }

    @Test
    void resolveShouldCreateIndependentVariablesForParallelAllMembers() {
        BpmEmployeeSnapshot startEmployee = new BpmEmployeeSnapshot(100L, "张三", 7L, "人事部", null, null);

        Map<String, Object> variables = resolver.resolve(
                List.of(
                        buildParallelNode("finance_review_1", "财务会签（1/2）", 101, 1, 2),
                        buildParallelNode("finance_review_2", "财务会签（2/2）", 102, 2, 2)
                ),
                startEmployee
        );

        assertThat(variables)
                .containsEntry("assignee_finance_review_1", "101")
                .containsEntry("assignee_finance_review_2", "102");
    }

    private BpmDefinitionNodeEntity buildNode(String nodeKey, String authoredRuleSnapshotJson) {
        BpmDefinitionNodeEntity entity = new BpmDefinitionNodeEntity();
        entity.setNodeKey(nodeKey);
        entity.setNodeType("userTask");
        entity.setNodeNameSnapshot(nodeKey);
        entity.setAuthoredRuleSnapshotJson(authoredRuleSnapshotJson);
        return entity;
    }

    private BpmDefinitionNodeEntity buildSequentialNode(
            String nodeKey,
            String nodeName,
            long employeeId,
            int sequentialIndex,
            int sequentialTotal
    ) {
        BpmDefinitionNodeEntity entity = buildNode(
                nodeKey,
                "{\"nodeKey\":\"task_finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[301,302]}"
        );
        entity.setNodeNameSnapshot(nodeName);
        entity.setCompiledNodeSnapshotJson(
                "{\"nodeKey\":\"" + nodeKey + "\",\"name\":\"" + nodeName + "\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":" + employeeId + ",\"authoredNodeKey\":\"task_finance\",\"authoredNodeName\":\"财务复核\",\"sequentialIndex\":" + sequentialIndex + ",\"sequentialTotal\":" + sequentialTotal + "}"
        );
        return entity;
    }

    private BpmDefinitionNodeEntity buildParallelNode(
            String nodeKey,
            String nodeName,
            long employeeId,
            int parallelIndex,
            int parallelTotal
    ) {
        BpmDefinitionNodeEntity entity = buildNode(
                nodeKey,
                "{\"nodeKey\":\"finance_review\",\"name\":\"财务会签\",\"type\":\"userTask\",\"approvalMode\":\"parallelAll\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[101,102]}"
        );
        entity.setNodeNameSnapshot(nodeName);
        entity.setCompiledNodeSnapshotJson(
                "{\"nodeKey\":\"" + nodeKey + "\",\"name\":\"" + nodeName
                        + "\",\"type\":\"userTask\",\"approvalMode\":\"parallelAll\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":"
                        + employeeId
                        + ",\"approvalGroupKey\":\"finance_review\",\"approvalGroupName\":\"财务会签\",\"parallelIndex\":"
                        + parallelIndex + ",\"parallelTotal\":" + parallelTotal + "}"
        );
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
