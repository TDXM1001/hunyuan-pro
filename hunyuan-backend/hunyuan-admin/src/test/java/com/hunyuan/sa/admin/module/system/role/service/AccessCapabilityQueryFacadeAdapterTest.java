package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.organization.OrganizationModuleAvailability;
import com.hunyuan.sa.admin.module.system.menu.dao.MenuDao;
import com.hunyuan.sa.admin.module.system.menu.domain.entity.MenuEntity;
import com.hunyuan.sa.admin.module.system.menu.domain.vo.MenuVO;
import com.hunyuan.sa.admin.module.system.role.dao.RoleMenuDao;
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
    private RoleMenuDao roleMenuDao;

    @Mock
    private MenuDao menuDao;

    @Mock
    private OrganizationModuleAvailability organizationModuleAvailability;

    private AccessCapabilityQueryFacadeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AccessCapabilityQueryFacadeAdapter();
        ReflectionTestUtils.setField(adapter, "roleMenuDao", roleMenuDao);
        ReflectionTestUtils.setField(adapter, "menuDao", menuDao);
        ReflectionTestUtils.setField(
                adapter,
                "organizationModuleAvailability",
                organizationModuleAvailability);
    }

    @Test
    void administratorQueryIncludesActionCapabilities() {
        MenuVO action = new MenuVO();
        action.setMenuType(3);
        action.setApiPerms("organization.department.create");
        action.setWebPerms("organization.department.create");
        when(menuDao.queryMenuList(false, false, null)).thenReturn(List.of(action));
        when(organizationModuleAvailability.enabled()).thenReturn(true);

        var result = adapter.listAuthorizationItems(List.of(), true);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getMenuType()).isEqualTo(3);
            assertThat(item.getApiPerms()).isEqualTo("organization.department.create");
        });
        verify(menuDao).queryMenuList(false, false, null);
        verify(roleMenuDao, never())
                .selectMenuListByRoleIdList(
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void nonAdministratorWithoutRolesReturnsEmptyItems() {
        var result = adapter.listAuthorizationItems(List.of(), false);

        assertThat(result).isEmpty();
        verify(menuDao, never()).queryMenuList(false, false, null);
        verify(roleMenuDao, never())
                .selectMenuListByRoleIdList(
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void nonAdministratorQueryMapsLegacyMenuEntities() {
        MenuEntity menu = new MenuEntity();
        menu.setMenuId(8L);
        menu.setMenuName("财务");
        menu.setApiPerms("invoice.read");
        when(roleMenuDao.selectMenuListByRoleIdList(List.of(3L), false))
                .thenReturn(List.of(menu));
        when(organizationModuleAvailability.enabled()).thenReturn(true);

        var result = adapter.listAuthorizationItems(List.of(3L), false);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getMenuId()).isEqualTo(8L);
            assertThat(item.getMenuName()).isEqualTo("财务");
            assertThat(item.getApiPerms()).isEqualTo("invoice.read");
        });
    }

    @Test
    void disabledOrganizationModuleFiltersMenuAndCapability() {
        MenuVO systemMenu = menu(1L, "/system/role", null);
        MenuVO organizationMenu = menu(2L, "/organization/directory", null);
        MenuVO organizationAction =
                menu(3L, null, "organization.department.create");
        when(menuDao.queryMenuList(false, false, null))
                .thenReturn(List.of(systemMenu, organizationMenu, organizationAction));
        when(organizationModuleAvailability.enabled()).thenReturn(false);

        var result = adapter.listAuthorizationItems(List.of(), true);

        assertThat(result)
                .extracting(item -> item.getMenuId())
                .containsExactly(1L);
    }

    private MenuVO menu(Long menuId, String path, String apiPerms) {
        MenuVO menu = new MenuVO();
        menu.setMenuId(menuId);
        menu.setPath(path);
        menu.setApiPerms(apiPerms);
        return menu;
    }
}
