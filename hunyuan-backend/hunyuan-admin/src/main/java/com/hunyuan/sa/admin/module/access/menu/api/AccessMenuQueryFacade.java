package com.hunyuan.sa.admin.module.access.menu.api;

import java.util.List;

/**
 * 菜单授权查询公开边界。
 */
public interface AccessMenuQueryFacade {

    /**
     * 查询全部未删除且已启用的菜单和能力项。
     */
    List<AccessMenu> listEnabledMenus();

    /**
     * 查询指定角色拥有的未删除菜单和能力项。
     */
    List<AccessMenu> listAuthorizedMenusByRoleIds(List<Long> roleIds);
}
