package com.hunyuan.sa.admin.module.access.role.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 访问控制模块公开的角色成员摘要。
 */
@Schema(description = "角色成员摘要，不包含认证秘密和管理员状态")
public record AccessRoleMember(
        @Schema(description = "员工编号") Long employeeId,
        @Schema(description = "登录账号") String loginName,
        @Schema(description = "员工姓名") String actualName,
        @Schema(description = "头像访问地址") String avatar,
        @Schema(description = "性别") Integer gender,
        @Schema(description = "手机号") String phone,
        @Schema(description = "邮箱") String email,
        @Schema(description = "部门编号") Long departmentId,
        @Schema(description = "部门路径名称") String departmentName,
        @Schema(description = "职务级别编号") Long positionId,
        @Schema(description = "是否禁用") Boolean disabled,
        @Schema(description = "创建时间") LocalDateTime createTime
) {
    public AccessRoleMember withDepartmentName(String name) {
        return new AccessRoleMember(
                employeeId,
                loginName,
                actualName,
                avatar,
                gender,
                phone,
                email,
                departmentId,
                name,
                positionId,
                disabled,
                createTime);
    }
}
