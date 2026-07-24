package com.hunyuan.sa.admin.module.identity.employee.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 当前登录员工更新头像的 HTTP 请求。
 */
public record EmployeeAvatarRequest(
        @Schema(description = "头像文件引用")
        @NotBlank(message = "头像不能为空哦")
        String avatar
) {
}
