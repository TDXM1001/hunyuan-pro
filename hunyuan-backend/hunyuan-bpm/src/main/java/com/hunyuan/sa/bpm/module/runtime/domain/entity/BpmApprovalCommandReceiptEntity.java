package com.hunyuan.sa.bpm.module.runtime.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * M2 审批命令的持久化幂等回执。
 */
@Data
@TableName("t_bpm_approval_command_receipt")
public class BpmApprovalCommandReceiptEntity {

    @TableId(type = IdType.AUTO)
    private Long approvalCommandReceiptId;

    private Long tenantId;

    private Long instanceId;

    private Long taskId;

    private String requestId;

    private String commandFingerprint;

    private String actionType;

    private Long actorEmployeeId;

    private String receiptState;

    private Boolean responseOk;

    private Integer responseCode;

    private String responseMessage;

    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
