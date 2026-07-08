package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 运行时发起 / 重提草稿。
 */
@Data
public class BpmRuntimeStartDraftVO {

    @Schema(description = "定义ID")
    private Long definitionId;

    @Schema(description = "流程定义名称")
    private String definitionName;

    @Schema(description = "表单名称快照")
    private String formNameSnapshot;

    @Schema(description = "表单 schema 快照 JSON")
    private String formSchemaSnapshotJson;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "流程摘要")
    private String summary;

    @Schema(description = "表单数据 JSON")
    private String formDataJson;

    @Schema(description = "来源实例ID；首次发起时为空")
    private Long sourceInstanceId;
}
