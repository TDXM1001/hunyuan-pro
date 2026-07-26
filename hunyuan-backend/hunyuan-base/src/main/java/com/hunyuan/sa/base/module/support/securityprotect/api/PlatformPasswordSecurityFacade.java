package com.hunyuan.sa.base.module.support.securityprotect.api;

import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.domain.ResponseDTO;

/**
 * 密码策略、历史与随机凭证的稳定访问边界。
 */
public interface PlatformPasswordSecurityFacade {

    ResponseDTO<String> validatePasswordComplexity(String password);

    ResponseDTO<String> validatePasswordRepeatTimes(RequestUser requestUser, String newPassword);

    String randomPassword();

    void saveUserChangePasswordLog(RequestUser requestUser, String newPassword, String oldPassword);

    boolean checkNeedChangePassword(Integer userType, Long userId);
}
