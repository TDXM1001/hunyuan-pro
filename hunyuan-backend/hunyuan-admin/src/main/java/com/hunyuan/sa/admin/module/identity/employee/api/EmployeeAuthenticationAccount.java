package com.hunyuan.sa.admin.module.identity.employee.api;

/**
 * Authentication-only employee account data.
 *
 * <p>The password hash and employee UID must not be reused by general employee
 * queries or serialized by employee management APIs.</p>
 */
public record EmployeeAuthenticationAccount(
        Long employeeId,
        String employeeUid,
        String loginName,
        String passwordHash,
        String actualName,
        String avatar,
        Integer gender,
        String phone,
        String email,
        Long departmentId,
        Long positionId,
        Boolean administrator,
        Boolean disabled,
        Boolean deleted,
        String remark
) {
}
