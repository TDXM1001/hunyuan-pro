package com.hunyuan.sa.bpm.module.operations.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.operations.domain.entity.BpmOperationsCaseEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * BPM 运营治理异常工单 DAO。
 */
@Mapper
public interface BpmOperationsCaseDao extends BaseMapper<BpmOperationsCaseEntity> {
}
