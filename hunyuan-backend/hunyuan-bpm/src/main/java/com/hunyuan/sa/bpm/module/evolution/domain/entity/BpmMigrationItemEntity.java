package com.hunyuan.sa.bpm.module.evolution.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_bpm_migration_item")
public class BpmMigrationItemEntity {
    @TableId(type = IdType.AUTO) private Long migrationItemId;
    private Long migrationBatchId;
    private Long instanceId;
    private String idempotencyKey;
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
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
