package com.hunyuan.sa.bpm.module.sampleexpense.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BPM 样板费用申请。
 */
@Data
@TableName("t_bpm_sample_expense")
public class BpmSampleExpenseEntity {

    @TableId(type = IdType.AUTO)
    private Long expenseId;

    private String title;

    private BigDecimal amount;

    private BigDecimal approvedAmount;

    private Long applicantEmployeeId;

    private Integer approvalStatus;

    private Long instanceId;

    private String callbackEventId;

    private Long finalFormDataVersion;

    private Boolean callbackFailFlag;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
