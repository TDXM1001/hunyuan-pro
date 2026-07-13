package com.hunyuan.sa.bpm.module.businesscontract.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.module.businesscontract.domain.entity.BpmBusinessContractVersionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * M3 业务契约版本目录。
 */
@Mapper
public interface BpmBusinessContractVersionDao extends BaseMapper<BpmBusinessContractVersionEntity> {

    default BpmBusinessContractVersionEntity selectActiveByKeyAndVersion(String contractKey, Integer contractVersion) {
        return selectOne(Wrappers.<BpmBusinessContractVersionEntity>lambdaQuery()
                .eq(BpmBusinessContractVersionEntity::getContractKey, contractKey)
                .eq(BpmBusinessContractVersionEntity::getContractVersion, contractVersion)
                .eq(BpmBusinessContractVersionEntity::getLifecycleState, "ACTIVE"));
    }

    @Select("SELECT MAX(contract_version) FROM t_bpm_business_contract_version WHERE contract_key = #{contractKey}")
    Integer selectMaxContractVersion(@Param("contractKey") String contractKey);

    @Select("""
            SELECT * FROM t_bpm_business_contract_version
            WHERE contract_key = #{contractKey} AND contract_version = #{contractVersion}
            FOR UPDATE
            """)
    BpmBusinessContractVersionEntity selectByKeyAndVersionForUpdate(
            @Param("contractKey") String contractKey,
            @Param("contractVersion") Integer contractVersion
    );

    @Update("""
            UPDATE t_bpm_business_contract_version
            SET lifecycle_state = #{targetState},
                contract_json = #{contractJson},
                contract_digest = #{contractDigest},
                catalog_revision = #{nextRevision},
                activated_by_employee_id = CASE WHEN #{targetState} = 'ACTIVE' THEN #{actorEmployeeId} ELSE activated_by_employee_id END,
                activated_at = CASE WHEN #{targetState} = 'ACTIVE' THEN #{actionAt} ELSE activated_at END,
                retired_by_employee_id = CASE WHEN #{targetState} = 'RETIRED' THEN #{actorEmployeeId} ELSE retired_by_employee_id END,
                retired_at = CASE WHEN #{targetState} = 'RETIRED' THEN #{actionAt} ELSE retired_at END,
                update_time = #{actionAt}
            WHERE business_contract_version_id = #{versionId}
              AND lifecycle_state = #{expectedState}
              AND catalog_revision = #{expectedRevision}
            """)
    int transitionState(
            @Param("versionId") Long versionId,
            @Param("expectedState") String expectedState,
            @Param("targetState") String targetState,
            @Param("expectedRevision") Long expectedRevision,
            @Param("nextRevision") Long nextRevision,
            @Param("contractJson") String contractJson,
            @Param("contractDigest") String contractDigest,
            @Param("actorEmployeeId") Long actorEmployeeId,
            @Param("actionAt") LocalDateTime actionAt
    );
}
