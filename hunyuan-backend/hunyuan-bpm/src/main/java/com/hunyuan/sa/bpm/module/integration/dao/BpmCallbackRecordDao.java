package com.hunyuan.sa.bpm.module.integration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * BPM 业务回调记录 DAO。
 */
@Mapper
public interface BpmCallbackRecordDao extends BaseMapper<BpmCallbackRecordEntity> {
}
