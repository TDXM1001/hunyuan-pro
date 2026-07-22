package com.hunyuan.sa.admin.module.access;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyAccessConsumerRetirementTest {

    private static final Path MAIN_JAVA = Path.of("src", "main", "java");

    @Test
    void legacyControllersAndModelsAreRemoved() {
        List<Path> retiredSources = List.of(
                source("system", "role", "controller", "RoleController.java"),
                source("system", "role", "controller", "RoleEmployeeController.java"),
                source("system", "role", "controller", "RoleMenuController.java"),
                source("system", "role", "controller", "RoleDataScopeController.java"),
                source("system", "menu", "controller", "MenuController.java"),
                source("system", "datascope", "DataScopeController.java"),
                source("system", "role", "domain", "form", "RoleAddForm.java"),
                source("system", "role", "domain", "form", "RoleUpdateForm.java"),
                source("system", "role", "domain", "form", "RoleQueryForm.java"),
                source("system", "role", "domain", "form", "RoleEmployeeQueryForm.java"),
                source("system", "role", "domain", "form", "RoleEmployeeUpdateForm.java"),
                source("system", "role", "domain", "form", "RoleMenuUpdateForm.java"),
                source("system", "role", "domain", "form", "RoleDataScopeUpdateForm.java"),
                source("system", "role", "domain", "vo", "RoleCapabilityTreeNodeVO.java"),
                source("system", "role", "domain", "vo", "RoleDataScopeVO.java"),
                source("system", "role", "domain", "vo", "RoleEmployeeVO.java"),
                source("system", "role", "domain", "vo", "RoleMenuTreeVO.java"),
                source("system", "role", "domain", "vo", "RoleSelectedVO.java"),
                source("system", "menu", "domain", "form", "MenuAddForm.java"),
                source("system", "menu", "domain", "form", "MenuUpdateForm.java"),
                source("system", "menu", "domain", "vo", "MenuTreeVO.java"),
                source("system", "datascope", "domain", "DataScopeAndViewTypeVO.java"),
                source("system", "datascope", "domain", "DataScopeViewTypeVO.java"));

        assertThat(retiredSources)
                .as("旧访问控制兼容类必须保持删除状态")
                .allMatch(path -> Files.notExists(path));
    }

    @Test
    void productionSourcesOnlyExposeStableAccessRoutesAndPermissions() throws IOException {
        try (var files = Files.walk(MAIN_JAVA)) {
            for (Path sourceFile : files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList()) {
                assertThat(Files.readString(sourceFile, StandardCharsets.UTF_8))
                        .as("旧访问控制入口和权限码必须保持退役：%s", sourceFile)
                        .doesNotContain(
                                "\"/role/",
                                "\"/menu/",
                                "\"/dataScope/",
                                "system:role:",
                                "system:menu:");
            }
        }

        assertStableController(
                "role/api/AccessRoleController.java",
                "@RequestMapping(\"/api/admin/v1/access/roles\")");
        assertStableController(
                "role/api/AccessRoleMembershipController.java",
                "@RequestMapping(\"/api/admin/v1/access\")");
        assertStableController(
                "capability/api/AccessCapabilityGrantController.java",
                "@RequestMapping(\"/api/admin/v1/access/roles/{roleId}/capabilities\")");
        assertStableController(
                "menu/api/AccessMenuController.java",
                "@RequestMapping(\"/api/admin/v1/access/menus\")");
        assertStableController(
                "datascope/api/AccessDataScopeManagementController.java",
                "@RequestMapping(\"/api/admin/v1/access\")");
    }

    private void assertStableController(String relativePath, String route) throws IOException {
        Path controller = MAIN_JAVA.resolve(Path.of(
                "com", "hunyuan", "sa", "admin", "module", "access", relativePath));
        assertThat(Files.readString(controller, StandardCharsets.UTF_8)).contains(route);
    }

    private Path source(String... relativeSegments) {
        Path path = MAIN_JAVA.resolve(Path.of(
                "com", "hunyuan", "sa", "admin", "module"));
        for (String segment : relativeSegments) {
            path = path.resolve(segment);
        }
        return path;
    }
}
