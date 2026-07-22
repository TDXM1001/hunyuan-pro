package com.hunyuan.sa.admin.module.system.datascope.service;

import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeCollaborationProfile;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeCollaborationDirectory;
import com.hunyuan.sa.admin.module.organization.department.application.OrganizationDepartmentFacade;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeFacade;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeType;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeViewType;
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
    private AccessDataScopeFacade accessDataScopeFacade;
    @Mock
    private EmployeeCollaborationDirectory employeeCollaborationDirectory;
    @Mock
    private OrganizationDepartmentFacade organizationDepartmentFacade;

    private DataScopeViewService service;

    @BeforeEach
    void setUp() {
        service = new DataScopeViewService();
        ReflectionTestUtils.setField(service, "accessDataScopeFacade", accessDataScopeFacade);
        ReflectionTestUtils.setField(service, "employeeCollaborationDirectory", employeeCollaborationDirectory);
        ReflectionTestUtils.setField(service, "organizationDepartmentFacade", organizationDepartmentFacade);
    }

    @Test
    void missingEmployeeKeepsPersonalScopeFallback() {
        when(employeeCollaborationDirectory.findCollaborationProfileById(7L)).thenReturn(Optional.empty());

        assertThat(service.getEmployeeDataScopeViewType(AccessDataScopeType.NOTICE, 7L))
                .isEqualTo(AccessDataScopeViewType.ME);
        verify(accessDataScopeFacade, never()).resolveEmployeeViewType(7L, AccessDataScopeType.NOTICE.getValue());
    }

    @Test
    void administratorKeepsAllScope() {
        when(employeeCollaborationDirectory.findCollaborationProfileById(7L))
                .thenReturn(Optional.of(profile(7L, 20L, true)));

        assertThat(service.getEmployeeDataScopeViewType(AccessDataScopeType.NOTICE, 7L))
                .isEqualTo(AccessDataScopeViewType.ALL);
        verify(accessDataScopeFacade, never()).resolveEmployeeViewType(7L, AccessDataScopeType.NOTICE.getValue());
    }

    @Test
    void departmentAndDescendantQueriesExcludeDeletedEmployeesThroughDirectoryFacade() {
        when(employeeCollaborationDirectory.findCollaborationProfileById(7L))
                .thenReturn(Optional.of(profile(7L, 20L, false)));
        when(organizationDepartmentFacade.selfAndDescendantIdsForCollaboration(20L))
                .thenReturn(List.of(20L, 21L));
        when(employeeCollaborationDirectory.findNonDeletedEmployeeIdsByDepartmentIds(List.of(20L, 21L)))
                .thenReturn(List.of(7L, 8L));

        assertThat(service.getCanViewEmployeeId(AccessDataScopeViewType.DEPARTMENT_AND_SUB, 7L))
                .containsExactly(7L, 8L);
    }

    private EmployeeCollaborationProfile profile(Long employeeId, Long departmentId, boolean administrator) {
        return new EmployeeCollaborationProfile(
                employeeId, "Employee " + employeeId, departmentId, administrator, false, false);
    }
}
