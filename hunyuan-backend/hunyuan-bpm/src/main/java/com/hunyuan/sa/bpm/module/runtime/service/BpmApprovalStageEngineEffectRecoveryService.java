package com.hunyuan.sa.bpm.module.runtime.service;

import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.candidate.domain.model.EngineEffect;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.event.BpmApprovalStageEngineEffectRequestedEvent;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 对审批阶段引擎副作用进行受控恢复。除明确尚未领取的 PENDING 状态外，绝不重放 Flowable 调用。
 */
@Service
public class BpmApprovalStageEngineEffectRecoveryService {

    @Resource
    private BpmApprovalStageDao bpmApprovalStageDao;

    @Resource
    private FlowableProcessInstanceGateway flowableProcessInstanceGateway;

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Resource
    private BpmApprovalStageInstanceProjectionService bpmApprovalStageInstanceProjectionService;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public RecoveryResult recover(String stageInvocationId) {
        if (stageInvocationId == null || stageInvocationId.isBlank()) {
            throw new IllegalArgumentException("审批阶段调用标识不能为空");
        }
        BpmApprovalStageEntity stage = bpmApprovalStageDao.selectByStageInvocationIdForUpdate(stageInvocationId);
        if (stage == null) {
            return new RecoveryResult(RecoveryOutcome.NOT_FOUND, "审批阶段不存在");
        }
        if ("COMPLETED".equals(stage.getEngineEffectState())) {
            reconcileApprovedInstance(stage);
            return new RecoveryResult(RecoveryOutcome.ALREADY_COMPLETED, "引擎副作用已完成");
        }

        EngineEffect engineEffect = resolveEngineEffect(stage);
        if (engineEffect == null) {
            return new RecoveryResult(RecoveryOutcome.NO_EFFECT_REQUESTED, "阶段尚未形成可恢复的终态引擎副作用");
        }
        if ("PENDING".equals(stage.getEngineEffectState())) {
            applicationEventPublisher.publishEvent(new BpmApprovalStageEngineEffectRequestedEvent(
                    stage.getStageInvocationId(),
                    engineEffect,
                    stage.getTerminalReason()
            ));
            return new RecoveryResult(RecoveryOutcome.REQUEUED, "已重新登记提交后引擎副作用");
        }
        if (!"CLAIMED".equals(stage.getEngineEffectState()) && !"FAILED".equals(stage.getEngineEffectState())) {
            return new RecoveryResult(RecoveryOutcome.NO_EFFECT_REQUESTED, "当前引擎副作用状态不支持自动对账");
        }

        FlowableProcessInstanceGateway.EngineEffectObservation observation;
        try {
            observation = flowableProcessInstanceGateway.inspectApprovalStageEffect(
                    stage.getEngineProcessInstanceId(),
                    stage.getStageInvocationId(),
                    stage.getTerminalReason()
            );
        } catch (RuntimeException ex) {
            return markExceptionPending(stage, "Flowable 对账查询失败：" + detail(ex.getMessage()));
        }
        if (observation.confirmed()) {
            reconcileApprovedInstance(stage);
            if (bpmApprovalStageDao.markEngineEffectReconciledCompleted(stage.getApprovalStageId()) == 1) {
                return new RecoveryResult(RecoveryOutcome.RECONCILED_COMPLETED, observation.reason());
            }
            return new RecoveryResult(RecoveryOutcome.CONCURRENT_STATE_CHANGED, "阶段副作用状态已被其他恢复操作更新");
        }
        return markExceptionPending(stage, observation.reason());
    }

    private RecoveryResult markExceptionPending(BpmApprovalStageEntity stage, String reason) {
        if (bpmApprovalStageDao.markEngineEffectExceptionPending(stage.getApprovalStageId(), detail(reason)) == 1) {
            return new RecoveryResult(RecoveryOutcome.EXCEPTION_PENDING, reason);
        }
        return new RecoveryResult(RecoveryOutcome.CONCURRENT_STATE_CHANGED, "阶段副作用状态已被其他恢复操作更新");
    }

    private void reconcileApprovedInstance(BpmApprovalStageEntity stage) {
        if ("APPROVED".equals(stage.getTerminalReason())) {
            bpmApprovalStageInstanceProjectionService.reconcileApprovedCompletion(
                    stage.getInstanceId(),
                    stage.getEngineProcessInstanceId()
            );
        }
    }

    private EngineEffect resolveEngineEffect(BpmApprovalStageEntity stage) {
        if (stage.getTerminalReason() == null || stage.getTerminalReason().isBlank()) {
            return null;
        }
        return switch (stage.getTerminalReason()) {
            case "APPROVED" -> EngineEffect.COMPLETE_ONCE;
            case "REJECTED", "RETURNED", "CANCELLED" -> EngineEffect.CLOSE_ONCE;
            default -> null;
        };
    }

    private String detail(String reason) {
        if (reason == null || reason.isBlank()) {
            return "未获得可确认的 Flowable 历史证据";
        }
        return reason.length() <= 512 ? reason : reason.substring(0, 512);
    }

    public enum RecoveryOutcome {
        NOT_FOUND,
        NO_EFFECT_REQUESTED,
        REQUEUED,
        ALREADY_COMPLETED,
        RECONCILED_COMPLETED,
        EXCEPTION_PENDING,
        CONCURRENT_STATE_CHANGED
    }

    public record RecoveryResult(RecoveryOutcome outcome, String reason) {
    }
}
