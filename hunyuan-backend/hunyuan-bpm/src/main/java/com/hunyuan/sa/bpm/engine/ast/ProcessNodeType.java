package com.hunyuan.sa.bpm.engine.ast;

/**
 * Hunyuan 对外开放的受控流程节点类型。
 */
public enum ProcessNodeType {
    USER_TASK,
    HANDLE_TASK,
    COPY_TASK,
    DELAY,
    EXTERNAL_TRIGGER,
    EXCLUSIVE_BRANCH,
    PARALLEL_BRANCH,
    INCLUSIVE_BRANCH
}
