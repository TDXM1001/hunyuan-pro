package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.engine.internal.HunyuanTimeEventDelegate;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTimeEventService;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HunyuanTimeEventDelegateTest {

    @Test
    void executeShouldForwardControlledEventToRuntimeService() {
        BpmTimeEventService service = Mockito.mock(BpmTimeEventService.class);
        DelegateExecution execution = Mockito.mock(DelegateExecution.class);
        Expression eventKind = Mockito.mock(Expression.class);
        Expression nodeKey = Mockito.mock(Expression.class);
        when(execution.getProcessInstanceId()).thenReturn("process-31");
        when(execution.getId()).thenReturn("execution-71");
        when(eventKind.getValue(execution)).thenReturn("SLA_DUE");
        when(nodeKey.getValue(execution)).thenReturn("review");
        HunyuanTimeEventDelegate delegate = new HunyuanTimeEventDelegate();
        setField(delegate, "bpmTimeEventService", service);
        setField(delegate, "timeEventKind", eventKind);
        setField(delegate, "authoredNodeKey", nodeKey);

        delegate.execute(execution);

        verify(service).trigger("process-31", "review", "SLA_DUE", "execution-71", null);
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
