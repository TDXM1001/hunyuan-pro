package com.hunyuan.sa.bpm.module.approvaldata.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmRoutingFactSnapshotEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BpmRoutingFactSnapshotDao extends BaseMapper<BpmRoutingFactSnapshotEntity> {

    @Select("""
            SELECT * FROM t_bpm_routing_fact_snapshot
            WHERE approval_subject_snapshot_id = #{subjectId}
            ORDER BY routing_fact_version DESC
            LIMIT 1
            """)
    BpmRoutingFactSnapshotEntity selectLatestBySubject(@Param("subjectId") Long subjectId);
}
