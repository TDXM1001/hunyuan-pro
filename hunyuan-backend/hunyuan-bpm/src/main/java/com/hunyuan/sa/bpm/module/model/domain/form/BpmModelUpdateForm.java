package com.hunyuan.sa.bpm.module.model.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 更新流程模型表单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmModelUpdateForm extends BpmModelAddForm {

    @Schema(description = "模型ID")
    @NotNull(message = "模型ID不能为空")
    private Long modelId;
}
