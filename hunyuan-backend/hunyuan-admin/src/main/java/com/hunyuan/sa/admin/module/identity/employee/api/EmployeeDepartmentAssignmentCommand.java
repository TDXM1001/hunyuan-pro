package com.hunyuan.sa.admin.module.identity.employee.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EmployeeDepartmentAssignmentCommand(
        @Schema(description = "员工id")
        @NotEmpty(message = "员工id不能为空")
        @Size(max = 99, message = "一次最多调整99个员工")
        List<Long> employeeIds,

        @Schema(description = "部门ID")
        @NotNull(message = "部门ID不能为空")
        Long departmentId
) {
}
