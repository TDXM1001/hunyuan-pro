package com.hunyuan.sa.admin.module.organization.position;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyPositionConsumerRetirementTest {

    private static final Path MAIN_JAVA = Path.of("src", "main", "java");

    @Test
    void legacyPositionSourcesAreRemoved() {
        List<Path> retiredSources = List.of(
                legacySource("controller", "PositionController.java"),
                legacySource("service", "PositionService.java"),
                legacySource("manager", "PositionManager.java"),
                legacySource("dao", "PositionDao.java"),
                legacySource("domain", "entity", "PositionEntity.java"),
                legacySource("domain", "form", "PositionAddForm.java"),
                legacySource("domain", "form", "PositionQueryForm.java"),
                legacySource("domain", "form", "PositionUpdateForm.java"),
                legacySource("domain", "vo", "PositionVO.java"));

        assertThat(retiredSources)
                .as("旧岗位兼容类必须保持删除状态")
                .allMatch(Files::notExists);
    }

    @Test
    void productionSourcesOnlyExposeStablePositionRoutesAndPermissions() throws IOException {
        try (var files = Files.walk(MAIN_JAVA)) {
            List<Path> productionSources = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path sourceFile : productionSources) {
                assertThat(Files.readString(sourceFile, StandardCharsets.UTF_8))
                        .as("旧岗位入口和权限码必须保持退役：%s", sourceFile)
                        .doesNotContain(
                                "module.system.position",
                                "\"/position/",
                                "system:position:");
            }
        }

        Path stableController = MAIN_JAVA.resolve(Path.of(
                "com", "hunyuan", "sa", "admin", "module", "organization",
                "position", "api", "OrganizationPositionController.java"));
        assertThat(Files.readString(stableController, StandardCharsets.UTF_8))
                .contains("/api/admin/v1/organization/positions")
                .contains("organization.position.read")
                .contains("organization.position.create")
                .contains("organization.position.update")
                .contains("organization.position.delete");
    }

    private Path legacySource(String... relativeSegments) {
        Path path = MAIN_JAVA.resolve(Path.of(
                "com", "hunyuan", "sa", "admin", "module", "system", "position"));
        for (String segment : relativeSegments) {
            path = path.resolve(segment);
        }
        return path;
    }
}
