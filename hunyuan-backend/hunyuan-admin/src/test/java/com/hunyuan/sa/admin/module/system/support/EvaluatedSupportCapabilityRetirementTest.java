package com.hunyuan.sa.admin.module.system.support;

import com.hunyuan.sa.base.module.support.mail.api.PlatformMailFacade;
import com.hunyuan.sa.base.module.support.mail.application.PlatformMailApplicationService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluatedSupportCapabilityRetirementTest {

    private static final Path BASE_MAIN_JAVA = Path.of(
            "..", "hunyuan-base", "src", "main", "java");
    private static final Path BASE_MAIN_RESOURCES = Path.of(
            "..", "hunyuan-base", "src", "main", "resources");
    private static final Path ADMIN_MAIN_JAVA = Path.of("src", "main", "java");

    @Test
    void retiredCapabilitySourcesStayRemoved() throws IOException {
        List<Path> retiredPaths = List.of(
                BASE_MAIN_JAVA.resolve(Path.of(
                        "com", "hunyuan", "sa", "base", "module", "support", "helpdoc")),
                BASE_MAIN_JAVA.resolve(Path.of(
                        "com", "hunyuan", "sa", "base", "module", "support", "feedback")),
                BASE_MAIN_JAVA.resolve(Path.of(
                        "com", "hunyuan", "sa", "base", "module", "support", "datatracer")),
                BASE_MAIN_RESOURCES.resolve(Path.of("mapper", "support", "HelpDocDao.xml")),
                BASE_MAIN_RESOURCES.resolve(Path.of("mapper", "support", "FeedbackMapper.xml")),
                BASE_MAIN_RESOURCES.resolve(Path.of("mapper", "support", "DataTracerMapper.xml")),
                ADMIN_MAIN_JAVA.resolve(Path.of(
                        "com", "hunyuan", "sa", "admin", "module", "system", "support",
                        "AdminHelpDocController.java")));

        for (Path retiredPath : retiredPaths) {
            if (Files.notExists(retiredPath)) {
                continue;
            }
            if (Files.isDirectory(retiredPath)) {
                try (var files = Files.walk(retiredPath)) {
                    assertThat(files.filter(Files::isRegularFile).toList())
                            .as("已退役的平台评估能力不得恢复生产文件：%s", retiredPath)
                            .isEmpty();
                }
                continue;
            }
            assertThat(retiredPath)
                    .as("已退役的平台评估能力不得恢复生产文件：%s", retiredPath)
                    .doesNotExist();
        }
    }

    @Test
    void productionSourcesDoNotRestoreRetiredRoutesOrPermissions() throws IOException {
        for (Path sourceRoot : List.of(BASE_MAIN_JAVA, ADMIN_MAIN_JAVA)) {
            try (var files = Files.walk(sourceRoot)) {
                for (Path sourceFile : files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .toList()) {
                    assertThat(Files.readString(sourceFile, StandardCharsets.UTF_8))
                            .as("已退役的平台入口不得恢复：%s", sourceFile)
                            .doesNotContain(
                                    "module.support.helpdoc",
                                    "module.support.feedback",
                                    "module.support.datatracer",
                                    "\"/helpDoc/",
                                    "\"/feedback/",
                                    "\"/dataTracer/",
                                    "\"support:helpDoc:",
                                    "\"support:helpDocCatalog:");
                }
            }
        }
    }

    @Test
    void transactionalMailBoundaryRemainsAvailable() {
        assertThat(PlatformMailApplicationService.class)
                .isAssignableTo(PlatformMailFacade.class);
    }
}
