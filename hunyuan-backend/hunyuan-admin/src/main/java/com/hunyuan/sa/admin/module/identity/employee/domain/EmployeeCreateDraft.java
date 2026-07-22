package com.hunyuan.sa.admin.module.identity.employee.domain;

public record EmployeeCreateDraft(
        String employeeUid,
        String loginName,
        String passwordHash,
        String actualName,
        Integer gender,
        String phone,
        String email,
        Long departmentId,
        Long positionId,
        Boolean disabled,
        String remark
) {
}
