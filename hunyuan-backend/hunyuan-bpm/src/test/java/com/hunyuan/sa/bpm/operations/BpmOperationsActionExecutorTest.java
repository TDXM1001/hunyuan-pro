package com.hunyuan.sa.bpm.operations;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackService;
import com.hunyuan.sa.bpm.module.operations.domain.entity.BpmOperationsCaseEntity;
import com.hunyuan.sa.bpm.module.operations.service.BpmOperationsActionExecutor;
import com.hunyuan.sa.bpm.module.runtime.service.BpmExternalWaitOperationsService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTimeEventOperationsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class BpmOperationsActionExecutorTest {

    @Test
    void retryMustRouteOnlyToRegisteredRecoveryService() {
        Fixture fixture = fixture();
        BpmOperationsCaseEntity operationsCase = operationsCase("TIME_EVENT", 11L);
        operationsCase.setRetryableFlag(true);
        Mockito.when(fixture.timeEventService.retry(11L)).thenReturn(ResponseDTO.ok());

        ResponseDTO<String> response = fixture.executor.execute(operationsCase, "RETRY", "人工确认重试");

        assertThat(response.getOk()).isTrue();
        Mockito.verify(fixture.timeEventService).retry(11L);
        Mockito.verifyNoInteractions(fixture.externalWaitService, fixture.callbackService, fixture.instanceService);
    }

    @Test
    void compensationMustRouteToCallbackServiceWithReason() {
        Fixture fixture = fixture();
        BpmOperationsCaseEntity operationsCase = operationsCase("CALLBACK", 12L);
        operationsCase.setCompensableFlag(true);
        Mockito.when(fixture.callbackService.compensate(Mockito.eq(12L), Mockito.any())).thenReturn(ResponseDTO.ok());

        ResponseDTO<String> response = fixture.executor.execute(operationsCase, "COMPENSATE", "业务侧确认补偿完成");

        assertThat(response.getOk()).isTrue();
        Mockito.verify(fixture.callbackService).compensate(Mockito.eq(12L), Mockito.argThat(form ->
                "业务侧确认补偿完成".equals(form.getReason())
        ));
    }

    @Test
    void terminateMustUseAdminInstanceCancellation() {
        Fixture fixture = fixture();
        BpmOperationsCaseEntity operationsCase = operationsCase("COMMAND", 13L);
        operationsCase.setInstanceId(100L);
        Mockito.when(fixture.instanceService.adminCancel(Mockito.any())).thenReturn(ResponseDTO.ok());

        ResponseDTO<String> response = fixture.executor.execute(operationsCase, "TERMINATE", "外部副作用无法恢复，终止流程");

        assertThat(response.getOk()).isTrue();
        Mockito.verify(fixture.instanceService).adminCancel(Mockito.argThat(form ->
                Long.valueOf(100L).equals(form.getInstanceId())
                        && "外部副作用无法恢复，终止流程".equals(form.getCancelReason())
        ));
    }

    @Test
    void unregisteredRetryMustFailClosed() {
        Fixture fixture = fixture();
        BpmOperationsCaseEntity operationsCase = operationsCase("COMMAND", 13L);
        operationsCase.setRetryableFlag(true);

        ResponseDTO<String> response = fixture.executor.execute(operationsCase, "RETRY", "重试命令");

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("没有登记安全重试路径");
        Mockito.verifyNoInteractions(fixture.timeEventService, fixture.externalWaitService,
                fixture.callbackService, fixture.instanceService);
    }

    private Fixture fixture() {
        BpmOperationsActionExecutor executor = new BpmOperationsActionExecutor();
        BpmTimeEventOperationsService timeEventService = Mockito.mock(BpmTimeEventOperationsService.class);
        BpmExternalWaitOperationsService externalWaitService = Mockito.mock(BpmExternalWaitOperationsService.class);
        BpmBusinessCallbackService callbackService = Mockito.mock(BpmBusinessCallbackService.class);
        BpmInstanceService instanceService = Mockito.mock(BpmInstanceService.class);
        setField(executor, "bpmTimeEventOperationsService", timeEventService);
        setField(executor, "bpmExternalWaitOperationsService", externalWaitService);
        setField(executor, "bpmBusinessCallbackService", callbackService);
        setField(executor, "bpmInstanceService", instanceService);
        return new Fixture(executor, timeEventService, externalWaitService, callbackService, instanceService);
    }

    private BpmOperationsCaseEntity operationsCase(String sourceType, Long sourceId) {
        BpmOperationsCaseEntity operationsCase = new BpmOperationsCaseEntity();
        operationsCase.setSourceType(sourceType);
        operationsCase.setSourceId(sourceId);
        return operationsCase;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record Fixture(
            BpmOperationsActionExecutor executor,
            BpmTimeEventOperationsService timeEventService,
            BpmExternalWaitOperationsService externalWaitService,
            BpmBusinessCallbackService callbackService,
            BpmInstanceService instanceService
    ) {
    }
}
