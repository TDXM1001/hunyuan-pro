package com.hunyuan.sa.bpm.module.integration.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * BPM 业务回调人工补偿表单。
 */
@Data
public class BpmCallbackCompensateForm {

    @Schema(description = "人工补偿说明")
    @NotBlank(message = "人工补偿说明不能为空")
    @Size(max = 500, message = "人工补偿说明最多500个字符")
    private String reason;
}
