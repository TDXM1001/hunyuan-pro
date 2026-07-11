package com.hunyuan.sa.bpm.engine.compiler;

import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionValidationReportVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class BpmCandidatePrecheckServiceTest {

    private BpmCandidatePrecheckService service;

    private BpmOrgIdentityGateway identityGateway;

    @BeforeEach
    void setUp() {
        service = new BpmCandidatePrecheckService();
        identityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        setField(service, "bpmOrgIdentityGateway", identityGateway);
    }

    @Test
    void precheckShouldExplainSequentialEmployeeApproval() {
        when(identityGateway.requireEmployee(301L))
                .thenReturn(new BpmEmployeeSnapshot(301L, "审批人A", 8L, "财务部", null, null));
        when(identityGateway.requireEmployee(302L))
                .thenReturn(new BpmEmployeeSnapshot(302L, "审批人B", 8L, "财务部", null, null));

        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"task_finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[301,302]}]}",
                "[]"
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("READY");
        assertThat(checks.get(0).getRequiredConfig()).contains("301").contains("302");
        assertThat(checks.get(0).getMessage()).contains("顺序审批").contains("2");
    }

    @Test
    void precheckShouldBlockSequentialEmployeeApprovalWhenEmployeeMissing() {
        when(identityGateway.requireEmployee(301L))
                .thenReturn(new BpmEmployeeSnapshot(301L, "审批人A", 8L, "财务部", null, null));
        when(identityGateway.requireEmployee(302L)).thenThrow(new IllegalArgumentException("员工不存在"));

        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"task_finance\",\"name\":\"财务复核\",\"type\":\"userTask\",\"approvalMode\":\"sequential\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[301,302]}]}",
                "[]"
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("BLOCKING");
        assertThat(checks.get(0).getCode()).isEqualTo("EMPLOYEE_NOT_FOUND");
        assertThat(checks.get(0).getMessage()).contains("员工不存在");
    }

    @Test
    void precheckShouldExplainParallelAllEmployeeApproval() {
        when(identityGateway.requireEmployee(101L))
                .thenReturn(new BpmEmployeeSnapshot(101L, "审批人A", 8L, "财务部", null, null));
        when(identityGateway.requireEmployee(102L))
                .thenReturn(new BpmEmployeeSnapshot(102L, "审批人B", 8L, "财务部", null, null));

        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"finance_review\",\"name\":\"财务会签\",\"type\":\"userTask\",\"approvalMode\":\"parallelAll\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[101,102]}]}",
                "[]"
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("READY");
        assertThat(checks.get(0).getRequiredConfig()).contains("101").contains("102");
        assertThat(checks.get(0).getMessage()).contains("并行会签").contains("2");
    }

    @Test
    void precheckShouldBlockParallelAllWhenAnyEmployeeUnavailable() {
        when(identityGateway.requireEmployee(101L))
                .thenReturn(new BpmEmployeeSnapshot(101L, "审批人A", 8L, "财务部", null, null));
        when(identityGateway.requireEmployee(102L)).thenThrow(new IllegalArgumentException("员工已禁用"));

        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"finance_review\",\"name\":\"财务会签\",\"type\":\"userTask\",\"approvalMode\":\"parallelAll\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[101,102]}]}",
                "[]"
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("BLOCKING");
        assertThat(checks.get(0).getCode()).isEqualTo("EMPLOYEE_NOT_FOUND");
        assertThat(checks.get(0).getMessage()).contains("员工已禁用");
    }

    @Test
    void precheckShouldBlockRoleWhenNoEmployeeCanBeResolved() {
        when(identityGateway.listEmployeeIdsByRoleId(9L)).thenReturn(List.of());

        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"task_role\",\"name\":\"角色审批\",\"type\":\"userTask\",\"candidateResolverType\":\"ROLE\",\"roleId\":9}]}",
                "[]"
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("BLOCKING");
        assertThat(checks.get(0).getCode()).isEqualTo("ROLE_EMPLOYEE_EMPTY");
        assertThat(checks.get(0).getMessage()).contains("角色").contains("员工");
    }

    @Test
    void precheckShouldCountOnlyAvailableRoleEmployees() {
        when(identityGateway.listEmployeeIdsByRoleId(9L)).thenReturn(List.of(301L, 302L));
        when(identityGateway.requireEmployee(301L)).thenThrow(new IllegalArgumentException("员工已禁用"));
        when(identityGateway.requireEmployee(302L))
                .thenReturn(new BpmEmployeeSnapshot(302L, "审批人B", 8L, "财务部", null, null));

        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"task_role\",\"name\":\"角色审批\",\"type\":\"userTask\",\"candidateResolverType\":\"ROLE\",\"roleId\":9}]}",
                "[]"
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("READY");
        assertThat(checks.get(0).getMessage()).contains("1 名员工");
    }

    @Test
    void precheckShouldBlockRoleWhenAllMembersAreUnavailable() {
        when(identityGateway.listEmployeeIdsByRoleId(9L)).thenReturn(List.of(301L, 302L));
        when(identityGateway.requireEmployee(301L)).thenThrow(new IllegalArgumentException("员工已禁用"));
        when(identityGateway.requireEmployee(302L)).thenThrow(new IllegalArgumentException("员工已删除"));

        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"task_role\",\"name\":\"角色审批\",\"type\":\"userTask\",\"candidateResolverType\":\"ROLE\",\"roleId\":9}]}",
                "[]"
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("BLOCKING");
        assertThat(checks.get(0).getCode()).isEqualTo("ROLE_EMPLOYEE_EMPTY");
    }

    @Test
    void precheckShouldBlockConfiguredDepartmentWhenManagerCannotBeResolved() {
        when(identityGateway.resolveDepartmentManagerEmployeeId(8L)).thenReturn(null);

        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"task_manager\",\"name\":\"部门主管审批\",\"type\":\"userTask\",\"candidateResolverType\":\"DEPARTMENT_MANAGER\",\"departmentId\":8}]}",
                "[]"
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("BLOCKING");
        assertThat(checks.get(0).getCode()).isEqualTo("DEPARTMENT_MANAGER_EMPTY");
        assertThat(checks.get(0).getMessage()).contains("部门").contains("主管");
    }

    @Test
    void precheckShouldBlockConfiguredDepartmentWhenManagerUnavailable() {
        when(identityGateway.resolveDepartmentManagerEmployeeId(8L)).thenReturn(301L);
        when(identityGateway.requireEmployee(301L)).thenThrow(new IllegalArgumentException("员工已禁用"));

        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"task_manager\",\"name\":\"部门主管审批\",\"type\":\"userTask\",\"candidateResolverType\":\"DEPARTMENT_MANAGER\",\"departmentId\":8}]}",
                "[]"
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("BLOCKING");
        assertThat(checks.get(0).getCode()).isEqualTo("DEPARTMENT_MANAGER_INVALID");
        assertThat(checks.get(0).getMessage()).contains("员工已禁用");
    }

    @Test
    void precheckShouldBlockDepartmentManagerWhenSimulatedStarterDepartmentHasNoManager() {
        when(identityGateway.resolveDepartmentManagerEmployeeId(8L)).thenReturn(null);

        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"task_manager\",\"name\":\"部门主管审批\",\"type\":\"userTask\",\"candidateResolverType\":\"DEPARTMENT_MANAGER\"}]}",
                "[]",
                new BpmCandidatePrecheckContext(100L, 8L, null)
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("BLOCKING");
        assertThat(checks.get(0).getCode()).isEqualTo("DEPARTMENT_MANAGER_EMPTY");
    }

    @Test
    void precheckShouldBlockStartDepartmentManagerWhenSimulatedStarterDepartmentHasNoManager() {
        when(identityGateway.resolveDepartmentManagerEmployeeId(8L)).thenReturn(null);

        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"task_start_manager\",\"name\":\"发起人部门主管审批\",\"type\":\"userTask\",\"candidateResolverType\":\"START_DEPARTMENT_MANAGER\"}]}",
                "[]",
                new BpmCandidatePrecheckContext(100L, 8L, null)
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("BLOCKING");
        assertThat(checks.get(0).getCode()).isEqualTo("DEPARTMENT_MANAGER_EMPTY");
    }

    @Test
    void precheckShouldBlockStartEmployeeWhenSimulatedStarterUnavailable() {
        when(identityGateway.requireEmployee(100L)).thenThrow(new IllegalArgumentException("员工已禁用"));

        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"task_starter\",\"name\":\"发起人审批\",\"type\":\"userTask\",\"candidateResolverType\":\"START_EMPLOYEE\"}]}",
                "[]",
                new BpmCandidatePrecheckContext(100L, 8L, null)
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("BLOCKING");
        assertThat(checks.get(0).getCode()).isEqualTo("EMPLOYEE_NOT_FOUND");
        assertThat(checks.get(0).getMessage()).contains("员工已禁用");
    }

    @Test
    void precheckShouldRejectEmployeeSelectFieldWhenSchemaOnlyProvidesAliasName() {
        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}]}",
                "{\"fields\":[{\"name\":\"approverEmployeeId\",\"type\":\"employeeSelect\"}]}"
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("BLOCKING");
        assertThat(checks.get(0).getCode()).isEqualTo("EMPLOYEE_SELECT_FIELD_MISSING");
    }

    @Test
    void precheckShouldBlockFractionalFixedEmployeeId() {
        when(identityGateway.requireEmployee(301L))
                .thenReturn(new BpmEmployeeSnapshot(301L, "审批人A", 8L, "财务部", null, null));

        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"task_finance\",\"name\":\"财务审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":301.9}]}",
                "[]"
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("BLOCKING");
        assertThat(checks.get(0).getCode()).isEqualTo("EMPLOYEE_ID_MISSING");
    }

    @Test
    void precheckShouldBlockFractionalEmployeeIdFromSimulatedFormData() {
        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}]}",
                "{\"fields\":[{\"field\":\"approverEmployeeId\",\"type\":\"employeeSelect\"}]}",
                new BpmCandidatePrecheckContext(100L, 8L, "{\"approverEmployeeId\":301.9}")
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("BLOCKING");
        assertThat(checks.get(0).getCode()).isEqualTo("EMPLOYEE_SELECT_FORM_DATA_INVALID");
    }

    @Test
    void precheckShouldBlockMalformedSimulatedFormData() {
        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}]}",
                "{\"fields\":[{\"field\":\"approverEmployeeId\",\"type\":\"employeeSelect\"}]}",
                new BpmCandidatePrecheckContext(100L, 8L, "{\"approverEmployeeId\":")
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("BLOCKING");
        assertThat(checks.get(0).getCode()).isEqualTo("EMPLOYEE_SELECT_FORM_DATA_INVALID");
        assertThat(checks.get(0).getMessage()).contains("模拟表单").contains("JSON");
    }

    @Test
    void precheckShouldBlockEmployeeSelectWhenSimulatedEmployeeUnavailable() {
        when(identityGateway.requireEmployee(301L)).thenThrow(new IllegalArgumentException("员工已删除"));

        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"task_selected\",\"name\":\"发起时选择审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE_SELECT_AT_START\",\"employeeSelectFieldKey\":\"approverEmployeeId\"}]}",
                "{\"fields\":[{\"field\":\"approverEmployeeId\",\"type\":\"employeeSelect\"}]}",
                new BpmCandidatePrecheckContext(100L, 8L, "{\"approverEmployeeId\":301}")
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("BLOCKING");
        assertThat(checks.get(0).getCode()).isEqualTo("EMPLOYEE_NOT_FOUND");
        assertThat(checks.get(0).getMessage()).contains("员工已删除");
    }

    @Test
    void precheckShouldBlockOverflowFixedEmployeeId() {
        List<BpmDefinitionValidationReportVO.CandidateCheck> checks = service.precheck(
                "{\"nodes\":[{\"nodeKey\":\"task_finance\",\"name\":\"财务审批\",\"type\":\"userTask\",\"candidateResolverType\":\"EMPLOYEE\",\"employeeId\":9223372036854775808}]}",
                "[]"
        );

        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatus()).isEqualTo("BLOCKING");
        assertThat(checks.get(0).getCode()).isEqualTo("EMPLOYEE_ID_MISSING");
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
