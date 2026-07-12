package com.hunyuan.sa.bpm.candidate;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyCatalogControllerContractTest {

    @Test
    void controllerShouldExposeVersionedPolicyCatalogLifecycleRoutes() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/hunyuan/sa/bpm/controller/admin/AdminBpmPolicyCatalogController.java"
        ));

        assertThat(source).contains(
                "/bpm/policy-catalog/list",
                "/bpm/policy-catalog/detail/{type}/{policyKey}/{policyVersion}",
                "/bpm/policy-catalog/validate",
                "/bpm/policy-catalog/draft",
                "/bpm/policy-catalog/copy",
                "/bpm/policy-catalog/activate",
                "/bpm/policy-catalog/retire"
        );
        assertThat(source).contains(
                "bpm:policy-catalog:list",
                "bpm:policy-catalog:detail",
                "bpm:policy-catalog:add",
                "bpm:policy-catalog:copy",
                "bpm:policy-catalog:activate",
                "bpm:policy-catalog:retire"
        );
        assertThat(source).contains("BpmCurrentActorProvider");
    }
}
