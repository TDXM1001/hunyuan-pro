package com.hunyuan.sa.bpm.module.definition.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程定义节点 DAO。
 */
@Mapper
public interface BpmDefinitionNodeDao extends BaseMapper<BpmDefinitionNodeEntity> {
}
