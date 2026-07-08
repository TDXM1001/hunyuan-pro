package com.hunyuan.sa.bpm.module.runtime.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员取消流程实例表单。
 */
@Data
public class BpmAdminInstanceCancelForm {

    @Schema(description = "实例ID")
    @NotNull(message = "实例ID不能为空")
    private Long instanceId;

    @Schema(description = "取消原因")
    @NotBlank(message = "取消原因不能为空")
    @Size(max = 500, message = "取消原因最多 500 个字符")
    private String cancelReason;
}
