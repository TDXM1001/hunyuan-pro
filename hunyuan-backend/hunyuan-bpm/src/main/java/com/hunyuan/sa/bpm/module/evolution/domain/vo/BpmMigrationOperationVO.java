package com.hunyuan.sa.bpm.module.evolution.domain.vo;

import lombok.Data;
import java.util.List;

@Data
public class BpmMigrationOperationVO {
    private Long migrationBatchId;
    private String batchCode;
    private String batchStatus;
    private int totalCount;
    private int eligibleCount;
    private int blockedCount;
    private int succeededCount;
    private int failedCount;
    private List<BpmMigrationOperationItemVO> items;

    public static BpmMigrationOperationVO from(BpmMigrationBatchDetailVO detail) {
        BpmMigrationOperationVO result = new BpmMigrationOperationVO();
        result.setMigrationBatchId(detail.getMigrationBatchId());
        result.setBatchCode(detail.getBatchCode());
        result.setBatchStatus(detail.getBatchStatus());
        result.setTotalCount(detail.getTotalCount());
        result.setEligibleCount(detail.getEligibleCount());
        result.setBlockedCount(detail.getBlockedCount());
        result.setSucceededCount(detail.getSucceededCount());
        result.setFailedCount(detail.getFailedCount());
        result.setItems(detail.getItems().stream().map(item -> {
            BpmMigrationOperationItemVO value = new BpmMigrationOperationItemVO();
            value.setMigrationItemId(item.getMigrationItemId());
            value.setInstanceId(item.getInstanceId());
            value.setItemStatus(item.getItemStatus());
            value.setBlockersJson(item.getBlockersJson());
            value.setFailureReason(item.getFailureReason());
            return value;
        }).toList());
        return result;
    }
}
