package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程实例详情。
 */
@Data
public class BpmInstanceDetailVO {

    @Schema(description = "实例ID")
    private Long instanceId;

    @Schema(description = "实例编号")
    private String instanceNo;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "运行状态")
    private Integer runState;

    @Schema(description = "结果状态")
    private Integer resultState;

    @Schema(description = "发起人姓名快照")
    private String startEmployeeNameSnapshot;

    @Schema(description = "发起部门名称快照")
    private String startDepartmentNameSnapshot;

    @Schema(description = "当前表单数据快照JSON")
    private String currentFormDataSnapshotJson;

    @Schema(description = "当前节点摘要JSON")
    private String currentNodeSummaryJson;

    @Schema(description = "发起时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    private LocalDateTime finishedAt;

    @Schema(description = "当前待办任务")
    private List<BpmTaskVO> currentTasks;

    @Schema(description = "动作轨迹")
    private List<BpmTaskActionLogVO> actionLogs;

    @Schema(description = "审批组列表")
    private List<BpmApprovalGroupDetailVO> approvalGroups;
}
