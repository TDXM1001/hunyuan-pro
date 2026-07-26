package com.hunyuan.sa.base.module.support.securityprotect.api;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;

/**
 * 登录失败计数、锁定和解除锁定的稳定访问边界。
 */
public interface PlatformLoginSecurityFacade {

    ResponseDTO<PlatformLoginFailureState> checkLogin(Long userId, UserTypeEnum userType);

    String recordLoginFail(
            Long userId,
            UserTypeEnum userType,
            String loginName,
            PlatformLoginFailureState failureState);

    void removeLoginFail(Long userId, UserTypeEnum userType);
}
