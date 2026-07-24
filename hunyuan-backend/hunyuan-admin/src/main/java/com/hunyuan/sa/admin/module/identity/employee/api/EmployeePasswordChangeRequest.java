package com.hunyuan.sa.admin.module.identity.employee.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 当前登录员工修改密码的 HTTP 请求。
 */
public record EmployeePasswordChangeRequest(
        @Schema(description = "原密码")
        @NotBlank(message = "原密码不能为空哦")
        String oldPassword,

        @Schema(description = "新密码")
        @NotBlank(message = "新密码不能为空哦")
        String newPassword
) {

    public EmployeePasswordChangeCommand toCommand(Long employeeId) {
        return new EmployeePasswordChangeCommand(employeeId, oldPassword, newPassword);
    }
}
