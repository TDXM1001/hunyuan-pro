package com.hunyuan.sa.admin.module.access.role.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessRoleApiContractTest {

    @Test
    void exposesVersionedRoleLifecycleEndpoints() throws Exception {
        RequestMapping baseMapping = AccessRoleController.class.getAnnotation(RequestMapping.class);
        assertThat(baseMapping.value()).containsExactly("/api/admin/v1/access/roles");

        assertEndpoint(
                AccessRoleController.class.getMethod("list"),
                GetMapping.class,
                "",
                "access.role.read",
                "accessRoleList");
        assertEndpoint(
                AccessRoleController.class.getMethod("get", Long.class),
                GetMapping.class,
                "/{roleId}",
                "access.role.read",
                "accessRoleGet");
        assertEndpoint(
                AccessRoleController.class.getMethod(
                        "create", AccessRoleController.AccessRoleRequest.class),
                PostMapping.class,
                "",
                "access.role.create",
                "accessRoleCreate");
        assertEndpoint(
                AccessRoleController.class.getMethod(
                        "update", Long.class, AccessRoleController.AccessRoleRequest.class),
                PutMapping.class,
                "/{roleId}",
                "access.role.update",
                "accessRoleUpdate");
        assertEndpoint(
                AccessRoleController.class.getMethod("delete", Long.class),
                DeleteMapping.class,
                "/{roleId}",
                "access.role.delete",
                "accessRoleDelete");
    }

    @Test
    void stableControllerPreservesDuplicateAndNotFoundErrors() {
        AccessRoleLifecycleFacade facade = mock(AccessRoleLifecycleFacade.class);
        AccessRoleController controller = new AccessRoleController();
        ReflectionTestUtils.setField(controller, "roleLifecycleFacade", facade);

        AccessRoleController.AccessRoleRequest request =
                new AccessRoleController.AccessRoleRequest("审计员", "auditor", null);
        when(facade.create(new CreateAccessRoleCommand("审计员", "auditor", null)))
                .thenReturn(AccessRoleResult.failure(
                        AccessRoleFailure.ROLE_CODE_DUPLICATED,
                        "角色编码重复"));

        var duplicateResponse = controller.create(request);
        assertThat(duplicateResponse.getCode()).isEqualTo(UserErrorCode.PARAM_ERROR.getCode());
        assertThat(duplicateResponse.getMsg()).isEqualTo("角色编码重复");

        when(facade.get(99L))
                .thenReturn(AccessRoleResult.failure(AccessRoleFailure.ROLE_NOT_FOUND, null));

        var missingResponse = controller.get(99L);
        assertThat(missingResponse.getCode()).isEqualTo(UserErrorCode.DATA_NOT_EXIST.getCode());
    }

    private void assertEndpoint(
            Method method,
            Class<?> mappingType,
            String path,
            String permission,
            String operationId) {
        String[] paths;
        if (mappingType == GetMapping.class) {
            paths = method.getAnnotation(GetMapping.class).value();
        } else if (mappingType == PostMapping.class) {
            paths = method.getAnnotation(PostMapping.class).value();
        } else if (mappingType == PutMapping.class) {
            paths = method.getAnnotation(PutMapping.class).value();
        } else {
            paths = method.getAnnotation(DeleteMapping.class).value();
        }
        if (path.isEmpty()) {
            assertThat(paths).isEmpty();
        } else {
            assertThat(paths).containsExactly(path);
        }
        assertThat(method.getAnnotation(SaCheckPermission.class).value()).containsExactly(permission);
        assertThat(method.getAnnotation(Operation.class).operationId()).isEqualTo(operationId);
    }
}
