package com.hunyuan.sa.base.module.support.audit.api;

import java.time.LocalDateTime;

/**
 * 登录模块展示上次成功登录所需的最小审计快照。
 */
public record PlatformRecentLogin(
        String loginIp,
        String loginIpRegion,
        String userAgent,
        LocalDateTime occurredAt) {
}
