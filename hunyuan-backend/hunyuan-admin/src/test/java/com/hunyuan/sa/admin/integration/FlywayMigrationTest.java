package com.hunyuan.sa.admin.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "HUNYUAN_IT_ENABLED", matches = "(?i)true")
class FlywayMigrationTest extends IsolatedInfrastructureTestSupport {

    @Test
    void emptyDatabaseShouldMigrateToPlatformSeedWithoutCredentialsOrBpmTables() {
        Flyway flyway = Flyway.configure()
                .dataSource(
                        requiredEnvironment("HUNYUAN_IT_DB_URL"),
                        requiredEnvironment("HUNYUAN_IT_DB_USERNAME"),
                        requiredEnvironment("HUNYUAN_IT_DB_PASSWORD"))
                .locations("classpath:db/migration")
                .placeholderReplacement(false)
                .cleanDisabled(true)
                .load();

        flyway.migrate();

        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("3.71.0");

        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                requiredEnvironment("HUNYUAN_IT_DB_URL"),
                requiredEnvironment("HUNYUAN_IT_DB_USERNAME"),
                requiredEnvironment("HUNYUAN_IT_DB_PASSWORD"));
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        assertThat(databaseName).endsWith("_it");
        Integer retiredTableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND (table_name LIKE 't_bpm\\_%' OR table_name LIKE 'act\\_%')
                """, Integer.class, databaseName);

        assertThat(retiredTableCount).isZero();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_config WHERE config_key IN ('super_password', 'level3_protect_config')",
                Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT config_value FROM t_config WHERE config_key = 'super_password'",
                String.class)).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_role WHERE role_code = 'platform_admin'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_role_data_scope ds
                JOIN t_role r ON r.role_id = ds.role_id
                WHERE r.role_code = 'platform_admin'
                  AND ds.data_scope_type = 1
                  AND ds.view_type = 10
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_dict WHERE dict_code IN ('SYS_GENDER', 'SYS_DISABLED_FLAG')",
                Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_menu WHERE deleted_flag = 0 AND menu_type IN (1, 2)",
                Integer.class)).isEqualTo(14);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_employee",
                Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT config_value FROM t_config WHERE config_key = 'module.organization.directory.enabled'",
                String.class)).isEqualTo("true");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_menu WHERE path = '/organization/directory' AND api_perms = 'organization.department.read'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_menu WHERE path = '/organization/directory' AND perms_type = 1",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_menu WHERE api_perms LIKE 'organization.department.%' AND perms_type = 1",
                Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_menu WHERE path = '/organization/department'",
                Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_menu WHERE api_perms LIKE 'system:department:%' OR web_perms LIKE 'system:department:%'",
                Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_role_menu role_menu
                JOIN t_menu menu ON menu.menu_id = role_menu.menu_id
                WHERE menu.path = '/organization/department'
                   OR menu.api_perms LIKE 'system:department:%'
                   OR menu.web_perms LIKE 'system:department:%'
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_role_data_scope scope
                JOIN t_role role ON role.role_id = scope.role_id
                WHERE role.role_code = 'platform_admin'
                  AND scope.data_scope_type = 2
                  AND scope.view_type = 10
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = ?
                  AND table_name = 't_employee'
                  AND index_name = 'idx_employee_directory_state'
                """, Integer.class, databaseName)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = ?
                  AND table_name = 't_employee'
                  AND index_name IN ('uk_employee_login_name', 'employee_uid_index')
                  AND non_unique = 0
                """, Integer.class, databaseName)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_menu WHERE api_perms LIKE 'identity.employee.%' AND menu_type = 3",
                Integer.class)).isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_menu WHERE api_perms LIKE 'system:employee:%' OR web_perms LIKE 'system:employee:%'",
                Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_menu WHERE path = '/organization/employee' AND deleted_flag = 0",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_role_menu role_menu
                JOIN t_role role ON role.role_id = role_menu.role_id
                JOIN t_menu menu ON menu.menu_id = role_menu.menu_id
                WHERE role.role_code = 'platform_admin'
                  AND menu.api_perms LIKE 'identity.employee.%'
                """, Integer.class)).isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_menu
                WHERE api_perms LIKE 'access.%'
                  AND web_perms = api_perms
                  AND menu_type = 3
                  AND perms_type = 1
                  AND deleted_flag = 0
                """, Integer.class)).isEqualTo(15);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_menu
                WHERE api_perms LIKE 'system:role:%'
                   OR web_perms LIKE 'system:role:%'
                   OR api_perms LIKE 'system:menu:%'
                   OR web_perms LIKE 'system:menu:%'
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_role_menu role_menu
                JOIN t_menu menu ON menu.menu_id = role_menu.menu_id
                WHERE menu.api_perms LIKE 'system:role:%'
                   OR menu.web_perms LIKE 'system:role:%'
                   OR menu.api_perms LIKE 'system:menu:%'
                   OR menu.web_perms LIKE 'system:menu:%'
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_menu WHERE path = '/organization/role' AND deleted_flag = 0",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_menu WHERE path = '/menu/list' AND deleted_flag = 0",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_role_menu role_menu
                JOIN t_role role ON role.role_id = role_menu.role_id
                JOIN t_menu menu ON menu.menu_id = role_menu.menu_id
                WHERE role.role_code = 'platform_admin'
                  AND menu.api_perms LIKE 'access.%'
                """, Integer.class)).isEqualTo(15);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = ?
                  AND table_name = 't_role_menu'
                  AND index_name = 'uk_role_menu_role_menu'
                  AND non_unique = 0
                """, Integer.class, databaseName)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = ?
                  AND table_name = 't_role_data_scope'
                  AND index_name = 'uk_role_data_scope_type'
                  AND non_unique = 0
                """, Integer.class, databaseName)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_role_data_scope scope
                JOIN t_role role ON role.role_id = scope.role_id
                WHERE role.role_code = 'platform_admin'
                  AND scope.data_scope_type = 1
                  AND scope.view_type = 10
                """, Integer.class)).isEqualTo(1);
    }
}
