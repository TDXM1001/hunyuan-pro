package com.hunyuan.sa.admin.module.platform.audit.application;

import com.hunyuan.sa.admin.module.platform.audit.loginlog.LoginLogService;
import com.hunyuan.sa.admin.module.platform.audit.loginlog.domain.LoginLogEntity;
import com.hunyuan.sa.admin.module.platform.audit.loginlog.domain.LoginLogVO;
import com.hunyuan.sa.base.module.support.audit.api.PlatformLoginLogCommand;
import com.hunyuan.sa.base.module.support.loginlog.LoginLogResultEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定登录模块与审计 owner 之间的稳定命令和最近登录快照映射。
 */
@ExtendWith(MockitoExtension.class)
class PlatformLoginAuditApplicationServiceTest {

    @Mock
    private LoginLogService loginLogService;

    private PlatformLoginAuditApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new PlatformLoginAuditApplicationService();
        ReflectionTestUtils.setField(applicationService, "loginLogService", loginLogService);
    }

    @Test
    void recordsLoginFactThroughSingleAuditWritePath() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 7, 25, 10, 30);
        PlatformLoginLogCommand command = new PlatformLoginLogCommand(
                11L, 1, "管理员", "127.0.0.1", "本机", "JUnit",
                "密码错误", "PC", LoginLogResultEnum.LOGIN_FAIL, occurredAt);

        applicationService.record(command);

        ArgumentCaptor<LoginLogEntity> captor = ArgumentCaptor.forClass(LoginLogEntity.class);
        verify(loginLogService).log(captor.capture());
        LoginLogEntity entity = captor.getValue();
        assertThat(entity.getUserId()).isEqualTo(11L);
        assertThat(entity.getLoginResult()).isEqualTo(LoginLogResultEnum.LOGIN_FAIL.getValue());
        assertThat(entity.getCreateTime()).isEqualTo(occurredAt);
    }

    @Test
    void exposesOnlyMinimalRecentSuccessfulLoginSnapshot() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 7, 24, 9, 0);
        LoginLogVO loginLog = new LoginLogVO();
        loginLog.setLoginIp("10.0.0.8");
        loginLog.setLoginIpRegion("内网");
        loginLog.setUserAgent("Browser");
        loginLog.setCreateTime(occurredAt);
        when(loginLogService.queryLastByUserId(
                7L, 1, LoginLogResultEnum.LOGIN_SUCCESS)).thenReturn(loginLog);

        var snapshot = applicationService.findLastSuccessfulLogin(7L, 1);

        assertThat(snapshot).isPresent();
        assertThat(snapshot.orElseThrow().loginIp()).isEqualTo("10.0.0.8");
        assertThat(snapshot.orElseThrow().occurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void returnsEmptyWhenNoSuccessfulLoginExists() {
        when(loginLogService.queryLastByUserId(
                7L, 1, LoginLogResultEnum.LOGIN_SUCCESS)).thenReturn(null);

        assertThat(applicationService.findLastSuccessfulLogin(7L, 1)).isEmpty();
    }
}
