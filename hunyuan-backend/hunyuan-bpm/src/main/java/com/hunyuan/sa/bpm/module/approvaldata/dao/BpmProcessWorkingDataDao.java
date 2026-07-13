package com.hunyuan.sa.bpm.module.approvaldata.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmProcessWorkingDataEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BpmProcessWorkingDataDao extends BaseMapper<BpmProcessWorkingDataEntity> {

    @Select("""
            SELECT * FROM t_bpm_process_working_data
            WHERE approval_subject_snapshot_id = #{subjectId}
            ORDER BY data_version DESC
            LIMIT 1
            FOR UPDATE
            """)
    BpmProcessWorkingDataEntity selectLatestBySubjectForUpdate(@Param("subjectId") Long subjectId);

    @Select("""
            SELECT * FROM t_bpm_process_working_data
            WHERE approval_subject_snapshot_id = #{subjectId}
            ORDER BY data_version DESC
            LIMIT 1
            """)
    BpmProcessWorkingDataEntity selectLatestBySubject(@Param("subjectId") Long subjectId);
}
