package com.hunyuan.sa.bpm.module.form.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增流程表单表单。
 */
@Data
public class BpmFormAddForm {

    @Schema(description = "表单编码")
    @NotBlank(message = "表单编码不能为空")
    @Size(max = 64, message = "表单编码最多 64 个字符")
    private String formKey;

    @Schema(description = "表单名称")
    @NotBlank(message = "表单名称不能为空")
    @Size(max = 128, message = "表单名称最多 128 个字符")
    private String formName;

    @Schema(description = "表单 schema")
    @NotBlank(message = "表单 schema 不能为空")
    private String schemaJson;

    @Schema(description = "表单布局")
    private String layoutJson;

    @Schema(description = "是否禁用")
    private Boolean disabledFlag;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注最多 500 个字符")
    private String remark;
}
