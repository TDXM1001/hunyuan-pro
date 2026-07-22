package com.hunyuan.sa.admin.module.access.role.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.admin.module.system.role.controller.RoleEmployeeController;
import com.hunyuan.sa.admin.module.system.role.domain.form.RoleEmployeeUpdateForm;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AccessRoleMembershipApiContractTest {

    @Test
    void exposesVersionedRoleMembershipEndpoints() throws Exception {
        RequestMapping baseMapping =
                AccessRoleMembershipController.class.getAnnotation(RequestMapping.class);
        assertThat(baseMapping.value()).containsExactly("/api/admin/v1/access");

        assertEndpoint(
                AccessRoleMembershipController.class.getMethod(
                        "queryMembers", Long.class, AccessRoleMemberQuery.class),
                PostMapping.class,
                "/roles/{roleId}/members/query",
                "access.role.employee.read",
                "accessRoleMemberQuery");
        assertEndpoint(
                AccessRoleMembershipController.class.getMethod(
                        "queryCandidates", Long.class, AccessRoleMemberQuery.class),
                PostMapping.class,
                "/roles/{roleId}/member-candidates/query",
                "access.role.employee.read",
                "accessRoleMemberCandidateQuery");
        assertEndpoint(
                AccessRoleMembershipController.class.getMethod("listMembers", Long.class),
                GetMapping.class,
                "/roles/{roleId}/members",
                "access.role.employee.read",
                "accessRoleMemberList");
        assertEndpoint(
                AccessRoleMembershipController.class.getMethod(
                        "assignMembers",
                        Long.class,
                        AccessRoleMembershipController.RoleEmployeesRequest.class),
                PostMapping.class,
                "/roles/{roleId}/members",
                "access.role.employee.assign",
                "accessRoleMemberAssign");
        assertEndpoint(
                AccessRoleMembershipController.class.getMethod(
                        "removeMembers",
                        Long.class,
                        AccessRoleMembershipController.RoleEmployeesRequest.class),
                DeleteMapping.class,
                "/roles/{roleId}/members",
                "access.role.employee.remove",
                "accessRoleMemberRemove");
        assertEndpoint(
                AccessRoleMembershipController.class.getMethod(
                        "listEmployeeRoles", Long.class),
                GetMapping.class,
                "/employees/{employeeId}/roles",
                "access.role.employee.read",
                "accessEmployeeRoleSelectionList");
    }

    @Test
    void legacyControllerDependsOnlyOnAccessRoleFacades() throws Exception {
        Field queryFacade = RoleEmployeeController.class.getDeclaredField("roleMembershipFacade");
        Field commandFacade = RoleEmployeeController.class.getDeclaredField("roleAssignmentFacade");

        assertThat(queryFacade.getType()).isEqualTo(AccessRoleMembershipFacade.class);
        assertThat(commandFacade.getType()).isEqualTo(AccessRoleAssignmentFacade.class);
        assertThat(RoleEmployeeController.class.getDeclaredFields())
                .extracting(Field::getType)
                .noneMatch(type -> type.getName().endsWith(".RoleEmployeeService"));
    }

    @Test
    void legacyControllerPreservesNullIdValidationAndWriteCommands() {
        AccessRoleMembershipFacade membershipFacade = mock(AccessRoleMembershipFacade.class);
        AccessRoleAssignmentFacade assignmentFacade = mock(AccessRoleAssignmentFacade.class);
        RoleEmployeeController controller = new RoleEmployeeController();
        ReflectionTestUtils.setField(controller, "roleMembershipFacade", membershipFacade);
        ReflectionTestUtils.setField(controller, "roleAssignmentFacade", assignmentFacade);

        var invalidResponse = controller.removeEmployee(null, 3L);
        assertThat(invalidResponse.getCode()).isEqualTo(UserErrorCode.PARAM_ERROR.getCode());

        RoleEmployeeUpdateForm form = new RoleEmployeeUpdateForm();
        form.setRoleId(3L);
        form.setEmployeeIdList(Set.of(7L, 8L));
        controller.addEmployeeList(form);
        controller.batchRemoveEmployee(form);

        verify(assignmentFacade).assignEmployees(
                new AssignRoleEmployeesCommand(3L, Set.of(7L, 8L)));
        verify(assignmentFacade).removeEmployees(
                new RemoveRoleEmployeesCommand(3L, Set.of(7L, 8L)));
    }

    private void assertEndpoint(
            Method method,
            Class<?> mappingType,
            String path,
            String permission,
            String operationId) {
        String[] paths;
        if (mappingType == GetMapping.class) {
            paths = method.getAnnotation(GetMapping.class).value();
        } else if (mappingType == PostMapping.class) {
            paths = method.getAnnotation(PostMapping.class).value();
        } else {
            paths = method.getAnnotation(DeleteMapping.class).value();
        }
        assertThat(paths).containsExactly(path);
        assertThat(method.getAnnotation(SaCheckPermission.class).value()).containsExactly(permission);
        assertThat(method.getAnnotation(Operation.class).operationId()).isEqualTo(operationId);
    }
}
