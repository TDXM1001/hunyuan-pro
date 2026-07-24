package com.hunyuan.sa.admin.module.access.menu.api;

/**
 * 更新菜单命令。
 */
public record UpdateAccessMenuCommand(
        Long menuId,
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
        Long contextMenuId,
        Long operatorId
) {
}
