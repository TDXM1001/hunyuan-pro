package com.hunyuan.sa.admin.module.system.login.manager;

import com.hunyuan.sa.admin.module.access.authorization.api.AccessAuthorizationFacade;
import com.hunyuan.sa.admin.module.access.authorization.api.AccessAuthorizationSnapshot;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeAuthenticationAccount;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeDirectoryFacade;
import com.hunyuan.sa.admin.module.organization.department.application.OrganizationDepartmentFacade;
import com.hunyuan.sa.admin.module.organization.department.domain.Department;
import com.hunyuan.sa.admin.module.system.login.domain.RequestEmployee;
import com.hunyuan.sa.base.common.domain.UserPermission;
import com.hunyuan.sa.base.module.support.file.api.PlatformFileFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginManagerIdentityEmployeeTest {

    @Mock
    private OrganizationDepartmentFacade organizationDepartmentFacade;
    @Mock
    private PlatformFileFacade platformFileFacade;
    @Mock
    private EmployeeDirectoryFacade employeeDirectoryFacade;
    @Mock
    private AccessAuthorizationFacade accessAuthorizationFacade;

    private LoginManager loginManager;

    @BeforeEach
    void setUp() {
        loginManager = new LoginManager();
        ReflectionTestUtils.setField(loginManager, "organizationDepartmentFacade", organizationDepartmentFacade);
        ReflectionTestUtils.setField(loginManager, "platformFileFacade", platformFileFacade);
        ReflectionTestUtils.setField(loginManager, "employeeDirectoryFacade", employeeDirectoryFacade);
        ReflectionTestUtils.setField(loginManager, "accessAuthorizationFacade", accessAuthorizationFacade);
    }

    @Test
    void mapsAuthenticationAccountToExistingRequestEmployeeContract() {
        EmployeeAuthenticationAccount account = new EmployeeAuthenticationAccount(
                7L, "uid", "zhangsan", "hash", "张三", null, 1,
                "13800000000", "a@example.com", 20L, 30L, true, false, false, "备注");
        when(organizationDepartmentFacade.findForCollaboration(20L))
                .thenReturn(Optional.of(new Department(20L, "研发部", null, 0L, 10, null, null, null)));

        RequestEmployee requestEmployee = loginManager.loadLoginInfo(account);

        assertThat(requestEmployee.getEmployeeId()).isEqualTo(7L);
        assertThat(requestEmployee.getLoginName()).isEqualTo("zhangsan");
        assertThat(requestEmployee.getDepartmentName()).isEqualTo("研发部");
        assertThat(requestEmployee.getAdministratorFlag()).isTrue();
        assertThat(requestEmployee.getDisabledFlag()).isFalse();
    }

    @Test
    void loadsPermissionsOnlyThroughAccessAuthorizationFacade() {
        EmployeeAuthenticationAccount account = new EmployeeAuthenticationAccount(
                7L, "uid", "zhangsan", "hash", "张三", null, 1,
                "13800000000", "a@example.com", 20L, 30L, false, false, false, "备注");
        when(employeeDirectoryFacade.findAuthenticationAccountById(7L))
                .thenReturn(Optional.of(account));
        when(accessAuthorizationFacade.loadEmployeeAuthorization(7L, false))
                .thenReturn(new AccessAuthorizationSnapshot(
                        Set.of("finance"),
                        Set.of("invoice.read"),
                        List.of()));

        UserPermission permission = loginManager.loadUserPermission(7L);

        assertThat(permission.getRoleList()).containsExactly("finance");
        assertThat(permission.getPermissionList()).containsExactly("invoice.read");
    }
}
