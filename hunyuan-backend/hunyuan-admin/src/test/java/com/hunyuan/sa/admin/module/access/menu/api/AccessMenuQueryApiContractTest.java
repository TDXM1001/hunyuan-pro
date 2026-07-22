package com.hunyuan.sa.admin.module.access.menu.api;

import com.hunyuan.sa.admin.module.system.role.service.AccessCapabilityGrantFacadeAdapter;
import com.hunyuan.sa.admin.module.system.role.service.AccessCapabilityQueryFacadeAdapter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccessMenuQueryApiContractTest {

    @Test
    void exposesOnlyAccessMenuModels() throws Exception {
        Method allMenus = AccessMenuQueryFacade.class.getMethod("listEnabledMenus");
        Method roleMenus = AccessMenuQueryFacade.class.getMethod(
                "listAuthorizedMenusByRoleIds",
                List.class);

        assertThat(allMenus.getGenericReturnType().getTypeName())
                .contains(AccessMenu.class.getName())
                .doesNotContain(".system.menu.");
        assertThat(roleMenus.getGenericReturnType().getTypeName())
                .contains(AccessMenu.class.getName())
                .doesNotContain(".system.menu.");
    }

    @Test
    void capabilityAdaptersUseMenuQueryBoundary() throws Exception {
        assertMenuQueryField(AccessCapabilityGrantFacadeAdapter.class);
        assertMenuQueryField(AccessCapabilityQueryFacadeAdapter.class);
    }

    private void assertMenuQueryField(Class<?> adapterType) throws Exception {
        Field field = adapterType.getDeclaredField("accessMenuQueryFacade");

        assertThat(field.getType()).isEqualTo(AccessMenuQueryFacade.class);
        assertThat(adapterType.getDeclaredFields())
                .extracting(Field::getType)
                .noneMatch(type -> type.getName().contains(".system.menu."));
    }
}
