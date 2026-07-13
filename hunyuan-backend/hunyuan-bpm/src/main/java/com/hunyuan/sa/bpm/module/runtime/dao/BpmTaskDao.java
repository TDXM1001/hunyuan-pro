package com.hunyuan.sa.bpm.module.runtime.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 流程任务 DAO。
 */
@Mapper
public interface BpmTaskDao extends BaseMapper<BpmTaskEntity> {

    /**
     * 分页查询流程任务。
     */
    List<BpmTaskVO> queryPage(Page page, @Param("queryForm") BpmTaskQueryForm queryForm);

    /**
     * 查询流程实例当前待办任务。
     */
    List<BpmTaskVO> queryCurrentTasksByInstanceId(@Param("instanceId") Long instanceId);

    /**
     * 按主键锁定任务。
     */
    BpmTaskEntity selectByIdForUpdate(@Param("taskId") Long taskId);

    /**
     * 锁定审批组内仍可处理的成员任务。
     */
    List<BpmTaskEntity> selectPendingByApprovalGroupIdForUpdate(@Param("approvalGroupId") Long approvalGroupId);

    /**
     * 查询审批组全部成员任务。
     */
    List<BpmTaskEntity> selectByApprovalGroupIds(@Param("approvalGroupIds") List<Long> approvalGroupIds);

    /**
     * 查询一个 M2 冻结成员对应的任务投影。
     */
    @Select("SELECT * FROM t_bpm_task WHERE approval_stage_member_id = #{approvalStageMemberId} LIMIT 1")
    BpmTaskEntity selectByApprovalStageMemberId(@Param("approvalStageMemberId") Long approvalStageMemberId);

    @Select("SELECT * FROM t_bpm_task WHERE approval_stage_member_id = #{approvalStageMemberId} LIMIT 1 FOR UPDATE")
    BpmTaskEntity selectByApprovalStageMemberIdForUpdate(@Param("approvalStageMemberId") Long approvalStageMemberId);

    @Update("UPDATE t_bpm_task SET assignee_employee_id = #{employeeId}, assignee_name_snapshot = #{employeeName}, "
            + "assignee_department_id_snapshot = #{departmentId}, assignee_department_name_snapshot = #{departmentName}, "
            + "runtime_assignment_snapshot_json = #{assignmentJson}, task_state = 1, cancelled_at = NULL, "
            + "assigned_at = NOW(), last_action_at = NOW(), update_time = NOW() WHERE task_id = #{taskId}")
    int restoreM2Task(
            @Param("taskId") Long taskId,
            @Param("employeeId") Long employeeId,
            @Param("employeeName") String employeeName,
            @Param("departmentId") Long departmentId,
            @Param("departmentName") String departmentName,
            @Param("assignmentJson") String assignmentJson
    );
}
