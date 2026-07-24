package com.hunyuan.sa.base.module.support.config.api;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

/**
 * 平台配置分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformConfigurationPageQuery extends PageParam {

    @Schema(description = "参数 KEY")
    @Length(max = 50, message = "参数 Key 最多 50 字符")
    private String configKey;
}
