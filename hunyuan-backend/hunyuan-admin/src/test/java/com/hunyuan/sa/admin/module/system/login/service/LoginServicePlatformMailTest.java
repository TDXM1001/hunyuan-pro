package com.hunyuan.sa.admin.module.system.login.service;

import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeAuthenticationAccount;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeDirectoryFacade;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.mail.api.PlatformMailFacade;
import com.hunyuan.sa.base.module.support.mail.api.PlatformMailTemplateCode;
import com.hunyuan.sa.base.module.support.mail.api.PlatformTemplateMailCommand;
import com.hunyuan.sa.base.module.support.redis.RedisService;
import com.hunyuan.sa.base.module.support.securityprotect.service.Level3ProtectConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定登录验证码通过平台邮件边界发送。
 */
@ExtendWith(MockitoExtension.class)
class LoginServicePlatformMailTest {

    @Mock
    private EmployeeDirectoryFacade employeeDirectoryFacade;

    @Mock
    private Level3ProtectConfigService level3ProtectConfigService;

    @Mock
    private RedisService redisService;

    @Mock
    private PlatformMailFacade platformMailFacade;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginService();
        ReflectionTestUtils.setField(
                loginService, "employeeDirectoryFacade", employeeDirectoryFacade);
        ReflectionTestUtils.setField(
                loginService, "level3ProtectConfigService", level3ProtectConfigService);
        ReflectionTestUtils.setField(loginService, "redisService", redisService);
        ReflectionTestUtils.setField(loginService, "platformMailFacade", platformMailFacade);
    }

    @Test
    void sendsLoginVerificationCodeThroughPlatformMailFacade() {
        when(level3ProtectConfigService.isTwoFactorLoginEnabled()).thenReturn(true);
        when(employeeDirectoryFacade.findAuthenticationAccountByLoginName("hunyuan"))
                .thenReturn(Optional.of(account()));
        when(redisService.generateRedisKey(anyString(), anyString()))
                .thenReturn("login-code-key");
        when(redisService.get("login-code-key")).thenReturn(null);
        when(platformMailFacade.sendTemplateMail(any())).thenReturn(ResponseDTO.ok());

        ResponseDTO<String> response = loginService.sendEmailCode("hunyuan");

        ArgumentCaptor<PlatformTemplateMailCommand> captor =
                ArgumentCaptor.forClass(PlatformTemplateMailCommand.class);
        verify(platformMailFacade).sendTemplateMail(captor.capture());
        assertThat(response.getOk()).isTrue();
        assertThat(captor.getValue().templateCode())
                .isEqualTo(PlatformMailTemplateCode.LOGIN_VERIFICATION_CODE);
        assertThat(captor.getValue().recipients())
                .containsExactly("hunyuan@example.com");
        assertThat(captor.getValue().templateParameters().get("code").toString())
                .matches("\\d{4}");
    }

    /**
     * 构造启用且绑定邮箱的登录账号。
     */
    private EmployeeAuthenticationAccount account() {
        return new EmployeeAuthenticationAccount(
                7L,
                "employee-7",
                "hunyuan",
                "password-hash",
                "混元管理员",
                null,
                1,
                "13800138000",
                "hunyuan@example.com",
                1L,
                1L,
                true,
                false,
                false,
                null);
    }
}
