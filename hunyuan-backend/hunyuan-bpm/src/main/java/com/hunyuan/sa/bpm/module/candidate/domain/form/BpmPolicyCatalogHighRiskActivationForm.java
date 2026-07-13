package com.hunyuan.sa.bpm.module.candidate.domain.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BpmPolicyCatalogHighRiskActivationForm extends BpmPolicyCatalogLifecycleForm {

    @NotBlank(message = "高风险策略确认原因不能为空")
    @Size(max = 512, message = "高风险策略确认原因最多 512 个字符")
    private String confirmationReason;
}
