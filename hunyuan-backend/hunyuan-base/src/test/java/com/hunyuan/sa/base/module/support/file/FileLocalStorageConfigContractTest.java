package com.hunyuan.sa.base.module.support.file;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileLocalStorageConfigContractTest {

    @Test
    void devConfigShouldStoreLocalUploadFilesUnderProjectRuntimeDirectory() throws IOException {
        Path devConfigPath = repoRoot().resolve("hunyuan-backend/hunyuan-base/src/main/resources/dev/hunyuan-base.yaml");
        String devConfig = Files.readString(devConfigPath, StandardCharsets.UTF_8);

        assertThat(devConfig).contains("upload-path: ${localPath:./runtime}/upload/");
    }

    @Test
    void gitIgnoreShouldExcludeRuntimeUploadFiles() throws IOException {
        Path gitIgnorePath = repoRoot().resolve(".gitignore");
        String gitIgnore = Files.readString(gitIgnorePath, StandardCharsets.UTF_8);

        assertThat(gitIgnore).contains("/hunyuan-backend/runtime/upload/");
    }

    private Path repoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve(".gitignore"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("未找到仓库根目录 .gitignore");
    }
}
