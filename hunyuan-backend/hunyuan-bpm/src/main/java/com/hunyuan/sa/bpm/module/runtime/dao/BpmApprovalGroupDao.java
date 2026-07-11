package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalGroupEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 并行审批组 DAO。
 */
@Mapper
public interface BpmApprovalGroupDao extends BaseMapper<BpmApprovalGroupEntity> {

    BpmApprovalGroupEntity selectByEngineProcessInstanceIdAndGroupKey(
            @Param("engineProcessInstanceId") String engineProcessInstanceId,
            @Param("approvalGroupKey") String approvalGroupKey
    );

    BpmApprovalGroupEntity selectByIdForUpdate(@Param("approvalGroupId") Long approvalGroupId);

    List<BpmApprovalGroupEntity> selectPendingByInstanceIdForUpdate(@Param("instanceId") Long instanceId);

    List<BpmApprovalGroupEntity> queryByIds(@Param("approvalGroupIds") Collection<Long> approvalGroupIds);

    List<BpmApprovalGroupEntity> selectByInstanceId(@Param("instanceId") Long instanceId);
}
