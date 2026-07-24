package com.hunyuan.sa.admin.module.access.menu.api;

import java.time.LocalDateTime;

/**
 * 菜单目录对外公开的稳定模型。
 */
public record AccessMenu(
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
        LocalDateTime createTime,
        Long createUserId,
        LocalDateTime updateTime,
        Long updateUserId
) {
}
