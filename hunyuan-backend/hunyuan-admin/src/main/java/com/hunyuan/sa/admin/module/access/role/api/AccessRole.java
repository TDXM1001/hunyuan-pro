package com.hunyuan.sa.admin.module.access.role.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 访问控制模块公开的角色摘要。
 */
@Schema(description = "访问控制角色摘要")
public record AccessRole(
        @Schema(description = "角色编号") Long roleId,
        @Schema(description = "角色名称") String roleName,
        @Schema(description = "稳定角色编码") String roleCode,
        @Schema(description = "角色备注") String remark
) {
}
