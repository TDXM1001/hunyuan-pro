package com.hunyuan.sa.base.module.support.job.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台定时任务公开视图。
 */
@Data
public class PlatformJobView {

    @Schema(description = "任务 ID")
    private Integer jobId;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务执行类")
    private String jobClass;

    @Schema(description = "触发类型")
    private String triggerType;

    @Schema(description = "触发配置")
    private String triggerValue;

    @Schema(description = "任务参数")
    private String param;

    @Schema(description = "是否启用")
    private Boolean enabledFlag;

    @Schema(description = "最后一次执行时间")
    private LocalDateTime lastExecuteTime;

    @Schema(description = "最后一次执行记录 ID")
    private Long lastExecuteLogId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "更新人")
    private String updateName;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最后一次执行记录")
    private PlatformJobLogView lastJobLog;

    @Schema(description = "未来任务执行时间")
    private List<LocalDateTime> nextJobExecuteTimeList;
}
