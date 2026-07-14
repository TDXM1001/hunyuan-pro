package com.hunyuan.sa.bpm.module.operations.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.operations.domain.entity.BpmOperationsRetentionPolicyEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * BPM 运营治理保留策略 DAO。
 */
@Mapper
public interface BpmOperationsRetentionPolicyDao extends BaseMapper<BpmOperationsRetentionPolicyEntity> {
}
