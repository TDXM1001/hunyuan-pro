package com.hunyuan.sa.base.module.support.job.api;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 平台定时任务立即执行命令。
 */
@Data
public class PlatformJobExecuteCommand {

    @NotNull(message = "任务 ID 不能为空")
    private Integer jobId;

    @Length(max = 2000, message = "任务参数最多2000字符")
    private String param;
}
