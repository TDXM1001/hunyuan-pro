package com.hunyuan.sa.bpm.integration;

import com.hunyuan.sa.bpm.common.enumeration.BpmCallbackStatusEnum;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackContext;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackExecuteResult;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackExecutor;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackHandler;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackResult;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackTriggerType;
import com.hunyuan.sa.bpm.module.sampleexpense.service.BpmSampleExpenseCallbackHandler;
import com.hunyuan.sa.bpm.module.sampleexpense.service.BpmSampleExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmBusinessCallbackExecutorTest {

    private BpmBusinessCallbackExecutor executor;

    private BpmCallbackRecordDao bpmCallbackRecordDao;

    private AtomicInteger handlerCalls;

    @BeforeEach
    void setUp() {
        executor = new BpmBusinessCallbackExecutor();
        bpmCallbackRecordDao = Mockito.mock(BpmCallbackRecordDao.class);
        handlerCalls = new AtomicInteger();
        setField(executor, "bpmCallbackRecordDao", bpmCallbackRecordDao);
    }

    @Test
    void executeShouldMarkSucceededWhenHandlerSucceeds() {
        setHandlers(List.of(successHandler("expense", "{\"handled\":true}")));
        BpmCallbackRecordEntity record = buildRecord(BpmCallbackStatusEnum.PENDING.getValue(), 0);
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);

        BpmBusinessCallbackExecuteResult result = executor.execute(1L, BpmBusinessCallbackTriggerType.MANUAL);

        assertThat(result.processed()).isTrue();
        assertThat(result.succeeded()).isTrue();
        ArgumentCaptor<BpmCallbackRecordEntity> captor = ArgumentCaptor.forClass(BpmCallbackRecordEntity.class);
        verify(bpmCallbackRecordDao).update(captor.capture(), any());
        assertThat(captor.getValue().getCallbackStatus()).isEqualTo(BpmCallbackStatusEnum.SUCCEEDED.getValue());
        assertThat(captor.getValue().getResponsePayloadJson()).isEqualTo("{\"handled\":true}");
        assertThat(captor.getValue().getFailureReason()).isNull();
        assertThat(captor.getValue().getNextRetryAt()).isNull();
        assertThat(handlerCalls.get()).isEqualTo(1);
    }

    @Test
    void executeShouldMarkFailedAndScheduleRetryWhenHandlerFails() {
        setHandlers(List.of(failingHandler("expense", "业务侧暂不可用")));
        BpmCallbackRecordEntity record = buildRecord(BpmCallbackStatusEnum.PENDING.getValue(), 0);
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);

        BpmBusinessCallbackExecuteResult result = executor.execute(1L, BpmBusinessCallbackTriggerType.AUTO);

        assertThat(result.processed()).isTrue();
        assertThat(result.succeeded()).isFalse();
        ArgumentCaptor<BpmCallbackRecordEntity> captor = ArgumentCaptor.forClass(BpmCallbackRecordEntity.class);
        verify(bpmCallbackRecordDao).update(captor.capture(), any());
        assertThat(captor.getValue().getCallbackStatus()).isEqualTo(BpmCallbackStatusEnum.FAILED.getValue());
        assertThat(captor.getValue().getRetryCount()).isEqualTo(1);
        assertThat(captor.getValue().getFailureReason()).isEqualTo("业务侧暂不可用");
        assertThat(captor.getValue().getNextRetryAt()).isNotNull();
    }

    @Test
    void executeShouldMoveToCompensationAfterMaxRetryCount() {
        setHandlers(List.of(failingHandler("expense", "仍然失败")));
        BpmCallbackRecordEntity record = buildRecord(BpmCallbackStatusEnum.FAILED.getValue(), 2);
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);

        executor.execute(1L, BpmBusinessCallbackTriggerType.AUTO);

        ArgumentCaptor<BpmCallbackRecordEntity> captor = ArgumentCaptor.forClass(BpmCallbackRecordEntity.class);
        verify(bpmCallbackRecordDao).update(captor.capture(), any());
        assertThat(captor.getValue().getCallbackStatus()).isEqualTo(BpmCallbackStatusEnum.NEEDS_COMPENSATION.getValue());
        assertThat(captor.getValue().getRetryCount()).isEqualTo(3);
        assertThat(captor.getValue().getNextRetryAt()).isNull();
    }

    @Test
    void executeShouldSkipTerminalRecords() {
        setHandlers(List.of(successHandler("expense", "{}")));
        BpmCallbackRecordEntity record = buildRecord(BpmCallbackStatusEnum.SUCCEEDED.getValue(), 0);
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);

        BpmBusinessCallbackExecuteResult result = executor.execute(1L, BpmBusinessCallbackTriggerType.MANUAL);

        assertThat(result.processed()).isFalse();
        verify(bpmCallbackRecordDao, never()).update(any(BpmCallbackRecordEntity.class), any());
        assertThat(handlerCalls.get()).isZero();
    }

    @Test
    void executeDueRecordsShouldProcessPendingAndDueFailedRecords() {
        setHandlers(List.of(successHandler("expense", "{}")));
        BpmCallbackRecordEntity pending = buildRecord(BpmCallbackStatusEnum.PENDING.getValue(), 0);
        pending.setCallbackRecordId(1L);
        BpmCallbackRecordEntity failed = buildRecord(BpmCallbackStatusEnum.FAILED.getValue(), 1);
        failed.setCallbackRecordId(2L);
        failed.setNextRetryAt(LocalDateTime.of(2026, 7, 9, 10, 0));
        when(bpmCallbackRecordDao.selectList(any())).thenReturn(List.of(pending, failed));
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(pending);
        when(bpmCallbackRecordDao.selectById(2L)).thenReturn(failed);

        int processed = executor.executeDueRecords(LocalDateTime.of(2026, 7, 9, 10, 1), 50);

        assertThat(processed).isEqualTo(2);
        assertThat(handlerCalls.get()).isEqualTo(2);
        verify(bpmCallbackRecordDao).selectById(1L);
        verify(bpmCallbackRecordDao).selectById(2L);
    }

    @Test
    void executeShouldRecordMissingHandlerAsFailure() {
        setHandlers(List.of());
        BpmCallbackRecordEntity record = buildRecord(BpmCallbackStatusEnum.PENDING.getValue(), 0);
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);

        BpmBusinessCallbackExecuteResult result = executor.execute(1L, BpmBusinessCallbackTriggerType.AUTO);

        assertThat(result.processed()).isTrue();
        assertThat(result.succeeded()).isFalse();
        ArgumentCaptor<BpmCallbackRecordEntity> captor = ArgumentCaptor.forClass(BpmCallbackRecordEntity.class);
        verify(bpmCallbackRecordDao).update(captor.capture(), any());
        assertThat(captor.getValue().getFailureReason()).contains("未找到业务回调处理器");
        assertThat(handlerCalls.get()).isZero();
    }

    @Test
    void executeShouldCallSampleExpenseHandlerByBusinessType() {
        BpmSampleExpenseService sampleService = Mockito.mock(BpmSampleExpenseService.class);
        BpmSampleExpenseCallbackHandler sampleHandler = new BpmSampleExpenseCallbackHandler();
        setField(sampleHandler, "bpmSampleExpenseService", sampleService);
        setHandlers(List.of(sampleHandler));
        BpmCallbackRecordEntity record = buildRecord(BpmCallbackStatusEnum.PENDING.getValue(), 0);
        record.setBusinessType("sample_expense");
        record.setRequestPayloadJson("{\"eventId\":\"event-1\",\"instanceId\":88,\"businessType\":\"sample_expense\",\"businessId\":1001,\"resultState\":1}");
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);
        when(sampleService.handleCallback(any(BpmBusinessCallbackContext.class)))
                .thenReturn(BpmBusinessCallbackResult.success("{\"approvalStatus\":2}"));

        BpmBusinessCallbackExecuteResult result = executor.execute(1L, BpmBusinessCallbackTriggerType.MANUAL);

        assertThat(result.processed()).isTrue();
        assertThat(result.succeeded()).isTrue();
        verify(sampleService).handleCallback(any(BpmBusinessCallbackContext.class));
        ArgumentCaptor<BpmCallbackRecordEntity> captor = ArgumentCaptor.forClass(BpmCallbackRecordEntity.class);
        verify(bpmCallbackRecordDao).update(captor.capture(), any());
        assertThat(captor.getValue().getCallbackStatus()).isEqualTo(BpmCallbackStatusEnum.SUCCEEDED.getValue());
        assertThat(captor.getValue().getResponsePayloadJson()).isEqualTo("{\"approvalStatus\":2}");
    }

    private BpmCallbackRecordEntity buildRecord(Integer status, Integer retryCount) {
        BpmCallbackRecordEntity record = new BpmCallbackRecordEntity();
        record.setCallbackRecordId(1L);
        record.setEventId("event-1");
        record.setInstanceId(88L);
        record.setBusinessType("expense");
        record.setBusinessId(1001L);
        record.setCallbackStatus(status);
        record.setRequestPayloadJson("{\"result\":\"APPROVED\"}");
        record.setRetryCount(retryCount);
        return record;
    }

    private BpmBusinessCallbackHandler successHandler(String businessType, String response) {
        return new BpmBusinessCallbackHandler() {
            @Override
            public String businessType() {
                return businessType;
            }

            @Override
            public BpmBusinessCallbackResult handle(BpmBusinessCallbackContext context) {
                assertThat(context.eventId()).isEqualTo("event-1");
                handlerCalls.incrementAndGet();
                return BpmBusinessCallbackResult.success(response);
            }
        };
    }

    private BpmBusinessCallbackHandler failingHandler(String businessType, String reason) {
        return new BpmBusinessCallbackHandler() {
            @Override
            public String businessType() {
                return businessType;
            }

            @Override
            public BpmBusinessCallbackResult handle(BpmBusinessCallbackContext context) {
                handlerCalls.incrementAndGet();
                return BpmBusinessCallbackResult.failed(reason, "{\"ok\":false}");
            }
        };
    }

    private void setHandlers(List<BpmBusinessCallbackHandler> handlers) {
        setField(executor, "callbackHandlers", handlers);
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
