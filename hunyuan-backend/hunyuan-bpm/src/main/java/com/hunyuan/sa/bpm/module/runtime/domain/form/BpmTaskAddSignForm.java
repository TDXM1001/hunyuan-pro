package com.hunyuan.sa.bpm.module.runtime.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 任务加签表单。
 */
@Data
public class BpmTaskAddSignForm {

    @Schema(description = "任务ID")
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @Schema(description = "加签员工ID")
    @NotNull(message = "加签员工不能为空")
    private Long targetEmployeeId;

    @Schema(description = "原因")
    @Size(max = 500, message = "原因最多 500 个字符")
    private String reason;
}
