package com.hunyuan.sa.admin.module.system.menu.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.admin.constant.AdminSwaggerTagConst;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenu;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenuCatalogFacade;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenuNode;
import com.hunyuan.sa.admin.module.access.menu.api.AccessMenuResult;
import com.hunyuan.sa.admin.module.access.menu.api.CreateAccessMenuCommand;
import com.hunyuan.sa.admin.module.access.menu.api.UpdateAccessMenuCommand;
import com.hunyuan.sa.admin.module.system.menu.domain.form.MenuAddForm;
import com.hunyuan.sa.admin.module.system.menu.domain.form.MenuUpdateForm;
import com.hunyuan.sa.admin.module.system.menu.domain.vo.MenuTreeVO;
import com.hunyuan.sa.admin.module.system.menu.domain.vo.MenuVO;
import com.hunyuan.sa.base.common.code.SystemErrorCode;
import com.hunyuan.sa.base.common.domain.RequestUrlVO;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单
 *
 * @Author 1024创新实验室: 善逸
 * @Date 2022-03-06 22:04:37
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_MENU)
public class MenuController {

    @Resource
    private AccessMenuCatalogFacade menuCatalogFacade;

    @Operation(summary = "添加菜单 @author 卓大")
    @PostMapping("/menu/add")
    @SaCheckPermission("system:menu:add")
    public ResponseDTO<String> addMenu(@RequestBody @Valid MenuAddForm menuAddForm) {
        AccessMenuResult<Long> result = menuCatalogFacade.create(toCreateCommand(
                menuAddForm,
                SmartRequestUtil.getRequestUserId()));
        return result.successful() ? ResponseDTO.ok() : legacyMutationError(result);
    }

    @Operation(summary = "更新菜单 @author 卓大")
    @PostMapping("/menu/update")
    @SaCheckPermission("system:menu:update")
    public ResponseDTO<String> updateMenu(@RequestBody @Valid MenuUpdateForm menuUpdateForm) {
        AccessMenuResult<Void> result = menuCatalogFacade.update(toUpdateCommand(
                menuUpdateForm,
                SmartRequestUtil.getRequestUserId()));
        return result.successful() ? ResponseDTO.ok() : legacyMutationError(result);
    }

    @Operation(summary = "批量删除菜单 @author 卓大")
    @GetMapping("/menu/batchDelete")
    @SaCheckPermission("system:menu:batchDelete")
    public ResponseDTO<String> batchDeleteMenu(@RequestParam("menuIdList") List<Long> menuIdList) {
        AccessMenuResult<Void> result = menuCatalogFacade.delete(
                menuIdList,
                SmartRequestUtil.getRequestUserId());
        return result.successful() ? ResponseDTO.ok() : legacyMutationError(result);
    }

    @Operation(summary = "查询菜单列表 @author 卓大")
    @GetMapping("/menu/query")
    public ResponseDTO<List<MenuVO>> queryMenuList() {
        return ResponseDTO.ok(menuCatalogFacade.list().stream()
                .map(this::toLegacyMenu)
                .toList());
    }

