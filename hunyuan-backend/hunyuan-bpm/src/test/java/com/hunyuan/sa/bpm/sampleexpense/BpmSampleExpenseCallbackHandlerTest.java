package com.hunyuan.sa.bpm.sampleexpense;

import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackContext;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackResult;
import com.hunyuan.sa.bpm.module.sampleexpense.service.BpmSampleExpenseCallbackHandler;
import com.hunyuan.sa.bpm.module.sampleexpense.service.BpmSampleExpenseService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmSampleExpenseCallbackHandlerTest {

    @Test
    void businessTypeShouldBeSampleExpense() {
        BpmSampleExpenseCallbackHandler handler = new BpmSampleExpenseCallbackHandler();

        assertThat(handler.businessType()).isEqualTo("sample_expense");
    }

    @Test
    void handleShouldDelegateToService() {
        BpmSampleExpenseCallbackHandler handler = new BpmSampleExpenseCallbackHandler();
        BpmSampleExpenseService service = Mockito.mock(BpmSampleExpenseService.class);
        setField(handler, "bpmSampleExpenseService", service);
        BpmBusinessCallbackContext context = new BpmBusinessCallbackContext(1L, "event-1", 88L, "sample_expense", 1001L, "{}");
        when(service.handleCallback(context)).thenReturn(BpmBusinessCallbackResult.success("{\"handled\":true}"));

        BpmBusinessCallbackResult result = handler.handle(context);

        assertThat(result.success()).isTrue();
        verify(service).handleCallback(context);
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
