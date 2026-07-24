package com.hunyuan.sa.base.module.support.codegenerator.application;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorColumnView;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorConfig;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorConfigUpdateCommand;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorFacade;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorPreviewCommand;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorTablePageQuery;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorTableView;
import com.hunyuan.sa.base.module.support.codegenerator.domain.form.CodeGeneratorPreviewForm;
import com.hunyuan.sa.base.module.support.codegenerator.domain.form.TableQueryForm;
import com.hunyuan.sa.base.module.support.codegenerator.service.CodeGeneratorService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 平台代码生成器应用服务，隔离稳定契约与历史生成实现。
 */
@Service
public class PlatformCodeGeneratorApplicationService implements PlatformCodeGeneratorFacade {

    @Resource
    private CodeGeneratorService codeGeneratorService;

    @Override
    public ResponseDTO<List<PlatformCodeGeneratorColumnView>> listColumns(String tableName) {
        return ResponseDTO.ok(PlatformCodeGeneratorMapper.toColumnViews(
                codeGeneratorService.getTableColumns(tableName)));
    }

    @Override
    public ResponseDTO<PageResult<PlatformCodeGeneratorTableView>> queryTables(
            PlatformCodeGeneratorTablePageQuery query) {
        PageResult<PlatformCodeGeneratorTableView> result =
                PlatformCodeGeneratorMapper.toTablePage(codeGeneratorService.queryTableList(
                        SmartBeanUtil.copy(query, TableQueryForm.class)));
        return ResponseDTO.ok(result);
    }

    @Override
    public ResponseDTO<PlatformCodeGeneratorConfig> getConfig(String tableName) {
        return ResponseDTO.ok(PlatformCodeGeneratorMapper.toConfig(
                codeGeneratorService.getTableConfig(tableName)));
    }

    @Override
    public ResponseDTO<String> updateConfig(
            PlatformCodeGeneratorConfigUpdateCommand command) {
        return codeGeneratorService.updateConfig(
                PlatformCodeGeneratorMapper.toLegacyUpdateForm(command));
    }

    @Override
    public ResponseDTO<String> preview(PlatformCodeGeneratorPreviewCommand command) {
        return codeGeneratorService.preview(SmartBeanUtil.copy(
                command, CodeGeneratorPreviewForm.class));
    }

    @Override
    public ResponseDTO<byte[]> download(String tableName) {
        return codeGeneratorService.download(tableName);
    }
}
