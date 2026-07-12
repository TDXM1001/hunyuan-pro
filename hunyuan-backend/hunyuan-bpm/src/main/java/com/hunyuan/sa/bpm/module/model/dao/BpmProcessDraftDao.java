package com.hunyuan.sa.bpm.module.model.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmProcessDraftEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Graph 草稿持久化与 revision 条件更新。
 */
@Mapper
public interface BpmProcessDraftDao extends BaseMapper<BpmProcessDraftEntity> {

    @Select("SELECT * FROM t_bpm_process_draft WHERE process_key = #{processKey}")
    BpmProcessDraftEntity selectByProcessKey(@Param("processKey") String processKey);

    @Update("""
            UPDATE t_bpm_process_draft
            SET revision = revision + 1,
                graph_json = #{graphJson},
                layout_json = #{layoutJson},
                semantic_hash = #{semanticHash},
                updated_by_employee_id = #{updatedByEmployeeId},
                update_time = NOW()
            WHERE draft_id = #{draftId} AND revision = #{expectedRevision}
            """)
    int updateIfRevision(
            @Param("draftId") Long draftId,
            @Param("expectedRevision") int expectedRevision,
            @Param("graphJson") String graphJson,
            @Param("layoutJson") String layoutJson,
            @Param("semanticHash") String semanticHash,
            @Param("updatedByEmployeeId") Long updatedByEmployeeId
    );
}
