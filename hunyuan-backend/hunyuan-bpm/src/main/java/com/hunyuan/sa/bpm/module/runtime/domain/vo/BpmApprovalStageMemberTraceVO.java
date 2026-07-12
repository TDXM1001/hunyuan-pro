package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实例追踪中可展示的冻结审批成员事实。
 */
@Data
public class BpmApprovalStageMemberTraceVO {

    private Long approvalStageMemberId;

    private Integer memberOrder;

    private Long sourceEmployeeId;

    private String sourceEmployeeNameSnapshot;

    private Long currentEmployeeId;

    private String currentEmployeeNameSnapshot;

    private String memberState;

    private String actionResult;

    private LocalDateTime activatedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private String changeReason;
}
