package com.hunyuan.sa.bpm.module.runtime.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 取消流程实例表单。
 */
@Data
public class BpmInstanceCancelForm {

    @Schema(description = "实例ID")
    @NotNull(message = "实例ID不能为空")
    private Long instanceId;

    @Schema(description = "取消原因")
    private String cancelReason;
}
