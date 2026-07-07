package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程任务列表返回结果。
 */
@Data
public class BpmTaskVO {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "实例ID")
    private Long instanceId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "实例编号")
    private String instanceNo;

    @Schema(description = "流程标题")
    private String instanceTitle;

    @Schema(description = "任务状态")
    private Integer taskState;

    @Schema(description = "任务结果")
    private Integer taskResult;

    @Schema(description = "当前处理人")
    private String assigneeNameSnapshot;

    @Schema(description = "到达时间")
    private LocalDateTime assignedAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;
}
