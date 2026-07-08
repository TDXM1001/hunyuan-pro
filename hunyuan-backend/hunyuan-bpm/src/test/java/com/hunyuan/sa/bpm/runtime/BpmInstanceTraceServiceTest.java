package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCallbackRecordVO;
import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCommandRecordVO;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessIntegrationRecordService;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceTraceVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmNotificationRecordVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskActionLogVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceTraceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmNotificationRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class BpmInstanceTraceServiceTest {

    private BpmInstanceTraceService traceService;

    private BpmInstanceService bpmInstanceService;

    private BpmBusinessIntegrationRecordService integrationRecordService;

    private BpmNotificationRecordService notificationRecordService;

    @BeforeEach
    void setUp() {
        traceService = new BpmInstanceTraceService();
        bpmInstanceService = Mockito.mock(BpmInstanceService.class);
        integrationRecordService = Mockito.mock(BpmBusinessIntegrationRecordService.class);
        notificationRecordService = Mockito.mock(BpmNotificationRecordService.class);
        setField(traceService, "bpmInstanceService", bpmInstanceService);
        setField(traceService, "integrationRecordService", integrationRecordService);
        setField(traceService, "notificationRecordService", notificationRecordService);
    }

    @Test
    void getTraceShouldAggregateInstanceTasksActionsCallbacksAndCommands() {
        BpmTaskVO task = new BpmTaskVO();
        task.setTaskId(11L);
        task.setTaskName("Manager approval");
        BpmTaskActionLogVO actionLog = new BpmTaskActionLogVO();
        actionLog.setActionLogId(21L);
        actionLog.setActionType("APPROVED");
        BpmInstanceDetailVO detail = new BpmInstanceDetailVO();
        detail.setInstanceId(88L);
        detail.setInstanceNo("DK20260708NO00001");
        detail.setTitle("Expense approval");
        detail.setCurrentTasks(List.of(task));
        detail.setActionLogs(List.of(actionLog));
        BpmCallbackRecordVO callbackRecord = new BpmCallbackRecordVO();
        callbackRecord.setCallbackRecordId(31L);
        callbackRecord.setInstanceId(88L);
        BpmCommandRecordVO commandRecord = new BpmCommandRecordVO();
        commandRecord.setCommandRecordId(41L);
        commandRecord.setInstanceId(88L);
        BpmNotificationRecordVO notificationRecord = new BpmNotificationRecordVO();
        notificationRecord.setNotificationRecordId(51L);
        notificationRecord.setInstanceId(88L);
        when(bpmInstanceService.getDetail(88L)).thenReturn(ResponseDTO.ok(detail));
        when(integrationRecordService.queryCallbackRecordsByInstanceId(88L)).thenReturn(List.of(callbackRecord));
        when(integrationRecordService.queryCommandRecordsByInstanceId(88L)).thenReturn(List.of(commandRecord));
        when(notificationRecordService.queryByInstanceId(88L)).thenReturn(List.of(notificationRecord));

        ResponseDTO<BpmInstanceTraceVO> response = traceService.getTrace(88L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getInstance().getInstanceId()).isEqualTo(88L);
        assertThat(response.getData().getCurrentTasks()).hasSize(1);
        assertThat(response.getData().getActionLogs()).hasSize(1);
        assertThat(response.getData().getCallbackRecords()).hasSize(1);
        assertThat(response.getData().getCommandRecords()).hasSize(1);
        assertThat(response.getData().getNotificationRecords()).hasSize(1);
    }

    @Test
    void getTraceShouldReturnDataNotExistWhenInstanceIsMissing() {
        when(bpmInstanceService.getDetail(404L)).thenReturn(ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST));

        ResponseDTO<BpmInstanceTraceVO> response = traceService.getTrace(404L);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getCode()).isEqualTo(UserErrorCode.DATA_NOT_EXIST.getCode());
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("设置测试字段失败: " + fieldName, ex);
        }
    }
}
