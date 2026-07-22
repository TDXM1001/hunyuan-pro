package com.hunyuan.sa.admin.module.system.menu.service;

import com.hunyuan.sa.admin.module.access.menu.api.AccessMenu;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenuQueryFacade;
import com.hunyuan.sa.admin.module.system.menu.dao.MenuDao;
import com.hunyuan.sa.admin.module.system.menu.domain.vo.MenuVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 基于旧菜单表实现菜单授权查询公开边界。
 */
@Service
public class AccessMenuQueryFacadeAdapter implements AccessMenuQueryFacade {

    @Resource
    private MenuDao menuDao;

    @Override
    public List<AccessMenu> listEnabledMenus() {
        return menuDao.queryMenuList(Boolean.FALSE, Boolean.FALSE, null).stream()
                .map(this::toAccessMenu)
                .toList();
    }

    @Override
    public List<AccessMenu> listAuthorizedMenusByRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return menuDao.queryAuthorizedMenuListByRoleIds(roleIds).stream()
                .map(this::toAccessMenu)
                .toList();
    }

    private AccessMenu toAccessMenu(MenuVO menu) {
        return new AccessMenu(
                menu.getMenuId(),
                menu.getMenuName(),
                menu.getMenuType(),
                menu.getParentId(),
                menu.getSort(),
                menu.getPath(),
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
}
