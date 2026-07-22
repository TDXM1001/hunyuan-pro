package com.hunyuan.sa.admin.module.system.role.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.admin.module.access.capability.api.AccessCapabilityGrantFacade;
import com.hunyuan.sa.admin.module.access.capability.api.AccessCapabilityGrantFailure;
import com.hunyuan.sa.admin.module.access.capability.api.AccessCapabilityGrantResult;
import com.hunyuan.sa.admin.module.access.capability.api.AccessCapabilityNode;
import com.hunyuan.sa.admin.module.access.capability.api.AccessRoleCapabilityGrant;
import com.hunyuan.sa.admin.module.access.capability.api.ReplaceRoleCapabilitiesCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.admin.constant.AdminSwaggerTagConst;
import com.hunyuan.sa.admin.module.system.menu.domain.vo.MenuSimpleTreeVO;
import com.hunyuan.sa.admin.module.system.role.domain.form.RoleMenuUpdateForm;
import com.hunyuan.sa.admin.module.system.role.domain.vo.RoleMenuTreeVO;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色能力授权兼容入口。
 */
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_ROLE_MENU)
public class RoleMenuController {

    @Resource
    private AccessCapabilityGrantFacade capabilityGrantFacade;

    @Operation(summary = "更新角色权限")
    @PostMapping("/role/menu/updateRoleMenu")
    @SaCheckPermission("system:role:menu:update")
    public ResponseDTO<String> updateRoleMenu(@Valid @RequestBody RoleMenuUpdateForm updateDTO) {
        AccessCapabilityGrantResult<Void> result = capabilityGrantFacade.replaceRoleCapabilities(
                new ReplaceRoleCapabilitiesCommand(updateDTO.getRoleId(), updateDTO.getMenuIdList()));
        if (result.successful()) {
            return ResponseDTO.ok();
        }
        if (result.failure() == AccessCapabilityGrantFailure.ROLE_NOT_FOUND) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return ResponseDTO.userErrorParam("角色能力授权失败");
    }

    @Operation(summary = "获取角色关联菜单权限")
    @GetMapping("/role/menu/getRoleSelectedMenu/{roleId}")
    public ResponseDTO<RoleMenuTreeVO> getRoleSelectedMenu(@PathVariable Long roleId) {
        AccessRoleCapabilityGrant grant = capabilityGrantFacade.getRoleCapabilities(roleId);
        RoleMenuTreeVO response = new RoleMenuTreeVO();
        response.setRoleId(grant.roleId());
        response.setSelectedMenuId(grant.selectedCapabilityIds());
        response.setMenuTreeList(toLegacyTree(grant.capabilityTree()));
        return ResponseDTO.ok(response);
    }

    private List<MenuSimpleTreeVO> toLegacyTree(List<AccessCapabilityNode> capabilities) {
        return capabilities.stream().map(capability -> {
            MenuSimpleTreeVO node = new MenuSimpleTreeVO();
            node.setMenuId(capability.capabilityId());
            node.setMenuName(capability.capabilityName());
            node.setContextMenuId(capability.contextCapabilityId());
            node.setParentId(capability.parentId());
            node.setMenuType(capability.capabilityType());
            node.setChildren(toLegacyTree(capability.children()));
            return node;
        }).toList();
    }
}
