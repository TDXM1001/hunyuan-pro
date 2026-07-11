package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionDao;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskFormContextVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskFormContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class BpmTaskFormContextServiceTest {

    private BpmTaskFormContextService service;
    private BpmDefinitionDao definitionDao;
    private BpmDefinitionNodeDao definitionNodeDao;

    @BeforeEach
    void setUp() {
        service = new BpmTaskFormContextService();
        definitionDao = Mockito.mock(BpmDefinitionDao.class);
        definitionNodeDao = Mockito.mock(BpmDefinitionNodeDao.class);
        setField(service, "bpmDefinitionDao", definitionDao);
        setField(service, "bpmDefinitionNodeDao", definitionNodeDao);
    }

    @Test
    void buildShouldRemoveHiddenFieldAndDefaultUnconfiguredFieldToReadonly() {
        BpmDefinitionEntity definition = new BpmDefinitionEntity();
        definition.setDefinitionId(1L);
        definition.setFormSchemaSnapshotJson("{\"fields\":[{\"field\":\"amount\",\"type\":\"number\"},{\"field\":\"applicant\",\"type\":\"input\"},{\"field\":\"internalCode\",\"type\":\"input\"}]}");
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(2L);
        node.setCompiledNodeSnapshotJson("{\"fieldPermissions\":[{\"fieldKey\":\"amount\",\"permission\":\"EDITABLE\",\"required\":true},{\"fieldKey\":\"internalCode\",\"permission\":\"HIDDEN\",\"required\":false}]}");
        when(definitionDao.selectById(1L)).thenReturn(definition);
        when(definitionNodeDao.selectById(2L)).thenReturn(node);

        BpmTaskEntity task = new BpmTaskEntity();
        task.setDefinitionId(1L);
        task.setDefinitionNodeId(2L);
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setFormDataVersion(3L);
        instance.setCurrentFormDataSnapshotJson("{\"amount\":100,\"applicant\":\"张三\",\"internalCode\":\"secret\"}");

        BpmTaskFormContextVO context = service.buildForEmployeeTask(task, instance);

        assertThat(context).isNotNull();
        assertThat(context.getDataVersion()).isEqualTo(3L);
        assertThat(context.getFormSchemaJson()).contains("amount").contains("applicant").doesNotContain("internalCode");
        assertThat(context.getFormDataJson()).contains("amount").contains("applicant").doesNotContain("secret");
        assertThat(context.getPermissions())
                .extracting(permission -> permission.getFieldKey() + ":" + permission.getPermission())
                .containsExactly("amount:EDITABLE", "applicant:READONLY");
    }

    @Test
    void buildShouldFailClosedWhenPermissionSnapshotIsCorrupt() {
        BpmDefinitionEntity definition = new BpmDefinitionEntity();
        definition.setFormSchemaSnapshotJson("{\"fields\":[{\"field\":\"amount\",\"type\":\"number\"}]}");
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setCompiledNodeSnapshotJson("{");
        when(definitionDao.selectById(1L)).thenReturn(definition);
        when(definitionNodeDao.selectById(2L)).thenReturn(node);
        BpmTaskEntity task = new BpmTaskEntity();
        task.setDefinitionId(1L);
        task.setDefinitionNodeId(2L);

        assertThat(service.buildForEmployeeTask(task, new BpmInstanceEntity())).isNull();
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
