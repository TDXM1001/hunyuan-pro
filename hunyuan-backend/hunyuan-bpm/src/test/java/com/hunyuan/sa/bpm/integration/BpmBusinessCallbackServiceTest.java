package com.hunyuan.sa.bpm.integration;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmBusinessCallbackServiceTest {

    private BpmBusinessCallbackService callbackService;

    private BpmCallbackRecordDao bpmCallbackRecordDao;

    @BeforeEach
    void setUp() {
        callbackService = new BpmBusinessCallbackService();
        bpmCallbackRecordDao = Mockito.mock(BpmCallbackRecordDao.class);
        setField(callbackService, "bpmCallbackRecordDao", bpmCallbackRecordDao);
    }

    @Test
    void retryShouldIncrementRetryCountForFailedCallbackRecord() {
        BpmCallbackRecordEntity record = new BpmCallbackRecordEntity();
        record.setCallbackRecordId(1L);
        record.setCallbackStatus(2);
        record.setRetryCount(3);
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);

        ResponseDTO<String> response = callbackService.retry(1L);

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmCallbackRecordEntity> updateCaptor = ArgumentCaptor.forClass(BpmCallbackRecordEntity.class);
        verify(bpmCallbackRecordDao).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getCallbackRecordId()).isEqualTo(1L);
        assertThat(updateCaptor.getValue().getRetryCount()).isEqualTo(4);
        assertThat(updateCaptor.getValue().getUpdateTime()).isNotNull();
    }

    @Test
    void retryShouldIgnoreAlreadySucceededCallbackRecord() {
        BpmCallbackRecordEntity record = new BpmCallbackRecordEntity();
        record.setCallbackRecordId(1L);
        record.setCallbackStatus(1);
        record.setRetryCount(3);
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);

        ResponseDTO<String> response = callbackService.retry(1L);

        assertThat(response.getOk()).isTrue();
        verify(bpmCallbackRecordDao, never()).updateById(Mockito.any(BpmCallbackRecordEntity.class));
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
