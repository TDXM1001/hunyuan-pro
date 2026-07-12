package com.hunyuan.sa.bpm.module.runtime.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批阶段中一个已冻结候选成员的运行事实。
 */
@Data
@TableName("t_bpm_approval_stage_member")
public class BpmApprovalStageMemberEntity {

    @TableId(type = IdType.AUTO)
    private Long approvalStageMemberId;

    private Long approvalStageId;

    private Integer memberOrder;

    private Long sourceEmployeeId;

    private Long currentEmployeeId;

    private String memberState;

    private String actionResult;

    private Long taskId;

    private String candidateSnapshotDigest;

    private String memberSnapshotJson;

    private LocalDateTime activatedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime stateChangedAt;

    private String changeReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
