package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmNotificationRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * BPM 通知投递记录 DAO。
 */
@Mapper
public interface BpmNotificationRecordDao extends BaseMapper<BpmNotificationRecordEntity> {
}
