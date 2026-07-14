package com.hunyuan.sa.bpm.module.evolution.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.evolution.domain.entity.BpmMigrationBatchEntity;
import org.apache.ibatis.annotations.*;

@Mapper
public interface BpmMigrationBatchDao extends BaseMapper<BpmMigrationBatchEntity> {
    @Select("SELECT * FROM t_bpm_migration_batch WHERE idempotency_key=#{key} LIMIT 1")
    BpmMigrationBatchEntity selectByIdempotencyKey(@Param("key") String key);

    @Select("SELECT * FROM t_bpm_migration_batch WHERE idempotency_key=#{key} LIMIT 1 FOR UPDATE")
    BpmMigrationBatchEntity selectByIdempotencyKeyForUpdate(@Param("key") String key);

    @Update("UPDATE t_bpm_migration_batch SET batch_status='EXECUTING', execution_owner_key=#{ownerKey}, "
            + "execution_lease_until=DATE_ADD(NOW(), INTERVAL 2 MINUTE), confirmed_at=COALESCE(confirmed_at,NOW()), "
            + "confirmed_by_employee_id=COALESCE(confirmed_by_employee_id,#{confirmedBy}), update_time=NOW() "
            + "WHERE migration_batch_id=#{batchId} AND (batch_status='PREVIEWED' OR "
            + "(batch_status='EXECUTING' AND execution_lease_until < NOW()))")
    int claimForExecution(@Param("batchId") Long batchId, @Param("ownerKey") String ownerKey,
                          @Param("confirmedBy") Long confirmedBy);

    @Update("UPDATE t_bpm_migration_batch SET execution_lease_until=DATE_ADD(NOW(), INTERVAL 2 MINUTE), "
            + "update_time=NOW() WHERE migration_batch_id=#{batchId} AND batch_status='EXECUTING' "
            + "AND execution_owner_key=#{ownerKey} AND execution_lease_until >= NOW()")
    int renewExecutionLease(@Param("batchId") Long batchId, @Param("ownerKey") String ownerKey);

    @Update("UPDATE t_bpm_migration_batch SET batch_status=#{status}, succeeded_count=#{succeeded}, "
            + "failed_count=#{failed}, completed_at=NOW(), execution_owner_key=NULL, execution_lease_until=NULL, "
            + "update_time=NOW() WHERE migration_batch_id=#{batchId} AND batch_status='EXECUTING' "
            + "AND execution_owner_key=#{ownerKey}")
    int finalizeExecution(@Param("batchId") Long batchId, @Param("ownerKey") String ownerKey,
                          @Param("status") String status, @Param("succeeded") int succeeded,
                          @Param("failed") int failed);
}
