package com.hunyuan.sa.bpm.module.runtime.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 并行审批组实体。
 */
@Data
@TableName("t_bpm_approval_group")
public class BpmApprovalGroupEntity {

    @TableId(type = IdType.AUTO)
    private Long approvalGroupId;

    private Long instanceId;

    private Long definitionId;

    private String engineProcessInstanceId;

    private String approvalGroupKey;

    private String approvalGroupName;

    private String approvalMode;

    private String groupState;

    private String closeReason;

    private Integer totalMemberCount;

    private Integer processedMemberCount;

    private Integer approvedMemberCount;

    private Integer rejectedMemberCount;

    private LocalDateTime closedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
