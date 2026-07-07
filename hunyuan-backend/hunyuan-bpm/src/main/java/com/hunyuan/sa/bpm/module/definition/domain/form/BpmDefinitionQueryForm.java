package com.hunyuan.sa.bpm.module.definition.domain.form;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程定义分页查询表单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmDefinitionQueryForm extends PageParam {

    @Schema(description = "定义编码")
    private String definitionKey;

    @Schema(description = "定义名称")
    private String definitionName;

    @Schema(description = "生命周期状态")
    private Integer lifecycleState;

    @Schema(description = "发起状态")
    private Integer startState;
}
