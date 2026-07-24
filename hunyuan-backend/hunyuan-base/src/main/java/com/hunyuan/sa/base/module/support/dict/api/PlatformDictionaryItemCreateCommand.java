package com.hunyuan.sa.base.module.support.dict.api;

import com.hunyuan.sa.base.common.swagger.SchemaEnum;
import com.hunyuan.sa.base.common.validator.enumeration.CheckEnum;
import com.hunyuan.sa.base.module.support.dict.constant.DictDataStyleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建平台字典项命令，所属字典标识由稳定 URL 路径提供。
 */
@Data
public class PlatformDictionaryItemCreateCommand {

    @Schema(description = "字典项值")
    @NotBlank(message = "字典项值不能为空")
    private String dataValue;

    @Schema(description = "字典项显示名称")
    @NotBlank(message = "字典项显示名称不能为空")
    private String dataLabel;

    @SchemaEnum(value = DictDataStyleEnum.class, desc = "数据样式")
    @CheckEnum(message = "样式参数错误", value = DictDataStyleEnum.class)
    private String dataStyle;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "排序，数值越大越靠前")
    @NotNull(message = "排序不能为空")
    private Integer sortOrder;
}
