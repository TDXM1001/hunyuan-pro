package com.hunyuan.sa.bpm.module.runtime.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发起流程实例表单。
 */
@Data
public class BpmInstanceStartForm {

    @Schema(description = "定义ID")
    private Long definitionId;

    @Schema(description = "Graph 定义版本ID")
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

    @AssertTrue(message = "流程定义来源必须且只能选择一种")
    public boolean hasExactlyOneDefinitionSource() {
        return (definitionId == null) != (graphDefinitionVersionId == null);
    }
}
