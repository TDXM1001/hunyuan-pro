package com.hunyuan.sa.base.module.support.sms.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SMS provider send result.
 */
@Data
public class SmsSendResult {

    @Schema(description = "Provider name")
    private String provider;

    @Schema(description = "Provider mode")
    private String mode;

    @Schema(description = "Provider request id")
    private String requestId;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "Template code")
    private String templateCode;
}
