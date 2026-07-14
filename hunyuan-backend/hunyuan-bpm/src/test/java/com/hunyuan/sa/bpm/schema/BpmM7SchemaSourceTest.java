package com.hunyuan.sa.bpm.schema;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BpmM7SchemaSourceTest {

    @Test
    void migrationMustContainOperationsGovernanceClosure() throws Exception {
        String sql = Files.readString(
                Path.of("..", "..", "数据库SQL脚本", "mysql", "sql-update-log", "v3.59.0.sql"),
                StandardCharsets.UTF_8
        );

        assertThat(sql).contains(
                "t_bpm_operations_case",
                "t_bpm_operations_action_log",
                "t_bpm_operations_retention_policy",
                "uk_bpm_operations_case_source",
                "uk_bpm_operations_action_idempotency",
                "idx_bpm_operations_case_search",
                "idx_bpm_operations_case_sla",
                "/system/bpm/operations/workbench.vue",
                "bpm:operations:query",
                "bpm:operations:update",
                "bpm:operations:export",
                "bpm:operations:archive"
        );
        assertThat(sql).contains("bpm:operations:high-risk", "bpm:operations:all");
        assertThat(sql).contains("business_evidence_ref_count", "migration_source_ref_count");
    }
}
