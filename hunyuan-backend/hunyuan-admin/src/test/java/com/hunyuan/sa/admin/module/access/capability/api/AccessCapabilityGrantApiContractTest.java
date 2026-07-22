package com.hunyuan.sa.admin.module.access.capability.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.admin.module.system.role.controller.RoleMenuController;
import com.hunyuan.sa.admin.module.system.role.domain.form.RoleMenuUpdateForm;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessCapabilityGrantApiContractTest {

    @Test
    void exposesVersionedRoleCapabilityEndpoints() throws Exception {
        RequestMapping baseMapping = AccessCapabilityGrantController.class.getAnnotation(RequestMapping.class);
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
    void legacyControllerDependsOnAccessCapabilityFacade() throws Exception {
        Field facadeField = RoleMenuController.class.getDeclaredField("capabilityGrantFacade");

        assertThat(facadeField.getType()).isEqualTo(AccessCapabilityGrantFacade.class);
        assertThat(RoleMenuController.class.getDeclaredFields())
                .extracting(Field::getType)
                .noneMatch(type -> type.getName().endsWith(".RoleMenuService"));
    }

    @Test
    void legacyControllerPreservesMissingRoleError() {
        AccessCapabilityGrantFacade facade = mock(AccessCapabilityGrantFacade.class);
        RoleMenuController controller = new RoleMenuController();
        ReflectionTestUtils.setField(controller, "capabilityGrantFacade", facade);

        RoleMenuUpdateForm form = new RoleMenuUpdateForm();
        form.setRoleId(99L);
        form.setMenuIdList(List.of(1L));
        when(facade.replaceRoleCapabilities(
                new ReplaceRoleCapabilitiesCommand(99L, List.of(1L))))
                .thenReturn(AccessCapabilityGrantResult.failure(
                        AccessCapabilityGrantFailure.ROLE_NOT_FOUND));

        var response = controller.updateRoleMenu(form);

        assertThat(response.getCode()).isEqualTo(UserErrorCode.DATA_NOT_EXIST.getCode());
    }

    @Test
    void legacyControllerMapsStableCapabilityTree() {
        AccessCapabilityGrantFacade facade = mock(AccessCapabilityGrantFacade.class);
        RoleMenuController controller = new RoleMenuController();
        ReflectionTestUtils.setField(controller, "capabilityGrantFacade", facade);
        AccessCapabilityNode child = new AccessCapabilityNode(
                2L, "角色管理", null, 1L, 1, List.of());
        AccessCapabilityNode root = new AccessCapabilityNode(
                1L, "系统管理", null, 0L, 1, List.of(child));
        when(facade.getRoleCapabilities(9L))
                .thenReturn(new AccessRoleCapabilityGrant(9L, List.of(2L), List.of(root)));

        var response = controller.getRoleSelectedMenu(9L);

        assertThat(response.getData().getRoleId()).isEqualTo(9L);
        assertThat(response.getData().getSelectedMenuId()).containsExactly(2L);
        assertThat(response.getData().getMenuTreeList().get(0).getChildren())
                .extracting(node -> node.getMenuId())
                .containsExactly(2L);
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
