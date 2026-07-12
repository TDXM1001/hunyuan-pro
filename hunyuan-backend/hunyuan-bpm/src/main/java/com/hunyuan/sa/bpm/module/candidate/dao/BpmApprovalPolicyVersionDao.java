package com.hunyuan.sa.bpm.module.candidate.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.candidate.domain.entity.BpmApprovalPolicyVersionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BpmApprovalPolicyVersionDao extends BaseMapper<BpmApprovalPolicyVersionEntity> {

    @Select("SELECT * FROM t_bpm_approval_policy_version "
            + "WHERE policy_key = #{policyKey} AND policy_version = #{policyVersion} FOR UPDATE")
    BpmApprovalPolicyVersionEntity selectByPolicyKeyAndVersionForUpdate(
            @Param("policyKey") String policyKey,
            @Param("policyVersion") Integer policyVersion
    );

    @Select("SELECT MAX(policy_version) FROM t_bpm_approval_policy_version WHERE policy_key = #{policyKey}")
    Integer selectMaxPolicyVersionByPolicyKey(@Param("policyKey") String policyKey);
}
