package com.hunyuan.sa.bpm.module.form.domain.form;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程表单分页查询表单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmFormQueryForm extends PageParam {

    @Schema(description = "表单编码")
    private String formKey;

    @Schema(description = "表单名称")
    private String formName;

    @Schema(description = "是否禁用")
    private Boolean disabledFlag;

    @Schema(hidden = true)
    private Boolean deletedFlag;
}
