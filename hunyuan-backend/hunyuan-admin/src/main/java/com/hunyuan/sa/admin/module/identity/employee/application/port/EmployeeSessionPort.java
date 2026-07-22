package com.hunyuan.sa.admin.module.identity.employee.application.port;

public interface EmployeeSessionPort {

    void clearCache(Long employeeId);

    void logout(Long employeeId);
}
