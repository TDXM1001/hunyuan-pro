package com.hunyuan.sa.base.module.support.audit.api;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台操作日志分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformOperateLogPageQuery extends PageParam {

    @Schema(description = "操作人标识")
    private Long operateUserId;

    @Schema(description = "操作人类型")
    private Integer operateUserType;

    @Schema(description = "模块或操作内容关键字")
    private String keywords;

    @Schema(description = "请求地址、方法或参数关键字")
    private String requestKeywords;

    @Schema(description = "开始日期")
    private String startDate;

    @Schema(description = "结束日期")
    private String endDate;

    @Schema(description = "操作人名称")
    private String userName;

    @Schema(description = "请求是否成功")
    private Boolean successFlag;
}
