package com.hunyuan.sa.base.module.support.job.api;

import com.hunyuan.sa.base.common.domain.PageParam;
import com.hunyuan.sa.base.common.validator.enumeration.CheckEnum;
import com.hunyuan.sa.base.module.support.job.constant.SmartJobTriggerTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

/**
 * 平台定时任务分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformJobPageQuery extends PageParam {

    @Schema(description = "搜索词")
    @Length(max = 50, message = "搜索词最多50字符")
    private String searchWord;

    @Schema(description = "触发类型")
    @CheckEnum(value = SmartJobTriggerTypeEnum.class, message = "触发类型错误")
    private String triggerType;

    @Schema(description = "是否启用")
    private Boolean enabledFlag;

    @Schema(description = "是否删除")
    private Boolean deletedFlag;
}
