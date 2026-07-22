package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.access.authorization.api.AccessAuthorizationSnapshot;
import com.hunyuan.sa.admin.module.access.authorization.api.AccessMenuItem;
import com.hunyuan.sa.admin.module.access.capability.api.AccessCapabilityQueryFacade;
import com.hunyuan.sa.admin.module.access.role.api.AccessRole;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleMembershipFacade;
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
class AccessAuthorizationFacadeAdapterTest {

    @Mock
    private AccessRoleMembershipFacade roleMembershipFacade;

    @Mock
    private AccessCapabilityQueryFacade accessCapabilityQueryFacade;

    private AccessAuthorizationFacadeAdapter facade;

    @BeforeEach
    void setUp() {
        facade = new AccessAuthorizationFacadeAdapter();
        ReflectionTestUtils.setField(facade, "roleMembershipFacade", roleMembershipFacade);
        ReflectionTestUtils.setField(
                facade,
                "accessCapabilityQueryFacade",
                accessCapabilityQueryFacade);
    }

    @Test
    void exposesRoleCapabilitiesAndMenuWithoutLeakingLegacyTypes() {
        AccessRole role = new AccessRole(3L, "财务", "finance", null);

        AccessMenuItem menu = new AccessMenuItem();
        menu.setMenuId(8L);
        menu.setMenuName("财务");
        menu.setPermsType(1);
        menu.setApiPerms("invoice.read,invoice.approve");

        when(roleMembershipFacade.listEmployeeRoles(7L)).thenReturn(List.of(role));
        when(accessCapabilityQueryFacade.listAuthorizationItems(List.of(3L), false))
                .thenReturn(List.of(menu));

        AccessAuthorizationSnapshot snapshot = facade.loadEmployeeAuthorization(7L, false);

        assertThat(snapshot.roleCodes()).containsExactly("finance");
        assertThat(snapshot.capabilityCodes()).containsExactlyInAnyOrder(
                "invoice.read", "invoice.approve");
        assertThat(snapshot.menuItems()).singleElement().satisfies(item -> {
            assertThat(item.getMenuId()).isEqualTo(8L);
            assertThat(item.getMenuName()).isEqualTo("财务");
        });
    }
}
