package com.hunyuan.sa.admin.module.bpm.adapter;

import com.hunyuan.sa.admin.module.system.department.service.DepartmentService;
import com.hunyuan.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import com.hunyuan.sa.admin.module.system.employee.service.EmployeeService;
import com.hunyuan.sa.admin.module.system.role.service.RoleEmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AdminBpmOrgIdentityGatewayTest {

    private AdminBpmOrgIdentityGateway gateway;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = Mockito.mock(EmployeeService.class);
        gateway = new AdminBpmOrgIdentityGateway(
                employeeService,
                Mockito.mock(DepartmentService.class),
                Mockito.mock(RoleEmployeeService.class)
        );
    }

    @Test
    void requireEmployeeShouldRejectDisabledEmployee() {
        EmployeeEntity employeeEntity = buildEmployee(301L);
        employeeEntity.setDisabledFlag(Boolean.TRUE);
        when(employeeService.getById(301L)).thenReturn(employeeEntity);

        assertThatThrownBy(() -> gateway.requireEmployee(301L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("员工已禁用")
                .hasMessageContaining("301");
    }

    @Test
    void requireEmployeeShouldRejectDeletedEmployee() {
        EmployeeEntity employeeEntity = buildEmployee(302L);
        employeeEntity.setDeletedFlag(Boolean.TRUE);
        when(employeeService.getById(302L)).thenReturn(employeeEntity);

        assertThatThrownBy(() -> gateway.requireEmployee(302L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("员工已删除")
                .hasMessageContaining("302");
    }

    @Test
    void positionResolutionShouldDelegateToActiveEmployeeQuery() {
        when(employeeService.listActiveEmployeeIdsByPositionId(8L)).thenReturn(java.util.List.of(20L, 30L));

        assertThat(gateway.listActiveEmployeeIdsByPositionId(8L)).containsExactly(20L, 30L);
    }

    private EmployeeEntity buildEmployee(Long employeeId) {
        EmployeeEntity employeeEntity = new EmployeeEntity();
        employeeEntity.setEmployeeId(employeeId);
        employeeEntity.setActualName("审批人");
        employeeEntity.setDisabledFlag(Boolean.FALSE);
        employeeEntity.setDeletedFlag(Boolean.FALSE);
        return employeeEntity;
    }
}
