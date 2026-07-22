package com.hunyuan.sa.admin.module.identity.employee.api;

/**
 * Purpose-specific employee data exposed to collaborating modules.
 * Authentication secrets and mutable administration details stay internal.
 */
public record EmployeeCollaborationProfile(
        Long employeeId,
        String actualName,
        Long departmentId,
        Boolean administrator,
        Boolean disabled,
        Boolean deleted
) {
}
