package com.hunyuan.sa.base.module.support.codegenerator.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代码生成器数据库表公开视图。
 */
@Data
public class PlatformCodeGeneratorTableView {

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "表备注")
    private String tableComment;

    @Schema(description = "配置时间")
    private LocalDateTime configTime;
}
