package com.hunyuan.sa.bpm.module.runtime.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员转交流程任务表单。
 */
@Data
public class BpmAdminTaskTransferForm {

    @Schema(description = "任务ID")
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @Schema(description = "转交到的员工ID")
    @NotNull(message = "转办员工不能为空")
    private Long targetEmployeeId;

    @Schema(description = "转交原因")
    @NotBlank(message = "转办原因不能为空")
    @Size(max = 500, message = "转办原因最多 500 个字符")
    private String reason;
}
