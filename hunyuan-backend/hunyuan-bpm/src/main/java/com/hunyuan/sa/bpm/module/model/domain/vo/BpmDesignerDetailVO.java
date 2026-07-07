package com.hunyuan.sa.bpm.module.model.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 设计器详情返回结果。
 */
@Data
public class BpmDesignerDetailVO {

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

    @Schema(description = "表单 schema")
    private String formSchemaJson;

    @Schema(description = "表单布局")
    private String formLayoutJson;

    @Schema(description = "设计器草稿 JSON")
    private String simpleModelJson;

    @Schema(description = "发起规则 JSON")
    private String startRuleJson;

    @Schema(description = "主管范围 JSON")
    private String managerScopeJson;

    @Schema(description = "标题规则 JSON")
    private String titleRuleJson;

    @Schema(description = "摘要规则 JSON")
    private String summaryRuleJson;

    @Schema(description = "变量映射 JSON")
    private String variableMappingJson;

    @Schema(description = "单号规则ID")
    private Integer instanceNoRuleId;

    @Schema(description = "已发布定义ID")
    private Long publishedDefinitionId;

    @Schema(description = "是否存在未发布变更")
    private Boolean hasUnpublishedChanges;
}
