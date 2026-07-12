package com.hunyuan.sa.bpm.module.model.domain.form;

import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端条件保存 Graph 草稿请求。
 */
@Data
public class BpmGraphDraftSaveForm {

    @Schema(description = "Graph 草稿 ID")
    @NotNull(message = "Graph 草稿 ID 不能为空")
    private Long draftId;

    @Schema(description = "当前草稿 revision")
    @Min(value = 1, message = "草稿 revision 必须大于 0")
    private int revision;

    @Schema(description = "正式流程图")
    @NotNull(message = "正式流程图不能为空")
    private HunyuanProcessDefinitionGraph graph;
}
