package com.hunyuan.sa.bpm.module.integration.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * BPM 命令执行记录 VO。
 */
@Data
public class BpmCommandRecordVO {

    private Long commandRecordId;

    private String commandKey;

    private String commandType;

    private Long instanceId;

    private String businessType;

    private Long businessId;

    private Integer commandStatus;

    private String failureReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
