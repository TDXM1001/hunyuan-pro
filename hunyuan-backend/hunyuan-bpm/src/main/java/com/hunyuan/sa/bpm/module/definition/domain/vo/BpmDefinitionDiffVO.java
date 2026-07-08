package com.hunyuan.sa.bpm.module.definition.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程定义发布差异预览。
 */
@Data
public class BpmDefinitionDiffVO {

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "上一版定义ID")
    private Long previousDefinitionId;

    @Schema(description = "上一版定义版本")
    private Integer previousVersion;

    @Schema(description = "变更项")
    private List<String> changedItems = new ArrayList<>();
}
