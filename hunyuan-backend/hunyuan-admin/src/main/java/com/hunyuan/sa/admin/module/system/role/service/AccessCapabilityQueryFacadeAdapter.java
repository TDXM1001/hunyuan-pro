package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.access.authorization.api.AccessMenuItem;
import com.hunyuan.sa.admin.module.access.capability.api.AccessCapabilityQueryFacade;
import com.hunyuan.sa.admin.module.organization.OrganizationModuleAvailability;
import com.hunyuan.sa.admin.module.system.menu.dao.MenuDao;
import com.hunyuan.sa.admin.module.system.menu.domain.entity.MenuEntity;
import com.hunyuan.sa.admin.module.system.role.dao.RoleMenuDao;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
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
    private RoleMenuDao roleMenuDao;

    @Resource
    private MenuDao menuDao;

    @Resource
    private OrganizationModuleAvailability organizationModuleAvailability;

    @Override
    public List<AccessMenuItem> listAuthorizationItems(
            List<Long> roleIds,
            Boolean administratorFlag) {
        if (administratorFlag) {
            return filterDisabledModules(
                    SmartBeanUtil.copyList(
                            menuDao.queryMenuList(Boolean.FALSE, Boolean.FALSE, null),
                            AccessMenuItem.class));
        }
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        List<MenuEntity> menus = roleMenuDao.selectMenuListByRoleIdList(roleIds, Boolean.FALSE);
        return filterDisabledModules(SmartBeanUtil.copyList(menus, AccessMenuItem.class));
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
}
