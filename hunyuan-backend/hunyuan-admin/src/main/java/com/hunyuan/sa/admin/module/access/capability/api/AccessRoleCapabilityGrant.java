package com.hunyuan.sa.admin.module.access.capability.api;

import java.util.List;

/**
 * 角色当前授权与可选能力目录。
 */
public record AccessRoleCapabilityGrant(
        Long roleId,
        List<Long> selectedCapabilityIds,
        List<AccessCapabilityNode> capabilityTree
) {
}
