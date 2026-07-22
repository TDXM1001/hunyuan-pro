package com.hunyuan.sa.admin.module.identity.employee.domain;

public record EmployeeProfileUpdate(
        Long employeeId,
        String loginName,
        String actualName,
        Integer gender,
        String phone,
        String email,
        Long departmentId,
        Long positionId,
        String remark
) {
}
