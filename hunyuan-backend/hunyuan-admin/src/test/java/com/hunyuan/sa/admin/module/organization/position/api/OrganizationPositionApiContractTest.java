package com.hunyuan.sa.admin.module.organization.position.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("岗位目录接口契约")
class OrganizationPositionApiContractTest {

    @Test
    @DisplayName("暴露版本化路径、稳定操作标识和能力码")
    void 暴露版本化路径稳定操作标识和能力码() throws Exception {
        RequestMapping baseMapping = OrganizationPositionController.class.getAnnotation(RequestMapping.class);
        assertThat(baseMapping.value()).containsExactly("/api/admin/v1/organization/positions");

        assertEndpoint("list", GetMapping.class, "organizationPositionList", "organization.position.read");
        assertEndpoint("get", GetMapping.class, "organizationPositionGet", "organization.position.read", Long.class);
        assertEndpoint(
                "create",
                PostMapping.class,
                "organizationPositionCreate",
                "organization.position.create",
                OrganizationPositionController.PositionRequest.class);
        assertEndpoint(
                "update",
                PutMapping.class,
                "organizationPositionUpdate",
                "organization.position.update",
                Long.class,
                OrganizationPositionController.PositionRequest.class);
        assertEndpoint(
                "delete",
                DeleteMapping.class,
                "organizationPositionDelete",
                "organization.position.delete",
                Long.class);
    }

    private void assertEndpoint(
            String methodName,
            Class<? extends Annotation> mappingType,
            String operationId,
            String capability,
            Class<?>... parameterTypes) throws Exception {
        Method method = OrganizationPositionController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(mappingType)).isNotNull();
        assertThat(method.getAnnotation(Operation.class).operationId()).isEqualTo(operationId);
        assertThat(method.getAnnotation(SaCheckPermission.class).value()).containsExactly(capability);
    }
}
