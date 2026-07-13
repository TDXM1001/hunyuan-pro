package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批阶段事实 DAO。
 */
@Mapper
public interface BpmApprovalStageDao extends BaseMapper<BpmApprovalStageEntity> {

    @Select("SELECT * FROM t_bpm_approval_stage "
            + "WHERE instance_id = #{instanceId} AND authored_node_id = #{authoredNodeId} "
            + "AND generation = #{generation} LIMIT 1")
    BpmApprovalStageEntity selectByInstanceIdAndAuthoredNodeIdAndGeneration(
            @Param("instanceId") Long instanceId,
            @Param("authoredNodeId") String authoredNodeId,
            @Param("generation") Integer generation
    );

    @Select("SELECT * FROM t_bpm_approval_stage WHERE stage_invocation_id = #{stageInvocationId} LIMIT 1")
    BpmApprovalStageEntity selectByStageInvocationId(@Param("stageInvocationId") String stageInvocationId);

    @Select("SELECT * FROM t_bpm_approval_stage WHERE stage_invocation_id = #{stageInvocationId} FOR UPDATE")
    BpmApprovalStageEntity selectByStageInvocationIdForUpdate(@Param("stageInvocationId") String stageInvocationId);

    @Select("SELECT * FROM t_bpm_approval_stage "
            + "WHERE instance_id = #{instanceId} AND authored_node_id = #{authoredNodeId} "
            + "AND engine_execution_id = #{engineExecutionId} "
            + "ORDER BY generation DESC LIMIT 1")
    BpmApprovalStageEntity selectLatestByEngineBinding(
            @Param("instanceId") Long instanceId,
            @Param("authoredNodeId") String authoredNodeId,
            @Param("engineExecutionId") String engineExecutionId
    );

    @Select("SELECT stage_invocation_id FROM t_bpm_approval_stage "
            + "WHERE stage_state IN ('APPROVED', 'REJECTED', 'RETURNED', 'CANCELLED') "
            + "AND engine_effect_state IN ('PENDING', 'CLAIMED', 'FAILED') "
            + "ORDER BY approval_stage_id ASC LIMIT #{limit}")
    List<String> selectRecoverableStageInvocationIds(@Param("limit") int limit);

    @Select("SELECT COALESCE(MAX(generation), -1) + 1 FROM t_bpm_approval_stage "
            + "WHERE instance_id = #{instanceId} AND authored_node_id = #{authoredNodeId}")
    Integer selectNextGeneration(
            @Param("instanceId") Long instanceId,
            @Param("authoredNodeId") String authoredNodeId
    );

    @Update("UPDATE t_bpm_approval_stage "
            + "SET engine_effect_state = 'CLAIMED', terminal_reason = #{terminalReason}, "
            + "engine_effect_claimed_at = NOW(), revision = revision + 1 "
            + "WHERE approval_stage_id = #{approvalStageId} AND engine_effect_state = 'PENDING'")
    int claimEngineEffect(
            @Param("approvalStageId") Long approvalStageId,
            @Param("terminalReason") String terminalReason
    );

    @Update("UPDATE t_bpm_approval_stage "
            + "SET engine_effect_state = 'COMPLETED', engine_effect_completed_at = NOW(), revision = revision + 1 "
            + "WHERE approval_stage_id = #{approvalStageId} AND engine_effect_state = 'CLAIMED'")
    int markEngineEffectCompleted(@Param("approvalStageId") Long approvalStageId);

    @Update("UPDATE t_bpm_approval_stage "
            + "SET engine_effect_state = 'FAILED', engine_effect_error = #{error}, revision = revision + 1 "
            + "WHERE approval_stage_id = #{approvalStageId} AND engine_effect_state = 'CLAIMED'")
    int markEngineEffectFailed(
            @Param("approvalStageId") Long approvalStageId,
            @Param("error") String error
    );

    @Update("UPDATE t_bpm_approval_stage "
            + "SET engine_effect_state = 'COMPLETED', engine_effect_completed_at = NOW(), "
            + "engine_effect_error = NULL, stage_state = terminal_reason, revision = revision + 1 "
            + "WHERE approval_stage_id = #{approvalStageId} "
            + "AND engine_effect_state IN ('CLAIMED', 'FAILED')")
    int markEngineEffectReconciledCompleted(@Param("approvalStageId") Long approvalStageId);

    @Update("UPDATE t_bpm_approval_stage "
            + "SET stage_state = 'EXCEPTION_PENDING', engine_effect_error = #{error}, revision = revision + 1 "
            + "WHERE approval_stage_id = #{approvalStageId} "
            + "AND engine_effect_state IN ('CLAIMED', 'FAILED')")
    int markEngineEffectExceptionPending(
            @Param("approvalStageId") Long approvalStageId,
            @Param("error") String error
    );

    @Select("SELECT * FROM t_bpm_approval_stage "
            + "WHERE approval_stage_id = #{approvalStageId} FOR UPDATE")
    BpmApprovalStageEntity selectByIdForUpdate(@Param("approvalStageId") Long approvalStageId);

    @Update("UPDATE t_bpm_approval_stage "
            + "SET stage_state = #{stageState}, terminal_reason = #{terminalReason}, "
            + "closed_at = #{closedAt}, revision = revision + 1, update_time = NOW() "
            + "WHERE approval_stage_id = #{approvalStageId} AND revision = #{expectedRevision}")
    int updateState(
            @Param("approvalStageId") Long approvalStageId,
            @Param("expectedRevision") Integer expectedRevision,
            @Param("stageState") String stageState,
            @Param("terminalReason") String terminalReason,
            @Param("closedAt") LocalDateTime closedAt
    );

    @Update("UPDATE t_bpm_approval_stage SET stage_state = 'EXCEPTION_PENDING', "
            + "terminal_reason = #{reason}, revision = revision + 1, update_time = NOW() "
            + "WHERE approval_stage_id = #{stageId} AND stage_state = 'ACTIVE'")
    int markMemberEligibilityException(@Param("stageId") Long stageId, @Param("reason") String reason);

    @Update("UPDATE t_bpm_approval_stage SET stage_state = 'ACTIVE', terminal_reason = NULL, "
            + "revision = revision + 1, update_time = NOW() "
            + "WHERE approval_stage_id = #{stageId} AND stage_state = 'EXCEPTION_PENDING'")
    int restoreFromMemberEligibilityException(@Param("stageId") Long stageId);
}
