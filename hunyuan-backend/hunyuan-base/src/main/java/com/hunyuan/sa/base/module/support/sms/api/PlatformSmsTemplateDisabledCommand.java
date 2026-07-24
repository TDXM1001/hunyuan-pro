package com.hunyuan.sa.base.module.support.sms.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改平台短信模板启停状态命令。
 */
@Data
public class PlatformSmsTemplateDisabledCommand {

    @Schema(description = "是否禁用")
    @NotNull(message = "禁用状态不能为空")
    private Boolean disableFlag;
}
