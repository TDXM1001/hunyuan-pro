package com.hunyuan.sa.admin.module.identity.employee.application;

import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeAuthenticationAccount;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeePasswordChangeCommand;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeePasswordSalt;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeSelfProfileUpdateCommand;
import com.hunyuan.sa.admin.module.identity.employee.application.port.EmployeeSessionPort;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeRepository;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeSelfProfileUpdate;
import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.securityprotect.service.Level3ProtectConfigService;
import com.hunyuan.sa.base.module.support.securityprotect.service.SecurityPasswordService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeAccountApplicationServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private SecurityPasswordService securityPasswordService;
    @Mock
    private Level3ProtectConfigService level3ProtectConfigService;
    @Mock
    private EmployeeSessionPort employeeSessionPort;

    private EmployeeAccountApplicationService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeAccountApplicationService();
        ReflectionTestUtils.setField(service, "employeeRepository", employeeRepository);
        ReflectionTestUtils.setField(service, "securityPasswordService", securityPasswordService);
        ReflectionTestUtils.setField(service, "level3ProtectConfigService", level3ProtectConfigService);
        ReflectionTestUtils.setField(service, "employeeSessionPort", employeeSessionPort);
    }

    @Test
    void updatesOnlySelfProfileFieldsAndClearsCache() {
        when(employeeRepository.findAuthenticationAccountById(7L))
                .thenReturn(Optional.of(account(7L, false)));
        when(employeeRepository.findIdByPhone("13800000007")).thenReturn(Optional.of(7L));
        when(employeeRepository.findIdByEmail("employee7@example.com")).thenReturn(Optional.of(7L));

        var response = service.updateSelfProfile(new EmployeeSelfProfileUpdateCommand(
                7L,
                "更新后的员工",
                1,
                "13800000007",
                "employee7@example.com",
                20L,
                "avatar-file-id",
                "个人备注"
        ));

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<EmployeeSelfProfileUpdate> captor =
                ArgumentCaptor.forClass(EmployeeSelfProfileUpdate.class);
        verify(employeeRepository).updateSelfProfile(captor.capture());
        assertThat(captor.getValue().employeeId()).isEqualTo(7L);
        assertThat(captor.getValue().avatar()).isEqualTo("avatar-file-id");
        verify(employeeSessionPort).clearCache(7L);
    }

    @Test
    void rejectsDuplicatePhoneBeforeWriting() {
        when(employeeRepository.findAuthenticationAccountById(7L))
                .thenReturn(Optional.of(account(7L, false)));
        when(employeeRepository.findIdByPhone("13800000008")).thenReturn(Optional.of(8L));

        var response = service.updateSelfProfile(new EmployeeSelfProfileUpdateCommand(
                7L,
                "员工",
                1,
                "13800000008",
                "employee7@example.com",
                null,
                null,
                null
        ));

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("手机号已存在");
        verify(employeeRepository, never()).updateSelfProfile(any());
        verify(employeeSessionPort, never()).clearCache(7L);
    }

    @Test
    void updatesAvatarAndClearsCache() {
        when(employeeRepository.findAuthenticationAccountById(7L))
                .thenReturn(Optional.of(account(7L, false)));

        var response = service.updateAvatar(7L, "avatar-file-id");

        assertThat(response.getOk()).isTrue();
        verify(employeeRepository).updateAvatar(7L, "avatar-file-id");
        verify(employeeSessionPort).clearCache(7L);
    }

    @Test
    void changesPasswordAndClearsCacheAfterSecurityChecks() {
        String oldPassword = "OldPassword_123";
        String newPassword = "NewPassword_456";
        EmployeeAuthenticationAccount account = accountWithPassword(7L, oldPassword);
        when(employeeRepository.findAuthenticationAccountById(7L))
                .thenReturn(Optional.of(account));
        when(securityPasswordService.validatePasswordComplexity(newPassword))
                .thenReturn(ResponseDTO.ok());
        when(securityPasswordService.validatePasswordRepeatTimes(any(), any()))
                .thenReturn(ResponseDTO.ok());

        var response = service.changePassword(
                requestUser(), new EmployeePasswordChangeCommand(7L, oldPassword, newPassword));

        assertThat(response.getOk()).isTrue();
        verify(employeeRepository).updatePassword(eq(7L), any());
        verify(securityPasswordService).saveUserChangePasswordLog(any(), any(), eq(account.passwordHash()));
        verify(employeeSessionPort).clearCache(7L);
    }

    @Test
    void rejectsWrongOldPasswordBeforeWriting() {
        EmployeeAuthenticationAccount account = accountWithPassword(7L, "OldPassword_123");
        when(employeeRepository.findAuthenticationAccountById(7L))
                .thenReturn(Optional.of(account));

        var response = service.changePassword(
                requestUser(), new EmployeePasswordChangeCommand(7L, "WrongPassword_123", "NewPassword_456"));

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("原密码有误");
        verify(employeeRepository, never()).updatePassword(any(), any());
        verify(employeeSessionPort, never()).clearCache(7L);
    }

    @Test
    void delegatesPasswordComplexityConfiguration() {
        when(level3ProtectConfigService.isPasswordComplexityEnabled()).thenReturn(true);

        assertThat(service.passwordComplexityEnabled()).isTrue();
        verify(level3ProtectConfigService).isPasswordComplexityEnabled();
    }

    private EmployeeAuthenticationAccount account(Long employeeId, boolean deleted) {
        return new EmployeeAuthenticationAccount(
                employeeId,
                "uid-" + employeeId,
                "employee-" + employeeId,
                "password-hash",
                "员工" + employeeId,
                null,
                1,
                "1380000000" + employeeId,
                "employee" + employeeId + "@example.com",
                10L,
                20L,
                false,
                false,
                deleted,
                null
        );
    }

    private EmployeeAuthenticationAccount accountWithPassword(Long employeeId, String password) {
        EmployeeAuthenticationAccount base = account(employeeId, false);
        return new EmployeeAuthenticationAccount(
                base.employeeId(),
                base.employeeUid(),
                base.loginName(),
                SecurityPasswordService.getEncryptPwd(EmployeePasswordSalt.apply(password, base.employeeUid())),
                base.actualName(),
                base.avatar(),
                base.gender(),
                base.phone(),
                base.email(),
                base.departmentId(),
                base.positionId(),
                base.administrator(),
                base.disabled(),
                base.deleted(),
                base.remark()
        );
    }

    private RequestUser requestUser() {
        return new RequestUser() {
            @Override
            public Long getUserId() {
                return 7L;
            }

            @Override
            public String getUserName() {
                return "employee-7";
            }

            @Override
            public com.hunyuan.sa.base.common.enumeration.UserTypeEnum getUserType() {
                return com.hunyuan.sa.base.common.enumeration.UserTypeEnum.ADMIN_EMPLOYEE;
            }

            @Override
            public String getIp() {
                return "127.0.0.1";
            }

            @Override
            public String getUserAgent() {
                return "测试客户端";
            }
        };
    }
}
