package com.hunyuan.sa.base.module.support.job.api;

import com.hunyuan.sa.base.common.validator.enumeration.CheckEnum;
import com.hunyuan.sa.base.module.support.job.constant.SmartJobTriggerTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 平台定时任务创建命令。
 */
@Data
public class PlatformJobCreateCommand {

    @NotBlank(message = "任务名称不能为空")
    @Length(max = 100, message = "任务名称最多100字符")
    private String jobName;

    @NotBlank(message = "任务执行类不能为空")
    @Length(max = 200, message = "任务执行类最多200字符")
    private String jobClass;

    @CheckEnum(value = SmartJobTriggerTypeEnum.class, required = true, message = "触发类型错误")
    private String triggerType;

    @NotBlank(message = "触发配置不能为空")
    @Length(max = 100, message = "触发配置最多100字符")
    private String triggerValue;

    @Schema(description = "任务参数")
    @Length(max = 1000, message = "任务参数最多1000字符")
    private String param;

    @NotNull(message = "是否开启不能为空")
    private Boolean enabledFlag;

    @Length(max = 250, message = "任务备注最多250字符")
    private String remark;

    @NotNull(message = "排序不能为空")
    private Integer sort;
}
