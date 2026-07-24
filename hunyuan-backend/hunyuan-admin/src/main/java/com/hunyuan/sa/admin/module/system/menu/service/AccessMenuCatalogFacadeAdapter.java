package com.hunyuan.sa.admin.module.system.menu.service;

import cn.hutool.core.collection.CollectionUtil;
import com.google.common.collect.Lists;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenu;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenuCatalogFacade;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenuFailure;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenuNode;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenuResult;
import com.hunyuan.sa.admin.module.access.menu.api.CreateAccessMenuCommand;
import com.hunyuan.sa.admin.module.access.menu.api.UpdateAccessMenuCommand;
import com.hunyuan.sa.admin.module.system.menu.constant.MenuTypeEnum;
import com.hunyuan.sa.admin.module.system.menu.dao.MenuDao;
import com.hunyuan.sa.admin.module.system.menu.domain.entity.MenuEntity;
import com.hunyuan.sa.admin.module.system.menu.domain.vo.MenuVO;
import com.hunyuan.sa.base.common.domain.RequestUrlVO;
import com.hunyuan.sa.base.common.util.SmartStringUtil;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于旧菜单表实现菜单目录公开边界。
 */
@Service
public class AccessMenuCatalogFacadeAdapter implements AccessMenuCatalogFacade {

    @Resource
    private MenuDao menuDao;

    @Resource
    private List<RequestUrlVO> authUrl;

    @Override
    public synchronized AccessMenuResult<Long> create(CreateAccessMenuCommand command) {
        if (isRouteIdRequired(command.menuType(), command.frameFlag())
                && SmartStringUtil.isEmpty(command.routeId())) {
            return AccessMenuResult.failure(
                    AccessMenuFailure.ROUTE_ID_REQUIRED,
                    "普通页面菜单必须填写稳定路由标识");
        }
        if (hasDuplicatedMenuName(command.menuName(), command.parentId(), null)) {
            return AccessMenuResult.failure(
                    AccessMenuFailure.MENU_NAME_DUPLICATED,
                    "菜单名称已存在");
        }
        if (hasDuplicatedWebPerms(command.webPerms(), null)) {
            return AccessMenuResult.failure(
                    AccessMenuFailure.WEB_PERMISSION_DUPLICATED,
                    "前端权限字符串已存在");
        }
        if (hasDuplicatedRouteId(command.routeId(), null)) {
            return AccessMenuResult.failure(
                    AccessMenuFailure.ROUTE_ID_DUPLICATED,
                    "稳定路由标识已存在");
        }

        MenuEntity menuEntity = toEntity(command);
        menuDao.insert(menuEntity);
        return AccessMenuResult.success(menuEntity.getMenuId());
    }

    @Override
    public synchronized AccessMenuResult<Void> update(UpdateAccessMenuCommand command) {
        MenuEntity currentMenu = menuDao.selectById(command.menuId());
        if (currentMenu == null) {
            return AccessMenuResult.failure(AccessMenuFailure.MENU_NOT_FOUND, "菜单不存在");
        }
        if (Boolean.TRUE.equals(currentMenu.getDeletedFlag())) {
            return AccessMenuResult.failure(AccessMenuFailure.MENU_DELETED, "菜单已被删除");
        }
        if (hasDuplicatedMenuName(command.menuName(), command.parentId(), command.menuId())) {
            return AccessMenuResult.failure(
                    AccessMenuFailure.MENU_NAME_DUPLICATED,
                    "菜单名称已存在");
        }
        if (hasDuplicatedWebPerms(command.webPerms(), command.menuId())) {
            return AccessMenuResult.failure(
                    AccessMenuFailure.WEB_PERMISSION_DUPLICATED,
                    "前端权限字符串已存在");
        }
        if (hasDuplicatedRouteId(command.routeId(), command.menuId())) {
            return AccessMenuResult.failure(
                    AccessMenuFailure.ROUTE_ID_DUPLICATED,
                    "稳定路由标识已存在");
        }
        if (command.menuId().equals(command.parentId())) {
            return AccessMenuResult.failure(
                    AccessMenuFailure.MENU_PARENT_SELF,
                    "上级菜单不能为自己");
        }

        menuDao.updateById(toEntity(command));
        return AccessMenuResult.success(null);
    }

    @Override
    public synchronized AccessMenuResult<Void> delete(List<Long> menuIds, Long operatorId) {
        if (CollectionUtils.isEmpty(menuIds)) {
            return AccessMenuResult.failure(
                    AccessMenuFailure.MENU_IDS_EMPTY,
                    "所选菜单不能为空");
        }

        menuDao.deleteByMenuIdList(menuIds, operatorId, Boolean.TRUE);
        recursiveDeleteChildren(menuIds, operatorId);
        return AccessMenuResult.success(null);
    }

