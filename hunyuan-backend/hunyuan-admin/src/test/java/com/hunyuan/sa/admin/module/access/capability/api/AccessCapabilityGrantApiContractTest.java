package com.hunyuan.sa.admin.module.access.capability.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessCapabilityGrantApiContractTest {

    @Test
    void exposesVersionedRoleCapabilityEndpoints() throws Exception {
        RequestMapping baseMapping =
                AccessCapabilityGrantController.class.getAnnotation(RequestMapping.class);
        assertThat(baseMapping.value())
                .containsExactly("/api/admin/v1/access/roles/{roleId}/capabilities");

        assertEndpoint(
                AccessCapabilityGrantController.class.getMethod("get", Long.class),
                GetMapping.class,
                "access.capability.read",
                "accessRoleCapabilityGet");
        assertEndpoint(
                AccessCapabilityGrantController.class.getMethod(
                        "replace",
                        Long.class,
                        AccessCapabilityGrantController.ReplaceRoleCapabilitiesRequest.class),
                PutMapping.class,
                "access.capability.grant",
                "accessRoleCapabilityReplace");
    }

    @Test
    void stableControllerMapsMissingRoleError() {
        AccessCapabilityGrantFacade facade = mock(AccessCapabilityGrantFacade.class);
        AccessCapabilityGrantController controller = new AccessCapabilityGrantController();
        ReflectionTestUtils.setField(controller, "capabilityGrantFacade", facade);
        when(facade.replaceRoleCapabilities(
                new ReplaceRoleCapabilitiesCommand(99L, List.of(1L))))
                .thenReturn(AccessCapabilityGrantResult.failure(
                        AccessCapabilityGrantFailure.ROLE_NOT_FOUND));

        var response = controller.replace(
                99L,
                new AccessCapabilityGrantController.ReplaceRoleCapabilitiesRequest(List.of(1L)));

        assertThat(response.getCode()).isEqualTo(UserErrorCode.DATA_NOT_EXIST.getCode());
    }

    private void assertEndpoint(
            Method method,
            Class<?> mappingType,
            String permission,
            String operationId) {
        if (mappingType == GetMapping.class) {
            assertThat(method.getAnnotation(GetMapping.class).value()).isEmpty();
        } else {
            assertThat(method.getAnnotation(PutMapping.class).value()).isEmpty();
        }
        assertThat(method.getAnnotation(SaCheckPermission.class).value()).containsExactly(permission);
        assertThat(method.getAnnotation(Operation.class).operationId()).isEqualTo(operationId);
    }
}
