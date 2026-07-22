package com.hunyuan.sa.admin.module.access.capability.api;

import java.util.List;

/**
 * 全量替换角色能力授权命令。
 */
public record ReplaceRoleCapabilitiesCommand(
        Long roleId,
        List<Long> capabilityIds
) {
}
