package com.hunyuan.sa.bpm.module.evolution.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BpmMigrationItemVO {
    private Long migrationItemId;
    private Long instanceId;
    private String itemStatus;
    private String blockersJson;
    private String sourceSnapshotJson;
    private String targetSnapshotJson;
    private String engineCommandEvidenceJson;
    private String failureReason;
    private String compensationResult;
    private Long executedByEmployeeId;
    private Long dispositionByEmployeeId;
    private LocalDateTime disposedAt;
    private LocalDateTime migratedAt;
}
