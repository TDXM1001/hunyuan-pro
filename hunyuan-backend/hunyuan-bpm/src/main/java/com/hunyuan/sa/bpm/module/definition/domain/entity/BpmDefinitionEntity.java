package com.hunyuan.sa.bpm.module.definition.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程定义发布快照实体。
 */
@Data
@TableName("t_bpm_definition")
public class BpmDefinitionEntity {

    @TableId(type = IdType.AUTO)
    private Long definitionId;

    private Long modelId;

    private String definitionKey;

    private String definitionName;

    private Integer definitionVersion;

    private Long categoryIdSnapshot;

    private String categoryNameSnapshot;

    private Integer formTypeSnapshot;

    private Long formIdSnapshot;

    private String formNameSnapshot;

    private String formSchemaSnapshotJson;

    private String simpleModelSnapshotJson;

    private String compiledBpmnXml;

    private String startRuleSnapshotJson;

    private String managerScopeSnapshotJson;

    private String titleRuleSnapshotJson;

    private String summaryRuleSnapshotJson;

    private String variableMappingSnapshotJson;

    private Integer instanceNoRuleIdSnapshot;

    private Integer lifecycleState;

    private Integer startState;

    private String engineProcessDefinitionId;

    private Long publishedByEmployeeId;

    private String publishedByNameSnapshot;

    private LocalDateTime publishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
