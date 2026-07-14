package com.hunyuan.sa.bpm.module.evolution.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BpmMigrationBatchDetailVO {
    private Long migrationBatchId;
    private String batchCode;
    private String idempotencyKey;
    private Long sourceVersionId;
    private Long targetVersionId;
    private String batchStatus;
    private String mappingJson;
    private String dataMappingJson;
    private String diffSnapshotJson;
    private String reason;
    private Long actorEmployeeId;
    private Long confirmedByEmployeeId;
    private int totalCount;
    private int eligibleCount;
    private int blockedCount;
    private int succeededCount;
    private int failedCount;
    private LocalDateTime previewedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime completedAt;
    private List<BpmMigrationItemVO> items;
}
