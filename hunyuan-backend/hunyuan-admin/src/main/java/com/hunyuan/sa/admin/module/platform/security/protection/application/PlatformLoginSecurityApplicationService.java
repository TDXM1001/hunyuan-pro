package com.hunyuan.sa.admin.module.platform.security.protection.application;

import com.hunyuan.sa.admin.module.platform.security.protection.domain.LoginFailEntity;
import com.hunyuan.sa.admin.module.platform.security.protection.service.SecurityLoginService;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import com.hunyuan.sa.base.module.support.securityprotect.api.PlatformLoginFailureState;
import com.hunyuan.sa.base.module.support.securityprotect.api.PlatformLoginSecurityFacade;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 登录失败安全协议适配器，登录 owner 不感知锁定记录 Entity 和 DAO。
 */
@Service
public class PlatformLoginSecurityApplicationService implements PlatformLoginSecurityFacade {

    @Resource
    private SecurityLoginService securityLoginService;

    @Override
    public ResponseDTO<PlatformLoginFailureState> checkLogin(
            Long userId, UserTypeEnum userType) {
        ResponseDTO<LoginFailEntity> response = securityLoginService.checkLogin(userId, userType);
        if (!response.getOk()) {
            return ResponseDTO.error(response);
        }
        return ResponseDTO.ok(toState(response.getData()));
    }

    @Override
    public String recordLoginFail(
            Long userId,
            UserTypeEnum userType,
            String loginName,
            PlatformLoginFailureState failureState) {
        return securityLoginService.recordLoginFail(
                userId, userType, loginName, toEntity(failureState));
    }

    @Override
    public void removeLoginFail(Long userId, UserTypeEnum userType) {
        securityLoginService.removeLoginFail(userId, userType);
    }

    private PlatformLoginFailureState toState(LoginFailEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PlatformLoginFailureState(
                entity.getLoginFailId(),
                entity.getUserId(),
                entity.getUserType(),
                entity.getLoginName(),
                entity.getLockFlag(),
                entity.getLoginFailCount(),
                entity.getLoginLockBeginTime());
    }

    private LoginFailEntity toEntity(PlatformLoginFailureState state) {
        if (state == null) {
            return null;
        }
        return LoginFailEntity.builder()
                .loginFailId(state.loginFailId())
                .userId(state.userId())
                .userType(state.userType())
                .loginName(state.loginName())
                .lockFlag(state.locked())
                .loginFailCount(state.failureCount())
                .loginLockBeginTime(state.lockBeginTime())
                .build();
    }
}
