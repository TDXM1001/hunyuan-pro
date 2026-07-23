package com.hunyuan.sa.admin.module.organization.position.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 岗位引用审计 SQL 的只读契约测试。
 */
@DisplayName("岗位引用审计 SQL")
class PositionReferenceAuditSqlTest {

    private static final String AUDIT_SQL = "db/audit/a3_3_position_reference_audit.sql";

    @Test
    @DisplayName("审计脚本只能包含只读查询")
    void 审计脚本只能包含只读查询() throws IOException {
        String sql = 读取审计脚本();
        String executableSql = sql.lines()
                .filter(line -> !line.stripLeading().startsWith("--"))
                .reduce("", (left, right) -> left + "\n" + right)
                .trim();

        assertThat(executableSql.split(";"))
                .filteredOn(statement -> !statement.isBlank())
                .allSatisfy(statement -> assertThat(statement.stripLeading())
                        .startsWithIgnoringCase("SELECT"));

        String normalizedSql = executableSql.toUpperCase(Locale.ROOT);
        assertThat(normalizedSql)
                .doesNotContain("INSERT ")
                .doesNotContain("UPDATE ")
                .doesNotContain("DELETE ")
                .doesNotContain("REPLACE ")
                .doesNotContain("ALTER ")
                .doesNotContain("CREATE ")
                .doesNotContain("DROP ")
                .doesNotContain("TRUNCATE ")
                .doesNotContain("CALL ")
                .doesNotContain("SET ")
                .doesNotContain("USE ");
    }

    @Test
    @DisplayName("审计脚本覆盖迁移版本、悬空明细和关闭计数")
    void 审计脚本覆盖迁移版本悬空明细和关闭计数() throws IOException {
        String sql = 读取审计脚本().toLowerCase(Locale.ROOT);

        assertThat(sql)
                .contains("flyway_schema_history")
                .contains("t_employee")
                .contains("t_position")
                .contains("employee_id")
                .contains("original_position_id")
                .contains("not exists")
                .contains("dangling_reference_count")
                .doesNotContain("login_pwd")
                .doesNotContain("phone")
                .doesNotContain("email");
    }

    private String 读取审计脚本() throws IOException {
        try (var input = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(AUDIT_SQL),
                "岗位引用审计 SQL 不存在")) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
