package com.hunyuan.sa.bpm.sampleexpense;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.business.BpmBusinessProcessApi;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessStartCommand;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackContext;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackResult;
import com.hunyuan.sa.bpm.module.sampleexpense.dao.BpmSampleExpenseDao;
import com.hunyuan.sa.bpm.module.sampleexpense.domain.entity.BpmSampleExpenseEntity;
import com.hunyuan.sa.bpm.module.sampleexpense.domain.form.BpmSampleExpenseCreateForm;
import com.hunyuan.sa.bpm.module.sampleexpense.service.BpmSampleExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmSampleExpenseServiceTest {

    private BpmSampleExpenseService service;

    private BpmSampleExpenseDao dao;

    private BpmBusinessProcessApi processApi;

    @BeforeEach
    void setUp() {
        service = new BpmSampleExpenseService();
        dao = Mockito.mock(BpmSampleExpenseDao.class);
        processApi = Mockito.mock(BpmBusinessProcessApi.class);
        setField(service, "bpmSampleExpenseDao", dao);
        setField(service, "bpmBusinessProcessApi", processApi);
    }

    @Test
    void createShouldInsertDraftExpense() {
        when(dao.insert(any(BpmSampleExpenseEntity.class))).thenAnswer(invocation -> {
            BpmSampleExpenseEntity entity = invocation.getArgument(0);
            entity.setExpenseId(1001L);
            return 1;
        });

        ResponseDTO<Long> response = service.create(buildCreateForm());

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(1001L);
        ArgumentCaptor<BpmSampleExpenseEntity> captor = ArgumentCaptor.forClass(BpmSampleExpenseEntity.class);
        verify(dao).insert(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("差旅费用样板");
        assertThat(captor.getValue().getApprovalStatus()).isEqualTo(0);
        assertThat(captor.getValue().getCallbackFailFlag()).isFalse();
    }

    @Test
    void startShouldCallBusinessProcessApiAndMarkExpenseApproving() {
        BpmSampleExpenseEntity entity = draftExpense();
        when(dao.selectById(1001L)).thenReturn(entity);
        when(processApi.start(any(BpmBusinessStartCommand.class))).thenReturn(88L);

        ResponseDTO<Long> response = service.start(1001L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(88L);
        ArgumentCaptor<BpmBusinessStartCommand> commandCaptor = ArgumentCaptor.forClass(BpmBusinessStartCommand.class);
        verify(processApi).start(commandCaptor.capture());
        assertThat(commandCaptor.getValue().getBusinessType()).isEqualTo("sample_expense");
        assertThat(commandCaptor.getValue().getBusinessId()).isEqualTo(1001L);
        assertThat(commandCaptor.getValue().getDefinitionKey()).isEqualTo("sample_expense_apply");
        assertThat(commandCaptor.getValue().getStartEmployeeId()).isEqualTo(10L);
        assertThat(commandCaptor.getValue().getFormDataJson()).contains("\"expenseId\":1001");
        ArgumentCaptor<BpmSampleExpenseEntity> updateCaptor = ArgumentCaptor.forClass(BpmSampleExpenseEntity.class);
        verify(dao).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getInstanceId()).isEqualTo(88L);
        assertThat(updateCaptor.getValue().getApprovalStatus()).isEqualTo(1);
    }

    @Test
    void startShouldReturnExistingInstanceWithoutStartingAgain() {
        BpmSampleExpenseEntity entity = draftExpense();
        entity.setApprovalStatus(1);
        entity.setInstanceId(88L);
        when(dao.selectById(1001L)).thenReturn(entity);

        ResponseDTO<Long> response = service.start(1001L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(88L);
        verify(processApi, never()).start(any());
    }

    @Test
    void handleCallbackShouldApproveExpense() {
        BpmSampleExpenseEntity entity = approvingExpense();
        when(dao.selectById(1001L)).thenReturn(entity);

        BpmBusinessCallbackResult result = service.handleCallback(approvedContext("event-1"));

        assertThat(result.success()).isTrue();
        ArgumentCaptor<BpmSampleExpenseEntity> updateCaptor = ArgumentCaptor.forClass(BpmSampleExpenseEntity.class);
        verify(dao).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getApprovalStatus()).isEqualTo(2);
        assertThat(updateCaptor.getValue().getCallbackEventId()).isEqualTo("event-1");
        assertThat(updateCaptor.getValue().getApprovedAt()).isNotNull();
    }

    @Test
    void handleCallbackShouldRejectExpense() {
        BpmSampleExpenseEntity entity = approvingExpense();
        when(dao.selectById(1001L)).thenReturn(entity);

        BpmBusinessCallbackResult result = service.handleCallback(rejectedContext("event-2"));

        assertThat(result.success()).isTrue();
        ArgumentCaptor<BpmSampleExpenseEntity> updateCaptor = ArgumentCaptor.forClass(BpmSampleExpenseEntity.class);
        verify(dao).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getApprovalStatus()).isEqualTo(3);
        assertThat(updateCaptor.getValue().getCallbackEventId()).isEqualTo("event-2");
        assertThat(updateCaptor.getValue().getRejectedAt()).isNotNull();
    }

    @Test
    void handleCallbackShouldFailOnceWhenFailFlagIsSetAndClearFlag() {
        BpmSampleExpenseEntity entity = approvingExpense();
        entity.setCallbackFailFlag(true);
        when(dao.selectById(1001L)).thenReturn(entity);

        BpmBusinessCallbackResult result = service.handleCallback(approvedContext("event-1"));

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).contains("样板费用申请模拟回调失败");
        ArgumentCaptor<BpmSampleExpenseEntity> updateCaptor = ArgumentCaptor.forClass(BpmSampleExpenseEntity.class);
        verify(dao).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getCallbackFailFlag()).isFalse();
        assertThat(updateCaptor.getValue().getApprovalStatus()).isNull();
    }

    @Test
    void handleCallbackShouldKeepIdempotentForSameEventId() {
        BpmSampleExpenseEntity entity = approvingExpense();
        entity.setApprovalStatus(2);
        entity.setCallbackEventId("event-1");
        entity.setApprovedAt(LocalDateTime.of(2026, 7, 9, 10, 0));
        when(dao.selectById(1001L)).thenReturn(entity);

        BpmBusinessCallbackResult result = service.handleCallback(approvedContext("event-1"));

        assertThat(result.success()).isTrue();
        verify(dao, never()).updateById(Mockito.<BpmSampleExpenseEntity>any());
    }

    @Test
    void handleCallbackShouldFailWhenTerminalStateConflicts() {
        BpmSampleExpenseEntity entity = approvingExpense();
        entity.setApprovalStatus(2);
        entity.setCallbackEventId("event-1");
        when(dao.selectById(1001L)).thenReturn(entity);

        BpmBusinessCallbackResult result = service.handleCallback(rejectedContext("event-2"));

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).contains("结果冲突");
        verify(dao, never()).updateById(Mockito.<BpmSampleExpenseEntity>any());
    }

    private BpmSampleExpenseCreateForm buildCreateForm() {
        BpmSampleExpenseCreateForm form = new BpmSampleExpenseCreateForm();
        form.setTitle("差旅费用样板");
        form.setAmount(new BigDecimal("1280.50"));
        form.setApplicantEmployeeId(10L);
        return form;
    }

    private BpmSampleExpenseEntity draftExpense() {
        BpmSampleExpenseEntity entity = new BpmSampleExpenseEntity();
        entity.setExpenseId(1001L);
        entity.setTitle("差旅费用样板");
        entity.setAmount(new BigDecimal("1280.50"));
        entity.setApplicantEmployeeId(10L);
        entity.setApprovalStatus(0);
        entity.setCallbackFailFlag(false);
        return entity;
    }

    private BpmSampleExpenseEntity approvingExpense() {
        BpmSampleExpenseEntity entity = draftExpense();
        entity.setApprovalStatus(1);
        entity.setInstanceId(88L);
        return entity;
    }

    private BpmBusinessCallbackContext approvedContext(String eventId) {
        return callbackContext(eventId, 1);
    }

    private BpmBusinessCallbackContext rejectedContext(String eventId) {
        return callbackContext(eventId, 2);
    }

    private BpmBusinessCallbackContext callbackContext(String eventId, int resultState) {
        return new BpmBusinessCallbackContext(
                31L,
                eventId,
                88L,
                "sample_expense",
                1001L,
                "{\"eventId\":\"" + eventId + "\",\"instanceId\":88,\"businessType\":\"sample_expense\",\"businessId\":1001,\"resultState\":" + resultState + "}"
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
