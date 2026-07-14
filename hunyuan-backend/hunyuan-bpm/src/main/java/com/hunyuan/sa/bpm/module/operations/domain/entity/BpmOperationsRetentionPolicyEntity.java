package com.hunyuan.sa.bpm.module.operations.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BPM 运营治理保留策略。
 */
@Data
@TableName("t_bpm_operations_retention_policy")
public class BpmOperationsRetentionPolicyEntity {

    @TableId(type = IdType.AUTO)
    private Long retentionPolicyId;

    private String policyKey;

    private Long definitionId;

    private String businessType;

    private Integer retentionDays;

    private Integer archiveAfterDays;

    private Boolean legalHoldFlag;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
