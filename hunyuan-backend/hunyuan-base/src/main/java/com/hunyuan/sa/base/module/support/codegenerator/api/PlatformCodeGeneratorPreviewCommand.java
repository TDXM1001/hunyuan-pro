package com.hunyuan.sa.base.module.support.codegenerator.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 代码生成预览命令。
 */
@Data
public class PlatformCodeGeneratorPreviewCommand {

    @NotBlank(message = "模板文件不能为空")
    @Schema(description = "模板文件")
    private String templateFile;

    @NotBlank(message = "表名不能为空")
    @Schema(description = "表名")
    private String tableName;
}
