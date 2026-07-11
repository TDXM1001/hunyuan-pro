package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmFormDataChangeDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmFormDataChangeEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmFieldPermissionVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskFormContextVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmFormDataMutationService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmRuntimeFormDataValidator;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskFormContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmFormDataMutationServiceTest {

    private BpmFormDataMutationService service;
    private BpmTaskFormContextService contextService;
    private BpmInstanceDao instanceDao;
    private BpmFormDataChangeDao changeDao;

    @BeforeEach
    void setUp() {
        service = new BpmFormDataMutationService();
        contextService = Mockito.mock(BpmTaskFormContextService.class);
        instanceDao = Mockito.mock(BpmInstanceDao.class);
        changeDao = Mockito.mock(BpmFormDataChangeDao.class);
        setField(service, "bpmTaskFormContextService", contextService);
        setField(service, "bpmRuntimeFormDataValidator", new BpmRuntimeFormDataValidator());
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "bpmFormDataChangeDao", changeDao);
    }

    @Test
    void applyShouldMergeEditablePatchIncrementVersionAndWriteAudit() {
        BpmTaskEntity task = task();
        BpmInstanceEntity instance = instance();
        when(contextService.buildForEmployeeTask(task, instance)).thenReturn(context("EDITABLE", true));
        when(contextService.getPublishedFormSchemaJson(task))
                .thenReturn("{\"fields\":[{\"field\":\"approvedAmount\",\"type\":\"number\"}]}");

        ResponseDTO<BpmFormDataMutationService.MutationResult> response = service.applyTaskApprovePatch(
                task,
                instance,
                new BpmEmployeeSnapshot(9L, "财务张三", 1L, "财务部", null, null),
                3L,
                "{\"approvedAmount\":98}"
        );

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().changed()).isTrue();
        assertThat(response.getData().afterVersion()).isEqualTo(4L);
        ArgumentCaptor<BpmInstanceEntity> updateCaptor = ArgumentCaptor.forClass(BpmInstanceEntity.class);
        verify(instanceDao).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getCurrentFormDataSnapshotJson()).contains("\"approvedAmount\":98");
        assertThat(updateCaptor.getValue().getFormDataVersion()).isEqualTo(4L);
        ArgumentCaptor<BpmFormDataChangeEntity> changeCaptor = ArgumentCaptor.forClass(BpmFormDataChangeEntity.class);
        verify(changeDao).insert(changeCaptor.capture());
        assertThat(changeCaptor.getValue().getChangeSource()).isEqualTo("TASK_APPROVED");
        assertThat(changeCaptor.getValue().getBeforeVersion()).isEqualTo(3L);
        assertThat(changeCaptor.getValue().getAfterVersion()).isEqualTo(4L);
    }

    @Test
    void applyShouldRejectStaleVersionBeforeWriting() {
        ResponseDTO<BpmFormDataMutationService.MutationResult> response = service.applyTaskApprovePatch(
                task(),
                instance(),
                new BpmEmployeeSnapshot(9L, "财务张三", 1L, "财务部", null, null),
                2L,
                "{\"approvedAmount\":98}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("FORM_DATA_VERSION_CONFLICT");
        verify(instanceDao, never()).updateById(Mockito.<BpmInstanceEntity>any());
        verify(changeDao, never()).insert(Mockito.<BpmFormDataChangeEntity>any());
    }

    @Test
    void applyShouldRejectReadonlyPatch() {
        BpmTaskEntity task = task();
        BpmInstanceEntity instance = instance();
        when(contextService.buildForEmployeeTask(task, instance)).thenReturn(context("READONLY", false));

        ResponseDTO<BpmFormDataMutationService.MutationResult> response = service.applyTaskApprovePatch(
                task,
                instance,
                new BpmEmployeeSnapshot(9L, "财务张三", 1L, "财务部", null, null),
                3L,
                "{\"approvedAmount\":98}"
        );

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("approvedAmount").contains("可编辑");
    }

    private BpmTaskFormContextVO context(String mode, boolean required) {
        BpmFieldPermissionVO permission = new BpmFieldPermissionVO();
        permission.setFieldKey("approvedAmount");
        permission.setPermission(mode);
        permission.setRequired(required);
        BpmTaskFormContextVO context = new BpmTaskFormContextVO();
        context.setDataVersion(3L);
        context.setFormSchemaJson("{\"fields\":[{\"field\":\"approvedAmount\",\"type\":\"number\"}]}"
        );
        context.setFormDataJson("{\"approvedAmount\":100}");
        context.setPermissions(List.of(permission));
        return context;
    }

    private BpmInstanceEntity instance() {
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(7L);
        instance.setFormDataVersion(3L);
        instance.setCurrentFormDataSnapshotJson("{\"approvedAmount\":100}");
        return instance;
    }

    private BpmTaskEntity task() {
        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(8L);
        task.setInstanceId(7L);
        task.setDefinitionNodeId(6L);
        task.setTaskKey("finance");
        return task;
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
