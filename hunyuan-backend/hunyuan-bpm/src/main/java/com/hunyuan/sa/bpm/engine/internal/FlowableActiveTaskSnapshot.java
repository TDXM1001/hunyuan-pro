package com.hunyuan.sa.bpm.engine.internal;

/**
 * Flowable 当前活动任务快照，仅允许在 BPM 模块内部流转。
 */
public record FlowableActiveTaskSnapshot(
        String engineTaskId,
        String engineExecutionId,
        String engineProcessInstanceId,
        String taskKey,
        String taskName,
        Long assigneeEmployeeId
) {
}
