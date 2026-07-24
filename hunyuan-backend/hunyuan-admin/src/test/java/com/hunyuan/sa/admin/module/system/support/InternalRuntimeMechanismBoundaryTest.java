package com.hunyuan.sa.admin.module.system.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定重复提交与 API 加密的内部机制属性，防止误扩建为平台运行时管理接口。
 */
class InternalRuntimeMechanismBoundaryTest {

    private static final Path BACKEND_ROOT = Path.of("..").toAbsolutePath().normalize();

    @Test
    void repeatSubmitPackageDoesNotExposeHttpController() throws IOException {
        Path packageRoot = BACKEND_ROOT.resolve(
                "hunyuan-base/src/main/java/com/hunyuan/sa/base/module/support/repeatsubmit");

        assertThat(readJavaSources(packageRoot))
                .doesNotContain("@RestController")
                .doesNotContain("@RequestMapping")
                .doesNotContain("@GetMapping")
                .doesNotContain("@PostMapping");
    }

    @Test
    void apiEncryptImplementationDoesNotExposeStableRuntimeRoute() throws IOException {
        Path packageRoot = BACKEND_ROOT.resolve(
                "hunyuan-base/src/main/java/com/hunyuan/sa/base/module/support/apiencrypt");
        String stableControllers = Files.readString(BACKEND_ROOT.resolve(
                "hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/system/support/"
                        + "PlatformRuntimeReloadController.java"), StandardCharsets.UTF_8)
                + Files.readString(BACKEND_ROOT.resolve(
                "hunyuan-admin/src/main/java/com/hunyuan/sa/admin/module/system/support/"
                        + "PlatformHeartbeatController.java"), StandardCharsets.UTF_8);

        assertThat(readJavaSources(packageRoot)).doesNotContain("@RestController");
        assertThat(stableControllers)
                .doesNotContain("module.support.apiencrypt")
                .doesNotContain("/apiEncrypt/");
    }

    /**
     * 按 UTF-8 汇总目录内 Java 源码，避免平台差异影响中文源码校验。
     */
    private String readJavaSources(Path packageRoot) throws IOException {
        StringBuilder source = new StringBuilder();
        try (var paths = Files.walk(packageRoot)) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                source.append(Files.readString(path, StandardCharsets.UTF_8));
            }
        }
        return source.toString();
    }
}
