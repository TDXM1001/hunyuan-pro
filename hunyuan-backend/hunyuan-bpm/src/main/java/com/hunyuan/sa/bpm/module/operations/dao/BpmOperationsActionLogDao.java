package com.hunyuan.sa.bpm.module.operations.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.operations.domain.entity.BpmOperationsActionLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

/**
 * BPM 运营治理处置审计 DAO。
 */
@Mapper
public interface BpmOperationsActionLogDao extends BaseMapper<BpmOperationsActionLogEntity> {

    @Insert("""
            INSERT IGNORE INTO t_bpm_operations_action_log
              (operations_case_id, action_type, action_status, idempotency_key, actor_employee_id,
               reason, before_snapshot_json, action_at, create_time, update_time)
            VALUES
              (#{operationsCaseId}, #{actionType}, #{actionStatus}, #{idempotencyKey}, #{actorEmployeeId},
               #{reason}, #{beforeSnapshotJson}, #{actionAt}, now(), now())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "operationsActionLogId")
    int insertIdempotencyClaim(BpmOperationsActionLogEntity entity);
}
