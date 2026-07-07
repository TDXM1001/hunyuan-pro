package com.hunyuan.sa.bpm.module.form.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 更新流程表单表单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmFormUpdateForm extends BpmFormAddForm {

    @Schema(description = "表单ID")
    @NotNull(message = "表单ID不能为空")
    private Long formId;
}
