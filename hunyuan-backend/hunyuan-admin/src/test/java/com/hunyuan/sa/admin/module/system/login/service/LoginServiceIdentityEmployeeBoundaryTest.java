package com.hunyuan.sa.admin.module.system.login.service;

import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeDirectoryFacade;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class LoginServiceIdentityEmployeeBoundaryTest {

    @Test
    void 登录服务只通过员工目录公开接口读取账号() {
        Field employeeDirectoryField = Arrays.stream(LoginService.class.getDeclaredFields())
                .filter(field -> field.getType().equals(EmployeeDirectoryFacade.class))
                .findFirst()
                .orElse(null);

        assertThat(employeeDirectoryField).as("登录服务必须注入员工目录公开接口").isNotNull();
        assertThat(Arrays.stream(LoginService.class.getDeclaredFields())
                .map(Field::getType)
                .map(Class::getName))
                .noneMatch(typeName -> typeName.equals("com.hunyuan.sa.admin.module.system.employee.service.EmployeeService")
                        || typeName.startsWith("com.hunyuan.sa.admin.module.system.employee.dao.")
                        || typeName.equals("com.hunyuan.sa.admin.module.system.employee.domain.entity.EmployeeEntity"));
    }
}
