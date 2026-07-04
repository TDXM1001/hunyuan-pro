package com.hunyuan.sa.base.module.support.sms.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * SMS send request.
 */
@Data
public class SmsSendForm {

    @Schema(description = "Phone number")
    @NotBlank(message = "phone cannot be blank")
    private String phone;

    @Schema(description = "SMS template code")
    @NotBlank(message = "template code cannot be blank")
    private String templateCode;

    @Schema(description = "SMS content after template rendering")
    private String content;

    @Schema(description = "Template parameters")
    private Map<String, Object> templateParams;

    @Schema(description = "Idempotency key")
    private String idempotentKey;
}
