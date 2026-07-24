package com.hunyuan.sa.base.module.support.sms.api;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台短信模板管理摘要。
 */
@Data
public class PlatformSmsTemplateSummary {

    private String templateCode;
    private String templateName;
    private String templateContent;
    private Boolean disableFlag;
    private String remark;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
}
