package com.hunyuan.sa.admin.module.business;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyExampleModuleRetirementTest {

    private static final Path MAIN_JAVA = Path.of("src", "main", "java");
    private static final Path MAIN_RESOURCES = Path.of("src", "main", "resources");

    @Test
    void goodsAndCategoryProductionSourcesAreRemoved() throws IOException {
        List<Path> retiredDirectories = List.of(
                MAIN_JAVA.resolve(Path.of(
                        "com", "hunyuan", "sa", "admin", "module", "business", "goods")),
                MAIN_JAVA.resolve(Path.of(
                        "com", "hunyuan", "sa", "admin", "module", "business", "category")),
                MAIN_RESOURCES.resolve(Path.of("mapper", "business", "goods")),
                MAIN_RESOURCES.resolve(Path.of("mapper", "business", "category")));

        for (Path retiredDirectory : retiredDirectories) {
            if (Files.notExists(retiredDirectory)) {
                continue;
            }
            try (var files = Files.walk(retiredDirectory)) {
                assertThat(files.filter(Files::isRegularFile).toList())
                        .as("商品与分类示例生产目录不得恢复文件：%s", retiredDirectory)
                        .isEmpty();
            }
        }
    }

    @Test
    void productionJavaSourcesDoNotRestoreLegacyRoutesPermissionsOrCaches() throws IOException {
        try (var files = Files.walk(MAIN_JAVA)) {
            List<Path> productionSources = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path sourceFile : productionSources) {
                assertThat(Files.readString(sourceFile, StandardCharsets.UTF_8))
                        .as("商品与分类示例入口必须保持退役：%s", sourceFile)
                        .doesNotContain(
                                "module.business.goods",
                                "module.business.category",
                                "\"/goods/",
                                "\"/category/",
                                "\"goods:",
                                "\"category:",
                                "AdminCacheConst.Category");
            }
        }
    }
}
