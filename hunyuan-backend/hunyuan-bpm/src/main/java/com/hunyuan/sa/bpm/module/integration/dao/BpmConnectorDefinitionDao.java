package com.hunyuan.sa.bpm.module.integration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmConnectorDefinitionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * BPM 登记连接器定义 DAO。
 */
@Mapper
public interface BpmConnectorDefinitionDao extends BaseMapper<BpmConnectorDefinitionEntity> {
}
