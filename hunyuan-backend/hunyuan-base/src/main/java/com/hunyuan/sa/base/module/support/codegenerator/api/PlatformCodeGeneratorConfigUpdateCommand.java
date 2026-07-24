package com.hunyuan.sa.base.module.support.codegenerator.api;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码生成配置更新命令。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformCodeGeneratorConfigUpdateCommand extends PlatformCodeGeneratorConfig {

    @NotBlank(message = "表名不能为空")
    private String tableName;
}
