package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmExternalWaitEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * BPM 外部等待 DAO。
 */
@Mapper
public interface BpmExternalWaitDao extends BaseMapper<BpmExternalWaitEntity> {
}
