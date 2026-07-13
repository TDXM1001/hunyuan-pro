package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageInstanceProjectionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmApprovalStageInstanceProjectionServiceTest {

    @Test
    void approvedStageShouldNotFinishInstanceWhileFlowableStillHasActiveExecution() {
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        FlowableProcessInstanceGateway gateway = Mockito.mock(FlowableProcessInstanceGateway.class);
        BpmApprovalStageInstanceProjectionService service =
                new BpmApprovalStageInstanceProjectionService(instanceDao, gateway);
        when(gateway.isProcessFinished("process-91")).thenReturn(false);

        assertThat(service.reconcileApprovedCompletion(81L, "process-91")).isFalse();

        verify(instanceDao, never()).finishApprovedIfRunning(81L);
    }

    @Test
    void approvedStageShouldFinishRunningInstanceAfterFlowableHistoryConfirmsCompletion() {
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        FlowableProcessInstanceGateway gateway = Mockito.mock(FlowableProcessInstanceGateway.class);
        BpmApprovalStageInstanceProjectionService service =
                new BpmApprovalStageInstanceProjectionService(instanceDao, gateway);
        when(gateway.isProcessFinished("process-91")).thenReturn(true);
        when(instanceDao.finishApprovedIfRunning(81L)).thenReturn(1);

        assertThat(service.reconcileApprovedCompletion(81L, "process-91")).isTrue();

        verify(instanceDao).finishApprovedIfRunning(81L);
    }
}
