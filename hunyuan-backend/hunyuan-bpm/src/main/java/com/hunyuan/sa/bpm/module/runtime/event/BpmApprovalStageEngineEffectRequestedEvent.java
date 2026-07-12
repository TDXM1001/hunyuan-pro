package com.hunyuan.sa.bpm.module.runtime.event;

import com.hunyuan.sa.bpm.module.candidate.domain.model.EngineEffect;

/**
 * 审批动作事务已写入终态事实后，等待提交完成再执行的引擎副作用请求。
 */
public record BpmApprovalStageEngineEffectRequestedEvent(
        String stageInvocationId,
        EngineEffect engineEffect,
        String terminalReason
) {

    public BpmApprovalStageEngineEffectRequestedEvent {
        if (stageInvocationId == null || stageInvocationId.isBlank()
                || engineEffect == null || engineEffect == EngineEffect.NONE
                || terminalReason == null || terminalReason.isBlank()) {
            throw new IllegalArgumentException("审批阶段引擎副作用事件参数不完整");
        }
    }
}
