package com.hunyuan.sa.bpm.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BpmApiIsolationTest {

    @Test
    void publicBpmContractsDoNotImportFlowableTypes() throws IOException {
        try (Stream<Path> stream = Files.walk(Path.of("src/main/java/com/hunyuan/sa/bpm"))) {
            stream.filter(path -> path.toString().endsWith(".java"))
                    .filter(this::isPublicContractPath)
                    .forEach(path -> {
                        try {
                            assertThat(Files.readString(path)).doesNotContain("org.flowable");
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        }
    }

    private boolean isPublicContractPath(Path path) {
        String normalizedPath = path.toString().replace('\\', '/');
        return normalizedPath.contains("/controller/")
                || normalizedPath.contains("/api/")
                || normalizedPath.contains("/domain/form/")
                || normalizedPath.contains("/domain/vo/");
    }
}
