package com.hunyuan.sa.bpm.schema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BpmVisualConfigurationSchemaSourceTest {

    @Test
    void migrationShouldAddPolicyDisplayColumnsAndSplitPermissionsIdempotently() throws IOException {
        String sql = Files.readString(Path.of(
                "../../数据库SQL脚本/mysql/sql-update-log/v3.62.0.sql"
        ));

        assertThat(sql).contains(
                "policy_name", "description", "business_summary", "calculated_risk_level",
                "bpm:policy-catalog:save", "bpm:policy-catalog:simulate",
                "bpm:policy-catalog:technical", "bpm:policy-catalog:delete",
                "审批规则", "information_schema.columns", "ON DUPLICATE KEY UPDATE",
                "WHERE NOT EXISTS"
        );
    }
}
