package com.hunyuan.sa.bpm.module.model.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端从草稿冻结 Graph 模板请求。
 */
@Data
public class BpmGraphTemplateCreateForm {

    @Schema(description = "模板编码")
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 64, message = "模板编码最多 64 个字符")
    private String templateKey;

    @Schema(description = "模板名称")
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称最多 128 个字符")
    private String templateName;

    @Schema(description = "来源 Graph 草稿 ID")
    @NotNull(message = "来源 Graph 草稿 ID 不能为空")
    private Long sourceDraftId;
}
