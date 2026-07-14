package com.hunyuan.sa.bpm.evolution;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class AdminBpmEvolutionControllerSourceTest {
    @Test
    void diffPreviewExecuteAndAuditMustHaveSeparatePermissions() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java", "com", "hunyuan", "sa", "bpm",
                "controller", "admin", "AdminBpmEvolutionController.java"), StandardCharsets.UTF_8);
        assertThat(source).contains("/bpm/evolution/diff", "/bpm/evolution/affected",
                "/bpm/evolution/migration/preview", "/bpm/evolution/migration/{batchId}/execute",
                "/bpm/evolution/migration/item/{itemId}/dispose",
                "bpm:evolution:query", "bpm:evolution:preview", "bpm:evolution:execute", "bpm:evolution:audit");
        assertThat(source).contains("ResponseDTO<BpmMigrationOperationVO> preview",
                "ResponseDTO<BpmMigrationOperationVO> execute",
                "ResponseDTO<BpmMigrationOperationVO> dispose");
        String narrow = Files.readString(Path.of("src", "main", "java", "com", "hunyuan", "sa", "bpm",
                "module", "evolution", "domain", "vo", "BpmMigrationOperationItemVO.java"), StandardCharsets.UTF_8);
        assertThat(narrow).doesNotContain("sourceSnapshotJson", "targetSnapshotJson", "engineCommandEvidenceJson",
                "compensationResult", "executedByEmployeeId", "dispositionByEmployeeId");
    }
}
