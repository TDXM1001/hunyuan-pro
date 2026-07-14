package com.hunyuan.sa.admin.bpm;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.bpm.controller.admin.AdminBpmBusinessContractController;
import com.hunyuan.sa.bpm.controller.admin.AdminBpmPolicyCatalogController;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class BpmVisualConfigurationPermissionTest {

    @Test
    void technicalEndpointsMustUseIndependentPermissions() {
        assertPermission(AdminBpmPolicyCatalogController.class, "technicalDetail", "bpm:policy-catalog:technical");
        assertPermission(AdminBpmPolicyCatalogController.class, "technicalDiff", "bpm:policy-catalog:technical");
        assertPermission(AdminBpmPolicyCatalogController.class, "technicalExport", "bpm:policy-catalog:technical");
        assertPermission(AdminBpmBusinessContractController.class, "technicalDetail", "bpm:business-contract:technical");
        assertPermission(AdminBpmBusinessContractController.class, "technicalDiff", "bpm:business-contract:technical");
        assertPermission(AdminBpmBusinessContractController.class, "technicalExport", "bpm:business-contract:technical");
    }

    @Test
    void businessDetailEndpointsMustNotRequireTechnicalPermission() {
        assertPermission(AdminBpmPolicyCatalogController.class, "detail", "bpm:policy-catalog:detail");
        assertPermission(AdminBpmBusinessContractController.class, "detail", "bpm:business-contract:detail");
    }

    private void assertPermission(Class<?> controller, String methodName, String expected) {
        Method method = Arrays.stream(controller.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
        assertThat(permission).isNotNull();
        assertThat(permission.value()).containsExactly(expected);
    }
}
