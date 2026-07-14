package com.hunyuan.sa.bpm.schema;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class BpmM8SchemaSourceTest {
    @Test
    void migrationMustContainEvolutionAndRestrictedMigrationClosure() throws Exception {
        String sql = Files.readString(Path.of("..", "..", "数据库SQL脚本", "mysql", "sql-update-log", "v3.60.0.sql"),
                StandardCharsets.UTF_8);
        assertThat(sql).contains(
                "t_bpm_migration_batch", "t_bpm_migration_item",
                "uk_bpm_migration_batch_idempotency", "uk_bpm_migration_item_instance",
                "idx_bpm_migration_batch_versions", "idx_bpm_migration_item_status",
                "/system/bpm/evolution/workbench.vue",
                "bpm:evolution:query", "bpm:evolution:preview", "bpm:evolution:execute", "bpm:evolution:audit");
        assertThat(sql).contains("source_snapshot_json", "target_snapshot_json",
                "engine_command_evidence_json", "compensation_result",
                "confirmed_by_employee_id", "execution_lease_until", "executed_by_employee_id",
                "disposition_by_employee_id");
    }
}
