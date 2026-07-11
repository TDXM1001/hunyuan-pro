package com.hunyuan.sa.bpm.module.runtime.service;

import com.hunyuan.sa.bpm.common.enumeration.BpmApprovalGroupStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceResultStateEnum;

/**
 * 并行审批组成员动作结果。
 */
public record BpmApprovalGroupActionResult(
        boolean ordinaryTask,
        boolean memberTask,
        boolean processed,
        boolean shouldCompleteCurrentFlowableTask,
        boolean shouldCancelEngineProcess,
        BpmInstanceResultStateEnum finishInstanceResultState,
        boolean waitResubmit,
        Long groupId,
        BpmApprovalGroupStateEnum groupState
) {

    public static BpmApprovalGroupActionResult forOrdinaryTask() {
        return new BpmApprovalGroupActionResult(true, false, false, false, false, null, false, null, null);
    }

    public static BpmApprovalGroupActionResult ignored(
            Long groupId,
            BpmApprovalGroupStateEnum groupState
    ) {
        return new BpmApprovalGroupActionResult(false, true, false, false, false, null, false, groupId, groupState);
    }
}
