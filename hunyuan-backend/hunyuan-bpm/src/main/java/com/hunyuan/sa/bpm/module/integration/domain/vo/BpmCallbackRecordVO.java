package com.hunyuan.sa.bpm.module.integration.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * BPM 业务回调记录 VO。
 */
@Data
public class BpmCallbackRecordVO {

    private Long callbackRecordId;

    private String eventId;

    private Long instanceId;

    private String businessType;

    private Long businessId;

    private Integer callbackStatus;

    private String failureReason;

    private Integer retryCount;

    private LocalDateTime nextRetryAt;

    private LocalDateTime compensatedAt;

    private Long compensatedBy;

    private String compensationReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
