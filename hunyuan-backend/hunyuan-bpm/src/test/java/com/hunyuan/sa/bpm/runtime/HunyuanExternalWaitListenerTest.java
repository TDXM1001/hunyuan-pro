package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.engine.internal.HunyuanExternalWaitListener;
import com.hunyuan.sa.bpm.module.runtime.service.BpmExternalWaitService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.common.engine.api.delegate.Expression;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HunyuanExternalWaitListenerTest {

    @Test
    void notifyShouldBindActualReceiveExecution() {
        BpmExternalWaitService service = Mockito.mock(BpmExternalWaitService.class);
        DelegateExecution execution = Mockito.mock(DelegateExecution.class);
        when(execution.getProcessInstanceId()).thenReturn("process-31");
        when(execution.getCurrentActivityId()).thenReturn("finance_sync_wait");
        when(execution.getId()).thenReturn("receive-execution-81");
        Expression authoredNodeId = Mockito.mock(Expression.class);
        when(authoredNodeId.getValue(execution)).thenReturn("finance_sync");
        HunyuanExternalWaitListener listener = new HunyuanExternalWaitListener();
        setField(listener, "bpmExternalWaitService", service);
        setField(listener, "authoredNodeId", authoredNodeId);

        listener.notify(execution);

        verify(service).bindExecution("process-31", "finance_sync", "receive-execution-81");
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
