package com.hunyuan.sa.admin.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "HUNYUAN_IT_ENABLED", matches = "(?i)true")
class FlywayMigrationTest extends IsolatedInfrastructureTestSupport {

    @Test
    void emptyDatabaseShouldMigrateToPlatformSeedWithoutCredentialsOrBpmTables() {
        Flyway prePositionMigrationFlyway = Flyway.configure()
                .dataSource(
                        requiredEnvironment("HUNYUAN_IT_DB_URL"),
                        requiredEnvironment("HUNYUAN_IT_DB_USERNAME"),
                        requiredEnvironment("HUNYUAN_IT_DB_PASSWORD"))
                .locations("classpath:db/migration")
                .placeholderReplacement(false)
                .cleanDisabled(true)
                .target(MigrationVersion.fromVersion("3.71.0"))
                .load();

        prePositionMigrationFlyway.migrate();

        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                requiredEnvironment("HUNYUAN_IT_DB_URL"),
                requiredEnvironment("HUNYUAN_IT_DB_USERNAME"),
                requiredEnvironment("HUNYUAN_IT_DB_PASSWORD"));
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // 在 3.71.0 隔离库中复制最小悬空样本，同时验证引用修复和最终权限退役。
        jdbcTemplate.update("""
                INSERT INTO t_employee
                    (employee_id, employee_uid, login_name, login_pwd, actual_name,
                     department_id, position_id, disabled_flag, deleted_flag)
                VALUES
                    (66, 'a3-3-it-employee-66', 'luoyi', '仅用于隔离迁移测试', '罗伊',
                     4, 2, 1, 0),
                    (67, 'a3-3-it-employee-67', 'chuxiao', '仅用于隔离迁移测试', '初晓',
                     1, 2, 1, 0)
                """);
        jdbcTemplate.update("""
                INSERT IGNORE INTO t_role_menu (role_id, menu_id, create_time, update_time)
                VALUES (1, 138, NOW(), NOW())
                """);

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

        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("3.78.0");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_employee
                WHERE employee_id IN (66, 67)
                  AND position_id IS NULL
                """, Integer.class)).isEqualTo(2);

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
                Integer.class)).isEqualTo(15);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_employee",
                Integer.class)).isEqualTo(2);
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
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_role_menu
                WHERE menu_id IN (132, 138, 142, 149, 150, 185, 186, 187, 188)
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
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_menu
                WHERE api_perms LIKE 'organization.position.%'
                  AND web_perms = api_perms
                  AND menu_type IN (2, 3)
                  AND perms_type = 1
                  AND deleted_flag = 0
                """, Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_role_menu role_menu
                JOIN t_role role ON role.role_id = role_menu.role_id
                JOIN t_menu menu ON menu.menu_id = role_menu.menu_id
                WHERE role.role_code = 'platform_admin'
                  AND menu.api_perms LIKE 'organization.position.%'
                """, Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_menu
                WHERE api_perms LIKE 'system:position:%'
                   OR web_perms LIKE 'system:position:%'
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_role_menu role_menu
                JOIN t_menu menu ON menu.menu_id = role_menu.menu_id
                WHERE menu.api_perms LIKE 'system:position:%'
                   OR menu.web_perms LIKE 'system:position:%'
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_menu WHERE path = '/organization/position' AND deleted_flag = 0",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_position",
                Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = ?
                  AND table_name = 't_position'
                  AND index_name = 'uk_position_active_name'
                  AND non_unique = 0
                """, Integer.class, databaseName)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_name IN ('t_goods', 't_category')
                """, Integer.class, databaseName)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_menu
                WHERE path IN ('/goods', '/erp/goods/list', '/erp/catalog/goods', '/erp/catalog/custom')
                   OR api_perms LIKE 'goods:%'
                   OR web_perms LIKE 'goods:%'
                   OR api_perms LIKE 'category:%'
                   OR web_perms LIKE 'category:%'
                   OR web_perms LIKE 'custom:category:%'
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_dict WHERE dict_code = 'GOODS_PLACE'",
                Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_name IN (
                      't_oa_enterprise',
                      't_oa_enterprise_employee',
                      't_oa_bank',
                      't_oa_invoice'
                  )
                """, Integer.class, databaseName)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_menu
                WHERE menu_id IN (144, 145)
                   OR api_perms LIKE 'oa:enterprise:%'
                   OR web_perms LIKE 'oa:enterprise:%'
                   OR api_perms LIKE 'oa:bank:%'
                   OR web_perms LIKE 'oa:bank:%'
                   OR api_perms LIKE 'oa:invoice:%'
                   OR web_perms LIKE 'oa:invoice:%'
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_name IN (
                      't_notice',
                      't_notice_type',
                      't_notice_view_record',
                      't_notice_visible_range'
                  )
                """, Integer.class, databaseName)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_menu
                WHERE menu_id IN (132, 138, 142, 149, 150, 185, 186, 187, 188)
                   OR api_perms LIKE 'oa:notice:%'
                   OR web_perms LIKE 'oa:notice:%'
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_name IN (
                      't_help_doc',
                      't_help_doc_catalog',
                      't_help_doc_relation',
                      't_help_doc_view_record',
                      't_feedback',
                      't_data_tracer'
                  )
                """, Integer.class, databaseName)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_menu
                WHERE path IN (
                        '/help-doc/help-doc-manage-list',
                        '/feedback/feedback-list'
                    )
                   OR api_perms LIKE 'support:helpDoc:%'
                   OR api_perms LIKE 'support:helpDocCatalog:%'
                   OR web_perms LIKE 'support:helpDoc:%'
                   OR web_perms LIKE 'support:helpDocCatalog:%'
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_name = 't_mail_template'
                """, Integer.class, databaseName)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_menu
                WHERE menu_type = 2
                  AND path = '/support/level3protect/data-masking-list'
                  AND component = '/support/level3protect/data-masking-list.vue'
                  AND deleted_flag = 0
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_menu
                WHERE menu_type = 3
                  AND perms_type = 1
                  AND api_perms = 'support:protect:dataMasking:query'
                  AND web_perms = api_perms
                  AND deleted_flag = 0
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_role_menu role_menu
                JOIN t_role role ON role.role_id = role_menu.role_id
                JOIN t_menu menu ON menu.menu_id = role_menu.menu_id
                WHERE role.role_code = 'platform_admin'
                  AND (
                       (menu.menu_type = 2 AND menu.path = '/support/level3protect/data-masking-list')
                    OR (menu.menu_type = 3 AND menu.api_perms = 'support:protect:dataMasking:query')
                  )
                """, Integer.class)).isEqualTo(2);
    }
}
