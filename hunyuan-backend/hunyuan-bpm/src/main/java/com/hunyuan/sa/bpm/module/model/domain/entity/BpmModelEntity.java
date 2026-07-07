package com.hunyuan.sa.bpm.module.model.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程模型草稿实体。
 */
@Data
@TableName("t_bpm_model")
public class BpmModelEntity {

    @TableId(type = IdType.AUTO)
    private Long modelId;

    private String modelKey;

    private String modelName;

    private Long categoryId;

    private Integer formType;

    private Long formId;

    private Boolean visibleFlag;

    private Integer sort;

    private String description;

    private String simpleModelJson;

    private String startRuleJson;

    private String managerScopeJson;

    private String titleRuleJson;

    private String summaryRuleJson;

    private String variableMappingJson;

    private Integer instanceNoRuleId;

    private Long publishedDefinitionId;

    private Boolean hasUnpublishedChanges;

    private Boolean deletedFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
