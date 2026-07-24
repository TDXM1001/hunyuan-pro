package com.hunyuan.sa.base.module.support.heartbeat.api;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 心跳记录分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformHeartbeatPageQuery extends PageParam {

    @Schema(description = "关键字")
    private String keywords;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;
}
