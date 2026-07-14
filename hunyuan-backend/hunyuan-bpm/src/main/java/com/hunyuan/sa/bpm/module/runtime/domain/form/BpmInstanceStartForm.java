package com.hunyuan.sa.bpm.module.runtime.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发起流程实例表单。
 */
@Data
public class BpmInstanceStartForm {

    @Schema(description = "Graph 定义版本ID")
    @jakarta.validation.constraints.NotNull(message = "Graph定义版本不能为空")
    private Long graphDefinitionVersionId;

    @Schema(description = "M3 审批对象快照ID，Graph流程必填")
    private Long approvalSubjectSnapshotId;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "流程摘要")
    private String summary;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务ID")
    private Long businessId;

    @Schema(description = "业务Key")
    private String businessKey;

    @Schema(description = "表单数据 JSON")
    @NotBlank(message = "表单数据不能为空")
    private String formDataJson;

}
