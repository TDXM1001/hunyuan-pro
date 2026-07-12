package com.hunyuan.sa.bpm.module.definition.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionElementMappingEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GraphDefinitionElementMappingDao extends BaseMapper<GraphDefinitionElementMappingEntity> {

    @Select("SELECT * FROM t_bpm_graph_definition_mapping "
            + "WHERE graph_definition_version_id = #{graphDefinitionVersionId} "
            + "AND compiled_element_id = #{compiledElementId} LIMIT 1")
    GraphDefinitionElementMappingEntity selectByGraphDefinitionVersionIdAndCompiledElementId(
            @Param("graphDefinitionVersionId") Long graphDefinitionVersionId,
            @Param("compiledElementId") String compiledElementId
    );
}
