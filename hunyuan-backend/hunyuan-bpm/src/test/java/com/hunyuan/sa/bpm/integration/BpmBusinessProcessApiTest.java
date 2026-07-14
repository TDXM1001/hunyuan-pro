package com.hunyuan.sa.bpm.integration;

import com.hunyuan.sa.bpm.api.business.BpmBusinessProcessApi;
import com.hunyuan.sa.bpm.api.business.BpmBusinessProcessApiImpl;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessResultEvent;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmBusinessProcessApiTest {

    private BpmBusinessProcessApi api;

    private BpmCallbackRecordDao bpmCallbackRecordDao;

    @BeforeEach
    void setUp() {
        BpmBusinessProcessApiImpl apiImpl = new BpmBusinessProcessApiImpl();
        api = apiImpl;
        bpmCallbackRecordDao = Mockito.mock(BpmCallbackRecordDao.class);
        setField(apiImpl, "bpmCallbackRecordDao", bpmCallbackRecordDao);
    }

    @Test
    void publishResultEventShouldCreateCallbackRecordOnce() {
        when(bpmCallbackRecordDao.selectOne(any())).thenReturn(null);
        BpmBusinessResultEvent event = new BpmBusinessResultEvent();
        event.setEventId("event-1");
        event.setInstanceId(88L);
        event.setBusinessType("expense");
        event.setBusinessId(1001L);
        event.setResultState(1);
        event.setPayloadJson("{\"approved\":true}");
        event.setOccurredAt(LocalDateTime.of(2026, 7, 8, 12, 30));

        api.publishResultEvent(event);

        ArgumentCaptor<BpmCallbackRecordEntity> callbackCaptor = ArgumentCaptor.forClass(BpmCallbackRecordEntity.class);
        verify(bpmCallbackRecordDao).insert(callbackCaptor.capture());
        assertThat(callbackCaptor.getValue().getEventId()).isEqualTo("event-1");
        assertThat(callbackCaptor.getValue().getInstanceId()).isEqualTo(88L);
        assertThat(callbackCaptor.getValue().getBusinessType()).isEqualTo("expense");
        assertThat(callbackCaptor.getValue().getBusinessId()).isEqualTo(1001L);
        assertThat(callbackCaptor.getValue().getCallbackStatus()).isEqualTo(0);
        assertThat(callbackCaptor.getValue().getRequestPayloadJson()).contains("\"approved\":true");
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
