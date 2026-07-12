package com.hunyuan.sa.bpm.engine.graph;

/**
 * M4 操作 Graph 审批等待点的唯一引擎端口。
 */
public interface ApprovalStageControl {

    boolean completeOnce(String stageInvocationId);

    boolean closeOnce(String stageInvocationId, String reason);
}
