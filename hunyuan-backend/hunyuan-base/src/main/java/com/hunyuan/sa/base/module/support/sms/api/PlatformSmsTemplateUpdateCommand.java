package com.hunyuan.sa.base.module.support.sms.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 更新平台短信模板命令，模板编码由路径参数确定。
 */
@Data
public class PlatformSmsTemplateUpdateCommand {

    @Schema(description = "模板名称")
    @NotBlank(message = "模板名称不能为空")
    @Length(max = 100, message = "模板名称最多 100 个字符")
    private String templateName;

    @Schema(description = "模板内容")
    @NotBlank(message = "模板内容不能为空")
    @Length(max = 5000, message = "模板内容最多 5000 个字符")
    private String templateContent;

    @Schema(description = "是否禁用")
    private Boolean disableFlag;

    @Schema(description = "备注")
    @Length(max = 500, message = "备注最多 500 个字符")
    private String remark;
}
