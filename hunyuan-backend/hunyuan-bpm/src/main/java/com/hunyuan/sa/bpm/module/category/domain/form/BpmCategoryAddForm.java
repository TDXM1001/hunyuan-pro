package com.hunyuan.sa.bpm.module.category.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增流程分类表单。
 */
@Data
public class BpmCategoryAddForm {

    @Schema(description = "分类编码")
    @NotBlank(message = "分类编码不能为空")
    @Size(max = 64, message = "分类编码最多 64 个字符")
    private String categoryCode;

    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 128, message = "分类名称最多 128 个字符")
    private String categoryName;

    @Schema(description = "图标")
    @Size(max = 255, message = "图标最多 255 个字符")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "是否禁用")
    private Boolean disabledFlag;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注最多 500 个字符")
    private String remark;
}
