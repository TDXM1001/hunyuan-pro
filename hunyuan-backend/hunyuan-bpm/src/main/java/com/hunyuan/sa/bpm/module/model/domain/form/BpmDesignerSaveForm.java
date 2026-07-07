package com.hunyuan.sa.bpm.module.model.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设计器保存草稿表单。
 */
@Data
public class BpmDesignerSaveForm {

    @Schema(description = "模型ID")
    @NotNull(message = "模型ID不能为空")
    private Long modelId;

    @Schema(description = "设计器草稿 JSON")
    @NotBlank(message = "设计器草稿不能为空")
    private String simpleModelJson;

    @Schema(description = "发起规则 JSON")
    @NotBlank(message = "发起规则不能为空")
    private String startRuleJson;

    @Schema(description = "主管范围 JSON")
    private String managerScopeJson;

    @Schema(description = "标题规则 JSON")
    private String titleRuleJson;

    @Schema(description = "摘要规则 JSON")
    private String summaryRuleJson;

    @Schema(description = "变量映射 JSON")
    private String variableMappingJson;
}
