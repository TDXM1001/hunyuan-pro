package com.hunyuan.sa.base.module.support.securityprotect.api;

/**
 * 登录与账号模块需要读取的安全策略最小公开面。
 */
public interface PlatformSecurityPolicyFacade {

    boolean isTwoFactorLoginEnabled();

    boolean isPasswordComplexityEnabled();
}
