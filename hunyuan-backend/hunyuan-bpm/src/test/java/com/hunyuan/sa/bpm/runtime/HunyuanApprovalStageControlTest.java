package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.engine.internal.HunyuanApprovalStageControl;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageActivationService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageService;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HunyuanApprovalStageControlTest {

    @Test
    void listenerShouldPassActualEngineIdentityAndPublishedGraphVersionToStageActivation() {
        GraphDefinitionVersionDao graphDefinitionVersionDao = Mockito.mock(GraphDefinitionVersionDao.class);
        BpmApprovalStageActivationService activationService = Mockito.mock(BpmApprovalStageActivationService.class);
        HunyuanApprovalStageControl control = new HunyuanApprovalStageControl();
        setField(control, "graphDefinitionVersionDao", graphDefinitionVersionDao);
        setField(control, "bpmApprovalStageActivationService", activationService);

        GraphDefinitionVersionEntity version = new GraphDefinitionVersionEntity();
        version.setGraphDefinitionVersionId(41L);
        when(graphDefinitionVersionDao.selectByEngineProcessDefinitionId("expense:7:900"))
                .thenReturn(version);

        DelegateExecution execution = Mockito.mock(DelegateExecution.class);
        when(execution.getVariable("hunyuanInstanceId")).thenReturn(81L);
        when(execution.getProcessDefinitionId()).thenReturn("expense:7:900");
        when(execution.getCurrentActivityId()).thenReturn("graph_stage_finance_review");
        when(execution.getProcessInstanceId()).thenReturn("process-91");
        when(execution.getId()).thenReturn("execution-92");

        control.notify(execution);

        ArgumentCaptor<BpmApprovalStageActivationService.ActivateApprovalStageCommand> command =
                ArgumentCaptor.forClass(BpmApprovalStageActivationService.ActivateApprovalStageCommand.class);
        verify(activationService).activate(command.capture());
        assertThat(command.getValue().instanceId()).isEqualTo(81L);
        assertThat(command.getValue().graphDefinitionVersionId()).isEqualTo(41L);
        assertThat(command.getValue().compiledActivityId()).isEqualTo("graph_stage_finance_review");
        assertThat(command.getValue().engineProcessInstanceId()).isEqualTo("process-91");
        assertThat(command.getValue().engineExecutionId()).isEqualTo("execution-92");
    }

    @Test
    void completeOnceShouldClaimBeforeTriggeringAndSkipARepeatedStageInvocation() {
        BpmApprovalStageService stageService = Mockito.mock(BpmApprovalStageService.class);
        FlowableProcessInstanceGateway processGateway = Mockito.mock(FlowableProcessInstanceGateway.class);
        HunyuanApprovalStageControl control = new HunyuanApprovalStageControl();
        setField(control, "bpmApprovalStageService", stageService);
        setField(control, "flowableProcessInstanceGateway", processGateway);
        when(stageService.claimEngineEffect("execution-92", "APPROVED")).thenReturn(
                new BpmApprovalStageService.EngineEffectClaim(
                        81L, "execution-92", "process-91", "execution-92", "APPROVED"
                )
        ).thenReturn(null);

        assertThat(control.completeOnce("execution-92")).isTrue();
        assertThat(control.completeOnce("execution-92")).isFalse();

        verify(processGateway).trigger(
                "execution-92",
                FlowableProcessInstanceGateway.approvalStageCompletionMarker("execution-92"),
                true
        );
        verify(stageService).markEngineEffectCompleted(81L);
    }

    @Test
    void closeOnceShouldClaimBeforeCancellingAndSkipARepeatedReturnedStageInvocation() {
        BpmApprovalStageService stageService = Mockito.mock(BpmApprovalStageService.class);
        FlowableProcessInstanceGateway processGateway = Mockito.mock(FlowableProcessInstanceGateway.class);
        HunyuanApprovalStageControl control = new HunyuanApprovalStageControl();
        setField(control, "bpmApprovalStageService", stageService);
        setField(control, "flowableProcessInstanceGateway", processGateway);
        when(stageService.claimEngineEffect("execution-92", "RETURNED")).thenReturn(
                new BpmApprovalStageService.EngineEffectClaim(
                        81L, "execution-92", "process-91", "execution-92", "RETURNED"
                )
        ).thenReturn(null);

        assertThat(control.closeOnce("execution-92", "RETURNED")).isTrue();
        assertThat(control.closeOnce("execution-92", "RETURNED")).isFalse();

        verify(processGateway).cancel("process-91", "RETURNED");
        verify(stageService).markEngineEffectCompleted(81L);
    }

    @Test
    void closeOnceShouldMarkRejectedStageEffectFailedWhenCancellationFails() {
        BpmApprovalStageService stageService = Mockito.mock(BpmApprovalStageService.class);
        FlowableProcessInstanceGateway processGateway = Mockito.mock(FlowableProcessInstanceGateway.class);
        HunyuanApprovalStageControl control = new HunyuanApprovalStageControl();
        setField(control, "bpmApprovalStageService", stageService);
        setField(control, "flowableProcessInstanceGateway", processGateway);
        when(stageService.claimEngineEffect("execution-92", "REJECTED")).thenReturn(
                new BpmApprovalStageService.EngineEffectClaim(
                        81L, "execution-92", "process-91", "execution-92", "REJECTED"
                )
        );
        IllegalStateException cancellationFailure = new IllegalStateException("engine unavailable");
        doThrow(cancellationFailure).when(processGateway).cancel("process-91", "REJECTED");

        assertThatThrownBy(() -> control.closeOnce("execution-92", "REJECTED"))
                .isSameAs(cancellationFailure);

        verify(stageService).markEngineEffectFailed(81L, "engine unavailable");
    }

    @Test
    void completeOnceShouldKeepTheClaimForReconciliationWhenCompletionMarkFails() {
        BpmApprovalStageService stageService = Mockito.mock(BpmApprovalStageService.class);
        FlowableProcessInstanceGateway processGateway = Mockito.mock(FlowableProcessInstanceGateway.class);
        HunyuanApprovalStageControl control = new HunyuanApprovalStageControl();
        setField(control, "bpmApprovalStageService", stageService);
        setField(control, "flowableProcessInstanceGateway", processGateway);
        when(stageService.claimEngineEffect("execution-92", "APPROVED")).thenReturn(
                new BpmApprovalStageService.EngineEffectClaim(
                        81L, "execution-92", "process-91", "execution-92", "APPROVED"
                )
        );
        IllegalStateException persistenceFailure = new IllegalStateException("stage store unavailable");
        doThrow(persistenceFailure).when(stageService).markEngineEffectCompleted(81L);

        assertThatThrownBy(() -> control.completeOnce("execution-92"))
                .isSameAs(persistenceFailure);

        verify(processGateway).trigger(
                "execution-92",
                FlowableProcessInstanceGateway.approvalStageCompletionMarker("execution-92"),
                true
        );
        verify(stageService, never()).markEngineEffectFailed(81L, "stage store unavailable");
    }

    @Test
    void closeOnceShouldKeepTheClaimForReconciliationWhenCompletionMarkFails() {
        BpmApprovalStageService stageService = Mockito.mock(BpmApprovalStageService.class);
        FlowableProcessInstanceGateway processGateway = Mockito.mock(FlowableProcessInstanceGateway.class);
        HunyuanApprovalStageControl control = new HunyuanApprovalStageControl();
        setField(control, "bpmApprovalStageService", stageService);
        setField(control, "flowableProcessInstanceGateway", processGateway);
        when(stageService.claimEngineEffect("execution-92", "REJECTED")).thenReturn(
                new BpmApprovalStageService.EngineEffectClaim(
                        81L, "execution-92", "process-91", "execution-92", "REJECTED"
                )
        );
        IllegalStateException persistenceFailure = new IllegalStateException("stage store unavailable");
        doThrow(persistenceFailure).when(stageService).markEngineEffectCompleted(81L);

        assertThatThrownBy(() -> control.closeOnce("execution-92", "REJECTED"))
                .isSameAs(persistenceFailure);

        verify(processGateway).cancel("process-91", "REJECTED");
        verify(stageService, never()).markEngineEffectFailed(81L, "stage store unavailable");
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
