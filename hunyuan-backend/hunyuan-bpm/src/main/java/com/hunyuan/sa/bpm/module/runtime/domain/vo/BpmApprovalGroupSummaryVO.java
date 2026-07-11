package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 审批组摘要。
 */
@Data
public class BpmApprovalGroupSummaryVO {

    @Schema(description = "审批组ID")
    private Long approvalGroupId;

    @Schema(description = "审批组业务标识")
    private String approvalGroupKey;

    @Schema(description = "审批组名称")
    private String approvalGroupName;

    @Schema(description = "审批模式")
    private String approvalMode;

    @Schema(description = "审批组状态")
    private String groupState;

    @Schema(description = "成员总数")
    private Integer totalMemberCount;

    @Schema(description = "已处理成员数")
    private Integer processedMemberCount;

    @Schema(description = "已通过成员数")
    private Integer approvedMemberCount;

    @Schema(description = "已拒绝成员数")
    private Integer rejectedMemberCount;
}
