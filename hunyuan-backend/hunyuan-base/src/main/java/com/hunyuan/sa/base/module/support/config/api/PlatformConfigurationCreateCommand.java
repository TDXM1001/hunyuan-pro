package com.hunyuan.sa.base.module.support.config.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 创建平台配置命令。
 */
@Data
public class PlatformConfigurationCreateCommand {

    @Schema(description = "参数 KEY")
    @NotBlank(message = "参数 KEY 不能为空")
    @Length(max = 255, message = "参数 KEY 最多 255 个字符")
    private String configKey;

    @Schema(description = "参数值")
    @NotBlank(message = "参数值不能为空")
    @Length(max = 60000, message = "参数值最多 60000 个字符")
    private String configValue;

    @Schema(description = "参数名称")
    @NotBlank(message = "参数名称不能为空")
    @Length(max = 255, message = "参数名称最多 255 个字符")
    private String configName;

    @Schema(description = "备注")
    @Length(max = 255, message = "备注最多 255 个字符")
    private String remark;
}
