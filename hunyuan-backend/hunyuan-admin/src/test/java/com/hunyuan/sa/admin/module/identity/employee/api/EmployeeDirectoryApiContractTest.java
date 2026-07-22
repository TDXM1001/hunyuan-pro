package com.hunyuan.sa.admin.module.identity.employee.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.exception.NotPermissionException;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.domain.SystemEnvironment;
import com.hunyuan.sa.base.common.enumeration.SystemEnvironmentEnum;
import com.hunyuan.sa.base.handler.GlobalExceptionHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmployeeDirectoryApiContractTest {

    @Test
    void exposesVersionedQueryWithEmployeeReadCapability() throws Exception {
        RequestMapping baseMapping = EmployeeDirectoryController.class.getAnnotation(RequestMapping.class);
        assertThat(baseMapping.value()).containsExactly("/api/admin/v1/identity/employees");
        assertThat(EmployeeDirectoryController.class.getAnnotation(Tag.class).name())
                .isEqualTo("身份目录 - 员工");

        Method query = EmployeeDirectoryController.class.getMethod("query", EmployeeQuery.class);
        assertThat(query.getAnnotation(PostMapping.class).value()).containsExactly("/query");
        assertOperation(query, "identityEmployeeQuery", "分页查询员工");
        assertThat(query.getAnnotation(SaCheckPermission.class).value()).containsExactly("identity.employee.read");
    }

    @Test
    void exposesVersionedAdministrationCapabilities() throws Exception {
        assertPermission(
                EmployeeDirectoryController.class.getMethod(
                        "create", EmployeeCreateCommand.class, jakarta.servlet.http.HttpServletResponse.class),
                PostMapping.class,
                "",
                "identity.employee.create",
                "identityEmployeeCreate",
                "创建员工");
        assertPermission(
                EmployeeDirectoryController.class.getMethod(
                        "update", Long.class, EmployeeUpdateCommand.class),
                PutMapping.class,
                "/{employeeId}",
                "identity.employee.update",
                "identityEmployeeUpdate",
                "更新员工资料");
        assertPermission(
                EmployeeDirectoryController.class.getMethod("enable", Long.class),
                PostMapping.class,
                "/{employeeId}/enable",
                "identity.employee.enable",
                "identityEmployeeEnable",
                "启用员工");
        assertPermission(
                EmployeeDirectoryController.class.getMethod("disable", Long.class),
                PostMapping.class,
                "/{employeeId}/disable",
                "identity.employee.disable",
                "identityEmployeeDisable",
                "禁用员工");
        assertPermission(
                EmployeeDirectoryController.class.getMethod(
                        "assignDepartment", EmployeeDepartmentAssignmentCommand.class),
                PostMapping.class,
                "/department-assignment",
                "identity.employee.department.assign",
                "identityEmployeeAssignDepartment",
                "批量调整员工部门");
        assertPermission(
                EmployeeDirectoryController.class.getMethod("delete", EmployeeDeleteCommand.class),
                PostMapping.class,
                "/delete",
                "identity.employee.delete",
                "identityEmployeeDelete",
                "批量删除员工");
        assertPermission(
                EmployeeDirectoryController.class.getMethod(
                        "resetPassword", Long.class, jakarta.servlet.http.HttpServletResponse.class),
                PostMapping.class,
                "/{employeeId}/password/reset",
                "identity.employee.password.reset",
                "identityEmployeeResetPassword",
                "重置员工密码");
    }

    @Test
    void updateContractKeepsStatusAndRolesOutOfEmployeeProfile() {
        assertThat(EmployeeUpdateCommand.class.getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("disabled", "disabledFlag", "roleIds", "roleIdList", "administrator");
        assertThat(EmployeeOneTimeCredential.class.getRecordComponents())
                .extracting(component -> component.getName())
                .containsExactly("employeeId", "temporaryPassword");
    }

    @Test
    void publicResponseSchemasDescribeFieldsWithoutSensitiveComponents() {
        assertThat(EmployeeSummary.class.getAnnotation(Schema.class).description())
                .contains("不包含认证秘密", "超级管理员标记");
        assertThat(EmployeeSummary.class.getRecordComponents())
                .extracting(component -> component.getName())
                .containsExactly(
                        "employeeId",
                        "loginName",
                        "actualName",
                        "avatar",
                        "gender",
                        "phone",
                        "email",
                        "departmentId",
                        "departmentName",
                        "positionId",
                        "disabled",
                        "createTime")
                .doesNotContain(
                        "employeeUid",
                        "passwordHash",
                        "loginPwd",
                        "deleted",
                        "administrator");
        assertThat(EmployeeSummary.class.getRecordComponents())
                .allSatisfy(component -> {
                    try {
                        assertThat(EmployeeSummary.class.getMethod(component.getName()).getAnnotation(Schema.class))
                                .as("OpenAPI schema for %s", component.getName())
                                .isNotNull();
                    } catch (NoSuchMethodException exception) {
                        throw new AssertionError("Missing record accessor: " + component.getName(), exception);
                    }
                });
        assertThat(EmployeeOneTimeCredential.class.getAnnotation(Schema.class).description())
                .contains("一次性员工凭据");
    }

    @Test
    void credentialResponsesDisableHttpCaching() {
        EmployeeAdministrationFacade administrationFacade = mock(EmployeeAdministrationFacade.class);
        when(administrationFacade.create(any()))
                .thenReturn(com.hunyuan.sa.base.common.domain.ResponseDTO.ok(
                        new EmployeeOneTimeCredential(7L, "temporary-password")));

        EmployeeDirectoryController controller = new EmployeeDirectoryController();
        ReflectionTestUtils.setField(controller, "administrationFacade", administrationFacade);
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.create(new EmployeeCreateCommand(
                "张三",
                "zhangsan",
                1,
                10L,
                false,
                "13800000000",
                "zhangsan@example.com",
                20L,
                "remark"
        ), response);

        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");

        when(administrationFacade.resetPassword(7L, null))
                .thenReturn(com.hunyuan.sa.base.common.domain.ResponseDTO.ok(
                        new EmployeeOneTimeCredential(7L, "replacement-password")));
        MockHttpServletResponse resetResponse = new MockHttpServletResponse();

        controller.resetPassword(7L, resetResponse);

        assertThat(resetResponse.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(resetResponse.getHeader("Pragma")).isEqualTo("no-cache");
    }

    @Test
    void enforcesAdministratorReadOnlyAndNoModulePermissionMatrix() throws Exception {
        Method query = EmployeeDirectoryController.class.getMethod("query", EmployeeQuery.class);
        List<Method> mutations = List.of(
                EmployeeDirectoryController.class.getMethod(
                        "create", EmployeeCreateCommand.class, jakarta.servlet.http.HttpServletResponse.class),
                EmployeeDirectoryController.class.getMethod(
                        "update", Long.class, EmployeeUpdateCommand.class),
                EmployeeDirectoryController.class.getMethod("enable", Long.class),
                EmployeeDirectoryController.class.getMethod("disable", Long.class),
                EmployeeDirectoryController.class.getMethod(
                        "assignDepartment", EmployeeDepartmentAssignmentCommand.class),
                EmployeeDirectoryController.class.getMethod("delete", EmployeeDeleteCommand.class),
                EmployeeDirectoryController.class.getMethod(
                        "resetPassword", Long.class, jakarta.servlet.http.HttpServletResponse.class));

        Set<String> administratorCapabilities = Set.of(
                "identity.employee.read",
                "identity.employee.create",
                "identity.employee.update",
                "identity.employee.enable",
                "identity.employee.disable",
                "identity.employee.department.assign",
                "identity.employee.delete",
                "identity.employee.password.reset");
        Set<String> readOnlyCapabilities = Set.of("identity.employee.read");
        Set<String> noModuleCapabilities = Set.of();

        assertThat(isAllowed(query, administratorCapabilities)).isTrue();
        assertThat(mutations).allSatisfy(method ->
                assertThat(isAllowed(method, administratorCapabilities)).as(method.getName()).isTrue());

        assertThat(isAllowed(query, readOnlyCapabilities)).isTrue();
        assertThat(mutations).allSatisfy(method ->
                assertThat(isAllowed(method, readOnlyCapabilities)).as(method.getName()).isFalse());

        assertThat(isAllowed(query, noModuleCapabilities)).isFalse();
        assertThat(mutations).allSatisfy(method ->
                assertThat(isAllowed(method, noModuleCapabilities)).as(method.getName()).isFalse());
    }

    @Test
    void mapsPermissionDenialsToNoPermissionErrorCode() {
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(
                exceptionHandler,
                "systemEnvironment",
                new SystemEnvironment(false, "hunyuan-admin", SystemEnvironmentEnum.DEV));

        ResponseDTO<String> response = exceptionHandler.permissionException(
                new NotPermissionException("identity.employee.read"));

        assertThat(response.getCode()).isEqualTo(UserErrorCode.NO_PERMISSION.getCode());
        assertThat(response.getCode()).isEqualTo(30005);
        assertThat(response.getOk()).isFalse();
    }

    private boolean isAllowed(Method method, Set<String> capabilities) {
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
        return permission != null
                && Arrays.stream(permission.value()).allMatch(capabilities::contains);
    }

    private void assertPermission(
            Method method,
            Class<? extends java.lang.annotation.Annotation> mappingType,
            String path,
            String permission,
            String operationId,
            String summary) {
        String[] mappingPaths;
        if (mappingType == PostMapping.class) {
            mappingPaths = method.getAnnotation(PostMapping.class).value();
        } else {
            mappingPaths = method.getAnnotation(PutMapping.class).value();
        }
        if (path.isEmpty()) {
            assertThat(mappingPaths).isEmpty();
        } else {
            assertThat(mappingPaths).containsExactly(path);
        }
        assertThat(method.getAnnotation(SaCheckPermission.class).value()).containsExactly(permission);
        assertOperation(method, operationId, summary);
    }

    private void assertOperation(Method method, String operationId, String summary) {
        Operation operation = method.getAnnotation(Operation.class);
        assertThat(operation.operationId()).isEqualTo(operationId);
        assertThat(operation.summary()).isEqualTo(summary);
        assertThat(operation.description()).isNotBlank();
    }
}
