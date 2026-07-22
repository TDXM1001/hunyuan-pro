package com.hunyuan.sa.admin.module.identity.employee.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EmployeeDeleteCommand(
        @Schema(description = "员工id")
        @NotEmpty(message = "员工id不能为空")
        @Size(max = 99, message = "一次最多删除99个员工")
        List<Long> employeeIds
) {
}
