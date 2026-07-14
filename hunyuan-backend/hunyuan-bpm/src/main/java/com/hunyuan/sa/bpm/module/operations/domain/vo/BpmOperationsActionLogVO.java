package com.hunyuan.sa.bpm.module.operations.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * BPM 运营治理处置审计视图，不返回原始业务载荷。
 */
@Data
public class BpmOperationsActionLogVO {

    private Long operationsActionLogId;
    private String actionType;
    private String actionStatus;
    private Long actorEmployeeId;
    private String reason;
    private String failureReason;
    private LocalDateTime actionAt;
}
