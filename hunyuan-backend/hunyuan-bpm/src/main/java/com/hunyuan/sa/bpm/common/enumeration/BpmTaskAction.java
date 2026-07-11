package com.hunyuan.sa.bpm.common.enumeration;

/**
 * 员工端可执行的流程任务动作。
 */
public enum BpmTaskAction {
    APPROVE,
    REJECT,
    COMPLETE,
    RETURN,
    TRANSFER,
    DELEGATE,
    ADD_SIGN,
    REDUCE_SIGN
}
