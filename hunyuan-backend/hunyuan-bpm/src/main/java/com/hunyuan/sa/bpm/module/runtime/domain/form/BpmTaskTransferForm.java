package com.hunyuan.sa.bpm.module.runtime.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 任务转办表单。
 */
@Data
public class BpmTaskTransferForm {

    @Schema(description = "任务ID")
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @Schema(description = "转办到的员工ID")
    @NotNull(message = "转办员工不能为空")
    private Long toEmployeeId;

    @Schema(description = "转办说明")
    private String commentText;
}
