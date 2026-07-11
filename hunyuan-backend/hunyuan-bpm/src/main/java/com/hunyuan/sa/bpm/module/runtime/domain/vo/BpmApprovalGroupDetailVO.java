package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批组详情。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmApprovalGroupDetailVO extends BpmApprovalGroupSummaryVO {

    @Schema(description = "审批组结束原因")
    private String closeReason;

    @Schema(description = "审批组结束时间")
    private LocalDateTime closedAt;

    @Schema(description = "审批组成员")
    private List<BpmApprovalGroupMemberVO> members;
}
