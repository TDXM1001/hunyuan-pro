package com.hunyuan.sa.base.module.support.sms.api;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台短信发送记录摘要。
 */
@Data
public class PlatformSmsSendLogSummary {

    private Long smsSendLogId;
    private String provider;
    private String requestId;
    private String phone;
    private String templateCode;
    private String sendContent;
    private Integer sendStatus;
    private String failReason;
    private LocalDateTime sendTime;
    private LocalDateTime createTime;
}
