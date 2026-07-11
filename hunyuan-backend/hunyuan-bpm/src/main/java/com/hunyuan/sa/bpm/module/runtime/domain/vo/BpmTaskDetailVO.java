package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import com.hunyuan.sa.bpm.common.enumeration.BpmTaskKind;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程任务详情。
 */
@Data
public class BpmTaskDetailVO {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "实例ID")
    private Long instanceId;

    @Schema(description = "实例编号")
    private String instanceNo;

    @Schema(description = "流程标题")
    private String instanceTitle;

    @Schema(description = "任务标识")
    private String taskKey;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "发起人姓名快照")
    private String startEmployeeNameSnapshot;

    @Schema(description = "当前处理人姓名快照")
    private String assigneeNameSnapshot;

    @Schema(description = "当前处理部门快照")
    private String assigneeDepartmentNameSnapshot;

    @Schema(description = "运行时分配快照JSON")
    private String runtimeAssignmentSnapshotJson;

    @Schema(description = "任务状态")
    private Integer taskState;

    @Schema(description = "任务结果")
    private Integer taskResult;

    @Schema(description = "任务业务类型")
    private BpmTaskKind taskKind;

    @Schema(description = "当前可用动作")
    private List<String> availableActions;

    @Schema(description = "到达时间")
    private LocalDateTime assignedAt;

    @Schema(description = "截止时间")
    private LocalDateTime dueAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "动作轨迹")
    private List<BpmTaskActionLogVO> actionLogs;

    @Schema(description = "审批组详情")
    private BpmApprovalGroupDetailVO approvalGroup;

    @Schema(description = "当前员工可访问的任务表单上下文")
    private BpmTaskFormContextVO formContext;
}
