package com.hunyuan.sa.base.module.support.sms.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SMS template view.
 */
@Data
public class SmsTemplateVO {

    @Schema(description = "Template code")
    private String templateCode;

    @Schema(description = "Template name")
    private String templateName;

    @Schema(description = "Template content")
    private String templateContent;

    @Schema(description = "Disabled flag")
    private Boolean disableFlag;

    @Schema(description = "Remark")
    private String remark;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;

    @Schema(description = "Create time")
    private LocalDateTime createTime;
}
