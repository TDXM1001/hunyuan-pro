package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 流程实例 DAO。
 */
@Mapper
public interface BpmInstanceDao extends BaseMapper<BpmInstanceEntity> {

    /**
     * 分页查询流程实例。
     */
    List<BpmInstanceVO> queryPage(Page page, @Param("queryForm") BpmInstanceQueryForm queryForm);

    /**
     * 按主键锁定流程实例。
     */
    BpmInstanceEntity selectByIdForUpdate(@Param("instanceId") Long instanceId);

    @Update("UPDATE t_bpm_instance SET run_state = 3, result_state = 1, "
            + "finished_at = COALESCE(finished_at, NOW()), update_time = NOW() "
            + "WHERE instance_id = #{instanceId} AND run_state = 1")
    int finishApprovedIfRunning(@Param("instanceId") Long instanceId);

    @Select("<script>SELECT * FROM t_bpm_instance WHERE instance_id IN "
            + "<foreach collection='instanceIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + " ORDER BY instance_id</script>")
    List<BpmInstanceEntity> selectByIdsForMigration(@Param("instanceIds") List<Long> instanceIds);

    @Update("UPDATE t_bpm_instance SET graph_definition_version_id=#{targetVersionId}, "
            + "engine_process_definition_id=#{targetEngineDefinitionId}, definition_version_snapshot="
            + "(SELECT definition_version FROM t_bpm_graph_definition_version WHERE graph_definition_version_id=#{targetVersionId}), "
            + "update_time=NOW() WHERE instance_id=#{instanceId} AND graph_definition_version_id=#{sourceVersionId}")
    int updateMigrationProjection(@Param("instanceId") Long instanceId,
                                  @Param("sourceVersionId") Long sourceVersionId,
                                  @Param("targetVersionId") Long targetVersionId,
                                  @Param("targetEngineDefinitionId") String targetEngineDefinitionId);
}
