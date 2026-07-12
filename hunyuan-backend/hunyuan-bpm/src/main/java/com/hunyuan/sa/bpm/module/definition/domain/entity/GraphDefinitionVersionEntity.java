package com.hunyuan.sa.bpm.module.definition.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("t_bpm_graph_definition_version")
public class GraphDefinitionVersionEntity {
 @TableId(type=IdType.AUTO) private Long graphDefinitionVersionId;
 private Long draftId; private String processKey; private Integer definitionVersion; private String lifecycleState; private String graphSnapshotJson; private String layoutSnapshotJson; private String semanticHash; private String dependencyVersionsJson; private String compilerVersion; private String compiledBpmnXml; private String deploymentId; private String engineProcessDefinitionId; private Long publishedByEmployeeId;
 @TableField(fill=FieldFill.INSERT) private LocalDateTime createTime; @TableField(fill=FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
