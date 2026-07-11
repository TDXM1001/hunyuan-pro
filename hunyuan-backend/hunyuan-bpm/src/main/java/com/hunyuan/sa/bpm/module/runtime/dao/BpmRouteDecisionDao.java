package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmRouteDecisionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 路由决定事实 DAO。
 */
@Mapper
public interface BpmRouteDecisionDao extends BaseMapper<BpmRouteDecisionEntity> {

    BpmRouteDecisionEntity selectByGenerationAndNode(
            @Param("instanceId") Long instanceId,
            @Param("engineProcessInstanceId") String engineProcessInstanceId,
            @Param("routeNodeKey") String routeNodeKey
    );

    List<BpmRouteDecisionEntity> queryByInstanceId(@Param("instanceId") Long instanceId);
}
