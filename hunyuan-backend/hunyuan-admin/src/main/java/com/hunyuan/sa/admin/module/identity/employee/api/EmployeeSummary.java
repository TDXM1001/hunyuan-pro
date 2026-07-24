package com.hunyuan.sa.admin.module.identity.employee.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** 不包含认证秘密和管理员状态的员工公开摘要。 */
@Schema(description = "员工管理公开摘要，不包含认证秘密、删除标记和超级管理员标记")
public record EmployeeSummary(
        @Schema(description = "员工id")
        Long employeeId,
        @Schema(description = "登录账号")
        String loginName,
        @Schema(description = "姓名")
        String actualName,
        @Schema(description = "头像访问地址")
        String avatar,
        @Schema(description = "性别")
        Integer gender,
        @Schema(description = "手机号")
        String phone,
        @Schema(description = "邮箱账号")
        String email,
        @Schema(description = "部门id")
        Long departmentId,
        @Schema(description = "部门路径名称")
        String departmentName,
        @Schema(description = "职务级别ID")
        Long positionId,
        @Schema(description = "是否被禁用")
        Boolean disabled,
        @Schema(description = "创建时间")
        LocalDateTime createTime
) {
    public EmployeeSummary withDepartmentName(String name) {
        return new EmployeeSummary(employeeId, loginName, actualName, avatar, gender, phone, email,
                departmentId, name, positionId, disabled, createTime);
    }

    public EmployeeSummary withAvatar(String resolvedAvatar) {
        return new EmployeeSummary(employeeId, loginName, actualName, resolvedAvatar, gender, phone, email,
                departmentId, departmentName, positionId, disabled, createTime);
    }
}
