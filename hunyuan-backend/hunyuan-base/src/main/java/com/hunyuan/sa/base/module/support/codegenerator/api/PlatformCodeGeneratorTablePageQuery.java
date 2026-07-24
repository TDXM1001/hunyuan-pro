package com.hunyuan.sa.base.module.support.codegenerator.api;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

/**
 * 代码生成器数据库表分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformCodeGeneratorTablePageQuery extends PageParam {

    @Schema(description = "表名关键字")
    @Length(max = 100, message = "表名关键字最多100字符")
    private String tableNameKeywords;
}
