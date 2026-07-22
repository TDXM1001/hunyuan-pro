package com.hunyuan.sa.admin.module.access.role.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 员工角色选择状态。
 */
@Schema(description = "员工角色选择状态")
public record AccessRoleSelection(
        @Schema(description = "角色编号") Long roleId,
        @Schema(description = "角色名称") String roleName,
        @Schema(description = "稳定角色编码") String roleCode,
        @Schema(description = "角色备注") String remark,
        @Schema(description = "是否已分配给员工") Boolean selected
) {
}
