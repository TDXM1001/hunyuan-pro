package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.module.runtime.service.BpmSubProcessFailureRecoveryScheduler;
import com.hunyuan.sa.bpm.module.runtime.service.BpmSubProcessService;
import org.flowable.engine.ManagementService;
import org.flowable.job.api.DeadLetterJobQuery;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmSubProcessFailureRecoverySchedulerTest {
    @Test
    void scanShouldProjectDeadLetterChildFailure() {
        ManagementService management = Mockito.mock(ManagementService.class);
        DeadLetterJobQuery query = Mockito.mock(DeadLetterJobQuery.class);
        Job job = Mockito.mock(Job.class);
        BpmSubProcessService service = Mockito.mock(BpmSubProcessService.class);
        when(management.createDeadLetterJobQuery()).thenReturn(query);
        when(query.listPage(0, 100)).thenReturn(List.of(job));
        when(job.getProcessInstanceId()).thenReturn("child-engine-1");
        when(job.getExceptionMessage()).thenReturn("delegate failed");
        BpmSubProcessFailureRecoveryScheduler scheduler = new BpmSubProcessFailureRecoveryScheduler();
        setField(scheduler, "managementService", management);
        setField(scheduler, "bpmSubProcessService", service);

        scheduler.scan();

        verify(service).recordTechnicalFailure("child-engine-1", "delegate failed");
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
