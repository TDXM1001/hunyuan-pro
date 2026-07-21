package com.hunyuan.sa.admin.bootstrap;

import com.hunyuan.sa.base.module.support.securityprotect.service.SecurityPasswordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class InitialAdminBootstrapService {

    private static final String PLATFORM_ADMIN_ROLE_CODE = "platform_admin";
    private static final int ADMIN_EMPLOYEE_USER_TYPE = 1;

    private final JdbcTemplate jdbcTemplate;

    public InitialAdminBootstrapService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(rollbackFor = Throwable.class)
    public BootstrapResult bootstrap(BootstrapCommand command) {
        validate(command);

        List<Map<String, Object>> matchingEmployees = jdbcTemplate.queryForList("""
                SELECT employee_id, administrator_flag, deleted_flag
                FROM t_employee
                WHERE login_name = ?
                """, command.loginName());
        if (!matchingEmployees.isEmpty()) {
            return handleExistingEmployee(command.loginName(), matchingEmployees.get(0));
        }

        Long activeAdministratorCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_employee
                WHERE administrator_flag = 1 AND deleted_flag = 0
                """, Long.class);
        if (activeAdministratorCount != null && activeAdministratorCount > 0) {
            throw new IllegalStateException(
                    "An active administrator already exists; refusing to create a second bootstrap administrator");
        }

        Long departmentId = resolveDepartmentId(command.departmentId());
        Long roleId = requiredRoleId();
        String employeeUid = UUID.randomUUID().toString().replace("-", "");
        String saltedPassword = command.password()
                + "_" + employeeUid.toUpperCase(Locale.ROOT)
                + "_" + employeeUid.toLowerCase(Locale.ROOT);
        String encryptedPassword = SecurityPasswordService.getEncryptPwd(saltedPassword);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO t_employee
                      (employee_uid, login_name, login_pwd, actual_name, gender, department_id,
                       disabled_flag, deleted_flag, administrator_flag, remark)
                    VALUES (?, ?, ?, ?, 0, ?, 0, 0, 1, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, employeeUid);
            statement.setString(2, command.loginName());
            statement.setString(3, encryptedPassword);
            statement.setString(4, command.actualName());
            statement.setLong(5, departmentId);
            statement.setString(6, "Created by environment-driven initial administrator bootstrap");
            return statement;
        }, keyHolder);

        Number generatedKey = keyHolder.getKey();
        if (generatedKey == null) {
            throw new IllegalStateException("Initial administrator insert did not return an employee id");
        }
        long employeeId = generatedKey.longValue();

        grantPlatformAdministratorRole(employeeId, roleId);
        requirePasswordChange(employeeId, encryptedPassword);
        writeAudit(command.loginName(), "CREATED", employeeId, "REQUIRED");
        log.info("Initial administrator bootstrap completed for loginName={} employeeId={}",
                command.loginName(), employeeId);
        return new BootstrapResult(BootstrapStatus.CREATED, employeeId);
    }

    private BootstrapResult handleExistingEmployee(String loginName, Map<String, Object> employee) {
        long employeeId = ((Number) employee.get("employee_id")).longValue();
        boolean administrator = ((Number) employee.get("administrator_flag")).intValue() == 1;
        boolean deleted = ((Number) employee.get("deleted_flag")).intValue() == 1;
        if (!administrator || deleted) {
            throw new IllegalStateException(
                    "Bootstrap login name already belongs to a non-active administrator account: " + loginName);
        }

        grantPlatformAdministratorRole(employeeId, requiredRoleId());
        writeAudit(loginName, "ALREADY_PRESENT", employeeId, "UNCHANGED");
        log.info("Initial administrator bootstrap found the existing administrator loginName={} employeeId={}",
                loginName, employeeId);
        return new BootstrapResult(BootstrapStatus.ALREADY_PRESENT, employeeId);
    }

    private Long resolveDepartmentId(Long configuredDepartmentId) {
        if (configuredDepartmentId != null) {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM t_department WHERE department_id = ?",
                    Long.class,
                    configuredDepartmentId);
            if (count == null || count != 1) {
                throw new IllegalStateException("Configured bootstrap department does not exist: " + configuredDepartmentId);
            }
            return configuredDepartmentId;
        }

        List<Long> rootDepartmentIds = jdbcTemplate.queryForList("""
                SELECT department_id
                FROM t_department
                WHERE parent_id = 0
                ORDER BY sort, department_id
                LIMIT 2
                """, Long.class);
        if (rootDepartmentIds.size() != 1) {
            throw new IllegalStateException(
                    "Bootstrap requires exactly one root department or HUNYUAN_BOOTSTRAP_ADMIN_DEPARTMENT_ID");
        }
        return rootDepartmentIds.get(0);
    }

    private Long requiredRoleId() {
        List<Long> roleIds = jdbcTemplate.queryForList(
                "SELECT role_id FROM t_role WHERE role_code = ?",
                Long.class,
                PLATFORM_ADMIN_ROLE_CODE);
        if (roleIds.size() != 1) {
            throw new IllegalStateException("Flyway platform seed role is missing: " + PLATFORM_ADMIN_ROLE_CODE);
        }
        return roleIds.get(0);
    }

    private void grantPlatformAdministratorRole(long employeeId, long roleId) {
        jdbcTemplate.update("""
                INSERT IGNORE INTO t_role_employee (role_id, employee_id)
                VALUES (?, ?)
                """, roleId, employeeId);
    }

    private void requirePasswordChange(long employeeId, String encryptedPassword) {
        jdbcTemplate.update("""
                INSERT INTO t_password_log
                  (user_id, user_type, old_password, new_password, create_time, update_time)
                VALUES (?, ?, ?, ?, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 4 MONTH),
                        DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 4 MONTH))
                """, employeeId, ADMIN_EMPLOYEE_USER_TYPE, encryptedPassword, encryptedPassword);
    }

    private void writeAudit(String loginName, String status, long employeeId, String passwordState) {
        jdbcTemplate.update("""
                INSERT IGNORE INTO t_bootstrap_audit
                  (bootstrap_type, subject, status, source, detail)
                VALUES ('INITIAL_ADMIN', ?, ?, 'ENVIRONMENT', ?)
                """, loginName, status,
                "employeeId=" + employeeId
                        + "; passwordChangeState=" + passwordState
                        + "; secretPersisted=false");
    }

    private void validate(BootstrapCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Bootstrap command is required");
        }
        if (command.loginName() == null || !command.loginName().matches("[A-Za-z0-9_-]{4,20}")) {
            throw new IllegalArgumentException("Bootstrap login name must match [A-Za-z0-9_-]{4,20}");
        }
        if (command.actualName() == null || command.actualName().isBlank() || command.actualName().length() > 30) {
            throw new IllegalArgumentException("Bootstrap administrator name must contain 1 to 30 characters");
        }
        if (command.password() == null
                || command.password().length() < 8
                || command.password().length() > 64
                || !command.password().matches(SecurityPasswordService.PASSWORD_PATTERN)) {
            throw new IllegalArgumentException(
                    "Bootstrap password must contain 8 to 64 characters and at least three character categories");
        }
    }

    public record BootstrapCommand(
            String loginName,
            String password,
            String actualName,
            Long departmentId) {

        @Override
        public String toString() {
            return "BootstrapCommand[loginName=" + loginName
                    + ", password=<redacted>, actualName=" + actualName
                    + ", departmentId=" + departmentId + "]";
        }
    }

    public record BootstrapResult(BootstrapStatus status, long employeeId) {
    }

    public enum BootstrapStatus {
        CREATED,
        ALREADY_PRESENT
    }
}
