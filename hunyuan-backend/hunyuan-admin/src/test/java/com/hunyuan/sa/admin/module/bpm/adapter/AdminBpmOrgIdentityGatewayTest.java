package com.hunyuan.sa.admin.module.bpm.adapter;

import com.hunyuan.sa.admin.module.system.department.service.DepartmentService;
import com.hunyuan.sa.admin.module.system.department.domain.vo.DepartmentVO;
import com.hunyuan.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import com.hunyuan.sa.admin.module.system.employee.dao.OrganizationRelationDao;
import com.hunyuan.sa.admin.module.system.employee.service.EmployeeService;
import com.hunyuan.sa.admin.module.system.role.service.RoleEmployeeService;
import com.hunyuan.sa.admin.module.system.role.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AdminBpmOrgIdentityGatewayTest {

    private AdminBpmOrgIdentityGateway gateway;

    private EmployeeService employeeService;

    private DepartmentService departmentService;

    private OrganizationRelationDao organizationRelationDao;

    @BeforeEach
    void setUp() {
        employeeService = Mockito.mock(EmployeeService.class);
        departmentService = Mockito.mock(DepartmentService.class);
        organizationRelationDao = Mockito.mock(OrganizationRelationDao.class);
        gateway = new AdminBpmOrgIdentityGateway(
                employeeService,
                departmentService,
                Mockito.mock(RoleEmployeeService.class),
                organizationRelationDao,
                Mockito.mock(RoleService.class)
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

    @Test
    void userGroupResolutionShouldReadOnlyActiveOrganizationMembers() {
        when(organizationRelationDao.listActiveEmployeeIdsByUserGroupId(8L))
                .thenReturn(java.util.List.of(20L, 30L));

        assertThat(gateway.listActiveEmployeeIdsByUserGroupId(8L)).containsExactly(20L, 30L);
    }

    @Test
    void departmentManagerChainShouldFollowParentDepartmentsNearestFirst() {
        when(departmentService.getDepartmentById(8L)).thenReturn(department(8L, 47L, 7L));
        when(departmentService.getDepartmentById(7L)).thenReturn(department(7L, 44L, 1L));
        when(departmentService.getDepartmentById(1L)).thenReturn(department(1L, 1L, 0L));
        when(employeeService.getById(47L)).thenReturn(buildEmployee(47L));
        when(employeeService.getById(44L)).thenReturn(buildEmployee(44L));
        when(employeeService.getById(1L)).thenReturn(buildEmployee(1L));

        assertThat(gateway.listDepartmentManagerChain(8L, 3)).containsExactly(47L, 44L, 1L);
    }

    @Test
    void employeeReportingChainShouldFailClosedOnCycles() {
        when(organizationRelationDao.selectActiveManagerEmployeeId(20L)).thenReturn(30L);
        when(organizationRelationDao.selectActiveManagerEmployeeId(30L)).thenReturn(20L);
        when(employeeService.getById(30L)).thenReturn(buildEmployee(30L));
        when(employeeService.getById(20L)).thenReturn(buildEmployee(20L));

        assertThatThrownBy(() -> gateway.listEmployeeReportingManagerChain(20L, 3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("循环");
    }

    private DepartmentVO department(Long id, Long managerId, Long parentId) {
        DepartmentVO department = new DepartmentVO();
        department.setDepartmentId(id);
        department.setManagerId(managerId);
        department.setParentId(parentId);
        return department;
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
