package com.hunyuan.sa.bpm.module.businesscontract.domain.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BpmBusinessContractReferenceForm {
    @NotBlank(message = "业务契约编码不能为空")
    private String contractKey;
    @NotNull(message = "业务契约版本不能为空")
    @Min(value = 1, message = "业务契约版本必须为正整数")
    private Integer contractVersion;
}
