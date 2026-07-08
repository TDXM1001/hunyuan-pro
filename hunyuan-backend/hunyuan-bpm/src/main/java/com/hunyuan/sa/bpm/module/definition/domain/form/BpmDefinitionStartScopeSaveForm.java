package com.hunyuan.sa.bpm.module.definition.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 流程定义可发起范围保存表单。
 */
@Data
public class BpmDefinitionStartScopeSaveForm {

    @Schema(description = "定义ID")
    @NotNull(message = "定义ID不能为空")
    private Long definitionId;

    @Schema(description = "可发起范围JSON")
    @NotBlank(message = "可发起范围不能为空")
    private String startScopeJson;
}
