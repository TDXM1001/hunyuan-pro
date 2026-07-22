package com.hunyuan.sa.admin.module.access.role.api;

import java.util.Set;

public record RemoveRoleEmployeesCommand(
        Long roleId,
        Set<Long> employeeIds
) {
}
