package com.hunyuan.sa.admin.module.access.menu.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.admin.module.system.menu.controller.MenuController;
import com.hunyuan.sa.base.common.code.SystemErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessMenuApiContractTest {

    @Test
    void exposesVersionedMenuCatalogEndpoints() throws Exception {
        RequestMapping baseMapping = AccessMenuController.class.getAnnotation(RequestMapping.class);
        assertThat(baseMapping.value()).containsExactly("/api/admin/v1/access/menus");

        assertEndpoint(
                AccessMenuController.class.getMethod("list"),
                GetMapping.class,
                "",
                "access.menu.read",
                "accessMenuList");
        assertEndpoint(
                AccessMenuController.class.getMethod("get", Long.class),
                GetMapping.class,
                "/{menuId}",
                "access.menu.read",
                "accessMenuGet");
        assertEndpoint(
                AccessMenuController.class.getMethod("tree", Boolean.class),
                GetMapping.class,
                "/tree",
                "access.menu.read",
                "accessMenuTree");
        assertEndpoint(
                AccessMenuController.class.getMethod("listAuthorizationUrls"),
                GetMapping.class,
                "/authorization-urls",
                "access.menu.read",
                "accessMenuAuthorizationUrlList");
        assertEndpoint(
                AccessMenuController.class.getMethod(
                        "create", AccessMenuController.AccessMenuRequest.class),
                PostMapping.class,
                "",
                "access.menu.create",
                "accessMenuCreate");
        assertEndpoint(
                AccessMenuController.class.getMethod(
                        "update", Long.class, AccessMenuController.AccessMenuRequest.class),
                PutMapping.class,
                "/{menuId}",
                "access.menu.update",
                "accessMenuUpdate");
        assertEndpoint(
                AccessMenuController.class.getMethod(
                        "delete", AccessMenuController.DeleteAccessMenusRequest.class),
                DeleteMapping.class,
                "",
                "access.menu.delete",
                "accessMenuDelete");
    }

    @Test
    void legacyControllerDependsOnAccessMenuFacade() throws Exception {
        Field facadeField = MenuController.class.getDeclaredField("menuCatalogFacade");

        assertThat(facadeField.getType()).isEqualTo(AccessMenuCatalogFacade.class);
        assertThat(MenuController.class.getDeclaredFields())
                .extracting(Field::getType)
                .noneMatch(type -> type.getName().endsWith(".MenuService"));
    }

    @Test
    void legacyControllerPreservesDetailErrorAndResponseModel() {
        AccessMenuCatalogFacade facade = mock(AccessMenuCatalogFacade.class);
        MenuController controller = new MenuController();
        ReflectionTestUtils.setField(controller, "menuCatalogFacade", facade);

        when(facade.get(99L)).thenReturn(AccessMenuResult.failure(
                AccessMenuFailure.MENU_NOT_FOUND,
                "菜单不存在"));

        var missingResponse = controller.getMenuDetail(99L);
        assertThat(missingResponse.getCode()).isEqualTo(SystemErrorCode.SYSTEM_ERROR.getCode());
        assertThat(missingResponse.getMsg()).isEqualTo("菜单不存在");

        when(facade.list()).thenReturn(List.of(menu(1L, "系统管理", 0L)));
        var listResponse = controller.queryMenuList();
        assertThat(listResponse.getData()).singleElement()
                .satisfies(menu -> {
                    assertThat(menu.getMenuId()).isEqualTo(1L);
                    assertThat(menu.getMenuName()).isEqualTo("系统管理");
                });
    }

    private AccessMenu menu(Long menuId, String menuName, Long parentId) {
        return new AccessMenu(
                menuId,
                menuName,
                1,
                parentId,
                1,
                null,
                null,
                false,
                null,
                false,
                true,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
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
