package com.hunyuan.sa.base.module.support.audit.api;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台登录日志分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformLoginLogPageQuery extends PageParam {

    @Schema(description = "用户标识")
    private Long userId;

    @Schema(description = "用户类型")
    private Integer userType;

    @Schema(description = "开始日期")
    private String startDate;

    @Schema(description = "结束日期")
    private String endDate;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "登录 IP")
    private String ip;
}
