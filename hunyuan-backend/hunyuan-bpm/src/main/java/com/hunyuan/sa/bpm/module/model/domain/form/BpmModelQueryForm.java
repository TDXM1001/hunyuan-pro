package com.hunyuan.sa.bpm.module.model.domain.form;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程模型分页查询表单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmModelQueryForm extends PageParam {

    @Schema(description = "模型编码")
    private String modelKey;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "表单ID")
    private Long formId;

    @Schema(description = "是否可见")
    private Boolean visibleFlag;

    @Schema(hidden = true)
    private Boolean deletedFlag;
}
