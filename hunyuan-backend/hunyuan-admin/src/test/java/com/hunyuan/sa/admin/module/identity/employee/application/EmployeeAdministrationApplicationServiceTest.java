package com.hunyuan.sa.admin.module.identity.employee.application;

import com.hunyuan.sa.admin.module.access.role.api.AccessRoleAssignmentFacade;
import com.hunyuan.sa.admin.module.access.role.api.ReplaceEmployeeRolesCommand;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeAuthenticationAccount;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeCreateCommand;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeDeleteCommand;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeDepartmentAssignmentCommand;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeOneTimeCredential;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeUpdateCommand;
import com.hunyuan.sa.admin.module.identity.employee.application.port.EmployeeSessionPort;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeCreateDraft;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeRepository;
import com.hunyuan.sa.admin.module.organization.department.application.OrganizationDepartmentFacade;
import com.hunyuan.sa.admin.module.organization.department.domain.Department;
import com.hunyuan.sa.admin.module.organization.position.application.OrganizationPositionFacade;
import com.hunyuan.sa.admin.module.organization.position.domain.Position;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.securityprotect.service.SecurityPasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeAdministrationApplicationServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private OrganizationDepartmentFacade organizationDepartmentFacade;
    @Mock
    private OrganizationPositionFacade organizationPositionFacade;
    @Mock
    private SecurityPasswordService securityPasswordService;
    @Mock
    private AccessRoleAssignmentFacade accessRoleAssignmentFacade;
    @Mock
    private EmployeeSessionPort employeeSessionPort;

    private EmployeeAdministrationApplicationService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeAdministrationApplicationService();
        ReflectionTestUtils.setField(service, "employeeRepository", employeeRepository);
        ReflectionTestUtils.setField(service, "organizationDepartmentFacade", organizationDepartmentFacade);
        ReflectionTestUtils.setField(service, "organizationPositionFacade", organizationPositionFacade);
        ReflectionTestUtils.setField(service, "securityPasswordService", securityPasswordService);
        ReflectionTestUtils.setField(service, "accessRoleAssignmentFacade", accessRoleAssignmentFacade);
        ReflectionTestUtils.setField(service, "employeeSessionPort", employeeSessionPort);
    }

    @Test
    @DisplayName("创建员工时校验岗位存在")
    void createsEmployeeAndReturnsPasswordOnlyAsOneTimeCredential() {
        EmployeeCreateCommand command = createCommand();
        when(organizationDepartmentFacade.findForCollaboration(10L))
                .thenReturn(Optional.of(department(10L)));
        when(organizationPositionFacade.findForCollaboration(20L))
                .thenReturn(Optional.of(position(20L)));
        when(securityPasswordService.randomPassword()).thenReturn("Temp-Password-1");
        when(employeeRepository.create(any(EmployeeCreateDraft.class))).thenReturn(7L);

        ResponseDTO<EmployeeOneTimeCredential> response =
                service.createWithLegacyRoles(command, List.of(3L, 4L));

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData())
                .isEqualTo(new EmployeeOneTimeCredential(7L, "Temp-Password-1"));
        ArgumentCaptor<EmployeeCreateDraft> draftCaptor =
                ArgumentCaptor.forClass(EmployeeCreateDraft.class);
        verify(employeeRepository).create(draftCaptor.capture());
        assertThat(draftCaptor.getValue().passwordHash())
                .isNotEqualTo("Temp-Password-1")
                .doesNotContain("Temp-Password-1");
        verify(accessRoleAssignmentFacade).replaceEmployeeRoles(
                new ReplaceEmployeeRolesCommand(7L, Set.of(3L, 4L)));
    }

    @Test
    @DisplayName("创建员工引用不存在岗位时拒绝写入")
    void creatingEmployeeRequiresExistingPosition() {
        when(organizationDepartmentFacade.findForCollaboration(10L))
                .thenReturn(Optional.of(department(10L)));
        when(organizationPositionFacade.findForCollaboration(20L)).thenReturn(Optional.empty());

        ResponseDTO<EmployeeOneTimeCredential> response =
                service.createWithLegacyRoles(createCommand(), List.of());

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("岗位不存在");
        verify(employeeRepository, never()).create(any());
    }

    @Test
    @DisplayName("更新员工引用不存在岗位时拒绝写入")
    void updatingEmployeeRequiresExistingPosition() {
        EmployeeUpdateCommand command = new EmployeeUpdateCommand(
                7L,
                "张三",
                "zhangsan",
                1,
                10L,
                "13800000000",
                "zhangsan@example.com",
                20L,
                "备注");
        when(employeeRepository.findAuthenticationAccountById(7L))
                .thenReturn(Optional.of(account(7L, false, false, false)));
        when(organizationDepartmentFacade.findForCollaboration(10L))
                .thenReturn(Optional.of(department(10L)));
        when(organizationPositionFacade.findForCollaboration(20L)).thenReturn(Optional.empty());

        ResponseDTO<String> response = service.updateWithLegacyRoles(command, false, List.of());

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("岗位不存在");
        verify(employeeRepository, never()).updateProfile(any());
    }

    @Test
    void refusesToDisableAdministrator() {
        when(employeeRepository.findAuthenticationAccountById(1L))
                .thenReturn(Optional.of(account(1L, true, false, false)));

        ResponseDTO<String> response = service.disable(1L);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("超级管理员");
        verify(employeeRepository, never()).updateDisabled(any(), anyBoolean());
        verify(employeeSessionPort, never()).logout(any());
    }

    @Test
    void enablingAlreadyEnabledEmployeeIsIdempotent() {
        when(employeeRepository.findAuthenticationAccountById(7L))
                .thenReturn(Optional.of(account(7L, false, false, false)));

        ResponseDTO<String> response = service.enable(7L);

        assertThat(response.getOk()).isTrue();
        verify(employeeRepository, never()).updateDisabled(any(), anyBoolean());
        verify(employeeSessionPort, never()).clearCache(any());
        verify(employeeSessionPort, never()).logout(any());
    }

    @Test
    void softDeletedEmployeeCannotBeEnabled() {
        when(employeeRepository.findAuthenticationAccountById(7L))
                .thenReturn(Optional.of(account(7L, false, true, true)));

        ResponseDTO<String> response = service.enable(7L);

        assertThat(response.getOk()).isFalse();
        verify(employeeRepository, never()).updateDisabled(any(), anyBoolean());
        verify(employeeSessionPort, never()).clearCache(any());
        verify(employeeSessionPort, never()).logout(any());
    }

    @Test
    void disablesEmployeeAndInvalidatesSession() {
        when(employeeRepository.findAuthenticationAccountById(7L))
                .thenReturn(Optional.of(account(7L, false, false, false)));

        ResponseDTO<String> response = service.disable(7L);

        assertThat(response.getOk()).isTrue();
        verify(employeeRepository).updateDisabled(7L, true);
        verify(employeeSessionPort).clearCache(7L);
        verify(employeeSessionPort).logout(7L);
    }

    @Test
    void departmentAssignmentRequiresExistingDepartment() {
        when(organizationDepartmentFacade.findForCollaboration(10L)).thenReturn(Optional.empty());

        ResponseDTO<String> response = service.assignDepartment(
                new EmployeeDepartmentAssignmentCommand(List.of(7L), 10L));

        assertThat(response.getOk()).isFalse();
        verify(employeeRepository, never()).findAuthenticationAccountsByIds(any());
        verify(employeeRepository, never()).assignDepartment(any(), any());
    }

    @Test
    void departmentAssignmentRequiresEveryEmployeeToExistAndBeActive() {
        when(organizationDepartmentFacade.findForCollaboration(10L))
                .thenReturn(Optional.of(department(10L)));
        when(employeeRepository.findAuthenticationAccountsByIds(List.of(7L, 8L)))
                .thenReturn(List.of(
                        account(7L, false, false, false),
                        account(8L, false, false, true)));

        ResponseDTO<String> response = service.assignDepartment(
                new EmployeeDepartmentAssignmentCommand(List.of(7L, 8L), 10L));

        assertThat(response.getOk()).isFalse();
        verify(employeeRepository, never()).assignDepartment(any(), any());
        verify(employeeSessionPort, never()).clearCache(any());
    }

    @Test
    void refusesToDeleteAnyBatchContainingAdministrator() {
        when(employeeRepository.findAuthenticationAccountsByIds(List.of(1L, 7L)))
                .thenReturn(List.of(
                        account(1L, true, false, false),
                        account(7L, false, false, false)));

        ResponseDTO<String> response = service.delete(new EmployeeDeleteCommand(List.of(1L, 7L)));

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("超级管理员");
        verify(employeeRepository, never()).markDeleted(any());
        verify(employeeSessionPort, never()).logout(any());
    }

    @Test
    void requiresAnotherActiveAdministratorToResetAdministratorPassword() {
        when(employeeRepository.findAuthenticationAccountById(1L))
                .thenReturn(Optional.of(account(1L, true, false, false)));

        ResponseDTO<EmployeeOneTimeCredential> response = service.resetPassword(1L, 1L);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("另一名");
        verify(employeeRepository, never()).updatePassword(any(), any());
    }

    @Test
    void anotherActiveAdministratorCanResetAdministratorPassword() {
        when(employeeRepository.findAuthenticationAccountById(1L))
                .thenReturn(Optional.of(account(1L, true, false, false)));
        when(employeeRepository.findAuthenticationAccountById(2L))
                .thenReturn(Optional.of(account(2L, true, false, false)));
        when(securityPasswordService.randomPassword()).thenReturn("Temp-Password-2");

        ResponseDTO<EmployeeOneTimeCredential> response = service.resetPassword(1L, 2L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().temporaryPassword()).isEqualTo("Temp-Password-2");
        verify(employeeRepository).updatePassword(any(), any());
        verify(employeeSessionPort).clearCache(1L);
        verify(employeeSessionPort).logout(1L);
    }

    private EmployeeCreateCommand createCommand() {
        return new EmployeeCreateCommand(
                "张三",
                "zhangsan",
                1,
                10L,
                false,
                "13800000000",
                "zhangsan@example.com",
                20L,
                "remark"
        );
    }

    private EmployeeAuthenticationAccount account(
            Long employeeId, boolean administrator, boolean disabled, boolean deleted) {
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
                administrator,
                disabled,
                deleted,
                null
        );
    }

    private Department department(Long departmentId) {
        return new Department(departmentId, "研发部", null, 0L, 1, null, null, null);
    }

    private Position position(Long positionId) {
        return new Position(positionId, "研发工程师", "P5", 10, null, null, null);
    }
}
