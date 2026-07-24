package com.hunyuan.sa.base.module.support.file.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 稳定文件 HTTP 路由契约，避免账号等业务消费者重新接入历史支持路由。
 */
class PlatformFileApiContractTest {

    @Test
    void exposesStableUploadAndUrlRoutes() throws Exception {
        RequestMapping mapping = PlatformFileController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/api/admin/v1/platform/files");

        assertThat(PlatformFileController.class
                .getMethod("upload", MultipartFile.class, Integer.class)
                .getAnnotation(PostMapping.class).value()).isEmpty();
        assertThat(PlatformFileController.class
                .getMethod("resolveUrl", String.class)
                .getAnnotation(GetMapping.class).value()).containsExactly("/url");

        assertThat(PlatformFileController.class
                .getMethod("upload", MultipartFile.class, Integer.class)
                .getParameters()[1]
                .getAnnotation(RequestParam.class).value()).isEqualTo("folder");
    }

    @Test
    void exposesStableFileManagementRoutes() throws Exception {
        assertThat(PlatformFileController.class
                .getMethod("queryPage", PlatformFilePageQuery.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/query");
        assertThat(PlatformFileController.class
                .getMethod("download", String.class, HttpServletRequest.class, HttpServletResponse.class)
                .getAnnotation(GetMapping.class).value()).containsExactly("/download");
        assertThat(PlatformFileController.class
                .getMethod("queryPage", PlatformFilePageQuery.class)
                .getAnnotation(SaCheckPermission.class).value()).containsExactly("support:file:query");
        assertThat(PlatformFileController.class
                .getMethod("download", String.class, HttpServletRequest.class, HttpServletResponse.class)
                .getAnnotation(SaCheckPermission.class).value()).containsExactly("support:file:query");
    }
}
