package com.hunyuan.sa.bpm.integration;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCommandRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCommandRecordEntity;
import com.hunyuan.sa.bpm.module.integration.domain.form.BpmCallbackRecordQueryForm;
import com.hunyuan.sa.bpm.module.integration.domain.form.BpmCommandRecordQueryForm;
import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCallbackRecordVO;
import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCommandRecordVO;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessIntegrationRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BpmBusinessIntegrationRecordServiceTest {

    private BpmBusinessIntegrationRecordService recordService;

    private BpmCallbackRecordDao bpmCallbackRecordDao;

    private BpmCommandRecordDao bpmCommandRecordDao;

    @BeforeEach
    void setUp() {
        recordService = new BpmBusinessIntegrationRecordService();
        bpmCallbackRecordDao = Mockito.mock(BpmCallbackRecordDao.class);
        bpmCommandRecordDao = Mockito.mock(BpmCommandRecordDao.class);
        setField(recordService, "bpmCallbackRecordDao", bpmCallbackRecordDao);
        setField(recordService, "bpmCommandRecordDao", bpmCommandRecordDao);
    }

    @Test
    void queryCallbackPageShouldReturnCallbackRecordVOs() {
        BpmCallbackRecordEntity record = new BpmCallbackRecordEntity();
        record.setCallbackRecordId(1L);
        record.setEventId("event-1");
        record.setInstanceId(88L);
        record.setBusinessType("expense");
        record.setBusinessId(1001L);
        record.setCallbackStatus(2);
        record.setRetryCount(3);
        when(bpmCallbackRecordDao.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<BpmCallbackRecordEntity> page = invocation.getArgument(0);
            page.setRecords(List.of(record));
            page.setTotal(1);
            return page;
        });

        BpmCallbackRecordQueryForm queryForm = new BpmCallbackRecordQueryForm();
        queryForm.setPageNum(1L);
        queryForm.setPageSize(10L);
        queryForm.setInstanceId(88L);
        queryForm.setBusinessType("expense");

        ResponseDTO<PageResult<BpmCallbackRecordVO>> response = recordService.queryCallbackPage(queryForm);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getList()).hasSize(1);
        assertThat(response.getData().getList().get(0).getCallbackRecordId()).isEqualTo(1L);
        assertThat(response.getData().getList().get(0).getEventId()).isEqualTo("event-1");
        assertThat(response.getData().getList().get(0).getInstanceId()).isEqualTo(88L);
        assertThat(response.getData().getList().get(0).getRetryCount()).isEqualTo(3);
    }

    @Test
    void queryCommandPageShouldReturnCommandRecordVOs() {
        BpmCommandRecordEntity record = new BpmCommandRecordEntity();
        record.setCommandRecordId(2L);
        record.setCommandKey("START:expense:1001:expense_apply");
        record.setCommandType("START");
        record.setInstanceId(88L);
        record.setBusinessType("expense");
        record.setBusinessId(1001L);
        record.setCommandStatus(1);
        when(bpmCommandRecordDao.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<BpmCommandRecordEntity> page = invocation.getArgument(0);
            page.setRecords(List.of(record));
            page.setTotal(1);
            return page;
        });

        BpmCommandRecordQueryForm queryForm = new BpmCommandRecordQueryForm();
        queryForm.setPageNum(1L);
        queryForm.setPageSize(10L);
        queryForm.setInstanceId(88L);
        queryForm.setBusinessType("expense");

        ResponseDTO<PageResult<BpmCommandRecordVO>> response = recordService.queryCommandPage(queryForm);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getList()).hasSize(1);
        assertThat(response.getData().getList().get(0).getCommandRecordId()).isEqualTo(2L);
        assertThat(response.getData().getList().get(0).getCommandKey()).isEqualTo("START:expense:1001:expense_apply");
        assertThat(response.getData().getList().get(0).getInstanceId()).isEqualTo(88L);
        assertThat(response.getData().getList().get(0).getCommandStatus()).isEqualTo(1);
    }

    @Test
    void queryCallbackRecordsByInstanceIdShouldReturnMappedRecords() {
        BpmCallbackRecordEntity record = new BpmCallbackRecordEntity();
        record.setCallbackRecordId(1L);
        record.setEventId("event-88");
        record.setInstanceId(88L);
        record.setBusinessType("expense");
        record.setBusinessId(1001L);
        record.setCallbackStatus(2);
        record.setRetryCount(1);
        when(bpmCallbackRecordDao.selectList(any())).thenReturn(List.of(record));

        List<BpmCallbackRecordVO> records = recordService.queryCallbackRecordsByInstanceId(88L);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getCallbackRecordId()).isEqualTo(1L);
        assertThat(records.get(0).getInstanceId()).isEqualTo(88L);
        assertThat(records.get(0).getEventId()).isEqualTo("event-88");
    }

    @Test
    void queryCommandRecordsByInstanceIdShouldReturnMappedRecords() {
        BpmCommandRecordEntity record = new BpmCommandRecordEntity();
        record.setCommandRecordId(2L);
        record.setCommandKey("START:expense:1001:expense_apply");
        record.setCommandType("START");
        record.setInstanceId(88L);
        record.setBusinessType("expense");
        record.setBusinessId(1001L);
        record.setCommandStatus(1);
        when(bpmCommandRecordDao.selectList(any())).thenReturn(List.of(record));

        List<BpmCommandRecordVO> records = recordService.queryCommandRecordsByInstanceId(88L);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getCommandRecordId()).isEqualTo(2L);
        assertThat(records.get(0).getInstanceId()).isEqualTo(88L);
        assertThat(records.get(0).getCommandKey()).isEqualTo("START:expense:1001:expense_apply");
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
