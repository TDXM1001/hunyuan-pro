package com.hunyuan.sa.base.module.support.codegenerator.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定代码生成器开发工具稳定路由和登录边界。
 */
class PlatformCodeGeneratorApiContractTest {

    @Test
    void exposesStableDevtoolsRoutesWithoutInventingBusinessPermissions() throws Exception {
        RequestMapping mapping = PlatformCodeGeneratorController.class
                .getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly(
                "/api/admin/v1/platform/devtools/code-generator");

        var columns = PlatformCodeGeneratorController.class.getMethod(
                "listColumns", String.class);
        assertThat(columns.getAnnotation(GetMapping.class).value())
                .containsExactly("/tables/{tableName}/columns");

        var tables = PlatformCodeGeneratorController.class.getMethod(
                "queryTables", PlatformCodeGeneratorTablePageQuery.class);
        assertThat(tables.getAnnotation(PostMapping.class).value())
                .containsExactly("/tables/query");

        var config = PlatformCodeGeneratorController.class.getMethod(
                "getConfig", String.class);
        assertThat(config.getAnnotation(GetMapping.class).value())
                .containsExactly("/tables/{tableName}/config");

        var update = PlatformCodeGeneratorController.class.getMethod(
                "updateConfig", PlatformCodeGeneratorConfigUpdateCommand.class);
        assertThat(update.getAnnotation(PutMapping.class).value())
                .containsExactly("/tables/config");

        var preview = PlatformCodeGeneratorController.class.getMethod(
                "preview", PlatformCodeGeneratorPreviewCommand.class);
        assertThat(preview.getAnnotation(PostMapping.class).value())
                .containsExactly("/preview");

        var download = PlatformCodeGeneratorController.class.getMethod(
                "download", String.class, HttpServletResponse.class);
        assertThat(download.getAnnotation(GetMapping.class).value())
                .containsExactly("/download/{tableName}");

        assertThat(PlatformCodeGeneratorController.class.getMethods())
                .filteredOn(method -> method.getDeclaringClass()
                        .equals(PlatformCodeGeneratorController.class))
                .allSatisfy(method -> assertThat(
                        method.getAnnotation(SaCheckPermission.class)).isNull());
    }
}
