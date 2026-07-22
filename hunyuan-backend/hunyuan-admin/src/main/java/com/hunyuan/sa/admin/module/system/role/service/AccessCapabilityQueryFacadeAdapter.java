package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.access.authorization.api.AccessMenuItem;
import com.hunyuan.sa.admin.module.access.capability.api.AccessCapabilityQueryFacade;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenu;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenuQueryFacade;
import com.hunyuan.sa.admin.module.organization.OrganizationModuleAvailability;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基于旧菜单与角色菜单关系实现登录能力查询公开边界。
 */
@Service
public class AccessCapabilityQueryFacadeAdapter implements AccessCapabilityQueryFacade {

    private static final String ORGANIZATION_PATH = "/organization/directory";
    private static final String ORGANIZATION_CAPABILITY_PREFIX = "organization.department.";

    @Resource
    private AccessMenuQueryFacade accessMenuQueryFacade;

    @Resource
    private OrganizationModuleAvailability organizationModuleAvailability;

    @Override
    public List<AccessMenuItem> listAuthorizationItems(
            List<Long> roleIds,
            Boolean administratorFlag) {
        if (administratorFlag) {
            return filterDisabledModules(accessMenuQueryFacade.listEnabledMenus().stream()
                    .map(this::toAccessMenuItem)
                    .toList());
        }
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return filterDisabledModules(accessMenuQueryFacade.listAuthorizedMenusByRoleIds(roleIds).stream()
                .map(this::toAccessMenuItem)
                .toList());
    }

    private List<AccessMenuItem> filterDisabledModules(List<AccessMenuItem> items) {
        if (organizationModuleAvailability.enabled()) {
            return items;
        }
        return items.stream()
                .filter(item -> !ORGANIZATION_PATH.equals(item.getPath())
                        && (item.getApiPerms() == null
                        || !item.getApiPerms().startsWith(ORGANIZATION_CAPABILITY_PREFIX)))
                .toList();
    }

    private AccessMenuItem toAccessMenuItem(AccessMenu menu) {
        AccessMenuItem item = new AccessMenuItem();
        item.setMenuId(menu.menuId());
        item.setMenuName(menu.menuName());
        item.setMenuType(menu.menuType());
        item.setParentId(menu.parentId());
        item.setSort(menu.sort());
        item.setPath(menu.path());
        item.setComponent(menu.component());
        item.setFrameFlag(menu.frameFlag());
        item.setFrameUrl(menu.frameUrl());
        item.setCacheFlag(menu.cacheFlag());
        item.setVisibleFlag(menu.visibleFlag());
        item.setDisabledFlag(menu.disabledFlag());
        item.setPermsType(menu.permsType());
        item.setWebPerms(menu.webPerms());
        item.setApiPerms(menu.apiPerms());
        item.setIcon(menu.icon());
        item.setContextMenuId(menu.contextMenuId());
        item.setCreateTime(menu.createTime());
        item.setCreateUserId(menu.createUserId());
        item.setUpdateTime(menu.updateTime());
        item.setUpdateUserId(menu.updateUserId());
        return item;
    }
}