    @Override
    public List<AccessMenu> list() {
        List<MenuVO> menuList = menuDao.queryMenuList(Boolean.FALSE, null, null);
        Map<Long, List<MenuVO>> parentMap = menuList.stream()
                .collect(Collectors.groupingBy(MenuVO::getParentId, Collectors.toList()));
        return flattenReachableMenus(parentMap, NumberUtils.LONG_ZERO).stream()
                .map(this::toAccessMenu)
                .toList();
    }

    @Override
    public AccessMenuResult<AccessMenu> get(Long menuId) {
        MenuEntity menuEntity = menuDao.selectById(menuId);
        if (menuEntity == null) {
            return AccessMenuResult.failure(AccessMenuFailure.MENU_NOT_FOUND, "菜单不存在");
        }
        if (Boolean.TRUE.equals(menuEntity.getDeletedFlag())) {
            return AccessMenuResult.failure(AccessMenuFailure.MENU_DELETED, "菜单已被删除");
        }
        return AccessMenuResult.success(toAccessMenu(menuEntity));
    }

    @Override
    public List<AccessMenuNode> tree(Boolean onlyMenu) {
        List<Integer> menuTypes = Lists.newArrayList();
        if (Boolean.TRUE.equals(onlyMenu)) {
            menuTypes = Lists.newArrayList(
                    MenuTypeEnum.CATALOG.getValue(),
                    MenuTypeEnum.MENU.getValue());
        }

        List<MenuVO> menuList = menuDao.queryMenuList(Boolean.FALSE, null, menuTypes);
        Map<Long, List<MenuVO>> parentMap = menuList.stream()
                .collect(Collectors.groupingBy(MenuVO::getParentId, Collectors.toList()));
        return buildMenuTree(parentMap, NumberUtils.LONG_ZERO);
    }

    @Override
    public List<RequestUrlVO> listAuthorizationUrls() {
        return authUrl;
    }

    private void recursiveDeleteChildren(List<Long> menuIds, Long operatorId) {
        List<Long> childMenuIds = menuDao.selectMenuIdByParentIdList(menuIds);
        if (CollectionUtil.isEmpty(childMenuIds)) {
            return;
        }
        menuDao.deleteByMenuIdList(childMenuIds, operatorId, Boolean.TRUE);
        recursiveDeleteChildren(childMenuIds, operatorId);
    }

    private boolean hasDuplicatedMenuName(String menuName, Long parentId, Long currentMenuId) {
        MenuEntity sameNameMenu = menuDao.getByMenuName(menuName, parentId, Boolean.FALSE);
        return sameNameMenu != null
                && (currentMenuId == null || !sameNameMenu.getMenuId().equals(currentMenuId));
    }

    private boolean hasDuplicatedWebPerms(String webPerms, Long currentMenuId) {
        if (SmartStringUtil.isEmpty(webPerms)) {
            return false;
        }
        MenuEntity sameWebPermsMenu = menuDao.getByWebPerms(webPerms, Boolean.FALSE);
        return sameWebPermsMenu != null
                && (currentMenuId == null || !sameWebPermsMenu.getMenuId().equals(currentMenuId));
    }

    private boolean hasDuplicatedRouteId(String routeId, Long currentMenuId) {
        if (SmartStringUtil.isEmpty(routeId)) {
            return false;
        }
        MenuEntity sameRouteMenu = menuDao.getByRouteId(routeId, Boolean.FALSE);
        return sameRouteMenu != null
                && (currentMenuId == null || !sameRouteMenu.getMenuId().equals(currentMenuId));
    }

    private boolean isRouteIdRequired(Integer menuType, Boolean frameFlag) {
        return MenuTypeEnum.MENU.getValue().equals(menuType) && !Boolean.TRUE.equals(frameFlag);
    }

    private List<MenuVO> flattenReachableMenus(
            Map<Long, List<MenuVO>> parentMap,
            Long parentId) {
        List<MenuVO> currentMenus = parentMap.getOrDefault(parentId, Lists.newArrayList());
        List<MenuVO> descendantMenus = Lists.newArrayList();
        currentMenus.forEach(menu ->
                descendantMenus.addAll(flattenReachableMenus(parentMap, menu.getMenuId())));
        currentMenus.addAll(descendantMenus);
        return currentMenus;
    }

    private List<AccessMenuNode> buildMenuTree(
            Map<Long, List<MenuVO>> parentMap,
            Long parentId) {
        return parentMap.getOrDefault(parentId, Lists.newArrayList()).stream()
                .map(menu -> toAccessMenuNode(
                        menu,
                        buildMenuTree(parentMap, menu.getMenuId())))
                .toList();
    }

