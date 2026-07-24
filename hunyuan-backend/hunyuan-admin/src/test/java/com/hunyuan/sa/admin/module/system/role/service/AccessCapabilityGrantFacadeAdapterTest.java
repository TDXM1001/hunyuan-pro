package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.access.capability.api.AccessCapabilityGrantFailure;
import com.hunyuan.sa.admin.module.access.capability.api.ReplaceRoleCapabilitiesCommand;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenu;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenuQueryFacade;
import com.hunyuan.sa.admin.module.organization.OrganizationModuleAvailability;
import com.hunyuan.sa.admin.module.system.role.dao.RoleDao;
import com.hunyuan.sa.admin.module.system.role.dao.RoleMenuDao;
import com.hunyuan.sa.admin.module.system.role.domain.entity.RoleEntity;
import com.hunyuan.sa.admin.module.system.role.domain.entity.RoleMenuEntity;
import com.hunyuan.sa.admin.module.system.role.manager.RoleMenuManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessCapabilityGrantFacadeAdapterTest {

    @Mock
    private RoleDao roleDao;

    @Mock
    private RoleMenuDao roleMenuDao;

    @Mock
    private RoleMenuManager roleMenuManager;

    @Mock
    private AccessMenuQueryFacade accessMenuQueryFacade;

    @Mock
    private OrganizationModuleAvailability organizationModuleAvailability;

    private AccessCapabilityGrantFacadeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AccessCapabilityGrantFacadeAdapter();
        ReflectionTestUtils.setField(adapter, "roleDao", roleDao);
        ReflectionTestUtils.setField(adapter, "roleMenuDao", roleMenuDao);
        ReflectionTestUtils.setField(adapter, "roleMenuManager", roleMenuManager);
        ReflectionTestUtils.setField(adapter, "accessMenuQueryFacade", accessMenuQueryFacade);
        ReflectionTestUtils.setField(adapter, "organizationModuleAvailability", organizationModuleAvailability);
    }

    @Test
    void replaceRejectsMissingRole() {
        when(roleDao.selectById(9L)).thenReturn(null);

        var result = adapter.replaceRoleCapabilities(
                new ReplaceRoleCapabilitiesCommand(9L, List.of(11L)));

        assertThat(result.successful()).isFalse();
        assertThat(result.failure()).isEqualTo(AccessCapabilityGrantFailure.ROLE_NOT_FOUND);
        verify(roleMenuManager, never()).updateRoleMenu(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void replaceMapsCapabilityIdsToLegacyRelations() {
        when(roleDao.selectById(9L)).thenReturn(new RoleEntity());
        ArgumentCaptor<List<RoleMenuEntity>> grantsCaptor = ArgumentCaptor.forClass(List.class);

        var result = adapter.replaceRoleCapabilities(
                new ReplaceRoleCapabilitiesCommand(9L, List.of(11L, 12L)));

        assertThat(result.successful()).isTrue();
        verify(roleMenuManager).updateRoleMenu(org.mockito.ArgumentMatchers.eq(9L), grantsCaptor.capture());
        assertThat(grantsCaptor.getValue())
                .extracting(RoleMenuEntity::getRoleId, RoleMenuEntity::getMenuId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(9L, 11L),
                        org.assertj.core.groups.Tuple.tuple(9L, 12L));
    }

    @Test
    void replaceAllowsClearingAllCapabilities() {
        when(roleDao.selectById(9L)).thenReturn(new RoleEntity());

        var result = adapter.replaceRoleCapabilities(
                new ReplaceRoleCapabilitiesCommand(9L, List.of()));

        assertThat(result.successful()).isTrue();
        verify(roleMenuManager).updateRoleMenu(9L, List.of());
    }

    @Test
    void queryBuildsCapabilityTreeAndPreservesSelectedIds() {
        AccessMenu root = menu(1L, 0L, "系统管理", null, null);
        AccessMenu child = menu(2L, 1L, "角色管理", "/system/role", null);
        when(roleMenuDao.queryMenuIdByRoleId(9L)).thenReturn(List.of(2L));
        when(accessMenuQueryFacade.listEnabledMenus()).thenReturn(List.of(root, child));
        when(organizationModuleAvailability.enabled()).thenReturn(true);

        var grant = adapter.getRoleCapabilities(9L);

        assertThat(grant.roleId()).isEqualTo(9L);
        assertThat(grant.selectedCapabilityIds()).containsExactly(2L);
        assertThat(grant.capabilityTree()).hasSize(1);
        assertThat(grant.capabilityTree().get(0).capabilityName()).isEqualTo("系统管理");
        assertThat(grant.capabilityTree().get(0).children())
                .extracting(node -> node.capabilityId())
                .containsExactly(2L);
    }

    @Test
    void queryFiltersOrganizationCapabilitiesWhenModuleIsDisabled() {
        AccessMenu systemRoot = menu(1L, 0L, "系统管理", null, null);
        AccessMenu organizationMenu = menu(2L, 0L, "组织架构", "/organization/directory", null);
        AccessMenu organizationAction = menu(
                3L,
                1L,
                "新增部门",
                null,
                "organization.department.create");
        when(roleMenuDao.queryMenuIdByRoleId(9L)).thenReturn(List.of(1L));
        when(accessMenuQueryFacade.listEnabledMenus())
                .thenReturn(List.of(systemRoot, organizationMenu, organizationAction));
        when(organizationModuleAvailability.enabled()).thenReturn(false);

        var grant = adapter.getRoleCapabilities(9L);

        assertThat(grant.capabilityTree())
                .extracting(node -> node.capabilityId())
                .containsExactly(1L);
        assertThat(grant.capabilityTree().get(0).children()).isEmpty();
    }

    private AccessMenu menu(Long menuId, Long parentId, String name, String path, String apiPerms) {
        return new AccessMenu(
                menuId,
                name,
                1,
                parentId,
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
                null,
                apiPerms,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