    @Operation(summary = "查询菜单详情 @author 卓大")
    @GetMapping("/menu/detail/{menuId}")
    public ResponseDTO<MenuVO> getMenuDetail(@PathVariable Long menuId) {
        AccessMenuResult<AccessMenu> result = menuCatalogFacade.get(menuId);
        if (!result.successful()) {
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, result.message());
        }
        return ResponseDTO.ok(toLegacyMenu(result.data()));
    }

    @Operation(summary = "查询菜单树 @author 卓大")
    @GetMapping("/menu/tree")
    public ResponseDTO<List<MenuTreeVO>> queryMenuTree(@RequestParam("onlyMenu") Boolean onlyMenu) {
        return ResponseDTO.ok(menuCatalogFacade.tree(onlyMenu).stream()
                .map(this::toLegacyTree)
                .toList());
    }

    @Operation(summary = "获取所有请求路径 @author 卓大")
    @GetMapping("/menu/auth/url")
    public ResponseDTO<List<RequestUrlVO>> getAuthUrl() {
        return ResponseDTO.ok(menuCatalogFacade.listAuthorizationUrls());
    }

    private CreateAccessMenuCommand toCreateCommand(MenuAddForm form, Long operatorId) {
        return new CreateAccessMenuCommand(
                form.getMenuName(),
                form.getMenuType(),
                form.getParentId(),
                form.getSort(),
                form.getPath(),
                form.getComponent(),
                form.getFrameFlag(),
                form.getFrameUrl(),
                form.getCacheFlag(),
                form.getVisibleFlag(),
                form.getDisabledFlag(),
                form.getPermsType(),
                form.getWebPerms(),
                form.getApiPerms(),
                form.getIcon(),
                form.getContextMenuId(),
                operatorId);
    }

    private UpdateAccessMenuCommand toUpdateCommand(MenuUpdateForm form, Long operatorId) {
        return new UpdateAccessMenuCommand(
                form.getMenuId(),
                form.getMenuName(),
                form.getMenuType(),
                form.getParentId(),
                form.getSort(),
                form.getPath(),
                form.getComponent(),
                form.getFrameFlag(),
                form.getFrameUrl(),
                form.getCacheFlag(),
                form.getVisibleFlag(),
                form.getDisabledFlag(),
                form.getPermsType(),
                form.getWebPerms(),
                form.getApiPerms(),
                form.getIcon(),
                form.getContextMenuId(),
                operatorId);
    }

    private ResponseDTO<String> legacyMutationError(AccessMenuResult<?> result) {
        return ResponseDTO.userErrorParam(result.message());
    }

    private MenuVO toLegacyMenu(AccessMenu menu) {
        MenuVO legacyMenu = new MenuVO();
        copyLegacyMenu(menu, legacyMenu);
        return legacyMenu;
    }

    private MenuTreeVO toLegacyTree(AccessMenuNode menu) {
        MenuTreeVO legacyMenu = new MenuTreeVO();
        copyLegacyMenu(menu, legacyMenu);
        legacyMenu.setChildren(menu.children().stream()
                .map(this::toLegacyTree)
                .toList());
        return legacyMenu;
    }

    private void copyLegacyMenu(AccessMenu menu, MenuVO legacyMenu) {
        legacyMenu.setMenuId(menu.menuId());
        legacyMenu.setMenuName(menu.menuName());
        legacyMenu.setMenuType(menu.menuType());
        legacyMenu.setParentId(menu.parentId());
        legacyMenu.setSort(menu.sort());
        legacyMenu.setPath(menu.path());
        legacyMenu.setComponent(menu.component());
        legacyMenu.setFrameFlag(menu.frameFlag());
        legacyMenu.setFrameUrl(menu.frameUrl());
        legacyMenu.setCacheFlag(menu.cacheFlag());
        legacyMenu.setVisibleFlag(menu.visibleFlag());
        legacyMenu.setDisabledFlag(menu.disabledFlag());
        legacyMenu.setPermsType(menu.permsType());
        legacyMenu.setWebPerms(menu.webPerms());
        legacyMenu.setApiPerms(menu.apiPerms());
        legacyMenu.setIcon(menu.icon());
        legacyMenu.setContextMenuId(menu.contextMenuId());
        legacyMenu.setCreateTime(menu.createTime());
        legacyMenu.setCreateUserId(menu.createUserId());
        legacyMenu.setUpdateTime(menu.updateTime());
        legacyMenu.setUpdateUserId(menu.updateUserId());
    }

    private void copyLegacyMenu(AccessMenuNode menu, MenuVO legacyMenu) {
        legacyMenu.setMenuId(menu.menuId());
        legacyMenu.setMenuName(menu.menuName());
        legacyMenu.setMenuType(menu.menuType());
        legacyMenu.setParentId(menu.parentId());
        legacyMenu.setSort(menu.sort());
        legacyMenu.setPath(menu.path());
        legacyMenu.setComponent(menu.component());
        legacyMenu.setFrameFlag(menu.frameFlag());
        legacyMenu.setFrameUrl(menu.frameUrl());
        legacyMenu.setCacheFlag(menu.cacheFlag());
        legacyMenu.setVisibleFlag(menu.visibleFlag());
        legacyMenu.setDisabledFlag(menu.disabledFlag());
        legacyMenu.setPermsType(menu.permsType());
        legacyMenu.setWebPerms(menu.webPerms());
        legacyMenu.setApiPerms(menu.apiPerms());
        legacyMenu.setIcon(menu.icon());
        legacyMenu.setContextMenuId(menu.contextMenuId());
        legacyMenu.setCreateTime(menu.createTime());
        legacyMenu.setCreateUserId(menu.createUserId());
        legacyMenu.setUpdateTime(menu.updateTime());
        legacyMenu.setUpdateUserId(menu.updateUserId());
    }
}
