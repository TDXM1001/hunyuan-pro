package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceCopyEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程抄送 DAO。
 */
@Mapper
public interface BpmInstanceCopyDao extends BaseMapper<BpmInstanceCopyEntity> {
}
