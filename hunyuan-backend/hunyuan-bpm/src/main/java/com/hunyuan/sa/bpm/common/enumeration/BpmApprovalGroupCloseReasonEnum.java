package com.hunyuan.sa.bpm.common.enumeration;

/**
 * 并行审批组结束原因。
 */
public enum BpmApprovalGroupCloseReasonEnum {

    ALL_APPROVED,
    MEMBER_REJECTED,
    MEMBER_RETURNED,
    INSTANCE_RECALLED,
    INSTANCE_CANCELLED
}
