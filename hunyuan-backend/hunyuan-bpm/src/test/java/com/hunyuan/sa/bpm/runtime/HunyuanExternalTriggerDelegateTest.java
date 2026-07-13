package com.hunyuan.sa.bpm.runtime;

import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.engine.internal.HunyuanExternalTriggerDelegate;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.integration.service.BpmConnectorInvocationService;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmExternalWaitService;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HunyuanExternalTriggerDelegateTest {

    @Test
    void executeShouldMapFrozenRequestPrepareWaitAndMapResponse() {
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        BpmDefinitionNodeDao nodeDao = Mockito.mock(BpmDefinitionNodeDao.class);
        BpmConnectorInvocationService invocationService = Mockito.mock(BpmConnectorInvocationService.class);
        BpmExternalWaitService waitService = Mockito.mock(BpmExternalWaitService.class);
        DelegateExecution execution = Mockito.mock(DelegateExecution.class);
        Expression nodeKey = expression("finance_sync", execution);
        Expression connectorKey = expression("finance", execution);
        Expression operationKey = expression("createExpense", execution);
        Expression waitMode = expression("WAIT_CALLBACK", execution);
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(31L);
        instance.setDefinitionId(41L);
        instance.setEngineProcessInstanceId("process-31");
        instance.setCurrentFormDataSnapshotJson("{\"approvedAmount\":1000}");
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(51L);
        node.setCompiledNodeSnapshotJson("""
                {
                  "connectorVersion":3,
                  "requestMapping":{"amount":"approvedAmount"},
                  "responseMapping":{"externalNo":"financeNo"},
                  "timeoutPolicy":{"timeoutAfter":"PT30M"}
                }
                """);
        when(execution.getVariable("hunyuanInstanceId")).thenReturn(31L);
        when(execution.getProcessInstanceId()).thenReturn("process-31");
        when(execution.getId()).thenReturn("execution-71");
        when(instanceDao.selectById(31L)).thenReturn(instance);
        when(nodeDao.selectOne(any())).thenReturn(node);
        when(waitService.prepareWait(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BpmExternalWaitService.PreparedWait("callback-token", "31:finance_sync:1", 3));
        when(invocationService.invokePersistent(
                eq("M5:CONNECTOR:31:finance_sync:execution-71"), eq(31L),
                eq("finance"), eq(3), eq("createExpense"), any()))
                .thenReturn(JSONObject.parseObject("{\"externalNo\":\"FIN-1001\"}"));
        HunyuanExternalTriggerDelegate delegate = new HunyuanExternalTriggerDelegate();
        setField(delegate, "bpmInstanceDao", instanceDao);
        setField(delegate, "bpmDefinitionNodeDao", nodeDao);
        setField(delegate, "bpmConnectorInvocationService", invocationService);
        setField(delegate, "bpmExternalWaitService", waitService);
        setField(delegate, "externalNodeKey", nodeKey);
        setField(delegate, "connectorKey", connectorKey);
        setField(delegate, "operationKey", operationKey);
        setField(delegate, "waitMode", waitMode);

        delegate.execute(execution);

        ArgumentCaptor<JSONObject> requestCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(invocationService).invokePersistent(
                eq("M5:CONNECTOR:31:finance_sync:execution-71"), eq(31L),
                eq("finance"), eq(3), eq("createExpense"), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getBigDecimal("amount")).isEqualByComparingTo("1000");
        assertThat(requestCaptor.getValue().getString("callbackToken")).isEqualTo("callback-token");
        assertThat(requestCaptor.getValue().getString("correlationKey")).isEqualTo("31:finance_sync:1");
        verify(execution).setVariable("financeNo", "FIN-1001");
    }

    private Expression expression(String value, DelegateExecution execution) {
        Expression expression = Mockito.mock(Expression.class);
        when(expression.getValue(execution)).thenReturn(value);
        return expression;
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
