package com.hunyuan.sa.bpm.module.evolution.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.evolution.domain.entity.BpmMigrationItemEntity;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface BpmMigrationItemDao extends BaseMapper<BpmMigrationItemEntity> {
    @Select("SELECT * FROM t_bpm_migration_item WHERE migration_batch_id=#{batchId} ORDER BY migration_item_id")
    List<BpmMigrationItemEntity> selectByBatchId(@Param("batchId") Long batchId);

    @Select("SELECT * FROM t_bpm_migration_item WHERE migration_item_id=#{itemId} FOR UPDATE")
    BpmMigrationItemEntity selectByIdForUpdate(@Param("itemId") Long itemId);
}
