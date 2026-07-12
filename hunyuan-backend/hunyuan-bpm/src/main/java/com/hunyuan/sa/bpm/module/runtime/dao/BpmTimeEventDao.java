package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTimeEventEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * BPM 时间事件 DAO。
 */
@Mapper
public interface BpmTimeEventDao extends BaseMapper<BpmTimeEventEntity> {
}
