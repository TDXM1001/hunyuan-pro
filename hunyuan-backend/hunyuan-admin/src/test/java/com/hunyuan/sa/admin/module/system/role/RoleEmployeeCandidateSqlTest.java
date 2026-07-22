package com.hunyuan.sa.admin.module.system.role;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RoleEmployeeCandidateSqlTest {

    @Test
    void roleEmployeeCandidateQueryExcludesEmployeesAlreadyInRole() throws IOException {
        String controllerSource = Files.readString(Path.of(
                "src/main/java/com/hunyuan/sa/admin/module/access/role/api/"
                        + "AccessRoleMembershipController.java"));
        String adapterSource = Files.readString(Path.of(
                "src/main/java/com/hunyuan/sa/admin/module/system/role/service/"
                        + "AccessRoleMembershipFacadeAdapter.java"));
        String mapperSource = Files.readString(Path.of("src/main/resources/mapper/system/role/RoleEmployeeMapper.xml"));

        assertThat(controllerSource).contains("queryCandidates");
        assertThat(adapterSource).contains("queryCandidates");
        assertThat(mapperSource).contains("selectCandidateEmployeeByName");
        assertThat(mapperSource).contains("NOT EXISTS");
        assertThat(mapperSource).contains("candidate_role_employee.role_id = #{queryForm.roleId}");
        assertThat(mapperSource).contains("t_employee.deleted_flag = false");
        assertThat(mapperSource).doesNotContain("t_employee.login_pwd");
    }
}
