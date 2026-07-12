package com.hunyuan.sa.bpm.module.candidate.domain.form;

import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端指向一个精确的不可变策略版本。
 */
@Data
public class BpmPolicyCatalogReferenceForm {

    @Schema(description = "策略类型")
    @NotNull(message = "策略类型不能为空")
    private PolicyType type;

    @Schema(description = "策略编码")
    @NotBlank(message = "策略编码不能为空")
    @Size(max = 128, message = "策略编码最多 128 个字符")
    private String policyKey;

    @Schema(description = "策略版本")
    @NotNull(message = "策略版本不能为空")
    @Min(value = 1, message = "策略版本必须为正整数")
    private Integer policyVersion;
}
