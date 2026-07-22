package com.hunyuan.sa.admin.module.access.capability.api;

/**
 * 角色能力授权的公开用例边界。
 */
public interface AccessCapabilityGrantFacade {

    AccessCapabilityGrantResult<Void> replaceRoleCapabilities(ReplaceRoleCapabilitiesCommand command);

    AccessRoleCapabilityGrant getRoleCapabilities(Long roleId);
}
