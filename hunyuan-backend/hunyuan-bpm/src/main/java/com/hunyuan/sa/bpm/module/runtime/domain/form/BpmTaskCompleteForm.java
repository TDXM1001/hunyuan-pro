package com.hunyuan.sa.bpm.module.runtime.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 办理任务完成表单。
 */
@Data
public class BpmTaskCompleteForm {

    @Schema(description = "任务ID")
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @Schema(description = "办理说明")
    private String commentText;
}
