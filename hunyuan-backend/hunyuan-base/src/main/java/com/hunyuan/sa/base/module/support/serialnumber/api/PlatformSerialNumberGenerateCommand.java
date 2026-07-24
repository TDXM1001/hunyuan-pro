package com.hunyuan.sa.base.module.support.serialnumber.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 平台序列号批量生成命令。
 */
@Data
public class PlatformSerialNumberGenerateCommand {

    @Schema(description = "序列号定义标识")
    @NotNull(message = "序列号定义标识不能为空")
    private Integer serialNumberId;

    @Schema(description = "生成数量")
    @NotNull(message = "生成数量不能为空")
    @Min(value = 1, message = "生成数量不能小于 1")
    @Max(value = 50, message = "生成数量不能大于 50")
    private Integer count;
}
