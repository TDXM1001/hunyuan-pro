package com.hunyuan.sa.admin.module.access.capability.api;

import java.util.List;

/**
 * 可授权能力目录节点。
 */
public record AccessCapabilityNode(
        Long capabilityId,
        String capabilityName,
        Long contextCapabilityId,
        Long parentId,
        Integer capabilityType,
        List<AccessCapabilityNode> children
) {
}
