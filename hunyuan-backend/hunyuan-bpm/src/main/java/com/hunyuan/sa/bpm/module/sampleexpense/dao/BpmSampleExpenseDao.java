package com.hunyuan.sa.bpm.module.sampleexpense.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.sampleexpense.domain.entity.BpmSampleExpenseEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * BPM 样板费用申请 DAO。
 */
@Mapper
public interface BpmSampleExpenseDao extends BaseMapper<BpmSampleExpenseEntity> {
}
