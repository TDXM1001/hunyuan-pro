package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmNotificationRecordVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmNotificationChannelSender;
import com.hunyuan.sa.bpm.module.runtime.service.BpmNotificationCommand;
import com.hunyuan.sa.bpm.module.runtime.service.BpmNotificationListenerService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmNotificationRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmNotificationListenerServiceTest {

    private BpmNotificationListenerService listenerService;

    private BpmNotificationChannelSender channelSender;

    private BpmNotificationRecordService notificationRecordService;

    @BeforeEach
    void setUp() {
        listenerService = new BpmNotificationListenerService();
        channelSender = Mockito.mock(BpmNotificationChannelSender.class);
        notificationRecordService = Mockito.mock(BpmNotificationRecordService.class);
        setField(listenerService, "channelSender", channelSender);
        setField(listenerService, "notificationRecordService", notificationRecordService);
    }

    @Test
    void dispatchShouldRecordSuccessForMessageChannel() {
        when(notificationRecordService.createPendingRecord(any(BpmNotificationCommand.class), eq("MESSAGE")))
                .thenReturn(record(10L));

        listenerService.dispatch(buildCommand(List.of("MESSAGE")));

        verify(channelSender).send(any(BpmNotificationCommand.class), eq("MESSAGE"));
        verify(notificationRecordService).markSuccess(eq(10L), any(String.class));
    }

    @Test
    void dispatchShouldRecordFailWhenMessageServiceThrows() {
        when(notificationRecordService.createPendingRecord(any(BpmNotificationCommand.class), eq("MESSAGE")))
                .thenReturn(record(10L));
        doThrow(new IllegalStateException("message failed"))
                .when(channelSender).send(any(BpmNotificationCommand.class), eq("MESSAGE"));

        listenerService.dispatch(buildCommand(List.of("MESSAGE")));

        verify(notificationRecordService).markFail(10L, "message failed");
    }

    @Test
    void dispatchShouldContinueOtherChannelsAfterFailure() {
        when(notificationRecordService.createPendingRecord(any(BpmNotificationCommand.class), eq("MESSAGE")))
                .thenReturn(record(10L));
        when(notificationRecordService.createPendingRecord(any(BpmNotificationCommand.class), eq("MAIL")))
                .thenReturn(record(20L));
        doThrow(new IllegalStateException("message failed"))
                .when(channelSender).send(any(BpmNotificationCommand.class), eq("MESSAGE"));

        listenerService.dispatch(buildCommand(List.of("MESSAGE", "MAIL")));

        verify(notificationRecordService).markFail(10L, "message failed");
        verify(notificationRecordService).markSuccess(eq(20L), any(String.class));
    }

    private BpmNotificationCommand buildCommand(List<String> channels) {
        return new BpmNotificationCommand(
                channels,
                88L,
                11L,
                6L,
                7L,
                "TASK_CREATED",
                1001L,
                "{\"employeeId\":1001,\"actualName\":\"Alice\"}",
                "13800000000",
                List.of("alice@example.com"),
                "待办提醒",
                "待办提醒",
                "你有一个新的待办任务",
                "bpm_task_created"
        );
    }

    private BpmNotificationRecordVO record(Long recordId) {
        BpmNotificationRecordVO record = new BpmNotificationRecordVO();
        record.setNotificationRecordId(recordId);
        return record;
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
