package com.hunyuan.sa.bpm.module.candidate.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * M2 审批完成策略的不可变版本目录。
 */
@Data
@TableName("t_bpm_approval_policy_version")
public class BpmApprovalPolicyVersionEntity {

    @TableId(type = IdType.AUTO)
    private Long approvalPolicyVersionId;

    private String policyKey;

    private Integer policyVersion;

    private String lifecycleState;

    private Integer schemaVersion;

    private String policyJson;

    private String policyDigest;

    private Long catalogRevision;

    private Long createdByEmployeeId;

    private Long activatedByEmployeeId;

    private LocalDateTime activatedAt;

    private Long retiredByEmployeeId;

    private LocalDateTime retiredAt;

    private String effectiveRisk;
    private Long highRiskConfirmedByEmployeeId;
    private String highRiskConfirmationReason;
    private LocalDateTime highRiskConfirmedAt;
    private String highRiskConfirmedDigest;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
