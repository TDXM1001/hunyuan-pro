package com.hunyuan.sa.bpm.module.model.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增流程模型表单。
 */
@Data
public class BpmModelAddForm {

    @Schema(description = "模型编码")
    @NotBlank(message = "模型编码不能为空")
    @Size(max = 64, message = "模型编码最多 64 个字符")
    private String modelKey;

    @Schema(description = "模型名称")
    @NotBlank(message = "模型名称不能为空")
    @Size(max = 128, message = "模型名称最多 128 个字符")
    private String modelName;

    @Schema(description = "分类ID")
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @Schema(description = "表单类型")
    @NotNull(message = "表单类型不能为空")
    private Integer formType;

    @Schema(description = "表单ID")
    @NotNull(message = "表单ID不能为空")
    private Long formId;

    @Schema(description = "是否可见")
    private Boolean visibleFlag;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "描述")
    @Size(max = 500, message = "描述最多 500 个字符")
    private String description;

    @Schema(description = "单号规则ID")
    private Integer instanceNoRuleId;
}
