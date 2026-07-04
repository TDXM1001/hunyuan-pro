package com.hunyuan.sa.base.module.support.sms.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * SMS template add form.
 */
@Data
public class SmsTemplateAddForm {

    @Schema(description = "Template code")
    @NotBlank(message = "template code cannot be blank")
    @Length(max = 100, message = "template code max length is 100")
    private String templateCode;

    @Schema(description = "Template name")
    @NotBlank(message = "template name cannot be blank")
    @Length(max = 100, message = "template name max length is 100")
    private String templateName;

    @Schema(description = "Template content")
    @NotBlank(message = "template content cannot be blank")
    @Length(max = 5000, message = "template content max length is 5000")
    private String templateContent;

    @Schema(description = "Disabled flag")
    private Boolean disableFlag;

    @Schema(description = "Remark")
    @Length(max = 500, message = "remark max length is 500")
    private String remark;
}
