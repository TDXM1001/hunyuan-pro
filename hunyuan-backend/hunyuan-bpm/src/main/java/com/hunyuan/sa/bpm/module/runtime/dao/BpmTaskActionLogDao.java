package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskActionLogEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskActionLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流程任务动作日志 DAO。
 */
@Mapper
public interface BpmTaskActionLogDao extends BaseMapper<BpmTaskActionLogEntity> {

    /**
     * 查询流程实例动作轨迹。
     */
    List<BpmTaskActionLogVO> queryByInstanceId(@Param("instanceId") Long instanceId);
}
