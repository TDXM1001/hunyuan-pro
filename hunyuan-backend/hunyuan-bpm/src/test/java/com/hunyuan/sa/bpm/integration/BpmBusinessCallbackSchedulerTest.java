package com.hunyuan.sa.bpm.integration;

import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackExecutor;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class BpmBusinessCallbackSchedulerTest {

    private BpmBusinessCallbackScheduler scheduler;

    private BpmBusinessCallbackExecutor callbackExecutor;

    @BeforeEach
    void setUp() {
        scheduler = new BpmBusinessCallbackScheduler();
        callbackExecutor = Mockito.mock(BpmBusinessCallbackExecutor.class);
        setField(scheduler, "callbackExecutor", callbackExecutor);
    }

    @Test
    void scanDueCallbackRecordsShouldUseExecutorBatchScan() {
        scheduler.scanDueCallbackRecords();

        verify(callbackExecutor).executeDueRecords(any(LocalDateTime.class), Mockito.eq(50));
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("设置测试字段失败: " + fieldName, ex);
        }
    }
}
