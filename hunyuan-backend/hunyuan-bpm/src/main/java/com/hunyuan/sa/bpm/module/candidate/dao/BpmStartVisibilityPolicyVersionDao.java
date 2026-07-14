package com.hunyuan.sa.bpm.module.candidate.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.candidate.domain.entity.BpmStartVisibilityPolicyVersionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BpmStartVisibilityPolicyVersionDao extends BaseMapper<BpmStartVisibilityPolicyVersionEntity> {

    @Select("SELECT * FROM t_bpm_start_visibility_policy_version "
            + "WHERE policy_key = #{policyKey} AND policy_version = #{policyVersion} FOR UPDATE")
    BpmStartVisibilityPolicyVersionEntity selectByPolicyKeyAndVersionForUpdate(
            @Param("policyKey") String policyKey,
            @Param("policyVersion") Integer policyVersion
    );

    @Select("SELECT MAX(policy_version) FROM t_bpm_start_visibility_policy_version WHERE policy_key = #{policyKey}")
    Integer selectMaxPolicyVersionByPolicyKey(@Param("policyKey") String policyKey);

    @Update("UPDATE t_bpm_start_visibility_policy_version SET policy_name=#{policyName}, description=#{description}, "
            + "policy_json=#{canonicalPayload}, policy_digest=#{digest}, business_summary=#{businessSummary}, "
            + "calculated_risk_level=#{riskLevel}, catalog_revision=catalog_revision+1, update_time=now() "
            + "WHERE policy_key=#{policyKey} AND policy_version=#{policyVersion} "
            + "AND lifecycle_state='DRAFT' AND catalog_revision=#{expectedRevision}")
    int saveDraftVisual(@Param("policyKey") String policyKey, @Param("policyVersion") Integer policyVersion,
                        @Param("expectedRevision") Long expectedRevision, @Param("policyName") String policyName,
                        @Param("description") String description, @Param("canonicalPayload") String canonicalPayload,
                        @Param("digest") String digest, @Param("riskLevel") String riskLevel,
                        @Param("businessSummary") String businessSummary);

    @Update("DELETE FROM t_bpm_start_visibility_policy_version WHERE policy_key=#{policyKey} AND policy_version=#{policyVersion} "
            + "AND lifecycle_state='DRAFT' AND catalog_revision=#{expectedRevision}")
    int deleteDraft(@Param("policyKey") String policyKey, @Param("policyVersion") Integer policyVersion,
                    @Param("expectedRevision") Long expectedRevision);
}
