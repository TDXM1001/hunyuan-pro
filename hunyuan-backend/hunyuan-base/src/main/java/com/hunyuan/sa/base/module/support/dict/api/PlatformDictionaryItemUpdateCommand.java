package com.hunyuan.sa.base.module.support.dict.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 更新平台字典项命令，字典项标识由稳定 URL 路径提供。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformDictionaryItemUpdateCommand extends PlatformDictionaryItemCreateCommand {

    @Schema(description = "字典编码，用于保持历史缓存失效语义")
    @NotNull(message = "字典编码不能为空")
    private String dictCode;
}
