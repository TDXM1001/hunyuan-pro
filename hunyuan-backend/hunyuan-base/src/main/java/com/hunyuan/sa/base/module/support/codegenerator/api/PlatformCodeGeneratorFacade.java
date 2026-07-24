package com.hunyuan.sa.base.module.support.codegenerator.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;

import java.util.List;

/**
 * 平台代码生成器公开边界。
 *
 * <p>该边界仅服务开发与工程验收，不属于生产业务运行能力。</p>
 */
public interface PlatformCodeGeneratorFacade {

    ResponseDTO<List<PlatformCodeGeneratorColumnView>> listColumns(String tableName);

    ResponseDTO<PageResult<PlatformCodeGeneratorTableView>> queryTables(
            PlatformCodeGeneratorTablePageQuery query);

    ResponseDTO<PlatformCodeGeneratorConfig> getConfig(String tableName);

    ResponseDTO<String> updateConfig(PlatformCodeGeneratorConfigUpdateCommand command);

    ResponseDTO<String> preview(PlatformCodeGeneratorPreviewCommand command);

    ResponseDTO<byte[]> download(String tableName);
}
