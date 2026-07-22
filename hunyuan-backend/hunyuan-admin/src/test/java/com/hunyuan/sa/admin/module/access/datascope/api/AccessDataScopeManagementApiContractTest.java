package com.hunyuan.sa.admin.module.access.datascope.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessDataScopeManagementApiContractTest {

    @Test
    void exposesVersionedDataScopeManagementEndpoints() throws Exception {
        RequestMapping baseMapping =
                AccessDataScopeManagementController.class.getAnnotation(RequestMapping.class);
        assertThat(baseMapping.value()).containsExactly("/api/admin/v1/access");

        assertEndpoint(
                AccessDataScopeManagementController.class.getMethod("list"),
                GetMapping.class,
                "/data-scopes",
                "access.data-scope.read",
                "accessDataScopeList");
        assertEndpoint(
                AccessDataScopeManagementController.class.getMethod("get", Long.class),
                GetMapping.class,
                "/roles/{roleId}/data-scopes",
                "access.data-scope.read",
                "accessRoleDataScopeGet");
        assertEndpoint(
                AccessDataScopeManagementController.class.getMethod(
                        "replace",
                        Long.class,
                        AccessDataScopeManagementController.ReplaceRoleDataScopesRequest.class),
                PutMapping.class,
                "/roles/{roleId}/data-scopes",
                "access.data-scope.update",
                "accessRoleDataScopeReplace");
    }

    @Test
    void stableControllerMapsMissingRoleError() {
        AccessDataScopeManagementFacade facade = mock(AccessDataScopeManagementFacade.class);
        AccessDataScopeManagementController controller =
                new AccessDataScopeManagementController();
        ReflectionTestUtils.setField(controller, "dataScopeManagementFacade", facade);
        when(facade.getRoleDataScopes(99L))
                .thenReturn(AccessDataScopeManagementResult.failure(
                        AccessDataScopeManagementFailure.ROLE_NOT_FOUND,
                        "角色不存在"));

        var response = controller.get(99L);

        assertThat(response.getCode()).isEqualTo(UserErrorCode.DATA_NOT_EXIST.getCode());
    }

    private void assertEndpoint(
            Method method,
            Class<?> mappingType,
            String path,
            String permission,
            String operationId) {
        if (mappingType == GetMapping.class) {
            assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly(path);
        } else {
            assertThat(method.getAnnotation(PutMapping.class).value()).containsExactly(path);
        }
        assertThat(method.getAnnotation(SaCheckPermission.class).value()).containsExactly(permission);
        assertThat(method.getAnnotation(Operation.class).operationId()).isEqualTo(operationId);
    }
}
