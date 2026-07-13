package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageMemberEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 审批阶段成员事实 DAO。
 */
@Mapper
public interface BpmApprovalStageMemberDao extends BaseMapper<BpmApprovalStageMemberEntity> {

    @Select("SELECT * FROM t_bpm_approval_stage_member "
            + "WHERE approval_stage_id = #{approvalStageId} ORDER BY member_order ASC")
    List<BpmApprovalStageMemberEntity> selectByApprovalStageId(
            @Param("approvalStageId") Long approvalStageId
    );

    @Select("SELECT * FROM t_bpm_approval_stage_member "
            + "WHERE approval_stage_id = #{approvalStageId} ORDER BY member_order ASC FOR UPDATE")
    List<BpmApprovalStageMemberEntity> selectByApprovalStageIdForUpdate(
            @Param("approvalStageId") Long approvalStageId
    );

    @Select("SELECT * FROM t_bpm_approval_stage_member "
            + "WHERE current_employee_id = #{employeeId} AND member_state IN ('PLANNED', 'ACTIVE') "
            + "ORDER BY approval_stage_id ASC, member_order ASC")
    List<BpmApprovalStageMemberEntity> selectOpenByCurrentEmployeeId(@Param("employeeId") Long employeeId);

    @Select("SELECT approval_stage_member_id FROM t_bpm_approval_stage_member "
            + "WHERE member_state IN ('PLANNED', 'ACTIVE') ORDER BY approval_stage_member_id LIMIT #{limit}")
    List<Long> selectOpenIdsForEligibilityScan(@Param("limit") int limit);

    @Select("SELECT * FROM t_bpm_approval_stage_member WHERE approval_stage_member_id = #{memberId} FOR UPDATE")
    BpmApprovalStageMemberEntity selectByIdForUpdate(@Param("memberId") Long memberId);
}
