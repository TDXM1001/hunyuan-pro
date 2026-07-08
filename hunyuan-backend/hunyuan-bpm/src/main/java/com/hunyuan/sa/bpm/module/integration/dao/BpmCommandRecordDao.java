package com.hunyuan.sa.bpm.module.integration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCommandRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * BPM 命令执行记录 DAO。
 */
@Mapper
public interface BpmCommandRecordDao extends BaseMapper<BpmCommandRecordEntity> {
}
