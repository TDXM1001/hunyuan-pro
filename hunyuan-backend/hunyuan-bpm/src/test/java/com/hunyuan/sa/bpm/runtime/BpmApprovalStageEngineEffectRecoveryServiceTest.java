package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.candidate.domain.model.EngineEffect;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.event.BpmApprovalStageEngineEffectRequestedEvent;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageEngineEffectRecoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmApprovalStageEngineEffectRecoveryServiceTest {

    private BpmApprovalStageEngineEffectRecoveryService service;
    private BpmApprovalStageDao stageDao;
    private FlowableProcessInstanceGateway processGateway;
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        service = new BpmApprovalStageEngineEffectRecoveryService();
        stageDao = Mockito.mock(BpmApprovalStageDao.class);
        processGateway = Mockito.mock(FlowableProcessInstanceGateway.class);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        setField(service, "bpmApprovalStageDao", stageDao);
        setField(service, "flowableProcessInstanceGateway", processGateway);
        setField(service, "applicationEventPublisher", eventPublisher);
    }

    @Test
    void pendingTerminalStageShouldRequeueAfterCommitEffectWithoutCallingFlowableSynchronously() {
        BpmApprovalStageEntity stage = stage("PENDING", "APPROVED", "APPROVED");
        when(stageDao.selectByStageInvocationIdForUpdate("execution-92")).thenReturn(stage);

        BpmApprovalStageEngineEffectRecoveryService.RecoveryResult result = service.recover("execution-92");

        assertThat(result.outcome()).isEqualTo(
                BpmApprovalStageEngineEffectRecoveryService.RecoveryOutcome.REQUEUED
        );
        ArgumentCaptor<BpmApprovalStageEngineEffectRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(BpmApprovalStageEngineEffectRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().engineEffect()).isEqualTo(EngineEffect.COMPLETE_ONCE);
        verify(processGateway, never()).inspectApprovalStageEffect("process-91", "execution-92", "APPROVED");
    }

    @Test
    void claimedStageWithConfirmedFlowableSignalShouldMarkCompletedWithoutReplayingTheSignal() {
        BpmApprovalStageEntity stage = stage("CLAIMED", "APPROVED", "APPROVED");
        when(stageDao.selectByStageInvocationIdForUpdate("execution-92")).thenReturn(stage);
        when(processGateway.inspectApprovalStageEffect("process-91", "execution-92", "APPROVED"))
                .thenReturn(FlowableProcessInstanceGateway.EngineEffectObservation.confirmed("approval marker recorded"));
        when(stageDao.markEngineEffectReconciledCompleted(81L)).thenReturn(1);

        BpmApprovalStageEngineEffectRecoveryService.RecoveryResult result = service.recover("execution-92");

        assertThat(result.outcome()).isEqualTo(
                BpmApprovalStageEngineEffectRecoveryService.RecoveryOutcome.RECONCILED_COMPLETED
        );
        verify(stageDao).markEngineEffectReconciledCompleted(81L);
        verify(eventPublisher, never()).publishEvent(Mockito.any());
    }

    @Test
    void failedStageWithoutSafeFlowableEvidenceShouldBecomeExceptionPendingInsteadOfBeingRetried() {
        BpmApprovalStageEntity stage = stage("FAILED", "REJECTED", "REJECTED");
        when(stageDao.selectByStageInvocationIdForUpdate("execution-92")).thenReturn(stage);
        when(processGateway.inspectApprovalStageEffect("process-91", "execution-92", "REJECTED"))
                .thenReturn(FlowableProcessInstanceGateway.EngineEffectObservation.unconfirmed("cancel deletion marker absent"));
        when(stageDao.markEngineEffectExceptionPending(81L, "cancel deletion marker absent")).thenReturn(1);

        BpmApprovalStageEngineEffectRecoveryService.RecoveryResult result = service.recover("execution-92");

        assertThat(result.outcome()).isEqualTo(
                BpmApprovalStageEngineEffectRecoveryService.RecoveryOutcome.EXCEPTION_PENDING
        );
        assertThat(result.reason()).isEqualTo("cancel deletion marker absent");
        verify(stageDao).markEngineEffectExceptionPending(81L, "cancel deletion marker absent");
        verify(eventPublisher, never()).publishEvent(Mockito.any());
    }

    @Test
    void activePendingStageShouldNotBeMistakenForAnInterruptedTerminalEffect() {
        BpmApprovalStageEntity stage = stage("PENDING", "ACTIVE", null);
        when(stageDao.selectByStageInvocationIdForUpdate("execution-92")).thenReturn(stage);

        BpmApprovalStageEngineEffectRecoveryService.RecoveryResult result = service.recover("execution-92");

        assertThat(result.outcome()).isEqualTo(
                BpmApprovalStageEngineEffectRecoveryService.RecoveryOutcome.NO_EFFECT_REQUESTED
        );
        verify(eventPublisher, never()).publishEvent(Mockito.any());
        verify(processGateway, never()).inspectApprovalStageEffect(Mockito.any(), Mockito.any(), Mockito.any());
    }

    private BpmApprovalStageEntity stage(String effectState, String stageState, String terminalReason) {
        BpmApprovalStageEntity stage = new BpmApprovalStageEntity();
        stage.setApprovalStageId(81L);
        stage.setStageInvocationId("execution-92");
        stage.setEngineProcessInstanceId("process-91");
        stage.setEngineExecutionId("execution-92");
        stage.setEngineEffectState(effectState);
        stage.setStageState(stageState);
        stage.setTerminalReason(terminalReason);
        return stage;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
