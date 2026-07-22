package com.hunyuan.sa.admin.integration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeCapabilityMigrationContractTest {

    @Test
    void employeeMigrationShouldKeepCompatibilityAndStableCapabilities() throws IOException {
        var migration = new ClassPathResource(
                "db/migration/V3_68_0__a3_1_employee_capability_and_constraints.sql");
        String sql;
        try (var inputStream = migration.getInputStream()) {
            sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        String normalized = sql.toUpperCase();

        assertThat(normalized)
                .contains("T_EMPLOYEE")
                .contains("T_ROLE_MENU")
                .contains("PLATFORM_ADMIN")
                .contains("IDX_EMPLOYEE_DIRECTORY_STATE")
                .contains("SYSTEM:EMPLOYEE:ADD")
                .contains("IDENTITY.EMPLOYEE.READ")
                .contains("IDENTITY.EMPLOYEE.CREATE")
                .contains("IDENTITY.EMPLOYEE.UPDATE")
                .contains("IDENTITY.EMPLOYEE.ENABLE")
                .contains("IDENTITY.EMPLOYEE.DISABLE")
                .contains("IDENTITY.EMPLOYEE.DEPARTMENT.ASSIGN")
                .contains("IDENTITY.EMPLOYEE.DELETE")
                .contains("IDENTITY.EMPLOYEE.PASSWORD.RESET")
                .doesNotContain("DROP TABLE")
                .doesNotContain("DELETE FROM `T_EMPLOYEE`")
                .doesNotContain("DROP INDEX `UK_EMPLOYEE_LOGIN_NAME`")
                .doesNotContain("DROP INDEX `EMPLOYEE_UID_INDEX`");
    }
}
