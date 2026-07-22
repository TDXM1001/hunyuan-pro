package com.hunyuan.sa.admin.module.system.datascope.service;

import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeCollaborationProfile;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeCollaborationDirectory;
import com.hunyuan.sa.admin.module.organization.department.application.OrganizationDepartmentFacade;
import com.hunyuan.sa.admin.module.system.datascope.constant.DataScopeTypeEnum;
import com.hunyuan.sa.admin.module.system.datascope.constant.DataScopeViewTypeEnum;
import com.hunyuan.sa.admin.module.system.role.dao.RoleDataScopeDao;
import com.hunyuan.sa.admin.module.system.role.dao.RoleEmployeeDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataScopeViewServiceTest {

    @Mock
    private RoleEmployeeDao roleEmployeeDao;
    @Mock
    private RoleDataScopeDao roleDataScopeDao;
    @Mock
    private EmployeeCollaborationDirectory employeeCollaborationDirectory;
    @Mock
    private OrganizationDepartmentFacade organizationDepartmentFacade;

    private DataScopeViewService service;

    @BeforeEach
    void setUp() {
        service = new DataScopeViewService();
        ReflectionTestUtils.setField(service, "roleEmployeeDao", roleEmployeeDao);
        ReflectionTestUtils.setField(service, "roleDataScopeDao", roleDataScopeDao);
        ReflectionTestUtils.setField(service, "employeeCollaborationDirectory", employeeCollaborationDirectory);
        ReflectionTestUtils.setField(service, "organizationDepartmentFacade", organizationDepartmentFacade);
    }

    @Test
    void missingEmployeeKeepsPersonalScopeFallback() {
        when(employeeCollaborationDirectory.findCollaborationProfileById(7L)).thenReturn(Optional.empty());

        assertThat(service.getEmployeeDataScopeViewType(DataScopeTypeEnum.NOTICE, 7L))
                .isEqualTo(DataScopeViewTypeEnum.ME);
        verify(roleEmployeeDao, never()).selectRoleIdByEmployeeId(7L);
    }

    @Test
    void administratorKeepsAllScope() {
        when(employeeCollaborationDirectory.findCollaborationProfileById(7L))
                .thenReturn(Optional.of(profile(7L, 20L, true)));

        assertThat(service.getEmployeeDataScopeViewType(DataScopeTypeEnum.NOTICE, 7L))
                .isEqualTo(DataScopeViewTypeEnum.ALL);
        verify(roleEmployeeDao, never()).selectRoleIdByEmployeeId(7L);
    }

    @Test
    void departmentAndDescendantQueriesExcludeDeletedEmployeesThroughDirectoryFacade() {
        when(employeeCollaborationDirectory.findCollaborationProfileById(7L))
                .thenReturn(Optional.of(profile(7L, 20L, false)));
        when(organizationDepartmentFacade.selfAndDescendantIdsForCollaboration(20L))
                .thenReturn(List.of(20L, 21L));
        when(employeeCollaborationDirectory.findNonDeletedEmployeeIdsByDepartmentIds(List.of(20L, 21L)))
                .thenReturn(List.of(7L, 8L));

        assertThat(service.getCanViewEmployeeId(DataScopeViewTypeEnum.DEPARTMENT_AND_SUB, 7L))
                .containsExactly(7L, 8L);
    }

    private EmployeeCollaborationProfile profile(Long employeeId, Long departmentId, boolean administrator) {
        return new EmployeeCollaborationProfile(
                employeeId, "Employee " + employeeId, departmentId, administrator, false, false);
    }
}
