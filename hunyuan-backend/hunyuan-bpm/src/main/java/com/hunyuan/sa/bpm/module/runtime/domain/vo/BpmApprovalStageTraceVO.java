package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 实例追踪中可展示的冻结审批阶段事实。
 */
@Data
public class BpmApprovalStageTraceVO {

    private Long approvalStageId;

    private String stageInvocationId;

    private String authoredNodeId;

    private Integer generation;

    private String stageState;

    private String terminalReason;

    private String completionMode;

    private Integer ratioPercent;

    private Integer effectiveMemberCount;

    private Integer requiredApprovalCount;

    private Integer approvedMemberCount;

    private Integer processedMemberCount;

    private Long candidatePolicyVersionId;

    private Long approvalPolicyVersionId;

    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    private List<BpmApprovalStageMemberTraceVO> members;
}
