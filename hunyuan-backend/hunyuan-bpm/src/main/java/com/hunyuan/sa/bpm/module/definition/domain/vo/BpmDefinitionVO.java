package com.hunyuan.sa.bpm.module.definition.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程定义列表返回结果。
 */
@Data
public class BpmDefinitionVO {

    @Schema(description = "定义ID")
    private Long definitionId;

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "定义编码")
    private String definitionKey;

    @Schema(description = "定义名称")
    private String definitionName;

    @Schema(description = "定义版本")
    private Integer definitionVersion;

    @Schema(description = "分类名称快照")
    private String categoryNameSnapshot;

    @Schema(description = "表单名称快照")
    private String formNameSnapshot;

    @Schema(description = "生命周期状态")
    private Integer lifecycleState;

    @Schema(description = "发起状态")
    private Integer startState;

    @Schema(description = "发布人姓名快照")
    private String publishedByNameSnapshot;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;
}
