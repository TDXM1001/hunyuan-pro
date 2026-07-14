package com.hunyuan.sa.bpm.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BpmGraphOnlyControlPlaneTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java/com/hunyuan/sa/bpm");

    @Test
    void legacyAuthoringAndPublishingControllersAreRemoved() {
        List<String> legacyControllers = List.of(
                "controller/admin/AdminBpmModelController.java",
                "controller/admin/AdminBpmDesignerController.java",
                "controller/admin/AdminBpmDefinitionController.java"
        );

        assertThat(legacyControllers)
                .map(SOURCE_ROOT::resolve)
                .noneMatch(Files::exists);
    }
}
