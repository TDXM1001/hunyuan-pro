package com.hunyuan.sa.bpm.module.sampleexpense.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * BPM 样板费用申请创建表单。
 */
@Data
public class BpmSampleExpenseCreateForm {

    @Schema(description = "申请标题")
    @NotBlank(message = "申请标题不能为空")
    @Size(max = 100, message = "申请标题最多100个字符")
    private String title;

    @Schema(description = "申请金额")
    @NotNull(message = "申请金额不能为空")
    @DecimalMin(value = "0.01", message = "申请金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "申请人员工ID")
    @NotNull(message = "申请人员工ID不能为空")
    private Long applicantEmployeeId;
}
