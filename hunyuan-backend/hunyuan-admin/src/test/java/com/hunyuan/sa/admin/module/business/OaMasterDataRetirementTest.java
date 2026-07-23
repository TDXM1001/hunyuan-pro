package com.hunyuan.sa.admin.module.business;

import com.hunyuan.sa.admin.constant.AdminSwaggerTagConst;
import com.hunyuan.sa.base.module.support.datatracer.constant.DataTracerTypeEnum;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OaMasterDataRetirementTest {

    private static final Path MAIN_JAVA = Path.of("src", "main", "java");
    private static final Path MAIN_RESOURCES = Path.of("src", "main", "resources");

    @Test
    void oaMasterDataProductionSourcesAreRemoved() throws IOException {
        List<Path> retiredDirectories = List.of(
                MAIN_JAVA.resolve(Path.of(
                        "com", "hunyuan", "sa", "admin", "module", "business", "oa", "enterprise")),
                MAIN_JAVA.resolve(Path.of(
                        "com", "hunyuan", "sa", "admin", "module", "business", "oa", "bank")),
                MAIN_JAVA.resolve(Path.of(
                        "com", "hunyuan", "sa", "admin", "module", "business", "oa", "invoice")),
                MAIN_JAVA.resolve(Path.of(
                        "com", "hunyuan", "sa", "admin", "module", "business", "oa", "notice")),
                MAIN_RESOURCES.resolve(Path.of("mapper", "business", "oa", "enterprise")),
                MAIN_RESOURCES.resolve(Path.of("mapper", "business", "oa", "bank")),
                MAIN_RESOURCES.resolve(Path.of("mapper", "business", "oa", "invoice")),
                MAIN_RESOURCES.resolve(Path.of("mapper", "business", "oa", "notice")));

        for (Path retiredDirectory : retiredDirectories) {
            if (Files.notExists(retiredDirectory)) {
                continue;
            }
            try (var files = Files.walk(retiredDirectory)) {
                assertThat(files.filter(Files::isRegularFile).toList())
                        .as("OA 主数据生产目录不得恢复文件：%s", retiredDirectory)
                        .isEmpty();
            }
        }
    }

    @Test
    void productionSourcesDoNotRestoreRetiredRoutesOrPermissions() throws IOException {
        try (var files = Files.walk(MAIN_JAVA)) {
            List<Path> productionSources = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path sourceFile : productionSources) {
                assertThat(Files.readString(sourceFile, StandardCharsets.UTF_8))
                        .as("OA 主数据入口必须保持退役：%s", sourceFile)
                        .doesNotContain(
                                "module.business.oa.enterprise",
                                "module.business.oa.bank",
                                "module.business.oa.invoice",
                                "module.business.oa.notice",
                                "\"/oa/enterprise",
                                "\"/oa/bank",
                                "\"/oa/invoice",
                                "\"/oa/notice",
                                "\"/oa/noticeType",
                                "\"/invoice/delete",
                                "\"oa:enterprise:",
                                "\"oa:bank:",
                                "\"oa:invoice:",
                                "\"oa:notice:");
            }
        }
    }

    @Test
    void sharedConstantsDoNotRestoreRetiredOaCapabilities() {
        assertThat(AdminSwaggerTagConst.Business.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("OA_ENTERPRISE", "OA_BANK", "OA_INVOICE", "OA_NOTICE");

        assertThat(DataTracerTypeEnum.values())
                .extracting(DataTracerTypeEnum::getValue)
                .doesNotContain(2, 3);
    }
}
