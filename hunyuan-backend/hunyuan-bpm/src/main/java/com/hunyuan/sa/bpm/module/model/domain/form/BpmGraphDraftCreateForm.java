package com.hunyuan.sa.bpm.module.model.domain.form;

import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端创建 Graph 草稿请求。
 */
@Data
public class BpmGraphDraftCreateForm {

    @Schema(description = "流程资产编码")
    @NotBlank(message = "流程资产编码不能为空")
    @Size(max = 64, message = "流程资产编码最多 64 个字符")
    private String processKey;

    @Schema(description = "流程资产名称")
    @NotBlank(message = "流程资产名称不能为空")
    @Size(max = 128, message = "流程资产名称最多 128 个字符")
    private String processName;

    @Schema(description = "流程分类 ID")
    @NotNull(message = "请选择流程分类")
    private Long categoryId;

    @Schema(description = "正式流程图")
    @NotNull(message = "正式流程图不能为空")
    private HunyuanProcessDefinitionGraph graph;
}
