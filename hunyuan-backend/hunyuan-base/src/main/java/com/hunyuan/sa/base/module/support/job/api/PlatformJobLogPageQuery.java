package com.hunyuan.sa.base.module.support.job.api;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

/**
 * 平台定时任务执行记录分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformJobLogPageQuery extends PageParam {

    @Schema(description = "搜索词")
    @Length(max = 50, message = "搜索词最多50字符")
    private String searchWord;

    @Schema(description = "任务 ID")
    private Integer jobId;

    @Schema(description = "执行是否成功")
    private Boolean successFlag;

    @Schema(description = "开始日期")
    private LocalDate startTime;

    @Schema(description = "截止日期")
    private LocalDate endTime;
}
