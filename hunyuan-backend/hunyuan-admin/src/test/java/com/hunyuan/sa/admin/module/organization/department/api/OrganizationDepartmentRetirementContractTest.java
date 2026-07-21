package com.hunyuan.sa.admin.module.organization.department.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizationDepartmentRetirementContractTest {

    @Test
    void legacyControllerAndRuntimeReferencesShouldStayRetired() throws IOException {
        assertThatThrownBy(() -> Class.forName(
                "com.hunyuan.sa.admin.module.system.department.controller.DepartmentController"))
                .isInstanceOf(ClassNotFoundException.class);

        Path mainSource = Path.of("src", "main", "java");
        List<Path> sourceFiles;
        try (var files = Files.walk(mainSource)) {
            sourceFiles = files.filter(path -> path.toString().endsWith(".java")).toList();
        }

        for (Path sourceFile : sourceFiles) {
            String source = Files.readString(sourceFile, StandardCharsets.UTF_8);
            assertThat(source)
                    .as("旧部门兼容引用不得重新进入运行时代码：%s", sourceFile)
                    .doesNotContain("/department/")
                    .doesNotContain("system:department:")
                    .doesNotContain("module.system.department");
        }
    }
}
