package com.hunyuan.sa.admin.module.system.role.service;

import com.google.common.collect.Lists;
import com.hunyuan.sa.admin.module.access.capability.api.AccessCapabilityGrantFacade;
import com.hunyuan.sa.admin.module.access.capability.api.AccessCapabilityGrantFailure;
import com.hunyuan.sa.admin.module.access.capability.api.AccessCapabilityGrantResult;
import com.hunyuan.sa.admin.module.access.capability.api.AccessCapabilityNode;
import com.hunyuan.sa.admin.module.access.capability.api.AccessRoleCapabilityGrant;
import com.hunyuan.sa.admin.module.access.capability.api.ReplaceRoleCapabilitiesCommand;
import com.hunyuan.sa.admin.module.organization.OrganizationModuleAvailability;
import com.hunyuan.sa.admin.module.system.menu.dao.MenuDao;
import com.hunyuan.sa.admin.module.system.menu.domain.vo.MenuVO;
import com.hunyuan.sa.admin.module.system.role.dao.RoleDao;
import com.hunyuan.sa.admin.module.system.role.dao.RoleMenuDao;
import com.hunyuan.sa.admin.module.system.role.domain.entity.RoleMenuEntity;
import com.hunyuan.sa.admin.module.system.role.manager.RoleMenuManager;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于旧角色菜单关系表实现角色能力授权公开边界。
 */
@Service
public class AccessCapabilityGrantFacadeAdapter implements AccessCapabilityGrantFacade {

    private static final String ORGANIZATION_PATH = "/organization/directory";
    private static final String ORGANIZATION_CAPABILITY_PREFIX = "organization.department.";

    @Resource
    private RoleDao roleDao;

    @Resource
    private RoleMenuDao roleMenuDao;

    @Resource
    private RoleMenuManager roleMenuManager;

    @Resource
    private MenuDao menuDao;

    @Resource
    private OrganizationModuleAvailability organizationModuleAvailability;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccessCapabilityGrantResult<Void> replaceRoleCapabilities(ReplaceRoleCapabilitiesCommand command) {
        if (roleDao.selectById(command.roleId()) == null) {
            return AccessCapabilityGrantResult.failure(AccessCapabilityGrantFailure.ROLE_NOT_FOUND);
        }

        List<RoleMenuEntity> grants = command.capabilityIds().stream()
                .map(capabilityId -> toRoleMenuEntity(command.roleId(), capabilityId))
                .toList();
        roleMenuManager.updateRoleMenu(command.roleId(), grants);
        return AccessCapabilityGrantResult.success(null);
    }

    @Override
    public AccessRoleCapabilityGrant getRoleCapabilities(Long roleId) {
        List<Long> selectedCapabilityIds = roleMenuDao.queryMenuIdByRoleId(roleId);
        List<MenuVO> capabilities = filterDisabledModules(
                menuDao.queryMenuList(Boolean.FALSE, Boolean.FALSE, null));
        Map<Long, List<MenuVO>> parentMap = capabilities.stream()
                .collect(Collectors.groupingBy(MenuVO::getParentId));
        return new AccessRoleCapabilityGrant(
                roleId,
                selectedCapabilityIds,
                buildCapabilityTree(parentMap, NumberUtils.LONG_ZERO));
    }

    private RoleMenuEntity toRoleMenuEntity(Long roleId, Long capabilityId) {
        RoleMenuEntity entity = new RoleMenuEntity();
        entity.setRoleId(roleId);
        entity.setMenuId(capabilityId);
        return entity;
    }

    private List<AccessCapabilityNode> buildCapabilityTree(
            Map<Long, List<MenuVO>> parentMap,
            Long parentId) {
        return parentMap.getOrDefault(parentId, Lists.newArrayList()).stream()
                .map(capability -> new AccessCapabilityNode(
                        capability.getMenuId(),
                        capability.getMenuName(),
                        capability.getContextMenuId(),
                        capability.getParentId(),
                        capability.getMenuType(),
                        buildCapabilityTree(parentMap, capability.getMenuId())))
                .toList();
    }

    private List<MenuVO> filterDisabledModules(List<MenuVO> capabilities) {
        if (organizationModuleAvailability.enabled()) {
            return capabilities;
        }
        return capabilities.stream()
                .filter(capability -> {
                    String path = capability.getPath();
                    String apiPerms = capability.getApiPerms();
                    return !ORGANIZATION_PATH.equals(path)
                            && (apiPerms == null
                            || !apiPerms.startsWith(ORGANIZATION_CAPABILITY_PREFIX));
                })
                .toList();
    }
}
