package com.hunyuan.sa.base.module.support.job.api;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台定时任务更新命令。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformJobUpdateCommand extends PlatformJobCreateCommand {

    @NotNull(message = "任务 ID 不能为空")
    private Integer jobId;
}
