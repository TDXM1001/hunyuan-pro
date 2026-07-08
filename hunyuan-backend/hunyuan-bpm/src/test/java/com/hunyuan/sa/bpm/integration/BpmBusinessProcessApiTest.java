package com.hunyuan.sa.bpm.integration;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.business.BpmBusinessProcessApi;
import com.hunyuan.sa.bpm.api.business.BpmBusinessProcessApiImpl;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessInstanceStatus;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessResultEvent;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessStartCommand;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCommandRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCommandRecordEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmBusinessProcessApiTest {

    private BpmBusinessProcessApi api;

    private BpmInstanceService bpmInstanceService;

    private BpmInstanceDao bpmInstanceDao;

    private BpmCommandRecordDao bpmCommandRecordDao;

    private BpmCallbackRecordDao bpmCallbackRecordDao;

    @BeforeEach
    void setUp() {
        BpmBusinessProcessApiImpl apiImpl = new BpmBusinessProcessApiImpl();
        api = apiImpl;
        bpmInstanceService = Mockito.mock(BpmInstanceService.class);
        bpmInstanceDao = Mockito.mock(BpmInstanceDao.class);
        bpmCommandRecordDao = Mockito.mock(BpmCommandRecordDao.class);
        bpmCallbackRecordDao = Mockito.mock(BpmCallbackRecordDao.class);
        setField(apiImpl, "bpmInstanceService", bpmInstanceService);
        setField(apiImpl, "bpmInstanceDao", bpmInstanceDao);
        setField(apiImpl, "bpmCommandRecordDao", bpmCommandRecordDao);
        setField(apiImpl, "bpmCallbackRecordDao", bpmCallbackRecordDao);
    }

    @Test
    void startShouldRequireBusinessTypeBusinessIdDefinitionKeyAndStartEmployee() {
        BpmBusinessStartCommand command = new BpmBusinessStartCommand();

        assertThatThrownBy(() -> api.start(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("businessType");
    }

    @Test
    void startShouldRecordCommandAndDelegateToInstanceService() {
        BpmBusinessStartCommand command = buildStartCommand();
        when(bpmCommandRecordDao.selectOne(any())).thenReturn(null);
        when(bpmInstanceService.startBusinessInstance(command)).thenReturn(ResponseDTO.ok(88L));

        Long instanceId = api.start(command);

        assertThat(instanceId).isEqualTo(88L);
        ArgumentCaptor<BpmCommandRecordEntity> insertCaptor = ArgumentCaptor.forClass(BpmCommandRecordEntity.class);
        verify(bpmCommandRecordDao).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getCommandKey()).isEqualTo("START:expense:1001:expense_apply");
        assertThat(insertCaptor.getValue().getCommandType()).isEqualTo("START");
        assertThat(insertCaptor.getValue().getCommandStatus()).isEqualTo(0);
        assertThat(insertCaptor.getValue().getBusinessType()).isEqualTo("expense");
        assertThat(insertCaptor.getValue().getBusinessId()).isEqualTo(1001L);

        ArgumentCaptor<BpmCommandRecordEntity> updateCaptor = ArgumentCaptor.forClass(BpmCommandRecordEntity.class);
        verify(bpmCommandRecordDao).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getInstanceId()).isEqualTo(88L);
        assertThat(updateCaptor.getValue().getCommandStatus()).isEqualTo(1);
    }

    @Test
    void startShouldReturnExistingInstanceWhenCommandAlreadySucceeded() {
        BpmBusinessStartCommand command = buildStartCommand();
        BpmCommandRecordEntity existingRecord = new BpmCommandRecordEntity();
        existingRecord.setCommandRecordId(9L);
        existingRecord.setCommandKey("START:expense:1001:expense_apply");
        existingRecord.setCommandStatus(1);
        existingRecord.setInstanceId(88L);
        when(bpmCommandRecordDao.selectOne(any())).thenReturn(existingRecord);

        Long instanceId = api.start(command);

        assertThat(instanceId).isEqualTo(88L);
        verify(bpmInstanceService, never()).startBusinessInstance(any());
        verify(bpmCommandRecordDao, never()).insert(any(BpmCommandRecordEntity.class));
    }

    @Test
    void getStatusShouldReturnLatestBusinessInstanceProjection() {
        BpmInstanceEntity instanceEntity = new BpmInstanceEntity();
        instanceEntity.setInstanceId(88L);
        instanceEntity.setInstanceNo("SN-001");
        instanceEntity.setBusinessType("expense");
        instanceEntity.setBusinessId(1001L);
        instanceEntity.setRunState(3);
        instanceEntity.setResultState(1);
        instanceEntity.setLastActionAt(LocalDateTime.of(2026, 7, 8, 12, 0));
        when(bpmInstanceDao.selectOne(any())).thenReturn(instanceEntity);

        BpmBusinessInstanceStatus status = api.getStatus("expense", 1001L);

        assertThat(status.getInstanceId()).isEqualTo(88L);
        assertThat(status.getInstanceNo()).isEqualTo("SN-001");
        assertThat(status.getBusinessType()).isEqualTo("expense");
        assertThat(status.getBusinessId()).isEqualTo(1001L);
        assertThat(status.getRunState()).isEqualTo(3);
        assertThat(status.getResultState()).isEqualTo(1);
        assertThat(status.getLastActionAt()).isEqualTo(LocalDateTime.of(2026, 7, 8, 12, 0));
    }

    @Test
    void publishResultEventShouldCreateCallbackRecordOnce() {
        when(bpmCallbackRecordDao.selectOne(any())).thenReturn(null);
        BpmBusinessResultEvent event = new BpmBusinessResultEvent();
        event.setEventId("event-1");
        event.setInstanceId(88L);
        event.setBusinessType("expense");
        event.setBusinessId(1001L);
        event.setResultState(1);
        event.setPayloadJson("{\"approved\":true}");
        event.setOccurredAt(LocalDateTime.of(2026, 7, 8, 12, 30));

        api.publishResultEvent(event);

        ArgumentCaptor<BpmCallbackRecordEntity> callbackCaptor = ArgumentCaptor.forClass(BpmCallbackRecordEntity.class);
        verify(bpmCallbackRecordDao).insert(callbackCaptor.capture());
        assertThat(callbackCaptor.getValue().getEventId()).isEqualTo("event-1");
        assertThat(callbackCaptor.getValue().getInstanceId()).isEqualTo(88L);
        assertThat(callbackCaptor.getValue().getBusinessType()).isEqualTo("expense");
        assertThat(callbackCaptor.getValue().getBusinessId()).isEqualTo(1001L);
        assertThat(callbackCaptor.getValue().getCallbackStatus()).isEqualTo(0);
        assertThat(callbackCaptor.getValue().getRequestPayloadJson()).contains("\"approved\":true");
    }

    private BpmBusinessStartCommand buildStartCommand() {
        BpmBusinessStartCommand command = new BpmBusinessStartCommand();
        command.setBusinessType("expense");
        command.setBusinessId(1001L);
        command.setBusinessKey("EXP-1001");
        command.setDefinitionKey("expense_apply");
        command.setStartEmployeeId(100L);
        command.setFormDataJson("{\"amount\":500}");
        command.setTitle("费用申请");
        command.setSummary("差旅报销");
        return command;
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
