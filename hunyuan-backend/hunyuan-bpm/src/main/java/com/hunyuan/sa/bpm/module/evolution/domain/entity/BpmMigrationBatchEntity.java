package com.hunyuan.sa.bpm.module.evolution.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_bpm_migration_batch")
public class BpmMigrationBatchEntity {
    @TableId(type = IdType.AUTO) private Long migrationBatchId;
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
    private String executionOwnerKey;
    private LocalDateTime executionLeaseUntil;
    private Integer totalCount;
    private Integer eligibleCount;
    private Integer blockedCount;
    private Integer succeededCount;
    private Integer failedCount;
    private LocalDateTime previewedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime completedAt;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
