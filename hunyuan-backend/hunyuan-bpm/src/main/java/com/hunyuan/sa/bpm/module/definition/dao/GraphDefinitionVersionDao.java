package com.hunyuan.sa.bpm.module.definition.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GraphDefinitionVersionDao extends BaseMapper<GraphDefinitionVersionEntity> {

    @Select("SELECT MAX(definition_version) FROM t_bpm_graph_definition_version WHERE process_key=#{processKey}")
    Integer selectMaxVersionByProcessKey(@Param("processKey") String processKey);

    @Select("SELECT * FROM t_bpm_graph_definition_version WHERE draft_id=#{draftId} ORDER BY definition_version DESC LIMIT 1")
    GraphDefinitionVersionEntity selectLatestByDraftId(@Param("draftId") Long draftId);

    @Select("SELECT * FROM t_bpm_graph_definition_version "
            + "WHERE engine_process_definition_id=#{engineProcessDefinitionId} LIMIT 1")
    GraphDefinitionVersionEntity selectByEngineProcessDefinitionId(
            @Param("engineProcessDefinitionId") String engineProcessDefinitionId
    );

    @Update("UPDATE t_bpm_graph_definition_version SET lifecycle_state='INACTIVE' WHERE graph_definition_version_id=#{id} AND lifecycle_state='ACTIVE'")
    int deactivate(@Param("id") Long id);

    @Select("SELECT * FROM t_bpm_graph_definition_version "
            + "WHERE lifecycle_state='ACTIVE' "
            + "ORDER BY process_name_snapshot ASC, definition_version DESC, graph_definition_version_id DESC")
    List<GraphDefinitionVersionEntity> selectActiveStartableList();
}
