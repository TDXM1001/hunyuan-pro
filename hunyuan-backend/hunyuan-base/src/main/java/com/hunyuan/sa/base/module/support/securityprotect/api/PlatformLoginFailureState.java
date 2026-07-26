package com.hunyuan.sa.base.module.support.securityprotect.api;

import java.time.LocalDateTime;

/**
 * 登录 owner 在一次认证尝试中需要传回安全模块的失败状态快照。
 */
public record PlatformLoginFailureState(
        Long loginFailId,
        Long userId,
        Integer userType,
        String loginName,
        Boolean locked,
        Integer failureCount,
        LocalDateTime lockBeginTime) {
}
