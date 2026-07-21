package com.hunyuan.sa.admin.integration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationContractTest {

    @Test
    void migrationSetShouldBeCompleteAndDatabaseScoped() throws IOException {
        Path migrationDirectory = new ClassPathResource("db/migration").getFile().toPath();
        List<Path> migrations;
        try (var files = Files.list(migrationDirectory)) {
            migrations = files
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList();
        }

        assertThat(migrations).hasSize(5);
        assertThat(migrations.get(0).getFileName().toString())
                .isEqualTo("V3_64_0__current_schema_baseline.sql");
        assertThat(migrations.get(1).getFileName().toString())
                .isEqualTo("V3_65_0__platform_seed.sql");
        assertThat(migrations.get(2).getFileName().toString())
                .isEqualTo("V3_66_0__a2_organization_directory.sql");
        assertThat(migrations.get(3).getFileName().toString())
                .isEqualTo("V3_66_1__a2_organization_permission_type.sql");
        assertThat(migrations.get(4).getFileName().toString())
                .isEqualTo("V3_67_0__a2_1_retire_legacy_department_access.sql");

        String baseline = Files.readString(migrations.get(0), StandardCharsets.UTF_8).toUpperCase();
        assertThat(baseline)
                .doesNotContain("DROP DATABASE")
                .doesNotContain("CREATE DATABASE")
                .doesNotContain("USE `HUNYUAN`");

        assertThat(baseline)
                .contains("CREATE TABLE `T_EMPLOYEE`")
                .contains("CREATE TABLE `T_MENU`")
                .doesNotContain("INSERT INTO")
                .doesNotContain("CREATE TABLE `T_BPM_")
                .doesNotContain("CREATE TABLE `ACT_");

        String platformSeed = Files.readString(migrations.get(1), StandardCharsets.UTF_8).toUpperCase();
        assertThat(platformSeed)
                .contains("CREATE TABLE `T_BOOTSTRAP_AUDIT`")
                .contains("'PLATFORM_ADMIN'")
                .contains("'SYS_GENDER'")
                .contains("'SYS_DISABLED_FLAG'")
                .contains("'SYSTEM:POSITION:ADD'")
                .contains("'SUPPORT:PROTECT:LEVEL3:UPDATE'")
                .doesNotContain("INSERT INTO `T_EMPLOYEE`")
                .doesNotContain("INSERT INTO T_EMPLOYEE")
                .doesNotContain("GOODS_PLACE");

        String organizationDirectory = Files.readString(migrations.get(2), StandardCharsets.UTF_8).toUpperCase();
        assertThat(organizationDirectory)
                .contains("'MODULE.ORGANIZATION.DIRECTORY.ENABLED'")
                .contains("'/ORGANIZATION/DIRECTORY'")
                .contains("'ORGANIZATION.DEPARTMENT.READ'")
                .contains("'ORGANIZATION.DEPARTMENT.CREATE'")
                .contains("'ORGANIZATION.DEPARTMENT.UPDATE'")
                .contains("'ORGANIZATION.DEPARTMENT.DELETE'")
                .doesNotContain("INSERT INTO `T_EMPLOYEE`");

        String organizationPermissionType = Files.readString(migrations.get(3), StandardCharsets.UTF_8).toUpperCase();
        assertThat(organizationPermissionType)
                .contains("`PERMS_TYPE` = 1")
                .contains("'/ORGANIZATION/DIRECTORY'")
                .contains("'ORGANIZATION.DEPARTMENT.%'");

        String legacyDepartmentRetirement = Files.readString(migrations.get(4), StandardCharsets.UTF_8).toUpperCase();
        assertThat(legacyDepartmentRetirement)
                .contains("'/ORGANIZATION/EMPLOYEE'")
                .contains("'ORGANIZATION.DEPARTMENT.READ'")
                .contains("'SYSTEM:DEPARTMENT:ADD'")
                .contains("DELETE ROLE_MENU")
                .contains("DELETE FROM `T_MENU`");
    }
}
