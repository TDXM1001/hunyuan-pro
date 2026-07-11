package com.hunyuan.sa.bpm.api.business.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * BPM 结果通知业务侧的事件载荷。
 */
@Data
public class BpmBusinessResultEvent {

    private String eventId;

    private Long instanceId;

    private String businessType;

    private Long businessId;

    private Integer resultState;

    private String payloadJson;

    private Long finalFormDataVersion;

    private String finalFormDataJson;

    private LocalDateTime formDataLastModifiedAt;

    private LocalDateTime occurredAt;
}
