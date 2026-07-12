package com.hunyuan.sa.bpm.module.definition.domain.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data;
@Data @TableName("t_bpm_graph_definition_mapping")
public class GraphDefinitionElementMappingEntity {
 @TableId(type=IdType.AUTO) private Long mappingId; private Long graphDefinitionVersionId; private String authoredElementId; private String authoredElementKind; private String compiledElementId; private String compiledElementType;
}
