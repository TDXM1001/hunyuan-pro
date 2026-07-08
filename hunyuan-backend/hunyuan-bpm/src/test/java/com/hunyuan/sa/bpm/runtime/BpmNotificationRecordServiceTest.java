package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.common.enumeration.BpmNotificationSendStatusEnum;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmNotificationRecordDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmNotificationRecordEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmNotificationRecordVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmNotificationCommand;
import com.hunyuan.sa.bpm.module.runtime.service.BpmNotificationRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmNotificationRecordServiceTest {

    private BpmNotificationRecordService recordService;

    private BpmNotificationRecordDao notificationRecordDao;

    @BeforeEach
    void setUp() {
        recordService = new BpmNotificationRecordService();
        notificationRecordDao = Mockito.mock(BpmNotificationRecordDao.class);
        setField(recordService, "notificationRecordDao", notificationRecordDao);
    }

    @Test
    void createPendingRecordShouldPersistBpmContext() {
        when(notificationRecordDao.insert(any(BpmNotificationRecordEntity.class))).thenAnswer(invocation -> {
            BpmNotificationRecordEntity entity = invocation.getArgument(0);
            entity.setNotificationRecordId(10L);
            return 1;
        });
        BpmNotificationCommand command = buildCommand();

        BpmNotificationRecordVO record = recordService.createPendingRecord(command, "MESSAGE");

        assertThat(record.getNotificationRecordId()).isEqualTo(10L);
        assertThat(record.getInstanceId()).isEqualTo(88L);
        assertThat(record.getTaskId()).isEqualTo(11L);
        assertThat(record.getEventKey()).isEqualTo("TASK_CREATED");
        assertThat(record.getChannel()).isEqualTo("MESSAGE");
        assertThat(record.getSendStatus()).isEqualTo(BpmNotificationSendStatusEnum.PENDING.getValue());
        verify(notificationRecordDao).insert(any(BpmNotificationRecordEntity.class));
    }

    @Test
    void markSuccessShouldUpdateStatusAndResponseSnapshot() {
        recordService.markSuccess(10L, "{\"messageId\":1}");

        verify(notificationRecordDao).updateById(any(BpmNotificationRecordEntity.class));
    }

    @Test
    void markFailShouldTruncateFailureReason() {
        String longReason = "x".repeat(1200);

        recordService.markFail(10L, longReason);

        verify(notificationRecordDao).updateById(any(BpmNotificationRecordEntity.class));
    }

    @Test
    void queryByInstanceIdShouldReturnMappedRecords() {
        BpmNotificationRecordEntity entity = new BpmNotificationRecordEntity();
        entity.setNotificationRecordId(10L);
        entity.setInstanceId(88L);
        entity.setTaskId(11L);
        entity.setEventKey("TASK_CREATED");
        entity.setChannel("MESSAGE");
        entity.setReceiverEmployeeId(1001L);
        entity.setSendStatus(BpmNotificationSendStatusEnum.SUCCESS.getValue());
        when(notificationRecordDao.selectList(any())).thenReturn(List.of(entity));

        List<BpmNotificationRecordVO> records = recordService.queryByInstanceId(88L);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getNotificationRecordId()).isEqualTo(10L);
        assertThat(records.get(0).getInstanceId()).isEqualTo(88L);
        assertThat(records.get(0).getChannel()).isEqualTo("MESSAGE");
    }

    private BpmNotificationCommand buildCommand() {
        return new BpmNotificationCommand(
                List.of("MESSAGE"),
                88L,
                11L,
                6L,
                7L,
                "TASK_CREATED",
                1001L,
                "{\"employeeId\":1001,\"actualName\":\"Alice\"}",
                null,
                List.of(),
                "待办提醒",
                "待办提醒",
                "你有一个新的待办任务",
                "bpm_task_created"
        );
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
