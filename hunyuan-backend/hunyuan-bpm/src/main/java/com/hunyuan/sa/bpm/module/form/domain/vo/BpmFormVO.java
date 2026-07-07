package com.hunyuan.sa.bpm.module.form.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程表单返回结果。
 */
@Data
public class BpmFormVO {

    @Schema(description = "表单ID")
    private Long formId;

    @Schema(description = "表单编码")
    private String formKey;

    @Schema(description = "表单名称")
    private String formName;

    @Schema(description = "表单 schema")
    private String schemaJson;

    @Schema(description = "表单布局")
    private String layoutJson;

    @Schema(description = "是否禁用")
    private Boolean disabledFlag;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
