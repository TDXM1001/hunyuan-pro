package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程任务动作轨迹。
 */
@Data
public class BpmTaskActionLogVO {

    @Schema(description = "动作日志ID")
    private Long actionLogId;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "节点ID")
    private Long definitionNodeId;

    @Schema(description = "动作类型")
    private String actionType;

    @Schema(description = "操作人员工ID")
    private Long actorEmployeeId;

    @Schema(description = "操作人姓名快照")
    private String actorNameSnapshot;

    @Schema(description = "原处理人")
    private Long fromAssigneeEmployeeId;

    @Schema(description = "新处理人")
    private Long toAssigneeEmployeeId;

    @Schema(description = "审批意见")
    private String commentText;

    @Schema(description = "动作时间")
    private LocalDateTime actionAt;
}