    private MenuEntity toEntity(CreateAccessMenuCommand command) {
        MenuEntity entity = new MenuEntity();
        copyDefinition(
                entity,
                command.menuName(),
                command.menuType(),
                command.parentId(),
                command.sort(),
                command.path(),
                command.routeId(),
                command.component(),
                command.frameFlag(),
                command.frameUrl(),
                command.cacheFlag(),
                command.visibleFlag(),
                command.disabledFlag(),
                command.permsType(),
                command.webPerms(),
                command.apiPerms(),
                command.icon(),
                command.contextMenuId());
        entity.setCreateUserId(command.operatorId());
        return entity;
    }

    private MenuEntity toEntity(UpdateAccessMenuCommand command) {
        MenuEntity entity = new MenuEntity();
        entity.setMenuId(command.menuId());
        copyDefinition(
                entity,
                command.menuName(),
                command.menuType(),
                command.parentId(),
                command.sort(),
                command.path(),
                command.routeId(),
                command.component(),
                command.frameFlag(),
                command.frameUrl(),
                command.cacheFlag(),
                command.visibleFlag(),
                command.disabledFlag(),
                command.permsType(),
                command.webPerms(),
                command.apiPerms(),
                command.icon(),
                command.contextMenuId());
        entity.setUpdateUserId(command.operatorId());
        return entity;
    }

    private void copyDefinition(
            MenuEntity entity,
            String menuName,
            Integer menuType,
            Long parentId,
            Integer sort,
            String path,
            String routeId,
            String component,
            Boolean frameFlag,
            String frameUrl,
            Boolean cacheFlag,
            Boolean visibleFlag,
            Boolean disabledFlag,
            Integer permsType,
            String webPerms,
            String apiPerms,
            String icon,
            Long contextMenuId) {
        entity.setMenuName(menuName);
        entity.setMenuType(menuType);
        entity.setParentId(parentId);
        entity.setSort(sort);
        entity.setPath(path);
        entity.setRouteId(routeId);
        entity.setComponent(component);
        entity.setFrameFlag(frameFlag);
        entity.setFrameUrl(frameUrl);
        entity.setCacheFlag(cacheFlag);
        entity.setVisibleFlag(visibleFlag);
        entity.setDisabledFlag(disabledFlag);
        entity.setPermsType(permsType);
        entity.setWebPerms(webPerms);
        entity.setApiPerms(apiPerms);
        entity.setIcon(icon);
        entity.setContextMenuId(contextMenuId);
    }

    private AccessMenu toAccessMenu(MenuVO menu) {
        return new AccessMenu(
                menu.getMenuId(),
                menu.getMenuName(),
                menu.getMenuType(),
                menu.getParentId(),
                menu.getSort(),
                menu.getPath(),
                menu.getRouteId(),
                menu.getComponent(),
                menu.getFrameFlag(),
                menu.getFrameUrl(),
                menu.getCacheFlag(),
                menu.getVisibleFlag(),
                menu.getDisabledFlag(),
                menu.getPermsType(),
                menu.getWebPerms(),
                menu.getApiPerms(),
                menu.getIcon(),
                menu.getContextMenuId(),
                menu.getCreateTime(),
                menu.getCreateUserId(),
                menu.getUpdateTime(),
                menu.getUpdateUserId());
    }

    private AccessMenu toAccessMenu(MenuEntity menu) {
        return new AccessMenu(
                menu.getMenuId(),
                menu.getMenuName(),
                menu.getMenuType(),
                menu.getParentId(),
                menu.getSort(),
                menu.getPath(),
                menu.getRouteId(),
                menu.getComponent(),
                menu.getFrameFlag(),
                menu.getFrameUrl(),
                menu.getCacheFlag(),
                menu.getVisibleFlag(),
                menu.getDisabledFlag(),
                menu.getPermsType(),
                menu.getWebPerms(),
                menu.getApiPerms(),
                menu.getIcon(),
                menu.getContextMenuId(),
                menu.getCreateTime(),
                menu.getCreateUserId(),
                menu.getUpdateTime(),
                menu.getUpdateUserId());
    }

    private AccessMenuNode toAccessMenuNode(
            MenuVO menu,
            List<AccessMenuNode> children) {
        return new AccessMenuNode(
                menu.getMenuId(),
                menu.getMenuName(),
                menu.getMenuType(),
                menu.getParentId(),
                menu.getSort(),
                menu.getPath(),
                menu.getRouteId(),
                menu.getComponent(),
                menu.getFrameFlag(),
                menu.getFrameUrl(),
                menu.getCacheFlag(),
                menu.getVisibleFlag(),
                menu.getDisabledFlag(),
                menu.getPermsType(),
                menu.getWebPerms(),
                menu.getApiPerms(),
                menu.getIcon(),
                menu.getContextMenuId(),
                menu.getCreateTime(),
                menu.getCreateUserId(),
                menu.getUpdateTime(),
                menu.getUpdateUserId(),
                children);
    }
}
