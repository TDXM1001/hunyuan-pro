package com.hunyuan.sa.bpm.module.businesscontract.domain.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BpmBusinessContractLifecycleForm extends BpmBusinessContractReferenceForm {
    @NotNull(message = "目录修订号不能为空")
    @Min(value = 0, message = "目录修订号不能为负数")
    private Long catalogRevision;
}
