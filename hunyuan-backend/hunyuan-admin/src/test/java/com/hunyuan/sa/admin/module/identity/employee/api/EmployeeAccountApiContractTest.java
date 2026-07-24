package com.hunyuan.sa.admin.module.identity.employee.api;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmployeeAccountApiContractTest {

    @Test
    void exposesStableSelfServiceRoutesWithoutEmployeeIdParameters() throws Exception {
        RequestMapping mapping = EmployeeAccountController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/api/admin/v1/identity/account");

        assertThat(EmployeeAccountController.class
                .getMethod("getCurrentProfile")
                .getAnnotation(GetMapping.class).value()).containsExactly("/me");
        assertThat(EmployeeAccountController.class
                .getMethod("updateProfile", EmployeeSelfProfileRequest.class)
                .getAnnotation(PutMapping.class).value()).containsExactly("/me/profile");
        assertThat(EmployeeAccountController.class
                .getMethod("updateAvatar", EmployeeAvatarRequest.class)
                .getAnnotation(PutMapping.class).value()).containsExactly("/me/avatar");
        assertThat(EmployeeAccountController.class
                .getMethod("changePassword", EmployeePasswordChangeRequest.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/me/password");
        assertThat(EmployeeAccountController.class
                .getMethod("getPasswordPolicy")
                .getAnnotation(GetMapping.class).value()).containsExactly("/me/password-policy");

        for (Method method : EmployeeAccountController.class.getDeclaredMethods()) {
            assertThat(method.getParameterTypes())
                    .as("自助接口不能接收前端传入的员工ID: %s", method.getName())
                    .doesNotContain(Long.class);
        }
    }

    @Test
    void usesCurrentRequestUserForSelfServiceCommands() {
        EmployeeAccountFacade accountFacade = mock(EmployeeAccountFacade.class);
        EmployeeDirectoryFacade directoryFacade = mock(EmployeeDirectoryFacade.class);
        EmployeeAccountController controller = new EmployeeAccountController();
        ReflectionTestUtils.setField(controller, "accountFacade", accountFacade);
        ReflectionTestUtils.setField(controller, "directoryFacade", directoryFacade);

        try (var mocked = org.mockito.Mockito.mockStatic(SmartRequestUtil.class)) {
            mocked.when(SmartRequestUtil::getRequestUserId).thenReturn(7L);
            when(accountFacade.updateSelfProfile(any())).thenReturn(ResponseDTO.ok("ok"));
            when(accountFacade.updateAvatar(7L, "avatar")).thenReturn(ResponseDTO.ok("ok"));

            controller.updateProfile(new EmployeeSelfProfileRequest(
                    "张三", 1, "13800000000", "zhangsan@example.com", null, null, null));
            controller.updateAvatar(new EmployeeAvatarRequest("avatar"));

            var command = org.mockito.ArgumentCaptor
                    .forClass(EmployeeSelfProfileUpdateCommand.class);
            org.mockito.Mockito.verify(accountFacade).updateSelfProfile(command.capture());
            assertThat(command.getValue().employeeId()).isEqualTo(7L);
            org.mockito.Mockito.verify(accountFacade).updateAvatar(7L, "avatar");
        }
    }

    @Test
    void readsCurrentProfileAndPasswordPolicyFromCurrentRequestUser() {
        EmployeeAccountFacade accountFacade = mock(EmployeeAccountFacade.class);
        EmployeeDirectoryFacade directoryFacade = mock(EmployeeDirectoryFacade.class);
        EmployeeAccountController controller = new EmployeeAccountController();
        ReflectionTestUtils.setField(controller, "accountFacade", accountFacade);
        ReflectionTestUtils.setField(controller, "directoryFacade", directoryFacade);

        EmployeeSummary summary = new EmployeeSummary(
                7L,
                "employee-7",
                "张三",
                "avatar-file-id",
                1,
                "13800000000",
                "zhangsan@example.com",
                10L,
                "研发部",
                20L,
                false,
                LocalDateTime.now());

        try (var mocked = org.mockito.Mockito.mockStatic(SmartRequestUtil.class)) {
            mocked.when(SmartRequestUtil::getRequestUserId).thenReturn(7L);
            when(directoryFacade.findSummaryById(7L)).thenReturn(Optional.of(summary));
            when(accountFacade.passwordComplexityEnabled()).thenReturn(true);

            assertThat(controller.getCurrentProfile().getData()).isEqualTo(summary);
            assertThat(controller.getPasswordPolicy().getData()).isTrue();
            org.mockito.Mockito.verify(directoryFacade).findSummaryById(7L);
        }
    }
}
