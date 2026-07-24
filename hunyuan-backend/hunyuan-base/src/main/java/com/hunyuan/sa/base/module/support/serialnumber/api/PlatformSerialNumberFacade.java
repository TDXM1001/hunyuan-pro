package com.hunyuan.sa.base.module.support.serialnumber.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;

import java.util.List;

/**
 * 平台序列号公开边界。
 *
 * <p>该边界承载序列号定义、生成记录和受控生成，不暴露持久化实体或具体生成器实现。</p>
 */
public interface PlatformSerialNumberFacade {

    ResponseDTO<List<PlatformSerialNumberDefinition>> listDefinitions();

    ResponseDTO<PageResult<PlatformSerialNumberRecord>> queryRecords(
            PlatformSerialNumberRecordPageQuery query);

    ResponseDTO<List<String>> generate(PlatformSerialNumberGenerateCommand command);
}
