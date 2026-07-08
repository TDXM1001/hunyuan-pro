package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程抄送列表返回结果。
 */
@Data
public class BpmInstanceCopyVO {

    @Schema(description = "抄送ID")
    private Long copyId;

    @Schema(description = "实例ID")
    private Long instanceId;

    @Schema(description = "实例编号")
    private String instanceNo;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "抄送类型")
    private String copyType;

    @Schema(description = "已读状态")
    private Integer readState;

    @Schema(description = "来源节点")
    private String sourceNodeName;

    @Schema(description = "被抄送人")
    private String targetNameSnapshot;

    @Schema(description = "抄送原因")
    private String reasonSnapshot;

    @Schema(description = "发送时间")
    private LocalDateTime sentAt;

    @Schema(description = "阅读时间")
    private LocalDateTime readAt;

    @Schema(description = "发起人姓名")
    private String startEmployeeNameSnapshot;

    @Schema(description = "运行状态")
    private Integer runState;

    @Schema(description = "结果状态")
    private Integer resultState;
}
