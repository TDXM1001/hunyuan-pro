package com.hunyuan.sa.bpm.module.businesscontract.domain.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BpmBusinessContractDraftForm {

    @NotBlank(message = "业务契约编码不能为空")
    @Size(max = 64, message = "业务契约编码最多 64 个字符")
    private String contractKey;

    @NotNull(message = "schema 版本不能为空")
    @Min(value = 1, message = "schema 版本必须为正整数")
    private Integer schemaVersion;

    @NotBlank(message = "业务契约内容不能为空")
    private String contractJson;
}
