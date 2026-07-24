package com.hunyuan.sa.base.module.support.job.api;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 平台定时任务启停命令。
 */
@Data
public class PlatformJobEnabledCommand {

    @NotNull(message = "任务 ID 不能为空")
    private Integer jobId;

    @NotNull(message = "是否启用不能为空")
    private Boolean enabledFlag;
}
