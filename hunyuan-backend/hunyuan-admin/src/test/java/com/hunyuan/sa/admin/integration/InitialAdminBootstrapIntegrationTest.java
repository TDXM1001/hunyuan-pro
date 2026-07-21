package com.hunyuan.sa.admin.integration;

import com.hunyuan.sa.admin.bootstrap.InitialAdminBootstrapService;
import com.hunyuan.sa.base.module.support.securityprotect.service.SecurityPasswordService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "HUNYUAN_IT_ENABLED", matches = "(?i)true")
class InitialAdminBootstrapIntegrationTest extends IsolatedInfrastructureTestSupport {

    private static final String LOGIN_NAME = "a1bootstrap";
    private static final String PASSWORD = "A1seed#2026";

    @Test
    void environmentBootstrapShouldCreateOneAuditedAdministratorAndRequirePasswordChange() {
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

        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                requiredEnvironment("HUNYUAN_IT_DB_URL"),
                requiredEnvironment("HUNYUAN_IT_DB_USERNAME"),
                requiredEnvironment("HUNYUAN_IT_DB_PASSWORD"));
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        removeTestAdministrator(jdbcTemplate);

        InitialAdminBootstrapService service = new InitialAdminBootstrapService(jdbcTemplate);
        InitialAdminBootstrapService.BootstrapResult created = service.bootstrap(
                new InitialAdminBootstrapService.BootstrapCommand(
                        LOGIN_NAME,
                        PASSWORD,
                        "A1 Bootstrap",
                        null));

        try {
            assertThat(created.status()).isEqualTo(InitialAdminBootstrapService.BootstrapStatus.CREATED);

            List<String> credentials = jdbcTemplate.queryForList("""
                    SELECT CONCAT(employee_uid, '\t', login_pwd)
                    FROM t_employee
                    WHERE employee_id = ?
                    """, String.class, created.employeeId());
            assertThat(credentials).hasSize(1);
            String[] credentialParts = credentials.get(0).split("\\t", 2);
            String employeeUid = credentialParts[0];
            String encryptedPassword = credentialParts[1];
            String saltedPassword = PASSWORD
                    + "_" + employeeUid.toUpperCase(Locale.ROOT)
                    + "_" + employeeUid.toLowerCase(Locale.ROOT);
            assertThat(SecurityPasswordService.matchesPwd(saltedPassword, encryptedPassword)).isTrue();

            assertThat(jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM t_role_employee re
                    JOIN t_role r ON r.role_id = re.role_id
                    WHERE re.employee_id = ? AND r.role_code = 'platform_admin'
                    """, Integer.class, created.employeeId())).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT TIMESTAMPDIFF(DAY, create_time, CURRENT_TIMESTAMP)
                    FROM t_password_log
                    WHERE user_id = ? AND user_type = 1
                    ORDER BY id DESC LIMIT 1
                    """, Integer.class, created.employeeId())).isGreaterThanOrEqualTo(90);
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM t_bootstrap_audit
                    WHERE bootstrap_type = 'INITIAL_ADMIN'
                      AND subject = ?
                      AND status = 'CREATED'
                    """, Integer.class, LOGIN_NAME)).isEqualTo(1);

            InitialAdminBootstrapService.BootstrapResult repeated = service.bootstrap(
                    new InitialAdminBootstrapService.BootstrapCommand(
                            LOGIN_NAME,
                            "Different#2026A",
                            "A1 Bootstrap",
                            null));
            assertThat(repeated.status())
                    .isEqualTo(InitialAdminBootstrapService.BootstrapStatus.ALREADY_PRESENT);
            assertThat(repeated.employeeId()).isEqualTo(created.employeeId());
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM t_employee WHERE login_name = ?",
                    Integer.class,
                    LOGIN_NAME)).isEqualTo(1);
        } finally {
            removeTestAdministrator(jdbcTemplate);
        }
    }

    private void removeTestAdministrator(JdbcTemplate jdbcTemplate) {
        List<Long> employeeIds = jdbcTemplate.queryForList(
                "SELECT employee_id FROM t_employee WHERE login_name = ?",
                Long.class,
                LOGIN_NAME);
        for (Long employeeId : employeeIds) {
            jdbcTemplate.update("DELETE FROM t_role_employee WHERE employee_id = ?", employeeId);
            jdbcTemplate.update("DELETE FROM t_password_log WHERE user_id = ? AND user_type = 1", employeeId);
            jdbcTemplate.update("DELETE FROM t_employee WHERE employee_id = ?", employeeId);
        }
        jdbcTemplate.update("""
                DELETE FROM t_bootstrap_audit
                WHERE bootstrap_type = 'INITIAL_ADMIN' AND subject = ?
                """, LOGIN_NAME);
    }
}
