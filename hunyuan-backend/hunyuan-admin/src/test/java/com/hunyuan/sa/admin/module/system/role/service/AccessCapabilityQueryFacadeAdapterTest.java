package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.organization.OrganizationModuleAvailability;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenu;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenuQueryFacade;
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
class AccessCapabilityQueryFacadeAdapterTest {

    @Mock
    private AccessMenuQueryFacade accessMenuQueryFacade;

    @Mock
    private OrganizationModuleAvailability organizationModuleAvailability;

    private AccessCapabilityQueryFacadeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AccessCapabilityQueryFacadeAdapter();
        ReflectionTestUtils.setField(adapter, "accessMenuQueryFacade", accessMenuQueryFacade);
        ReflectionTestUtils.setField(
                adapter,
                "organizationModuleAvailability",
                organizationModuleAvailability);
    }

    @Test
    void administratorQueryIncludesActionCapabilities() {
        AccessMenu action = menu(
                1L,
                null,
                "organization.department.create",
                "organization.department.create");
        when(accessMenuQueryFacade.listEnabledMenus()).thenReturn(List.of(action));
        when(organizationModuleAvailability.enabled()).thenReturn(true);

        var result = adapter.listAuthorizationItems(List.of(), true);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getMenuType()).isEqualTo(3);
            assertThat(item.getApiPerms()).isEqualTo("organization.department.create");
        });
        verify(accessMenuQueryFacade).listEnabledMenus();
        verify(accessMenuQueryFacade, never())
                .listAuthorizedMenusByRoleIds(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void nonAdministratorWithoutRolesReturnsEmptyItems() {
        var result = adapter.listAuthorizationItems(List.of(), false);

        assertThat(result).isEmpty();
        verify(accessMenuQueryFacade, never()).listEnabledMenus();
        verify(accessMenuQueryFacade, never())
                .listAuthorizedMenusByRoleIds(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void nonAdministratorQueryMapsPublicMenuModels() {
        AccessMenu menu = menu(8L, null, "invoice.read", null);
        when(accessMenuQueryFacade.listAuthorizedMenusByRoleIds(List.of(3L)))
                .thenReturn(List.of(menu));
        when(organizationModuleAvailability.enabled()).thenReturn(true);

        var result = adapter.listAuthorizationItems(List.of(3L), false);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getMenuId()).isEqualTo(8L);
            assertThat(item.getApiPerms()).isEqualTo("invoice.read");
        });
    }

    @Test
    void disabledOrganizationModuleFiltersMenuAndCapability() {
        AccessMenu systemMenu = menu(1L, "/system/role", null, null);
        AccessMenu organizationMenu = menu(2L, "/organization/directory", null, null);
        AccessMenu organizationAction =
                menu(3L, null, "organization.department.create", null);
        when(accessMenuQueryFacade.listEnabledMenus())
                .thenReturn(List.of(systemMenu, organizationMenu, organizationAction));
        when(organizationModuleAvailability.enabled()).thenReturn(false);

        var result = adapter.listAuthorizationItems(List.of(), true);

        assertThat(result)
                .extracting(item -> item.getMenuId())
                .containsExactly(1L);
    }

    private AccessMenu menu(Long menuId, String path, String apiPerms, String webPerms) {
        return new AccessMenu(
                menuId,
                menuId.equals(8L) ? "财务" : null,
                3,
                0L,
                null,
                path,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                webPerms,
                apiPerms,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
