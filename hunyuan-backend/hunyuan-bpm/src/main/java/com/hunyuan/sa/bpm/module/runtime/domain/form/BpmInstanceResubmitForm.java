package com.hunyuan.sa.bpm.module.runtime.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 重新提交流程实例表单。
 */
@Data
public class BpmInstanceResubmitForm {

    @Schema(description = "实例ID")
    @NotNull(message = "实例ID不能为空")
    private Long instanceId;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "流程摘要")
    private String summary;

    @Schema(description = "表单数据 JSON")
    @NotBlank(message = "表单数据不能为空")
    private String formDataJson;

    @Schema(description = "客户端加载的表单数据版本")
    @NotNull(message = "表单数据版本不能为空")
    private Long formDataVersion;
}
