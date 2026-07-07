package com.hunyuan.sa.bpm.module.definition.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发布流程定义表单。
 */
@Data
public class BpmDefinitionPublishForm {

    @Schema(description = "模型ID")
    @NotNull(message = "模型ID不能为空")
    private Long modelId;
}
