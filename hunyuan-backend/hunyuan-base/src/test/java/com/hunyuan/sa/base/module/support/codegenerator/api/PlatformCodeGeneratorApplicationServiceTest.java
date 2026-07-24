package com.hunyuan.sa.base.module.support.codegenerator.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.codegenerator.application.PlatformCodeGeneratorApplicationService;
import com.hunyuan.sa.base.module.support.codegenerator.domain.form.CodeGeneratorConfigForm;
import com.hunyuan.sa.base.module.support.codegenerator.domain.form.CodeGeneratorPreviewForm;
import com.hunyuan.sa.base.module.support.codegenerator.domain.form.TableQueryForm;
import com.hunyuan.sa.base.module.support.codegenerator.domain.model.CodeBasic;
import com.hunyuan.sa.base.module.support.codegenerator.domain.model.CodeInsertAndUpdate;
import com.hunyuan.sa.base.module.support.codegenerator.domain.model.CodeInsertAndUpdateField;
import com.hunyuan.sa.base.module.support.codegenerator.domain.vo.TableConfigVO;
import com.hunyuan.sa.base.module.support.codegenerator.domain.vo.TableVO;
import com.hunyuan.sa.base.module.support.codegenerator.service.CodeGeneratorService;
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
 * 锁定开发工具稳定契约与既有代码生成服务之间的映射。
 */
@ExtendWith(MockitoExtension.class)
class PlatformCodeGeneratorApplicationServiceTest {

    @Mock
    private CodeGeneratorService codeGeneratorService;

    private PlatformCodeGeneratorApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlatformCodeGeneratorApplicationService();
        ReflectionTestUtils.setField(service, "codeGeneratorService", codeGeneratorService);
    }

    @Test
    void mapsTablePageToStableViews() {
        TableVO table = new TableVO();
        table.setTableName("t_employee");
        when(codeGeneratorService.queryTableList(any())).thenReturn(pageOf(table));
        PlatformCodeGeneratorTablePageQuery query = new PlatformCodeGeneratorTablePageQuery();
        query.setPageNum(1L);
        query.setPageSize(10L);

        ResponseDTO<PageResult<PlatformCodeGeneratorTableView>> response =
                service.queryTables(query);

        ArgumentCaptor<TableQueryForm> captor = ArgumentCaptor.forClass(TableQueryForm.class);
        verify(codeGeneratorService).queryTableList(captor.capture());
        assertThat(captor.getValue().getPageNum()).isEqualTo(1L);
        assertThat(response.getData().getList()).singleElement().satisfies(item ->
                assertThat(item.getTableName()).isEqualTo("t_employee"));
    }

    @Test
    void mapsNestedConfigInBothDirections() {
        TableConfigVO legacyConfig = legacyConfig();
        when(codeGeneratorService.getTableConfig("t_employee")).thenReturn(legacyConfig);
        when(codeGeneratorService.updateConfig(any())).thenReturn(ResponseDTO.ok());

        PlatformCodeGeneratorConfig stable = service.getConfig("t_employee").getData();
        PlatformCodeGeneratorConfigUpdateCommand command =
                new PlatformCodeGeneratorConfigUpdateCommand();
        command.setTableName("t_employee");
        command.setBasic(stable.getBasic());
        command.setFields(stable.getFields());
        command.setInsertAndUpdate(stable.getInsertAndUpdate());
        command.setDeleteInfo(stable.getDeleteInfo());
        command.setQueryFields(stable.getQueryFields());
        command.setTableFields(stable.getTableFields());
        service.updateConfig(command);

        assertThat(stable.getBasic().getModuleName()).isEqualTo("员工");
        assertThat(stable.getInsertAndUpdate().getFieldList())
                .singleElement().satisfies(field ->
                        assertThat(field.getColumnName()).isEqualTo("employee_id"));
        ArgumentCaptor<CodeGeneratorConfigForm> captor =
                ArgumentCaptor.forClass(CodeGeneratorConfigForm.class);
        verify(codeGeneratorService).updateConfig(captor.capture());
        assertThat(captor.getValue().getTableName()).isEqualTo("t_employee");
        assertThat(captor.getValue().getInsertAndUpdate().getFieldList())
                .singleElement().satisfies(field ->
                        assertThat(field.getColumnName()).isEqualTo("employee_id"));
    }

    @Test
    void delegatesPreviewAndDownloadWithoutChangingFailureResponses() {
        ResponseDTO<String> previewFailure = ResponseDTO.userErrorParam("配置信息不存在");
        ResponseDTO<byte[]> downloadFailure = ResponseDTO.userErrorParam("表不存在");
        when(codeGeneratorService.preview(any())).thenReturn(previewFailure);
        when(codeGeneratorService.download("missing_table")).thenReturn(downloadFailure);
        PlatformCodeGeneratorPreviewCommand command =
                new PlatformCodeGeneratorPreviewCommand();
        command.setTableName("missing_table");
        command.setTemplateFile("controller.java.vm");

        ResponseDTO<String> preview = service.preview(command);
        ResponseDTO<byte[]> download = service.download("missing_table");

        ArgumentCaptor<CodeGeneratorPreviewForm> captor =
                ArgumentCaptor.forClass(CodeGeneratorPreviewForm.class);
        verify(codeGeneratorService).preview(captor.capture());
        assertThat(captor.getValue().getTemplateFile()).isEqualTo("controller.java.vm");
        assertThat(preview).isSameAs(previewFailure);
        assertThat(download).isSameAs(downloadFailure);
    }

    private TableConfigVO legacyConfig() {
        CodeBasic basic = new CodeBasic();
        basic.setModuleName("员工");
        CodeInsertAndUpdateField field = new CodeInsertAndUpdateField();
        field.setColumnName("employee_id");
        CodeInsertAndUpdate insertAndUpdate = new CodeInsertAndUpdate();
        insertAndUpdate.setFieldList(List.of(field));
        TableConfigVO config = new TableConfigVO();
        config.setBasic(basic);
        config.setInsertAndUpdate(insertAndUpdate);
        return config;
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
