package com.hunyuan.sa.admin.module.organization.department.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationDepartmentApiContractTest {

    @Test
    void exposesVersionedRestAndStableCapabilities() throws Exception {
        RequestMapping baseMapping = OrganizationDepartmentController.class.getAnnotation(RequestMapping.class);
        assertThat(baseMapping.value()).containsExactly("/api/admin/v1/organization/departments");

        assertEndpoint("list", GetMapping.class, "organizationDepartmentList", "organization.department.read");
        assertEndpoint("get", GetMapping.class, "organizationDepartmentGet", "organization.department.read", Long.class);
        assertEndpoint("managerOptions", GetMapping.class, "organizationDepartmentManagerOptions", "organization.department.read");
        assertEndpoint("create", PostMapping.class, "organizationDepartmentCreate", "organization.department.create",
                OrganizationDepartmentController.DepartmentRequest.class);
        assertEndpoint("update", PutMapping.class, "organizationDepartmentUpdate", "organization.department.update",
                Long.class, OrganizationDepartmentController.DepartmentRequest.class);
        assertEndpoint("delete", DeleteMapping.class, "organizationDepartmentDelete", "organization.department.delete", Long.class);
    }

    private void assertEndpoint(String methodName, Class<? extends Annotation> mappingType,
                                String operationId, String capability, Class<?>... parameterTypes) throws Exception {
        Method method = OrganizationDepartmentController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(mappingType)).isNotNull();
        assertThat(method.getAnnotation(Operation.class).operationId()).isEqualTo(operationId);
        assertThat(method.getAnnotation(SaCheckPermission.class).value()).containsExactly(capability);
    }
}
