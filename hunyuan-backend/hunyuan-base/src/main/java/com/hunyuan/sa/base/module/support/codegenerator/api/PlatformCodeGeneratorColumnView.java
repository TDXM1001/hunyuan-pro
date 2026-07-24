package com.hunyuan.sa.base.module.support.codegenerator.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 代码生成器数据库列公开视图。
 */
@Data
public class PlatformCodeGeneratorColumnView {

    @Schema(description = "列名")
    private String columnName;

    @Schema(description = "列备注")
    private String columnComment;

    @Schema(description = "数据库数据类型")
    private String dataType;

    @Schema(description = "是否允许为空")
    private Boolean nullableFlag;

    @Schema(description = "是否为主键")
    private Boolean primaryKeyFlag;

    @Schema(description = "是否自增")
    private Boolean autoIncreaseFlag;
}
