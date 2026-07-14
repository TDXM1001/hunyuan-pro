package com.hunyuan.sa.bpm.operations;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AdminBpmOperationsControllerSourceTest {

    @Test
    void highRiskAndArchiveActionsMustRequireDedicatedPermissions() throws Exception {
        String source = Files.readString(
                Path.of("src", "main", "java", "com", "hunyuan", "sa", "bpm", "controller", "admin", "AdminBpmOperationsController.java"),
                StandardCharsets.UTF_8
        );
        assertThat(source).contains(
                "bpm:operations:high-risk",
                "bpm:operations:archive",
                "StpUtil.checkPermission"
        );
    }
}
