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

        assertThat(migrations).hasSize(17);
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
        assertThat(migrations.get(5).getFileName().toString())
                .isEqualTo("V3_68_0__a3_1_employee_capability_and_constraints.sql");
        assertThat(migrations.get(6).getFileName().toString())
                .isEqualTo("V3_69_0__a3_1_retire_legacy_employee_access.sql");
        assertThat(migrations.get(7).getFileName().toString())
                .isEqualTo("V3_70_0__a3_2_access_capability_and_constraints.sql");
        assertThat(migrations.get(8).getFileName().toString())
                .isEqualTo("V3_71_0__a3_2_retire_legacy_access.sql");
        assertThat(migrations.get(9).getFileName().toString())
                .isEqualTo("V3_72_0__a3_3_repair_dangling_position_references.sql");
        assertThat(migrations.get(10).getFileName().toString())
                .isEqualTo("V3_72_1__a3_3_position_capability_and_constraints.sql");
        assertThat(migrations.get(11).getFileName().toString())
                .isEqualTo("V3_73_0__a3_3_retire_legacy_position_access.sql");
        assertThat(migrations.get(12).getFileName().toString())
                .isEqualTo("V3_74_0__a3_4_retire_goods_and_category_examples.sql");
        assertThat(migrations.get(13).getFileName().toString())
                .isEqualTo("V3_75_0__a3_4_retire_oa_master_data.sql");
        assertThat(migrations.get(14).getFileName().toString())
                .isEqualTo("V3_76_0__a3_4_retire_oa_notice.sql");
        assertThat(migrations.get(15).getFileName().toString())
                .isEqualTo("V3_76_1__a3_4_remove_retired_oa_notice_parent_grants.sql");
        assertThat(migrations.get(16).getFileName().toString())
                .isEqualTo("V3_77_0__a3_4_data_masking_validation_permission.sql");

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

        String employeeMigration = Files.readString(migrations.get(5), StandardCharsets.UTF_8).toUpperCase();
        assertThat(employeeMigration)
                .contains("IDX_EMPLOYEE_DIRECTORY_STATE")
                .contains("'IDENTITY.EMPLOYEE.READ'")
                .contains("'IDENTITY.EMPLOYEE.CREATE'")
                .contains("'IDENTITY.EMPLOYEE.UPDATE'")
                .contains("'IDENTITY.EMPLOYEE.ENABLE'")
                .contains("'IDENTITY.EMPLOYEE.DISABLE'")
                .contains("'IDENTITY.EMPLOYEE.DEPARTMENT.ASSIGN'")
                .contains("'IDENTITY.EMPLOYEE.DELETE'")
                .contains("'IDENTITY.EMPLOYEE.PASSWORD.RESET'")
                .contains("'SYSTEM:EMPLOYEE:DISABLED'");

        String legacyEmployeeRetirement = Files.readString(migrations.get(6), StandardCharsets.UTF_8).toUpperCase();
        assertThat(legacyEmployeeRetirement)
                .contains("'SYSTEM:EMPLOYEE:%'")
                .contains("DELETE ROLE_MENU")
                .contains("DELETE FROM `T_MENU`")
                .doesNotContain("'/ORGANIZATION/EMPLOYEE'")
                .doesNotContain("'IDENTITY.EMPLOYEE.%'")
                .doesNotContain("DELETE FROM `T_EMPLOYEE`");

        String accessMigration = Files.readString(migrations.get(7), StandardCharsets.UTF_8).toUpperCase();
        assertThat(accessMigration)
                .contains("UK_ROLE_MENU_ROLE_MENU")
                .contains("UK_ROLE_DATA_SCOPE_TYPE")
                .contains("'ACCESS.ROLE.READ'")
                .contains("'ACCESS.ROLE.CREATE'")
                .contains("'ACCESS.ROLE.UPDATE'")
                .contains("'ACCESS.ROLE.DELETE'")
                .contains("'ACCESS.ROLE.EMPLOYEE.READ'")
                .contains("'ACCESS.ROLE.EMPLOYEE.ASSIGN'")
                .contains("'ACCESS.ROLE.EMPLOYEE.REMOVE'")
                .contains("'ACCESS.CAPABILITY.READ'")
                .contains("'ACCESS.CAPABILITY.GRANT'")
                .contains("'ACCESS.MENU.READ'")
                .contains("'ACCESS.MENU.CREATE'")
                .contains("'ACCESS.MENU.UPDATE'")
                .contains("'ACCESS.MENU.DELETE'")
                .contains("'ACCESS.DATA-SCOPE.READ'")
                .contains("'ACCESS.DATA-SCOPE.UPDATE'")
                .contains("'SYSTEM:ROLE:EMPLOYEE:BATCH:DELETE'")
                .contains("'SYSTEM:ROLE:MENU:UPDATE'")
                .contains("'SYSTEM:ROLE:DATASCOPE:UPDATE'")
                .contains("'SYSTEM:MENU:BATCHDELETE'")
                .doesNotContain("DELETE FROM `T_MENU`");

        String legacyAccessRetirement =
                Files.readString(migrations.get(8), StandardCharsets.UTF_8).toUpperCase();
        assertThat(legacyAccessRetirement)
                .contains("'SYSTEM:ROLE:%'")
                .contains("'SYSTEM:MENU:%'")
                .contains("DELETE ROLE_MENU")
                .contains("DELETE FROM `T_MENU`")
                .doesNotContain("'ACCESS.%'")
                .doesNotContain("'/ORGANIZATION/ROLE'")
                .doesNotContain("'/MENU/LIST'");

        String positionReferenceRepair =
                Files.readString(migrations.get(9), StandardCharsets.UTF_8).toUpperCase();
        assertThat(positionReferenceRepair)
                .contains("`EMPLOYEE_ID` = 66")
                .contains("`EMPLOYEE_ID` = 67")
                .contains("`DISABLED_FLAG` = 1")
                .contains("`POSITION_ID` = 2")
                .contains("SET `POSITION_ID` = NULL")
                .doesNotContain("SET `POSITION_ID` = 3")
                .doesNotContain("INSERT INTO `T_EMPLOYEE`")
                .doesNotContain("DELETE FROM `T_EMPLOYEE`");

        String positionCapabilityMigration =
                Files.readString(migrations.get(10), StandardCharsets.UTF_8).toUpperCase();
        assertThat(positionCapabilityMigration)
                .contains("ACTIVE_POSITION_NAME")
                .contains("UK_POSITION_ACTIVE_NAME")
                .contains("'ORGANIZATION.POSITION.READ'")
                .contains("'ORGANIZATION.POSITION.CREATE'")
                .contains("'ORGANIZATION.POSITION.UPDATE'")
                .contains("'ORGANIZATION.POSITION.DELETE'")
                .contains("'SYSTEM:POSITION:ADD'")
                .contains("'SYSTEM:POSITION:UPDATE'")
                .contains("'SYSTEM:POSITION:DELETE'")
                .doesNotContain("DELETE FROM `T_MENU`")
                .doesNotContain("DELETE FROM `T_POSITION`");

        String legacyPositionRetirement =
                Files.readString(migrations.get(11), StandardCharsets.UTF_8).toUpperCase();
        assertThat(legacyPositionRetirement)
                .contains("'SYSTEM:POSITION:%'")
                .contains("DELETE ROLE_MENU")
                .contains("DELETE FROM `T_MENU`")
                .doesNotContain("'ORGANIZATION.POSITION.%'")
                .doesNotContain("'/ORGANIZATION/POSITION'")
                .doesNotContain("DELETE FROM `T_POSITION`");

        String legacyExampleRetirement =
                Files.readString(migrations.get(12), StandardCharsets.UTF_8).toUpperCase();
        assertThat(legacyExampleRetirement)
                .contains("'GOODS:%'")
                .contains("'CATEGORY:%'")
                .contains("'CUSTOM:CATEGORY:%'")
                .contains("'GOODS_PLACE'")
                .contains("DELETE ROLE_MENU")
                .contains("DELETE FROM `T_MENU`")
                .contains("DROP TABLE IF EXISTS `T_GOODS`")
                .contains("DROP TABLE IF EXISTS `T_CATEGORY`")
                .doesNotContain("'OA:%'")
                .doesNotContain("'SUPPORT:%'");

        String oaMasterDataRetirement =
                Files.readString(migrations.get(13), StandardCharsets.UTF_8).toUpperCase();
        assertThat(oaMasterDataRetirement)
                .contains("'OA:ENTERPRISE:QUERY'")
                .contains("'OA:BANK:QUERY'")
                .contains("'OA:INVOICE:QUERY'")
                .contains("`MENU_ID` IN (144, 145)")
                .contains("WHERE `TYPE` = 3")
                .contains("DROP TABLE IF EXISTS `T_OA_BANK`")
                .contains("DROP TABLE IF EXISTS `T_OA_INVOICE`")
                .contains("DROP TABLE IF EXISTS `T_OA_ENTERPRISE_EMPLOYEE`")
                .contains("DROP TABLE IF EXISTS `T_OA_ENTERPRISE`")
                .doesNotContain("'OA:NOTICE:")
                .doesNotContain("DROP TABLE IF EXISTS `T_NOTICE")
                .doesNotContain("`MENU_ID` = 138");

        String oaNoticeRetirement =
                Files.readString(migrations.get(14), StandardCharsets.UTF_8).toUpperCase();
        assertThat(oaNoticeRetirement)
                .contains("'OA:NOTICE:%'")
                .contains("`MENU_ID` IN (132, 142, 149, 150, 185, 186, 187, 188)")
                .contains("PARENT.`MENU_ID` = 138")
                .contains("WHERE `TYPE` = 2")
                .contains("DROP TABLE IF EXISTS `T_NOTICE_VISIBLE_RANGE`")
                .contains("DROP TABLE IF EXISTS `T_NOTICE_VIEW_RECORD`")
                .contains("DROP TABLE IF EXISTS `T_NOTICE`")
                .contains("DROP TABLE IF EXISTS `T_NOTICE_TYPE`")
                .doesNotContain("DROP TABLE IF EXISTS `T_MESSAGE")
                .doesNotContain("'SUPPORT:MESSAGE:")
                .doesNotContain("'SUPPORT:SMS:");

        String oaNoticeParentGrantCleanup =
                Files.readString(migrations.get(15), StandardCharsets.UTF_8).toUpperCase();
        assertThat(oaNoticeParentGrantCleanup)
                .contains("ROLE_MENU.`MENU_ID` = 138")
                .contains("MENU.`MENU_ID` IS NULL")
                .doesNotContain("DELETE FROM `T_MENU`")
                .doesNotContain("DROP TABLE");

        String dataMaskingValidationPermission =
                Files.readString(migrations.get(16), StandardCharsets.UTF_8).toUpperCase();
        assertThat(dataMaskingValidationPermission)
                .contains("'/SUPPORT/LEVEL3PROTECT/DATA-MASKING-LIST'")
                .contains("'/SUPPORT/LEVEL3PROTECT/DATA-MASKING-LIST.VUE'")
                .contains("'SUPPORT:PROTECT:DATAMASKING:QUERY'")
                .contains("'PLATFORM_ADMIN'")
                .contains("MENU.`MENU_TYPE` = 2")
                .contains("MENU.`MENU_TYPE` = 3")
                .doesNotContain("DROP TABLE")
                .doesNotContain("DELETE FROM `T_MENU`");
    }
}
