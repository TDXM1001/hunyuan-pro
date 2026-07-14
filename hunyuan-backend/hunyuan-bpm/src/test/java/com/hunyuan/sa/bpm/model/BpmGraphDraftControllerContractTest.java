package com.hunyuan.sa.bpm.model;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BpmGraphDraftControllerContractTest {

    @Test
    void controllerShouldExposeSeparateGraphDraftRoutes() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmGraphDraftController.java"
        ));

        assertThat(source).contains("/bpm/graph-draft/create");
        assertThat(source).contains("/bpm/graph-draft/query");
        assertThat(source).contains("/bpm/graph-draft/save");
        assertThat(source).contains("/bpm/graph-draft/detail/{draftId}");
        assertThat(source).contains("/bpm/graph-draft/export/{draftId}");
        assertThat(source).contains("/bpm/graph-draft/import");
        assertThat(source).contains(
                "bpm:graph-draft:query", "bpm:graph-draft:add",
                "bpm:graph-draft:update", "bpm:graph-draft:detail"
        );
        assertThat(source).contains("BpmCurrentActorProvider");
    }
}
