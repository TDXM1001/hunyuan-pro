package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 员工可发起定义列表返回结果。
 */
@Data
public class BpmStartableDefinitionVO {

    @Schema(description = "定义ID")
    private Long definitionId;

    @Schema(description = "定义编码")
    private String definitionKey;

    @Schema(description = "定义名称")
    private String definitionName;

    @Schema(description = "定义版本")
    private Integer definitionVersion;

    @Schema(description = "分类名称")
    private String categoryNameSnapshot;

    @Schema(description = "表单名称")
    private String formNameSnapshot;
}
