package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageEngineEffectRecoveryScheduler;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageEngineEffectRecoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmApprovalStageEngineEffectRecoverySchedulerTest {

    private BpmApprovalStageEngineEffectRecoveryScheduler scheduler;
    private BpmApprovalStageDao stageDao;
    private BpmApprovalStageEngineEffectRecoveryService recoveryService;

    @BeforeEach
    void setUp() {
        scheduler = new BpmApprovalStageEngineEffectRecoveryScheduler();
        stageDao = Mockito.mock(BpmApprovalStageDao.class);
        recoveryService = Mockito.mock(BpmApprovalStageEngineEffectRecoveryService.class);
        setField(scheduler, "bpmApprovalStageDao", stageDao);
        setField(scheduler, "recoveryService", recoveryService);
    }

    @Test
    void scheduledScanShouldRecoverEveryOutstandingTerminalEffectInOneFiniteBatch() {
        when(stageDao.selectRecoverableStageInvocationIds(50))
                .thenReturn(List.of("stage-101", "stage-102"));

        scheduler.scanRecoverableStageEffects();

        verify(recoveryService).recover("stage-101");
        verify(recoveryService).recover("stage-102");
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
