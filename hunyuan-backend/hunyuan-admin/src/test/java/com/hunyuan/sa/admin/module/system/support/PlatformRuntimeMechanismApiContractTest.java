package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.module.support.heartbeat.api.PlatformHeartbeatPageQuery;
import com.hunyuan.sa.base.module.support.reload.api.PlatformRuntimeReloadUpdateCommand;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定运行时重载与心跳记录稳定路由及历史权限映射。
 */
class PlatformRuntimeMechanismApiContractTest {

    @Test
    void exposesStableReloadRoutesAndLegacyPermissions() throws Exception {
        assertBasePath(PlatformRuntimeReloadController.class,
                "/api/admin/v1/platform/runtime/reloads");

        var listMethod = PlatformRuntimeReloadController.class.getMethod("listItems");
        assertThat(listMethod.getAnnotation(GetMapping.class).value()).isEmpty();

        var resultMethod = PlatformRuntimeReloadController.class.getMethod(
                "listResults", String.class);
        assertThat(resultMethod.getAnnotation(GetMapping.class).value())
                .containsExactly("/{tag}/results");
        assertThat(resultMethod.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("support:reload:result");

        var updateMethod = PlatformRuntimeReloadController.class.getMethod(
                "updateItem", PlatformRuntimeReloadUpdateCommand.class);
        assertThat(updateMethod.getAnnotation(PutMapping.class).value()).isEmpty();
        assertThat(updateMethod.getAnnotation(SaCheckPermission.class).value())
                .containsExactly("support:reload:update");
    }

    @Test
    void exposesHeartbeatAsStableReadOnlyQuery() throws Exception {
        assertBasePath(PlatformHeartbeatController.class,
                "/api/admin/v1/platform/runtime/heartbeats");

        var method = PlatformHeartbeatController.class.getMethod(
                "queryRecords", PlatformHeartbeatPageQuery.class);
        assertThat(method.getAnnotation(PostMapping.class).value()).containsExactly("/query");
        assertThat(PlatformHeartbeatController.class.getDeclaredMethods())
                .allMatch(candidate -> candidate.getAnnotation(PutMapping.class) == null);
    }

    @Test
    void keepsSecurityMechanismsOutOfStableRuntimeControllerSurface() {
        assertThat(PlatformRuntimeReloadController.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().toLowerCase().contains("encrypt"));
        assertThat(PlatformHeartbeatController.class.getDeclaredMethods())
                .noneMatch(method -> method.getName().toLowerCase().contains("repeat"));
    }

    private void assertBasePath(Class<?> controllerType, String expectedPath) {
        RequestMapping mapping = controllerType.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly(expectedPath);
    }
}
