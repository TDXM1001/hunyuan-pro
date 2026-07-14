package com.hunyuan.sa.bpm.module.candidate.domain.form;

import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyReference;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BpmPolicyTechnicalDiffForm {

    @NotNull private PolicyType leftType;
    @NotBlank private String leftPolicyKey;
    @NotNull @Min(1) private Integer leftPolicyVersion;
    @NotNull private PolicyType rightType;
    @NotBlank private String rightPolicyKey;
    @NotNull @Min(1) private Integer rightPolicyVersion;

    public PolicyReference toLeftReference() {
        return new PolicyReference(leftType, leftPolicyKey, leftPolicyVersion);
    }

    public PolicyReference toRightReference() {
        return new PolicyReference(rightType, rightPolicyKey, rightPolicyVersion);
    }
}
