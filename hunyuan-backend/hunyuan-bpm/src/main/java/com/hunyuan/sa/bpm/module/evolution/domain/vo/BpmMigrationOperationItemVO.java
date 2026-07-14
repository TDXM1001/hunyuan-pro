package com.hunyuan.sa.bpm.module.evolution.domain.vo;

import lombok.Data;

@Data
public class BpmMigrationOperationItemVO {
    private Long migrationItemId;
    private Long instanceId;
    private String itemStatus;
    private String blockersJson;
    private String failureReason;
}
