package com.hunyuan.sa.admin.module.access.capability.api;

import com.hunyuan.sa.admin.module.access.authorization.api.AccessMenuItem;
import com.hunyuan.sa.admin.module.system.role.service.AccessAuthorizationFacadeAdapter;
import com.hunyuan.sa.admin.module.system.role.service.AccessCapabilityQueryFacadeAdapter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccessCapabilityQueryApiContractTest {

    @Test
    void exposesAuthorizationItemsWithoutLegacyMenuTypes() throws Exception {
        Method method = AccessCapabilityQueryFacade.class.getMethod(
                "listAuthorizationItems",
                List.class,
                Boolean.class);

        assertThat(method.getReturnType()).isEqualTo(List.class);
        assertThat(method.getGenericReturnType().getTypeName())
                .contains(AccessMenuItem.class.getName())
                .doesNotContain(".system.menu.");
        assertThat(method.getGenericParameterTypes())
                .extracting(type -> type.getTypeName())
                .noneMatch(type -> type.contains(".system.menu."));
    }

    @Test
    void authorizationAdapterUsesCapabilityQueryBoundary() throws Exception {
        Field queryFacadeField =
                AccessAuthorizationFacadeAdapter.class.getDeclaredField(
                        "accessCapabilityQueryFacade");

        assertThat(queryFacadeField.getType()).isEqualTo(AccessCapabilityQueryFacade.class);
        assertThat(AccessCapabilityQueryFacade.class)
                .isAssignableFrom(AccessCapabilityQueryFacadeAdapter.class);
    }
}
