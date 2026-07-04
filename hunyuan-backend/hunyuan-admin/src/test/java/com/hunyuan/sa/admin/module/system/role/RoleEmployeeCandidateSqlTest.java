package com.hunyuan.sa.admin.module.system.role;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RoleEmployeeCandidateSqlTest {

    @Test
    void roleEmployeeCandidateQueryExcludesEmployeesAlreadyInRole() throws IOException {
        String controllerSource = Files.readString(Path.of("src/main/java/com/hunyuan/sa/admin/module/system/role/controller/RoleEmployeeController.java"));
        String serviceSource = Files.readString(Path.of("src/main/java/com/hunyuan/sa/admin/module/system/role/service/RoleEmployeeService.java"));
        String mapperSource = Files.readString(Path.of("src/main/resources/mapper/system/role/RoleEmployeeMapper.xml"));

        assertThat(controllerSource).contains("queryCandidateEmployee");
        assertThat(serviceSource).contains("queryCandidateEmployee");
        assertThat(mapperSource).contains("selectCandidateEmployeeByName");
        assertThat(mapperSource).contains("NOT EXISTS");
        assertThat(mapperSource).contains("candidate_role_employee.role_id = #{queryForm.roleId}");
    }
}
