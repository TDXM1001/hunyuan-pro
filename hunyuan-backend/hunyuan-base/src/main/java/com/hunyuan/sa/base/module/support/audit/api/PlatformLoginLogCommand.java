package com.hunyuan.sa.base.module.support.audit.api;

import com.hunyuan.sa.base.module.support.loginlog.LoginLogResultEnum;

import java.time.LocalDateTime;

/**
 * 登录模块写入审计日志的稳定命令。
 *
 * <p>命令只描述审计事实，不暴露登录日志 Entity、DAO 或具体持久化方式。</p>
 */
public record PlatformLoginLogCommand(
        Long userId,
        Integer userType,
        String userName,
        String loginIp,
        String loginIpRegion,
        String userAgent,
        String remark,
        String loginDevice,
        LoginLogResultEnum loginResult,
        LocalDateTime occurredAt) {
}
