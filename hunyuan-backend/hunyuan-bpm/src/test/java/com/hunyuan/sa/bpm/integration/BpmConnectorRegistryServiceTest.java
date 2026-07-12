package com.hunyuan.sa.bpm.integration;

import com.hunyuan.sa.bpm.module.integration.dao.BpmConnectorDefinitionDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmConnectorDefinitionEntity;
import com.hunyuan.sa.bpm.module.integration.service.BpmConnectorRegistryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BpmConnectorRegistryServiceTest {

    @Test
    void requireOperationShouldReturnFrozenWhitelistedOperation() {
        BpmConnectorDefinitionDao dao = Mockito.mock(BpmConnectorDefinitionDao.class);
        BpmConnectorDefinitionEntity entity = connector("ENABLED");
        when(dao.selectOne(any())).thenReturn(entity);
        BpmConnectorRegistryService service = new BpmConnectorRegistryService();
        setField(service, "bpmConnectorDefinitionDao", dao);

        BpmConnectorRegistryService.RegisteredConnector connector =
                service.requireOperation("finance", 3, "createExpense");

        assertThat(connector.definition()).isSameAs(entity);
        assertThat(connector.operation().path()).isEqualTo("/expenses");
        assertThat(connector.operation().method()).isEqualTo("POST");
        assertThat(connector.operation().idempotent()).isTrue();
    }

    @Test
    void requireOperationShouldRejectDisabledConnectorAndUnknownOperation() {
        BpmConnectorDefinitionDao dao = Mockito.mock(BpmConnectorDefinitionDao.class);
        when(dao.selectOne(any())).thenReturn(connector("DISABLED"));
        BpmConnectorRegistryService service = new BpmConnectorRegistryService();
        setField(service, "bpmConnectorDefinitionDao", dao);

        assertThatThrownBy(() -> service.requireOperation("finance", 3, "createExpense"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未启用");

        when(dao.selectOne(any())).thenReturn(connector("ENABLED"));
        assertThatThrownBy(() -> service.requireOperation("finance", 3, "deleteEverything"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未登记");
    }

    private BpmConnectorDefinitionEntity connector(String state) {
        BpmConnectorDefinitionEntity entity = new BpmConnectorDefinitionEntity();
        entity.setConnectorKey("finance");
        entity.setConnectorVersion(3);
        entity.setEnabledState(state);
        entity.setAllowedOperationsJson("""
                [{"operationKey":"createExpense","path":"/expenses","method":"POST","idempotent":true}]
                """);
        return entity;
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
