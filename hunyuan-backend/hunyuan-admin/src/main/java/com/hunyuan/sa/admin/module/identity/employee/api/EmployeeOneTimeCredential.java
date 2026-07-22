package com.hunyuan.sa.admin.module.identity.employee.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "仅在创建或密码重置成功响应中返回的一次性员工凭据")
public record EmployeeOneTimeCredential(
        @Schema(description = "员工id")
        Long employeeId,
        @Schema(description = "仅本次响应可见的临时密码")
        String temporaryPassword
) {
}
