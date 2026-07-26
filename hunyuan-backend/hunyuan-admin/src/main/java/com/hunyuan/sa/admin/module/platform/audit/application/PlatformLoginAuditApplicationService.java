package com.hunyuan.sa.admin.module.platform.audit.application;

import com.hunyuan.sa.admin.module.platform.audit.loginlog.LoginLogService;
import com.hunyuan.sa.admin.module.platform.audit.loginlog.domain.LoginLogEntity;
import com.hunyuan.sa.admin.module.platform.audit.loginlog.domain.LoginLogVO;
import com.hunyuan.sa.base.module.support.audit.api.PlatformLoginAuditFacade;
import com.hunyuan.sa.base.module.support.audit.api.PlatformLoginLogCommand;
import com.hunyuan.sa.base.module.support.audit.api.PlatformRecentLogin;
import com.hunyuan.sa.base.module.support.loginlog.LoginLogResultEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 登录审计协议实现，负责把稳定命令适配到 audit owner 的单一写入路径。
 */
@Service
public class PlatformLoginAuditApplicationService implements PlatformLoginAuditFacade {

    @Resource
    private LoginLogService loginLogService;

    @Override
    public void record(PlatformLoginLogCommand command) {
        LoginLogEntity entity = LoginLogEntity.builder()
                .userId(command.userId())
                .userType(command.userType())
                .userName(command.userName())
                .loginIp(command.loginIp())
                .loginIpRegion(command.loginIpRegion())
                .userAgent(command.userAgent())
                .remark(command.remark())
                .loginDevice(command.loginDevice())
                .loginResult(command.loginResult().getValue())
                .createTime(command.occurredAt())
                .build();
        loginLogService.log(entity);
    }

    @Override
    public Optional<PlatformRecentLogin> findLastSuccessfulLogin(Long userId, Integer userType) {
        LoginLogVO loginLog = loginLogService.queryLastByUserId(
                userId, userType, LoginLogResultEnum.LOGIN_SUCCESS);
        if (loginLog == null) {
            return Optional.empty();
        }
        return Optional.of(new PlatformRecentLogin(
                loginLog.getLoginIp(),
                loginLog.getLoginIpRegion(),
                loginLog.getUserAgent(),
                loginLog.getCreateTime()));
    }
}
