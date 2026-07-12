package com.hunyuan.sa.bpm.module.candidate.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.candidate.domain.entity.BpmCandidatePolicyVersionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * M2 候选策略版本目录。
 */
@Mapper
public interface BpmCandidatePolicyVersionDao extends BaseMapper<BpmCandidatePolicyVersionEntity> {

    @Select("SELECT * FROM t_bpm_candidate_policy_version "
            + "WHERE policy_key = #{policyKey} AND policy_version = #{policyVersion} FOR UPDATE")
    BpmCandidatePolicyVersionEntity selectByPolicyKeyAndVersionForUpdate(
            @Param("policyKey") String policyKey,
            @Param("policyVersion") Integer policyVersion
    );

    @Select("SELECT MAX(policy_version) FROM t_bpm_candidate_policy_version WHERE policy_key = #{policyKey}")
    Integer selectMaxPolicyVersionByPolicyKey(@Param("policyKey") String policyKey);
}
