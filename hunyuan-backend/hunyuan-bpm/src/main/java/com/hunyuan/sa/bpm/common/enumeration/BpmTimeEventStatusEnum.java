package com.hunyuan.sa.bpm.common.enumeration;

/**
 * BPM 时间事件状态。
 */
public enum BpmTimeEventStatusEnum {
    SCHEDULED,
    TRIGGERED,
    SUCCEEDED,
    FAILED_RETRYABLE,
    FAILED_MANUAL,
    CANCELLED
}
