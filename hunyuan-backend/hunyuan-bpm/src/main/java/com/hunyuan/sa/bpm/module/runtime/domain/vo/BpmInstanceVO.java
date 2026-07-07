package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程实例列表返回结果。
 */
@Data
public class BpmInstanceVO {

    @Schema(description = "实例ID")
    private Long instanceId;

    @Schema(description = "实例编号")
    private String instanceNo;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "运行状态")
    private Integer runState;

    @Schema(description = "结果状态")
    private Integer resultState;

    @Schema(description = "发起人姓名")
    private String startEmployeeNameSnapshot;

    @Schema(description = "发起时间")
    private LocalDateTime startedAt;

    @Schema(description = "结束时间")
    private LocalDateTime finishedAt;
}
