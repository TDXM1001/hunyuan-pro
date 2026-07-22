package com.hunyuan.sa.admin.module.access.role.api;

import java.util.Set;

public record ReplaceEmployeeRolesCommand(
        Long employeeId,
        Set<Long> roleIds
) {
}
