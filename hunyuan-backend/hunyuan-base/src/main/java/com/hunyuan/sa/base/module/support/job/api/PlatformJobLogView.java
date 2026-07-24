package com.hunyuan.sa.base.module.support.job.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台定时任务执行记录公开视图。
 */
@Data
public class PlatformJobLogView {

    private Long logId;

    private Integer jobId;

    private String jobName;

    private String param;

    @Schema(description = "执行是否成功")
    private Boolean successFlag;

    private LocalDateTime executeStartTime;

    private Long executeTimeMillis;

    private String executeResult;

    private LocalDateTime executeEndTime;

    private String ip;

    private String processId;

    private String programPath;

    private String createName;

    private LocalDateTime createTime;
}
