package com.hunyuan.sa.base.module.support.audit.api;

import java.util.Optional;

/**
 * 登录认证与审计 owner 之间的稳定协作边界。
 *
 * <p>登录模块只能提交审计事实和读取最近成功登录快照，不能访问审计内部模型或存储。</p>
 */
public interface PlatformLoginAuditFacade {

    void record(PlatformLoginLogCommand command);

    Optional<PlatformRecentLogin> findLastSuccessfulLogin(Long userId, Integer userType);
}
