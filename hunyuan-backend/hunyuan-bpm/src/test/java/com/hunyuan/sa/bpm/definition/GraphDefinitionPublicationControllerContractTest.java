package com.hunyuan.sa.bpm.definition;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GraphDefinitionPublicationControllerContractTest {

    @Test
    void controllerShouldExposeInspectableGraphDefinitionRoutes() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminGraphDefinitionController.java"
        ));

        assertThat(source).contains("/bpm/graph-definition/publish");
        assertThat(source).contains("/bpm/graph-definition/deactivate/");
        assertThat(source).contains("/bpm/graph-definition/detail/{versionId}");
        assertThat(source).contains("/bpm/graph-definition/latest-by-draft/{draftId}");
        assertThat(source).contains(
                "bpm:graph-definition:publish",
                "bpm:graph-definition:deactivate",
                "bpm:graph-definition:detail"
        );
        assertThat(source).contains("getLatestDefinitionDetail");
        assertThat(source).contains("BpmCurrentActorProvider");
    }
}
