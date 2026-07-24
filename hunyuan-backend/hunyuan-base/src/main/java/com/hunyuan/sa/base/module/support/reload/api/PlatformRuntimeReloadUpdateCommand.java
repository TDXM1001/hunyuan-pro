package com.hunyuan.sa.base.module.support.reload.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新运行时重载项命令。
 */
@Data
public class PlatformRuntimeReloadUpdateCommand {

    @Schema(description = "重载项标签")
    @NotBlank(message = "标签不能为空")
    private String tag;

    @Schema(description = "运行标识")
    @NotBlank(message = "状态标识不能为空")
    private String identification;

    @Schema(description = "重载参数")
    private String args;
}
