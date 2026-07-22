package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.access.role.api.AccessRole;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleMember;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleSelection;
import com.hunyuan.sa.admin.module.organization.department.application.OrganizationDepartmentFacade;
import com.hunyuan.sa.admin.module.organization.department.domain.Department;
import com.hunyuan.sa.admin.module.system.role.dao.RoleDao;
import com.hunyuan.sa.admin.module.system.role.dao.RoleEmployeeDao;
import com.hunyuan.sa.admin.module.system.role.domain.entity.RoleEntity;
import com.hunyuan.sa.admin.module.system.role.domain.vo.RoleEmployeeSummaryRow;
import com.hunyuan.sa.admin.module.system.role.domain.vo.RoleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRoleMembershipFacadeAdapterTest {

    @Mock
    private RoleEmployeeDao roleEmployeeDao;

    @Mock
    private RoleDao roleDao;

    @Mock
    private OrganizationDepartmentFacade organizationDepartmentFacade;

    private AccessRoleMembershipFacadeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AccessRoleMembershipFacadeAdapter();
        ReflectionTestUtils.setField(adapter, "roleEmployeeDao", roleEmployeeDao);
        ReflectionTestUtils.setField(adapter, "roleDao", roleDao);
        ReflectionTestUtils.setField(
                adapter, "organizationDepartmentFacade", organizationDepartmentFacade);
    }

    @Test
    void roleMembersUseIdentityEmployeeSummary() {
        RoleEmployeeSummaryRow row = new RoleEmployeeSummaryRow();
        row.setEmployeeId(7L);
        row.setLoginName("employee-7");
        row.setActualName("Employee 7");
        row.setDepartmentId(10L);
        row.setDisabled(false);
        row.setCreateTime(LocalDateTime.of(2026, 7, 22, 10, 0));
        when(roleEmployeeDao.selectEmployeeByRoleId(3L)).thenReturn(List.of(row));
        when(organizationDepartmentFacade.listForCollaboration())
                .thenReturn(List.of(new Department(
                        10L, "Engineering", null, 0L, 1, null, null, null)));

        List<AccessRoleMember> result = adapter.listMembers(3L);

        assertThat(result).containsExactly(new AccessRoleMember(
                7L,
                "employee-7",
                "Employee 7",
                null,
                null,
                null,
                null,
                10L,
                "Engineering",
                null,
                false,
                LocalDateTime.of(2026, 7, 22, 10, 0)));
    }

    @Test
    void employeeRoleSelectionsUseStableAccessModels() {
        RoleEntity selectedRole = role(3L, "财务", "finance");
        RoleEntity candidateRole = role(4L, "审计", "audit");
        when(roleEmployeeDao.selectRoleIdByEmployeeId(7L)).thenReturn(List.of(3L));
        when(roleDao.selectList(null)).thenReturn(List.of(selectedRole, candidateRole));

        List<AccessRoleSelection> result = adapter.listEmployeeRoleSelections(7L);

        assertThat(result).containsExactly(
                new AccessRoleSelection(3L, "财务", "finance", null, true),
                new AccessRoleSelection(4L, "审计", "audit", null, false));
    }

    @Test
    void authorizationRoleLookupDoesNotExposeLegacyRoleType() {
        RoleVO role = new RoleVO();
        role.setRoleId(3L);
        role.setRoleName("财务");
        role.setRoleCode("finance");
        when(roleEmployeeDao.selectRoleByEmployeeId(7L)).thenReturn(List.of(role));

        List<AccessRole> result = adapter.listEmployeeRoles(7L);

        assertThat(result).containsExactly(new AccessRole(3L, "财务", "finance", null));
    }

    private RoleEntity role(Long roleId, String roleName, String roleCode) {
        RoleEntity role = new RoleEntity();
        role.setRoleId(roleId);
        role.setRoleName(roleName);
        role.setRoleCode(roleCode);
        return role;
    }
}
