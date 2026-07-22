package com.hunyuan.sa.admin.module.identity.employee.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyEmployeeConsumerRetirementTest {

    @Test
    void migratedConsumersNoLongerDependOnLegacyEmployeeImplementation() throws IOException {
        List<Path> roots = List.of(
                Path.of("src", "main", "java", "com", "hunyuan", "sa", "admin", "module", "system", "login"),
                Path.of("src", "main", "java", "com", "hunyuan", "sa", "admin", "module", "organization"),
                Path.of("src", "main", "java", "com", "hunyuan", "sa", "admin", "module", "system", "datascope"),
                Path.of("src", "main", "java", "com", "hunyuan", "sa", "admin", "module", "business", "oa", "notice")
        );

        for (Path root : roots) {
            try (var files = Files.walk(root)) {
                for (Path sourceFile : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    assertThat(Files.readString(sourceFile, StandardCharsets.UTF_8))
                            .as("legacy employee implementation must stay retired: %s", sourceFile)
                            .doesNotContain("module.system.employee");
                }
            }
        }
    }

    @Test
    void legacyEmployeeManagementEndpointsAndPermissionsAreRetired() throws IOException {
        Path legacyController = Path.of(
                "src", "main", "java", "com", "hunyuan", "sa", "admin",
                "module", "system", "employee", "controller", "EmployeeController.java");
        String legacyControllerSource = Files.readString(legacyController, StandardCharsets.UTF_8);

        assertThat(legacyControllerSource)
                .contains(
                        "\"/employee/update/center\"",
                        "\"/employee/update/avatar\"",
                        "\"/employee/update/password\"",
                        "\"/employee/getPasswordComplexityEnabled\"")
                .doesNotContain(
                        "\"/employee/query\"",
                        "\"/employee/add\"",
                        "\"/employee/update\"",
                        "\"/employee/update/disabled/",
                        "\"/employee/update/batch/delete\"",
                        "\"/employee/update/batch/department\"",
                        "\"/employee/update/password/reset/",
                        "\"/employee/getAllEmployeeByDepartmentId/",
                        "\"/employee/queryAll\"");

        Path identityController = Path.of(
                "src", "main", "java", "com", "hunyuan", "sa", "admin",
                "module", "identity", "employee", "api", "EmployeeDirectoryController.java");
        String identityControllerSource = Files.readString(identityController, StandardCharsets.UTF_8);
        assertThat(identityControllerSource)
                .contains(
                        "@RequestMapping(\"/api/admin/v1/identity/employees\")",
                        "@PostMapping(\"/query\")",
                        "@PostMapping",
                        "@PutMapping(\"/{employeeId}\")",
                        "@PostMapping(\"/{employeeId}/enable\")",
                        "@PostMapping(\"/{employeeId}/disable\")",
                        "@PostMapping(\"/department-assignment\")",
                        "@PostMapping(\"/delete\")",
                        "@PostMapping(\"/{employeeId}/password/reset\")");

        List<Path> sourceRoots = List.of(Path.of("src", "main", "java"));
        for (Path root : sourceRoots) {
            try (var files = Files.walk(root)) {
                for (Path sourceFile : files.filter(Files::isRegularFile).toList()) {
                    assertThat(Files.readString(sourceFile, StandardCharsets.UTF_8))
                            .as("legacy employee permission must stay retired: %s", sourceFile)
                            .doesNotContain("system:employee:");
                }
            }
        }
    }
}
