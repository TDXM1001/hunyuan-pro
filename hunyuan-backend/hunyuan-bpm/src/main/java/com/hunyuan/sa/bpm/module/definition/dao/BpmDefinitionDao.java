package com.hunyuan.sa.bpm.module.definition.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程定义 DAO。
 */
@Mapper
public interface BpmDefinitionDao extends BaseMapper<BpmDefinitionEntity> {

}
