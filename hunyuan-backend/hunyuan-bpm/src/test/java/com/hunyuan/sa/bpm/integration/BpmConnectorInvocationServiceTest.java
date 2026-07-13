package com.hunyuan.sa.bpm.integration;

import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmConnectorDefinitionEntity;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCommandRecordEntity;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCommandRecordDao;
import com.hunyuan.sa.bpm.module.integration.service.BpmConnectorEndpointPolicy;
import com.hunyuan.sa.bpm.module.integration.service.BpmConnectorInvocationService;
import com.hunyuan.sa.bpm.module.integration.service.BpmConnectorReferenceResolver;
import com.hunyuan.sa.bpm.module.integration.service.BpmConnectorRegistryService;
import com.hunyuan.sa.bpm.module.integration.service.BpmConnectorTransport;
import com.hunyuan.sa.bpm.module.integration.service.BpmConnectorCommandStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class BpmConnectorInvocationServiceTest {

    @Test
    void invokeShouldRetryOnlyIdempotentOperation() {
        Fixture fixture = fixture(true);
        JSONObject success = JSONObject.parseObject("{\"externalNo\":\"FIN-1\"}");
        when(fixture.transport.invoke(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("timeout"))
                .thenThrow(new IllegalStateException("timeout"))
                .thenReturn(success);

        JSONObject result = fixture.service.invoke("finance", 3, "createExpense", new JSONObject());

        assertThat(result.getString("externalNo")).isEqualTo("FIN-1");
        Mockito.verify(fixture.transport, Mockito.times(3)).invoke(any(), any(), any(), any(), any());
    }

    @Test
    void invokeShouldNotRetryNonIdempotentOperation() {
        Fixture fixture = fixture(false);
        when(fixture.transport.invoke(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("timeout"));

        assertThatThrownBy(() -> fixture.service.invoke("finance", 3, "createExpense", new JSONObject()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("timeout");
        Mockito.verify(fixture.transport).invoke(any(), any(), any(), any(), any());
    }

    @Test
    void persistentInvokeShouldReuseSucceededResponseAndKeepStableIdempotencyKey() {
        Fixture fixture = fixture(true);
        BpmCommandRecordEntity succeeded = new BpmCommandRecordEntity();
        succeeded.setCommandRecordId(11L);
        succeeded.setCommandKey("M5:CONNECTOR:31:external:execution-1");
        succeeded.setCommandStatus(1);
        succeeded.setResponsePayloadJson("{\"externalNo\":\"FIN-9\"}");
        when(fixture.commandDao.selectOne(any())).thenReturn(succeeded);

        JSONObject result = fixture.service.invokePersistent(succeeded.getCommandKey(), 31L,
                "finance", 3, "createExpense", new JSONObject());

        assertThat(result.getString("externalNo")).isEqualTo("FIN-9");
        Mockito.verifyNoInteractions(fixture.transport);
    }

    @Test
    void persistentInvokeShouldRecordFailureAndRetrySchedule() {
        Fixture fixture = fixture(false);
        when(fixture.commandDao.selectOne(any())).thenReturn(null);
        Mockito.doAnswer(invocation -> {
            invocation.getArgument(0, BpmCommandRecordEntity.class).setCommandRecordId(12L);
            return 1;
        }).when(fixture.commandDao).insert(any(BpmCommandRecordEntity.class));
        when(fixture.transport.invoke(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("provider down"));

        assertThatThrownBy(() -> fixture.service.invokePersistent("M5:CONNECTOR:31:external:execution-2",
                31L, "finance", 3, "createExpense", new JSONObject()))
                .hasMessageContaining("provider down");

        verify(fixture.commandDao).updateById(Mockito.argThat((BpmCommandRecordEntity update) ->
                Integer.valueOf(2).equals(update.getCommandStatus())
                        && Integer.valueOf(1).equals(update.getAttemptCount())
                        && update.getNextRetryAt() != null
                        && update.getFailureReason().contains("provider down")
        ));
    }

    private Fixture fixture(boolean idempotent) {
        BpmConnectorRegistryService registry = Mockito.mock(BpmConnectorRegistryService.class);
        BpmConnectorReferenceResolver resolver = Mockito.mock(BpmConnectorReferenceResolver.class);
        BpmConnectorEndpointPolicy endpointPolicy = Mockito.mock(BpmConnectorEndpointPolicy.class);
        BpmConnectorTransport transport = Mockito.mock(BpmConnectorTransport.class);
        BpmCommandRecordDao commandDao = Mockito.mock(BpmCommandRecordDao.class);
        BpmConnectorDefinitionEntity definition = new BpmConnectorDefinitionEntity();
        definition.setBaseEndpointRef("env:FINANCE_ENDPOINT");
        definition.setCredentialRef("env:FINANCE_TOKEN");
        definition.setTimeoutMillis(5000);
        definition.setRetryPolicyJson("{\"maxAttempts\":3}");
        BpmConnectorRegistryService.RegisteredOperation operation =
                new BpmConnectorRegistryService.RegisteredOperation("createExpense", "/expenses", "POST", idempotent);
        when(registry.requireOperation("finance", 3, "createExpense"))
                .thenReturn(new BpmConnectorRegistryService.RegisteredConnector(definition, operation));
        when(resolver.resolve("env:FINANCE_ENDPOINT")).thenReturn("https://api.example.com");
        when(resolver.resolve("env:FINANCE_TOKEN")).thenReturn("credential");
        BpmConnectorInvocationService service = new BpmConnectorInvocationService();
        BpmConnectorCommandStore commandStore = new BpmConnectorCommandStore();
        setField(commandStore, "bpmCommandRecordDao", commandDao);
        setField(service, "bpmConnectorRegistryService", registry);
        setField(service, "bpmConnectorReferenceResolver", resolver);
        setField(service, "bpmConnectorEndpointPolicy", endpointPolicy);
        setField(service, "bpmConnectorTransport", transport);
        setField(service, "bpmConnectorCommandStore", commandStore);
        return new Fixture(service, transport, commandDao);
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

    private record Fixture(BpmConnectorInvocationService service, BpmConnectorTransport transport,
                           BpmCommandRecordDao commandDao) {
    }
}
