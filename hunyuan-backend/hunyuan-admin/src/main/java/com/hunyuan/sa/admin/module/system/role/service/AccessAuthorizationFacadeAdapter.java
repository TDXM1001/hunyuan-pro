package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.access.authorization.api.AccessAuthorizationFacade;
import com.hunyuan.sa.admin.module.access.authorization.api.AccessAuthorizationSnapshot;
import com.hunyuan.sa.admin.module.access.authorization.api.AccessMenuItem;
import com.hunyuan.sa.admin.module.access.capability.api.AccessCapabilityQueryFacade;
import com.hunyuan.sa.admin.module.access.role.api.AccessRole;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleMembershipFacade;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于旧角色成员关系实现访问授权公开边界。
 */
@Service
public class AccessAuthorizationFacadeAdapter implements AccessAuthorizationFacade {

    @Resource
    private AccessRoleMembershipFacade roleMembershipFacade;

    @Resource
    private AccessCapabilityQueryFacade accessCapabilityQueryFacade;

    @Override
    public AccessAuthorizationSnapshot loadEmployeeAuthorization(Long employeeId, Boolean administratorFlag) {
        List<AccessRole> roles = roleMembershipFacade.listEmployeeRoles(employeeId);
        List<Long> roleIds = roles.stream().map(AccessRole::roleId).toList();
        List<AccessMenuItem> authorizationItems =
                accessCapabilityQueryFacade.listAuthorizationItems(roleIds, administratorFlag);

        Set<String> roleCodes = roles.stream()
                .map(AccessRole::roleCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        Set<String> capabilityCodes = new HashSet<>();
        for (AccessMenuItem item : authorizationItems) {
            if (item.getPermsType() == null || StringUtils.isBlank(item.getApiPerms())) {
                continue;
            }
            capabilityCodes.addAll(Arrays.asList(item.getApiPerms().split(",")));
        }

        return new AccessAuthorizationSnapshot(
                roleCodes,
                capabilityCodes,
                authorizationItems);
    }
}
