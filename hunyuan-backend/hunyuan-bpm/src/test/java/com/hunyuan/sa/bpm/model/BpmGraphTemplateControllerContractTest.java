package com.hunyuan.sa.bpm.model;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BpmGraphTemplateControllerContractTest {

    @Test
    void controllerShouldExposeTemplateFreezeAndCopyRoutes() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmGraphTemplateController.java"
        ));

        assertThat(source).contains("/bpm/graph-template/create");
        assertThat(source).contains("/bpm/graph-template/copy");
        assertThat(source).contains("bpm:graph-template:add", "bpm:graph-template:copy");
        assertThat(source).contains("BpmCurrentActorProvider");
    }
}
