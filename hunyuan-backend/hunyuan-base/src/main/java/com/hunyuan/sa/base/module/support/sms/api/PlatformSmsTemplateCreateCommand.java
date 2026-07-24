package com.hunyuan.sa.base.module.support.sms.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 创建平台短信模板命令。
 */
@Data
public class PlatformSmsTemplateCreateCommand {

    @Schema(description = "模板编码")
    @NotBlank(message = "模板编码不能为空")
    @Length(max = 100, message = "模板编码最多 100 个字符")
    private String templateCode;

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
