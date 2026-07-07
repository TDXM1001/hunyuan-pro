package com.hunyuan.sa.bpm.module.model.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程模型返回结果。
 */
@Data
public class BpmModelVO {

    @Schema(description = "模型ID")
    private Long modelId;

    @Schema(description = "模型编码")
    private String modelKey;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "表单类型")
    private Integer formType;

    @Schema(description = "表单ID")
    private Long formId;

    @Schema(description = "表单名称")
    private String formName;

    @Schema(description = "是否可见")
    private Boolean visibleFlag;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "已发布定义ID")
    private Long publishedDefinitionId;

    @Schema(description = "是否存在未发布变更")
    private Boolean hasUnpublishedChanges;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
