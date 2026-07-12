package com.hunyuan.sa.bpm.module.candidate.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 策略启用或退休时的精确版本和目录 CAS 条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmPolicyCatalogLifecycleForm extends BpmPolicyCatalogReferenceForm {

    @Schema(description = "页面读取到的目录 revision")
    @NotNull(message = "策略目录版本不能为空")
    @Min(value = 0, message = "策略目录版本不能为负数")
    private Long catalogRevision;
}
