package com.hunyuan.sa.base.module.support.dict.api;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台字典分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformDictionaryPageQuery extends PageParam {

    @Schema(description = "关键字")
    private String keywords;

    @Schema(description = "禁用状态")
    private Boolean disabledFlag;
}
