package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批组成员详情。
 */
@Data
public class BpmApprovalGroupMemberVO {

    @Schema(description = "成员任务ID")
    private Long taskId;

    @Schema(description = "成员序号")
    private Integer memberIndex;

    @Schema(description = "成员总数")
    private Integer memberTotal;

    @Schema(description = "当前处理人员工ID")
    private Long assigneeEmployeeId;

    @Schema(description = "当前处理人姓名快照")
    private String assigneeNameSnapshot;

    @Schema(description = "当前处理部门名称快照")
    private String assigneeDepartmentNameSnapshot;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务状态")
    private Integer taskState;

    @Schema(description = "任务结果")
    private Integer taskResult;

    @Schema(description = "到达时间")
    private LocalDateTime assignedAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "取消时间")
    private LocalDateTime cancelledAt;

    @Schema(description = "最后一次可展示动作")
    private BpmTaskActionLogVO lastAction;
}
