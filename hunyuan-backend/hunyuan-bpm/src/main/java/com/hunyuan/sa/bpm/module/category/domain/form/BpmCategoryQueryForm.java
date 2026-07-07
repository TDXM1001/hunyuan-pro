package com.hunyuan.sa.bpm.module.category.domain.form;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程分类分页查询表单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmCategoryQueryForm extends PageParam {

    @Schema(description = "分类编码")
    private String categoryCode;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "是否禁用")
    private Boolean disabledFlag;

    @Schema(hidden = true)
    private Boolean deletedFlag;
}
