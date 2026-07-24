package com.hunyuan.sa.base.module.support.dict.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建平台字典命令。
 */
@Data
public class PlatformDictionaryCreateCommand {

    @Schema(description = "字典名称")
    @NotBlank(message = "字典名称不能为空")
    private String dictName;

    @Schema(description = "字典编码")
    @NotBlank(message = "字典编码不能为空")
    private String dictCode;

    @Schema(description = "备注")
    private String remark;
}
