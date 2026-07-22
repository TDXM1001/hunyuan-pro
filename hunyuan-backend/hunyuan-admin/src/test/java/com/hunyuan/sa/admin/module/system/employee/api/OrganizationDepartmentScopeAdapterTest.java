package com.hunyuan.sa.admin.module.system.employee.api;

import com.hunyuan.sa.admin.module.access.datascope.api.AccessDepartmentScope;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDepartmentScopeFacade;
import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationDepartmentScopeAdapterTest {

    @Mock
    private AccessDepartmentScopeFacade accessDepartmentScopeFacade;
    @Mock
    private RequestUser requestUser;

    private OrganizationDepartmentScopeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OrganizationDepartmentScopeAdapter();
        ReflectionTestUtils.setField(adapter, "accessDepartmentScopeFacade", accessDepartmentScopeFacade);
    }

    @AfterEach
    void tearDown() {
        SmartRequestUtil.remove();
    }

    @Test
    void missingRequestUserCannotAccessDepartmentOrCreateRoot() {
        assertThat(adapter.canAccess(20L)).isFalse();
        assertThat(adapter.canCreateUnder(0L)).isFalse();
        verify(accessDepartmentScopeFacade, never()).resolveOrganizationDirectoryScope(7L);
    }

    @Test
    void restrictedScopeOnlyAllowsConfiguredDepartments() {
        bindRequestUser(7L);
        when(accessDepartmentScopeFacade.resolveOrganizationDirectoryScope(7L))
                .thenReturn(AccessDepartmentScope.restricted(List.of(20L, 21L)));

        assertThat(adapter.canAccess(20L)).isTrue();
        assertThat(adapter.canAccess(30L)).isFalse();
        assertThat(adapter.canCreateUnder(20L)).isTrue();
        assertThat(adapter.canCreateUnder(0L)).isFalse();
    }

    @Test
    void unrestrictedScopeAllowsDepartmentsAndRootCreation() {
        bindRequestUser(7L);
        when(accessDepartmentScopeFacade.resolveOrganizationDirectoryScope(7L))
                .thenReturn(AccessDepartmentScope.allDepartments());

        assertThat(adapter.canAccess(30L)).isTrue();
        assertThat(adapter.canCreateUnder(0L)).isTrue();
    }

    private void bindRequestUser(Long employeeId) {
        when(requestUser.getUserId()).thenReturn(employeeId);
        SmartRequestUtil.setRequestUser(requestUser);
    }
}
