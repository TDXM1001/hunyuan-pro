package com.hunyuan.sa.bpm.module.candidate.domain.form;

import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端校验或新建策略草稿的受控输入。
 */
@Data
public class BpmPolicyCatalogDraftForm {

    @Schema(description = "策略类型")
    @NotNull(message = "策略类型不能为空")
    private PolicyType type;

    @Schema(description = "策略编码")
    @NotBlank(message = "策略编码不能为空")
    @Size(max = 128, message = "策略编码最多 128 个字符")
    private String policyKey;

    @Schema(description = "策略 schema 版本")
    @NotNull(message = "策略 schema 版本不能为空")
    private Integer schemaVersion;

    @Schema(description = "受控策略 JSON")
    @NotBlank(message = "策略内容不能为空")
    private String policyJson;
}
