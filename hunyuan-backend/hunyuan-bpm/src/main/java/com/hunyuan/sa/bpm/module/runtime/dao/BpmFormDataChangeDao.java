package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmFormDataChangeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流程表单数据变更 DAO。
 */
@Mapper
public interface BpmFormDataChangeDao extends BaseMapper<BpmFormDataChangeEntity> {

    /**
     * 按发生顺序查询实例数据变更。
     */
    List<BpmFormDataChangeEntity> queryByInstanceId(@Param("instanceId") Long instanceId);
}
