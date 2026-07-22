package com.hunyuan.sa.admin.module.system.datascope.service;

import com.hunyuan.sa.admin.module.access.datascope.api.AccessDepartmentScope;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeType;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeViewType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessDepartmentScopeFacadeAdapterTest {

    @Mock
    private DataScopeViewService dataScopeViewService;

    private AccessDepartmentScopeFacadeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AccessDepartmentScopeFacadeAdapter();
        ReflectionTestUtils.setField(adapter, "dataScopeViewService", dataScopeViewService);
    }

    @Test
    void emptyLegacyDepartmentListBecomesUnrestrictedScope() {
        when(dataScopeViewService.getEmployeeDataScopeViewType(
                AccessDataScopeType.ORGANIZATION_DIRECTORY, 7L))
                .thenReturn(AccessDataScopeViewType.ALL);
        when(dataScopeViewService.getCanViewDepartmentId(AccessDataScopeViewType.ALL, 7L))
                .thenReturn(List.of());

        assertThat(adapter.resolveOrganizationDirectoryScope(7L))
                .isEqualTo(AccessDepartmentScope.allDepartments());
    }

    @Test
    void configuredDepartmentListBecomesRestrictedScope() {
        when(dataScopeViewService.getEmployeeDataScopeViewType(
                AccessDataScopeType.ORGANIZATION_DIRECTORY, 7L))
                .thenReturn(AccessDataScopeViewType.DEPARTMENT_AND_SUB);
        when(dataScopeViewService.getCanViewDepartmentId(AccessDataScopeViewType.DEPARTMENT_AND_SUB, 7L))
                .thenReturn(List.of(20L, 21L));

        assertThat(adapter.resolveOrganizationDirectoryScope(7L))
                .isEqualTo(AccessDepartmentScope.restricted(List.of(20L, 21L)));
    }
}
