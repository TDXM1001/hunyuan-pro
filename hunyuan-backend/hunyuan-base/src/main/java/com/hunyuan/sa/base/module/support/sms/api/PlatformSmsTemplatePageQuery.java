package com.hunyuan.sa.base.module.support.sms.api;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

/**
 * 平台短信模板分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformSmsTemplatePageQuery extends PageParam {

    @Schema(description = "模板编码")
    @Length(max = 100, message = "模板编码最多 100 个字符")
    private String templateCode;

    @Schema(description = "模板名称")
    @Length(max = 100, message = "模板名称最多 100 个字符")
    private String templateName;

    @Schema(description = "是否禁用")
    private Boolean disableFlag;
}
