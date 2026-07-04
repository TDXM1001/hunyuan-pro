package com.hunyuan.sa.base.module.support.sms.domain;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

/**
 * SMS template query form.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SmsTemplateQueryForm extends PageParam {

    @Schema(description = "Template code")
    @Length(max = 100, message = "template code max length is 100")
    private String templateCode;

    @Schema(description = "Template name")
    @Length(max = 100, message = "template name max length is 100")
    private String templateName;

    @Schema(description = "Disabled flag")
    private Boolean disableFlag;
}
