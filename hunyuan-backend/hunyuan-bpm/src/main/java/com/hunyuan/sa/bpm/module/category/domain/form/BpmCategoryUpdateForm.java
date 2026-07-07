package com.hunyuan.sa.bpm.module.category.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 更新流程分类表单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmCategoryUpdateForm extends BpmCategoryAddForm {

    @Schema(description = "分类ID")
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;
}
