package com.hunyuan.sa.admin.module.access.role.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
    void stableControllerDelegatesMembershipCommands() {
        AccessRoleAssignmentFacade assignmentFacade = mock(AccessRoleAssignmentFacade.class);
        AccessRoleMembershipController controller = new AccessRoleMembershipController();
        ReflectionTestUtils.setField(controller, "roleAssignmentFacade", assignmentFacade);

        var request =
                new AccessRoleMembershipController.RoleEmployeesRequest(Set.of(7L, 8L));
        controller.assignMembers(3L, request);
        controller.removeMembers(3L, request);

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
