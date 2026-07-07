package com.hunyuan.sa.bpm.module.definition.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程定义节点快照实体。
 */
@Data
@TableName("t_bpm_definition_node")
public class BpmDefinitionNodeEntity {

    @TableId(type = IdType.AUTO)
    private Long definitionNodeId;

    private Long definitionId;

    private String nodeKey;

    private String nodeType;

    private String nodeNameSnapshot;

    private Integer sortOrder;

    private String authoredRuleSnapshotJson;

    private String compiledNodeSnapshotJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
