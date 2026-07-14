package com.hunyuan.sa.bpm.integration;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.common.enumeration.BpmCallbackStatusEnum;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.integration.domain.form.BpmCallbackCompensateForm;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackExecuteResult;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackExecutor;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackService;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackTriggerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmBusinessCallbackServiceTest {

    private BpmBusinessCallbackService callbackService;

    private BpmCallbackRecordDao bpmCallbackRecordDao;

    private BpmBusinessCallbackExecutor callbackExecutor;

    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @BeforeEach
    void setUp() {
        callbackService = new BpmBusinessCallbackService();
        bpmCallbackRecordDao = Mockito.mock(BpmCallbackRecordDao.class);
        callbackExecutor = Mockito.mock(BpmBusinessCallbackExecutor.class);
        bpmCurrentActorProvider = Mockito.mock(BpmCurrentActorProvider.class);
        setField(callbackService, "bpmCallbackRecordDao", bpmCallbackRecordDao);
        setField(callbackService, "callbackExecutor", callbackExecutor);
        setField(callbackService, "bpmCurrentActorProvider", bpmCurrentActorProvider);
    }

    @Test
    void retryShouldUseUnifiedExecutor() {
        when(callbackExecutor.execute(1L, BpmBusinessCallbackTriggerType.MANUAL))
                .thenReturn(BpmBusinessCallbackExecuteResult.success());

        ResponseDTO<String> response = callbackService.retry(1L);

        assertThat(response.getOk()).isTrue();
        verify(callbackExecutor).execute(1L, BpmBusinessCallbackTriggerType.MANUAL);
        verify(bpmCallbackRecordDao, never()).updateById(any(BpmCallbackRecordEntity.class));
    }

    @Test
    void retryShouldReturnUserErrorWhenRecordIsMissing() {
        when(callbackExecutor.execute(1L, BpmBusinessCallbackTriggerType.MANUAL))
                .thenReturn(BpmBusinessCallbackExecuteResult.skipped("回调记录不存在"));

        ResponseDTO<String> response = callbackService.retry(1L);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).isEqualTo("回调记录不存在");
    }

    @Test
    void retryShouldReturnUserErrorWhenExecutionStillFails() {
        when(callbackExecutor.execute(1L, BpmBusinessCallbackTriggerType.MANUAL))
                .thenReturn(BpmBusinessCallbackExecuteResult.failed("外部系统仍不可用"));

        ResponseDTO<String> response = callbackService.retry(1L);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).isEqualTo("外部系统仍不可用");
    }

    @Test
    void compensateShouldMarkNeedsCompensationRecordAsCompensated() {
        BpmCallbackRecordEntity record = new BpmCallbackRecordEntity();
        record.setCallbackRecordId(1L);
        record.setCallbackStatus(BpmCallbackStatusEnum.NEEDS_COMPENSATION.getValue());
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);
        when(bpmCurrentActorProvider.requireCurrentEmployeeId()).thenReturn(900L);
        BpmCallbackCompensateForm form = new BpmCallbackCompensateForm();
        form.setReason("业务侧已线下补偿");

        ResponseDTO<String> response = callbackService.compensate(1L, form);

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmCallbackRecordEntity> captor = ArgumentCaptor.forClass(BpmCallbackRecordEntity.class);
        verify(bpmCallbackRecordDao).update(captor.capture(), any());
        assertThat(captor.getValue().getCallbackStatus()).isEqualTo(BpmCallbackStatusEnum.COMPENSATED.getValue());
        assertThat(captor.getValue().getCompensatedBy()).isEqualTo(900L);
        assertThat(captor.getValue().getCompensationReason()).isEqualTo("业务侧已线下补偿");
        assertThat(captor.getValue().getCompensatedAt()).isNotNull();
        assertThat(captor.getValue().getUpdateTime()).isNotNull();
    }

    @Test
    void compensateShouldRejectNonCompensationRecord() {
        BpmCallbackRecordEntity record = new BpmCallbackRecordEntity();
        record.setCallbackRecordId(1L);
        record.setCallbackStatus(BpmCallbackStatusEnum.FAILED.getValue());
        when(bpmCallbackRecordDao.selectById(1L)).thenReturn(record);
        BpmCallbackCompensateForm form = new BpmCallbackCompensateForm();
        form.setReason("业务侧已线下补偿");

        ResponseDTO<String> response = callbackService.compensate(1L, form);

        assertThat(response.getOk()).isFalse();
        verify(bpmCallbackRecordDao, never()).update(any(BpmCallbackRecordEntity.class), any());
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
