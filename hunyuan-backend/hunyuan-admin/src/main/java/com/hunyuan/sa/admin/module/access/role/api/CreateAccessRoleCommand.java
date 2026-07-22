package com.hunyuan.sa.admin.module.access.role.api;

/**
 * 创建角色命令。
 */
public record CreateAccessRoleCommand(
        String roleName,
        String roleCode,
        String remark
) {
}
