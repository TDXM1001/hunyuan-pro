package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTimeEventDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTimeEventEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTimeEventService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmRuntimeCommandCoordinator;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmTimeEventServiceTest {

    @Test
    void scheduleTaskSlaShouldCreateReminderAndDueFactsAndUpdateTaskDueAt() {
        BpmTimeEventService service = new BpmTimeEventService();
        BpmTimeEventDao eventDao = Mockito.mock(BpmTimeEventDao.class);
        BpmTaskDao taskDao = Mockito.mock(BpmTaskDao.class);
        setField(service, "bpmTimeEventDao", eventDao);
        setField(service, "bpmTaskDao", taskDao);
        when(eventDao.selectCount(any())).thenReturn(0L);

        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(31L);
        instance.setDefinitionId(41L);
        instance.setEngineProcessInstanceId("process-31");
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(51L);
        node.setCompiledNodeSnapshotJson("""
                {"authoredNodeKey":"review","taskSlaPolicy":{"dueAfter":"PT2H","reminderSchedule":["PT1H"],"timeoutAction":"REMIND_ONLY"}}
                """);
        BpmTaskEntity task = new BpmTaskEntity();
        task.setTaskId(61L);
        task.setEngineTaskId("task-61");
        task.setEngineExecutionId("execution-61");
        task.setAssignedAt(LocalDateTime.of(2026, 7, 11, 9, 0));

        int count = service.scheduleTaskSla(instance, node, task);

        assertThat(count).isEqualTo(2);
        ArgumentCaptor<BpmTimeEventEntity> eventCaptor = ArgumentCaptor.forClass(BpmTimeEventEntity.class);
        verify(eventDao, Mockito.times(2)).insert(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).extracting(BpmTimeEventEntity::getEventKey)
                .containsExactly("TASK:61:SLA_REMINDER:1", "TASK:61:SLA_DUE");
        assertThat(eventCaptor.getAllValues()).extracting(BpmTimeEventEntity::getScheduledAt)
                .containsExactly(
                        LocalDateTime.of(2026, 7, 11, 10, 0),
                        LocalDateTime.of(2026, 7, 11, 11, 0)
                );
        ArgumentCaptor<BpmTaskEntity> taskCaptor = ArgumentCaptor.forClass(BpmTaskEntity.class);
        verify(taskDao).updateById(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getDueAt()).isEqualTo(LocalDateTime.of(2026, 7, 11, 11, 0));
    }

    @Test
    void triggerShouldExecuteScheduledEventOnlyOnce() {
        BpmTimeEventService service = new BpmTimeEventService();
        BpmTimeEventDao eventDao = Mockito.mock(BpmTimeEventDao.class);
        BpmRuntimeCommandCoordinator coordinator = Mockito.mock(BpmRuntimeCommandCoordinator.class);
        setField(service, "bpmTimeEventDao", eventDao);
        setCoordinatorProvider(service, coordinator);
        BpmTimeEventEntity event = new BpmTimeEventEntity();
        event.setTimeEventId(71L);
        event.setEventKey("TASK:61:SLA_DUE");
        event.setEventKind("SLA_DUE");
        event.setEventStatus("SCHEDULED");
        event.setTriggerCount(0);
        when(eventDao.selectOne(any())).thenReturn(event);
        when(eventDao.update(any(), any())).thenReturn(1);

        boolean processed = service.trigger("process-31", "review", "SLA_DUE", "execution-71", "job-71");

        assertThat(processed).isTrue();
        verify(coordinator).executeTimeEvent(event);
        verify(eventDao, Mockito.times(2)).updateById(any(BpmTimeEventEntity.class));
    }

    @Test
    void triggerShouldIgnoreSucceededEvent() {
        BpmTimeEventService service = new BpmTimeEventService();
        BpmTimeEventDao eventDao = Mockito.mock(BpmTimeEventDao.class);
        BpmRuntimeCommandCoordinator coordinator = Mockito.mock(BpmRuntimeCommandCoordinator.class);
        setField(service, "bpmTimeEventDao", eventDao);
        setCoordinatorProvider(service, coordinator);
        BpmTimeEventEntity event = new BpmTimeEventEntity();
        event.setTimeEventId(71L);
        event.setEventStatus("SUCCEEDED");
        when(eventDao.selectOne(any())).thenReturn(event);

        boolean processed = service.trigger("process-31", "review", "SLA_DUE", "execution-71", "job-71");

        assertThat(processed).isFalse();
        verify(coordinator, Mockito.never()).executeTimeEvent(any());
    }

    @Test
    void scheduleDelayShouldProjectDurationFromFrozenNodeSnapshot() {
        BpmTimeEventService service = new BpmTimeEventService();
        BpmTimeEventDao eventDao = Mockito.mock(BpmTimeEventDao.class);
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        BpmDefinitionNodeDao nodeDao = Mockito.mock(BpmDefinitionNodeDao.class);
        setField(service, "bpmTimeEventDao", eventDao);
        setField(service, "bpmInstanceDao", instanceDao);
        setField(service, "bpmDefinitionNodeDao", nodeDao);
        when(eventDao.selectCount(any())).thenReturn(0L);
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(31L); instance.setDefinitionId(41L); instance.setEngineProcessInstanceId("process-31");
        when(instanceDao.selectById(31L)).thenReturn(instance);
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setDefinitionNodeId(51L); node.setNodeKey("cooling");
        node.setCompiledNodeSnapshotJson("{\"mode\":\"DURATION\",\"value\":\"PT30M\",\"timezone\":\"Asia/Shanghai\"}");
        when(nodeDao.selectOne(any())).thenReturn(node);

        service.scheduleDelay(31L, "process-31", "execution-81", "cooling");

        verify(eventDao).insert(Mockito.argThat((BpmTimeEventEntity event) ->
                "EXEC:execution-81:DELAY".equals(event.getEventKey())
                        && "DELAY".equals(event.getEventKind())
                        && "cooling".equals(event.getNodeKey())
                        && event.getScheduledAt().isAfter(LocalDateTime.now().plusMinutes(29))
        ));
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

    private static void setCoordinatorProvider(
            BpmTimeEventService service,
            BpmRuntimeCommandCoordinator coordinator
    ) {
        @SuppressWarnings("unchecked")
        ObjectProvider<BpmRuntimeCommandCoordinator> provider = Mockito.mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(coordinator);
        setField(service, "bpmRuntimeCommandCoordinatorProvider", provider);
    }
}
