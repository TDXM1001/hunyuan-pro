package com.hunyuan.sa.base.module.support.serialnumber.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.serialnumber.application.PlatformSerialNumberApplicationService;
import com.hunyuan.sa.base.module.support.serialnumber.constant.SerialNumberIdEnum;
import com.hunyuan.sa.base.module.support.serialnumber.dao.SerialNumberDao;
import com.hunyuan.sa.base.module.support.serialnumber.domain.SerialNumberEntity;
import com.hunyuan.sa.base.module.support.serialnumber.domain.SerialNumberRecordEntity;
import com.hunyuan.sa.base.module.support.serialnumber.domain.SerialNumberRecordQueryForm;
import com.hunyuan.sa.base.module.support.serialnumber.service.SerialNumberRecordService;
import com.hunyuan.sa.base.module.support.serialnumber.service.SerialNumberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定平台序列号边界与历史实现之间的映射。
 */
@ExtendWith(MockitoExtension.class)
class PlatformSerialNumberApplicationServiceTest {

    @Mock
    private SerialNumberDao serialNumberDao;

    @Mock
    private SerialNumberService serialNumberService;

    @Mock
    private SerialNumberRecordService serialNumberRecordService;

    private PlatformSerialNumberApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlatformSerialNumberApplicationService();
        ReflectionTestUtils.setField(service, "serialNumberDao", serialNumberDao);
        ReflectionTestUtils.setField(service, "serialNumberService", serialNumberService);
        ReflectionTestUtils.setField(
                service, "serialNumberRecordService", serialNumberRecordService);
    }

    @Test
    void mapsDefinitionEntitiesToPublicSummaries() {
        SerialNumberEntity definition = new SerialNumberEntity();
        definition.setSerialNumberId(1);
        definition.setBusinessName("订单编号");
        definition.setFormat("DD{yyyyMMdd}{number}");
        when(serialNumberDao.selectList(null)).thenReturn(List.of(definition));

        ResponseDTO<List<PlatformSerialNumberDefinition>> response =
                service.listDefinitions();

        assertThat(response.getData()).singleElement().satisfies(item -> {
            assertThat(item.getSerialNumberId()).isEqualTo(1);
            assertThat(item.getBusinessName()).isEqualTo("订单编号");
        });
    }

    @Test
    void mapsRecordQueryAndPageResult() {
        SerialNumberRecordEntity record = new SerialNumberRecordEntity();
        record.setSerialNumberId(1);
        record.setRecordDate(LocalDate.of(2026, 7, 24));
        when(serialNumberRecordService.query(any())).thenReturn(pageOf(record));
        PlatformSerialNumberRecordPageQuery query = new PlatformSerialNumberRecordPageQuery();
        query.setPageNum(1L);
        query.setPageSize(10L);
        query.setSerialNumberId(1);

        ResponseDTO<PageResult<PlatformSerialNumberRecord>> response =
                service.queryRecords(query);

        ArgumentCaptor<SerialNumberRecordQueryForm> captor =
                ArgumentCaptor.forClass(SerialNumberRecordQueryForm.class);
        verify(serialNumberRecordService).query(captor.capture());
        assertThat(captor.getValue().getSerialNumberId()).isEqualTo(1);
        assertThat(response.getData().getList()).singleElement().satisfies(item ->
                assertThat(item.getRecordDate()).isEqualTo(LocalDate.of(2026, 7, 24)));
    }

    @Test
    void mapsStableGenerateCommandToExistingGenerator() {
        PlatformSerialNumberGenerateCommand command = command(1, 2);
        when(serialNumberService.generate(SerialNumberIdEnum.ORDER, 2))
                .thenReturn(List.of("DD202607240001", "DD202607240002"));

        ResponseDTO<List<String>> response = service.generate(command);

        assertThat(response.getData())
                .containsExactly("DD202607240001", "DD202607240002");
    }

    @Test
    void rejectsUnknownDefinitionBeforeCallingGenerator() {
        ResponseDTO<List<String>> response = service.generate(command(99, 1));

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("序列号定义不存在");
        verify(serialNumberService, never()).generate(any(), anyInt());
    }

    private PlatformSerialNumberGenerateCommand command(int serialNumberId, int count) {
        PlatformSerialNumberGenerateCommand command = new PlatformSerialNumberGenerateCommand();
        command.setSerialNumberId(serialNumberId);
        command.setCount(count);
        return command;
    }

    private <T> PageResult<T> pageOf(T item) {
        PageResult<T> page = new PageResult<>();
        page.setPageNum(1L);
        page.setPageSize(10L);
        page.setTotal(1L);
        page.setPages(1L);
        page.setEmptyFlag(false);
        page.setList(List.of(item));
        return page;
    }
}
