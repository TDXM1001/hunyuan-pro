package com.hunyuan.sa.bpm.module.businesscontract.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.businesscontract.domain.entity.BpmBusinessContractVersionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * M3 业务契约版本目录。
 */
@Mapper
public interface BpmBusinessContractVersionDao extends BaseMapper<BpmBusinessContractVersionEntity> {
}
