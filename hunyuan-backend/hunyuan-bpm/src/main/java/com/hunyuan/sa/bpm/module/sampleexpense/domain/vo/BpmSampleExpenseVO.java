package com.hunyuan.sa.bpm.module.sampleexpense.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BPM 样板费用申请详情。
 */
@Data
public class BpmSampleExpenseVO {

    private Long expenseId;

    private String title;

    private BigDecimal amount;

    private Long applicantEmployeeId;

    private Integer approvalStatus;

    private Long instanceId;

    private String callbackEventId;

    private Boolean callbackFailFlag;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
