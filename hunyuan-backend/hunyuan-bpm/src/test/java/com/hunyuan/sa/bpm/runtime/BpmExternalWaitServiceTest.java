package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmExternalWaitDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTimeEventDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTimeEventEntity;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmExternalWaitEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmExternalWaitService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

class BpmExternalWaitServiceTest {

    @Test
    void resumeShouldVerifySignatureAndTriggerWaitingExecutionOnce() {
        BpmExternalWaitDao dao = Mockito.mock(BpmExternalWaitDao.class);
        FlowableProcessInstanceGateway gateway = Mockito.mock(FlowableProcessInstanceGateway.class);
        BpmExternalWaitService service = new BpmExternalWaitService();
        setField(service, "bpmExternalWaitDao", dao);
        setField(service, "flowableProcessInstanceGateway", gateway);
        BpmExternalWaitEntity wait = waiting("callback-token");
        when(dao.selectOne(any())).thenReturn(wait);
        when(dao.update(any(), any())).thenReturn(1);
        String payload = "{\"externalNo\":\"FIN-1001\"}";

        boolean resumed = service.resume(
                "callback-token",
                BpmExternalWaitService.sign("callback-token", payload),
                payload
        );

        assertThat(resumed).isTrue();
        verify(gateway).trigger("execution-81", "externalCallbackPayload", payload);
    }

    @Test
    void resumeShouldRejectForgedSignatureAndIgnoreDuplicateClaim() {
        BpmExternalWaitDao dao = Mockito.mock(BpmExternalWaitDao.class);
        FlowableProcessInstanceGateway gateway = Mockito.mock(FlowableProcessInstanceGateway.class);
        BpmExternalWaitService service = new BpmExternalWaitService();
        setField(service, "bpmExternalWaitDao", dao);
        setField(service, "flowableProcessInstanceGateway", gateway);
        when(dao.selectOne(any())).thenReturn(waiting("callback-token"));
        String payload = "{}";

        assertThatThrownBy(() -> service.resume("callback-token", "forged", payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("签名");

        when(dao.update(any(), any())).thenReturn(0);
        boolean resumed = service.resume(
                "callback-token",
                BpmExternalWaitService.sign("callback-token", payload),
                payload
        );
        assertThat(resumed).isFalse();
        verify(gateway, never()).trigger(any(), any(), any());
    }

    @Test
    void prepareWaitShouldCreateMatchingExternalTimeoutFact() {
        BpmExternalWaitDao waitDao = Mockito.mock(BpmExternalWaitDao.class);
        BpmTimeEventDao timeEventDao = Mockito.mock(BpmTimeEventDao.class);
        BpmExternalWaitService service = new BpmExternalWaitService();
        setField(service, "bpmExternalWaitDao", waitDao);
        setField(service, "bpmTimeEventDao", timeEventDao);
        when(waitDao.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            BpmExternalWaitEntity wait = invocation.getArgument(0);
            wait.setExternalWaitId(91L);
            return 1;
        }).when(waitDao).insert(any(BpmExternalWaitEntity.class));
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(31L);
        instance.setDefinitionId(41L);
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(51L);
        node.setCompiledNodeSnapshotJson("{\"connectorVersion\":3}");

        BpmExternalWaitService.PreparedWait prepared = service.prepareWait(
                instance, node, "process-31", "execution-71", "finance_sync",
                "finance", "createExpense", "PT30M", new JSONObject()
        );

        assertThat(prepared.connectorVersion()).isEqualTo(3);
        verify(timeEventDao).insert(Mockito.argThat((BpmTimeEventEntity event) ->
                "WAIT:91:EXTERNAL_TIMEOUT".equals(event.getEventKey())
                        && "EXTERNAL_TIMEOUT".equals(event.getEventKind())
                        && "finance_sync".equals(event.getNodeKey())
        ));
    }

    private BpmExternalWaitEntity waiting(String token) {
        BpmExternalWaitEntity wait = new BpmExternalWaitEntity();
        wait.setExternalWaitId(91L);
        wait.setCallbackTokenHash(BpmExternalWaitService.sha256(token));
        wait.setEngineExecutionId("execution-81");
        wait.setWaitStatus("WAITING");
        return wait;
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
