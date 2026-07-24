package com.hunyuan.sa.base.module.support.audit.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import com.hunyuan.sa.base.common.swagger.SchemaEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台操作日志摘要与详情。
 */
@Data
public class PlatformOperateLogSummary {

    @Schema(description = "操作日志标识")
    private Long operateLogId;

    @Schema(description = "操作人标识")
    private Long operateUserId;

    @SchemaEnum(value = UserTypeEnum.class, desc = "操作人类型")
    private Integer operateUserType;

    @Schema(description = "操作人名称")
    private String operateUserName;

    private String module;
    private String content;
    private String url;
    private String method;
    private String param;
    private String response;
    private String ip;
    private String ipRegion;
    private String userAgent;
    private Boolean successFlag;
    private String failReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
