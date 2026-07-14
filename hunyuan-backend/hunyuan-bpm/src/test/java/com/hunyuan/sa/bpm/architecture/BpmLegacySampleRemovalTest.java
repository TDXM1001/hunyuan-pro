package com.hunyuan.sa.bpm.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BpmLegacySampleRemovalTest {

    @Test
    void productionSourceDoesNotContainTheRetiredSampleExpenseModule() throws IOException {
        Path sourceRoot = Path.of("src/main/java/com/hunyuan/sa/bpm");

        Path sampleModule = sourceRoot.resolve("module/sampleexpense");
        if (Files.exists(sampleModule)) {
            try (var sources = Files.walk(sampleModule)) {
                assertThat(sources).noneMatch(path -> path.toString().endsWith(".java"));
            }
        }
        assertThat(sourceRoot.resolve("controller/admin/AdminBpmSampleExpenseController.java")).doesNotExist();
        try (var sources = Files.walk(sourceRoot)) {
            assertThat(sources.filter(path -> path.toString().endsWith(".java")))
                    .allSatisfy(path -> assertThat(Files.readString(path))
                            .doesNotContain("com.hunyuan.sa.bpm.module.sampleexpense"));
        }
    }

    @Test
    void retiredBusinessStartFacadeDoesNotRemainAsAnUnusedParallelEntry() throws IOException {
        Path businessApiRoot = Path.of("src/main/java/com/hunyuan/sa/bpm/api/business");
        String businessApi = Files.readString(businessApiRoot.resolve("BpmBusinessProcessApi.java"));

        assertThat(businessApiRoot.resolve("domain/BpmBusinessStartCommand.java")).doesNotExist();
        assertThat(businessApiRoot.resolve("domain/BpmBusinessInstanceStatus.java")).doesNotExist();
        assertThat(businessApi)
                .doesNotContain("Long start(")
                .doesNotContain("getStatus(");
    }
}
