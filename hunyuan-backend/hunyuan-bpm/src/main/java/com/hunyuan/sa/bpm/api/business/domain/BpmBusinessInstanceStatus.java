package com.hunyuan.sa.bpm.api.business.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务侧可读取的流程实例状态。
 */
@Data
public class BpmBusinessInstanceStatus {

    private Long instanceId;

    private String instanceNo;

    private String businessType;

    private Long businessId;

    private Integer runState;

    private Integer resultState;

    private LocalDateTime lastActionAt;
}
