package com.hunyuan.sa.base.module.support.audit.api;

import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import com.hunyuan.sa.base.common.swagger.SchemaEnum;
import com.hunyuan.sa.base.module.support.loginlog.LoginLogResultEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台登录日志摘要。
 */
@Data
public class PlatformLoginLogSummary {

    private Long loginLogId;
    private Long userId;

    @SchemaEnum(value = UserTypeEnum.class, desc = "用户类型")
    private Integer userType;

    private String userName;
    private String loginIp;
    private String loginIpRegion;
    private String userAgent;
    private String remark;

    @SchemaEnum(LoginLogResultEnum.class)
    private Integer loginResult;

    private String loginDevice;
    private LocalDateTime createTime;
}
