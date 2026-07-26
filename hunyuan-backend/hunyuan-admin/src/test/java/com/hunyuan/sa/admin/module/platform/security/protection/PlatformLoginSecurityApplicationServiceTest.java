package com.hunyuan.sa.admin.module.platform.security.protection;

import com.hunyuan.sa.admin.module.platform.security.protection.application.PlatformLoginSecurityApplicationService;
import com.hunyuan.sa.admin.module.platform.security.protection.domain.LoginFailEntity;
import com.hunyuan.sa.admin.module.platform.security.protection.service.SecurityLoginService;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import com.hunyuan.sa.base.module.support.securityprotect.api.PlatformLoginFailureState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformLoginSecurityApplicationServiceTest {

    @Mock
    private SecurityLoginService securityLoginService;

    private PlatformLoginSecurityApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlatformLoginSecurityApplicationService();
        ReflectionTestUtils.setField(service, "securityLoginService", securityLoginService);
    }

    @Test
    void mapsEntityToStableStateAndBack() {
        LocalDateTime lockBeginTime = LocalDateTime.of(2026, 7, 25, 19, 0);
        LoginFailEntity entity = LoginFailEntity.builder()
                .loginFailId(7L)
                .userId(9L)
                .userType(UserTypeEnum.ADMIN_EMPLOYEE.getValue())
                .loginName("operator")
                .lockFlag(true)
                .loginFailCount(5)
                .loginLockBeginTime(lockBeginTime)
                .build();
        when(securityLoginService.checkLogin(9L, UserTypeEnum.ADMIN_EMPLOYEE))
                .thenReturn(ResponseDTO.ok(entity));
        when(securityLoginService.recordLoginFail(
                eq(9L), eq(UserTypeEnum.ADMIN_EMPLOYEE), eq("operator"), any()))
                .thenReturn("locked");

        PlatformLoginFailureState state = service
                .checkLogin(9L, UserTypeEnum.ADMIN_EMPLOYEE)
                .getData();
        String message = service.recordLoginFail(
                9L, UserTypeEnum.ADMIN_EMPLOYEE, "operator", state);

        assertThat(state.failureCount()).isEqualTo(5);
        assertThat(message).isEqualTo("locked");
        ArgumentCaptor<LoginFailEntity> captor = ArgumentCaptor.forClass(LoginFailEntity.class);
        verify(securityLoginService).recordLoginFail(
                eq(9L), eq(UserTypeEnum.ADMIN_EMPLOYEE), eq("operator"), captor.capture());
        assertThat(captor.getValue().getLoginFailId()).isEqualTo(7L);
        assertThat(captor.getValue().getLoginLockBeginTime()).isEqualTo(lockBeginTime);
    }
}
