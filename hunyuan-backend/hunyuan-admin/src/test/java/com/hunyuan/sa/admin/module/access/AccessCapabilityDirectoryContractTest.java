package com.hunyuan.sa.admin.module.access;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.admin.module.access.capability.api.AccessCapabilityGrantController;
import com.hunyuan.sa.admin.module.access.datascope.api.AccessDataScopeManagementController;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenuController;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleController;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleMembershipController;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AccessCapabilityDirectoryContractTest {

    private static final Set<String> FROZEN_CAPABILITIES = Set.of(
            "access.role.read",
            "access.role.create",
            "access.role.update",
            "access.role.delete",
            "access.role.employee.read",
            "access.role.employee.assign",
            "access.role.employee.remove",
            "access.capability.read",
            "access.capability.grant",
            "access.menu.read",
            "access.menu.create",
            "access.menu.update",
            "access.menu.delete",
            "access.data-scope.read",
            "access.data-scope.update");

    private static final Set<Class<?>> ACCESS_CONTROLLERS = Set.of(
            AccessRoleController.class,
            AccessRoleMembershipController.class,
            AccessCapabilityGrantController.class,
            AccessMenuController.class,
            AccessDataScopeManagementController.class);

    private static final Pattern ACCESS_CAPABILITY_PATTERN =
            Pattern.compile("'(access\\.[a-z0-9.-]+)'");

    @Test
    void controllerPermissionsShouldMatchFrozenCapabilityDirectory() {
        Set<String> controllerCapabilities = ACCESS_CONTROLLERS.stream()
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods()))
                .map(method -> method.getAnnotation(SaCheckPermission.class))
                .filter(annotation -> annotation != null)
                .flatMap(annotation -> Arrays.stream(annotation.value()))
                .filter(permission -> permission.startsWith("access."))
                .collect(Collectors.toSet());

        assertThat(controllerCapabilities).containsExactlyInAnyOrderElementsOf(FROZEN_CAPABILITIES);
    }

    @Test
    void flywayCapabilityDirectoryShouldMatchControllerPermissions() throws IOException {
        var migration = new ClassPathResource(
                "db/migration/V3_70_0__a3_2_access_capability_and_constraints.sql");
        String sql;
        try (var inputStream = migration.getInputStream()) {
            sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        Set<String> migrationCapabilities = ACCESS_CAPABILITY_PATTERN.matcher(sql)
                .results()
                .map(result -> result.group(1))
                .collect(Collectors.toSet());

        assertThat(migrationCapabilities).containsExactlyInAnyOrderElementsOf(FROZEN_CAPABILITIES);
    }
}
