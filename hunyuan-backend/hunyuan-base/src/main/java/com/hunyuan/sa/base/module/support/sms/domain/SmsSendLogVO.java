package com.hunyuan.sa.base.module.support.sms.domain;

import com.hunyuan.sa.base.common.swagger.SchemaEnum;
import com.hunyuan.sa.base.module.support.sms.constant.SmsSendStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SMS send log view.
 */
@Data
public class SmsSendLogVO {

    private Long smsSendLogId;

    @Schema(description = "Provider name")
    private String provider;

    @Schema(description = "Provider request id")
    private String requestId;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "Template code")
    private String templateCode;

    @Schema(description = "Send content")
    private String sendContent;

    @SchemaEnum(value = SmsSendStatusEnum.class)
    private Integer sendStatus;

    @Schema(description = "Fail reason")
    private String failReason;

    @Schema(description = "Send time")
    private LocalDateTime sendTime;

    @Schema(description = "Create time")
    private LocalDateTime createTime;
}
