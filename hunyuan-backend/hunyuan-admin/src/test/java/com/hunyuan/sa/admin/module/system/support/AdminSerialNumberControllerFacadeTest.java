package com.hunyuan.sa.admin.module.system.support;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberDefinition;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberFacade;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberRecord;
import com.hunyuan.sa.base.module.support.serialnumber.domain.SerialNumberGenerateForm;
import com.hunyuan.sa.base.module.support.serialnumber.domain.SerialNumberRecordQueryForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 锁定历史序列号路由通过平台运行时 Facade 适配。
 */
@ExtendWith(MockitoExtension.class)
class AdminSerialNumberControllerFacadeTest {

    @Mock
    private PlatformSerialNumberFacade facade;

    private AdminSerialNumberController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminSerialNumberController();
        ReflectionTestUtils.setField(controller, "platformSerialNumberFacade", facade);
    }

    @Test
    void mapsLegacyDefinitionAndRecordResponses() {
        PlatformSerialNumberDefinition definition = new PlatformSerialNumberDefinition();
        definition.setSerialNumberId(1);
        PlatformSerialNumberRecord record = new PlatformSerialNumberRecord();
        record.setSerialNumberId(1);
        when(facade.listDefinitions()).thenReturn(ResponseDTO.ok(List.of(definition)));
        when(facade.queryRecords(any())).thenReturn(ResponseDTO.ok(pageOf(record)));

        assertThat(controller.getAll().getData()).singleElement().satisfies(item ->
                assertThat(item.getSerialNumberId()).isEqualTo(1));
        assertThat(controller.queryRecord(new SerialNumberRecordQueryForm())
                .getData().getList()).singleElement().satisfies(item ->
                assertThat(item.getSerialNumberId()).isEqualTo(1));
    }

    @Test
    void keepsLegacyGenerateResponse() {
        when(facade.generate(any()))
                .thenReturn(ResponseDTO.ok(List.of("DD202607240001")));
        SerialNumberGenerateForm form = new SerialNumberGenerateForm();
        form.setSerialNumberId(1);
        form.setCount(1);

        assertThat(controller.generate(form).getData())
                .containsExactly("DD202607240001");
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
