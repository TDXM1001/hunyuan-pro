package com.hunyuan.sa.admin.module.access.menu.api;

import com.hunyuan.sa.base.common.domain.RequestUrlVO;

import java.util.List;

/**
 * 菜单目录生命周期公开用例边界。
 */
public interface AccessMenuCatalogFacade {

    AccessMenuResult<Long> create(CreateAccessMenuCommand command);

    AccessMenuResult<Void> update(UpdateAccessMenuCommand command);

    AccessMenuResult<Void> delete(List<Long> menuIds, Long operatorId);

    List<AccessMenu> list();

    AccessMenuResult<AccessMenu> get(Long menuId);

    List<AccessMenuNode> tree(Boolean onlyMenu);

    List<RequestUrlVO> listAuthorizationUrls();
}
