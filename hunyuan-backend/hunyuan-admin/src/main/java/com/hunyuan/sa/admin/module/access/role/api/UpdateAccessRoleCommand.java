package com.hunyuan.sa.admin.module.access.role.api;

/**
 * 更新角色命令。
 */
public record UpdateAccessRoleCommand(
        Long roleId,
        String roleName,
        String roleCode,
        String remark
) {
}
