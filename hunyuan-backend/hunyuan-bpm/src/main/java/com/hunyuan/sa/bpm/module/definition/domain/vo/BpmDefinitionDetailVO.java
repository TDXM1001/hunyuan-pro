package com.hunyuan.sa.bpm.module.definition.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程定义详情返回结果。
 */
@Data
public class BpmDefinitionDetailVO {

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

    @Schema(description = "分类快照ID")
    private Long categoryIdSnapshot;

    @Schema(description = "分类名称快照")
    private String categoryNameSnapshot;

    @Schema(description = "表单类型快照")
    private Integer formTypeSnapshot;

    @Schema(description = "表单快照ID")
    private Long formIdSnapshot;

    @Schema(description = "表单名称快照")
    private String formNameSnapshot;

    @Schema(description = "表单 schema 快照")
    private String formSchemaSnapshotJson;

    @Schema(description = "设计器草稿快照")
    private String simpleModelSnapshotJson;

    @Schema(description = "编译后的 BPMN XML")
    private String compiledBpmnXml;

    @Schema(description = "发起规则快照")
    private String startRuleSnapshotJson;

    @Schema(description = "主管范围快照")
    private String managerScopeSnapshotJson;

    @Schema(description = "标题规则快照")
    private String titleRuleSnapshotJson;

    @Schema(description = "摘要规则快照")
    private String summaryRuleSnapshotJson;

    @Schema(description = "变量映射快照")
    private String variableMappingSnapshotJson;

    @Schema(description = "生命周期状态")
    private Integer lifecycleState;

    @Schema(description = "发起状态")
    private Integer startState;

    @Schema(description = "Flowable 定义ID")
    private String engineProcessDefinitionId;

    @Schema(description = "发布人姓名快照")
    private String publishedByNameSnapshot;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;
}
