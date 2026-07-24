package com.hunyuan.sa.base.module.support.codegenerator.controller;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorConfig;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorConfigUpdateCommand;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorFacade;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorTableView;
import com.hunyuan.sa.base.module.support.codegenerator.domain.form.CodeGeneratorConfigForm;
import com.hunyuan.sa.base.module.support.codegenerator.domain.form.TableQueryForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定历史代码生成器路由通过开发工具 Facade 兼容适配。
 */
@ExtendWith(MockitoExtension.class)
class CodeGeneratorControllerFacadeTest {

    @Mock
    private PlatformCodeGeneratorFacade facade;

    private CodeGeneratorController controller;

    @BeforeEach
    void setUp() {
        controller = new CodeGeneratorController();
        ReflectionTestUtils.setField(controller, "platformCodeGeneratorFacade", facade);
    }

    @Test
    void mapsStableTablePageToLegacyResponse() {
        PlatformCodeGeneratorTableView table = new PlatformCodeGeneratorTableView();
        table.setTableName("t_employee");
        when(facade.queryTables(any())).thenReturn(ResponseDTO.ok(pageOf(table)));

        var response = controller.queryTableList(new TableQueryForm());

        assertThat(response.getData().getList()).singleElement().satisfies(item ->
                assertThat(item.getTableName()).isEqualTo("t_employee"));
    }

    @Test
    void preservesUnconfiguredNullFieldsForLegacyConfigResponse() {
        when(facade.getConfig("t_employee"))
                .thenReturn(ResponseDTO.ok(new PlatformCodeGeneratorConfig()));

        var response = controller.getTableConfig("t_employee");

        assertThat(response.getData().getBasic()).isNull();
        assertThat(response.getData().getFields()).isNull();
    }

    @Test
    void mapsLegacyUpdateFormToStableCommand() {
        when(facade.updateConfig(any())).thenReturn(ResponseDTO.ok());
        CodeGeneratorConfigForm form = new CodeGeneratorConfigForm();
        form.setTableName("t_employee");

        controller.updateConfig(form);

        ArgumentCaptor<PlatformCodeGeneratorConfigUpdateCommand> captor =
                ArgumentCaptor.forClass(PlatformCodeGeneratorConfigUpdateCommand.class);
        verify(facade).updateConfig(captor.capture());
        assertThat(captor.getValue().getTableName()).isEqualTo("t_employee");
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
