package com.hunyuan.sa.admin.module.system.role.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.admin.constant.AdminSwaggerTagConst;
import com.hunyuan.sa.admin.module.access.role.api.AccessRole;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleFailure;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleLifecycleFacade;
import com.hunyuan.sa.admin.module.access.role.api.AccessRoleResult;
import com.hunyuan.sa.admin.module.access.role.api.CreateAccessRoleCommand;
import com.hunyuan.sa.admin.module.access.role.api.UpdateAccessRoleCommand;
import com.hunyuan.sa.admin.module.system.role.domain.form.RoleAddForm;
import com.hunyuan.sa.admin.module.system.role.domain.form.RoleUpdateForm;
import com.hunyuan.sa.admin.module.system.role.domain.vo.RoleVO;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理兼容入口。
 */
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_ROLE)
public class RoleController {

    @Resource
    private AccessRoleLifecycleFacade roleLifecycleFacade;

    @Operation(summary = "添加角色")
    @PostMapping("/role/add")
    @SaCheckPermission("system:role:add")
    public ResponseDTO<String> addRole(@Valid @RequestBody RoleAddForm roleAddForm) {
        AccessRoleResult<Long> result = roleLifecycleFacade.create(new CreateAccessRoleCommand(
                roleAddForm.getRoleName(),
                roleAddForm.getRoleCode(),
                roleAddForm.getRemark()));
        return toMutationResponse(result);
    }

    @Operation(summary = "删除角色")
    @GetMapping("/role/delete/{roleId}")
    @SaCheckPermission("system:role:delete")
    public ResponseDTO<String> deleteRole(@PathVariable Long roleId) {
        return toMutationResponse(roleLifecycleFacade.delete(roleId));
    }

    @Operation(summary = "更新角色")
    @PostMapping("/role/update")
    @SaCheckPermission("system:role:update")
    public ResponseDTO<String> updateRole(@Valid @RequestBody RoleUpdateForm roleUpdateForm) {
        return toMutationResponse(roleLifecycleFacade.update(new UpdateAccessRoleCommand(
                roleUpdateForm.getRoleId(),
                roleUpdateForm.getRoleName(),
                roleUpdateForm.getRoleCode(),
                roleUpdateForm.getRemark())));
    }

    @Operation(summary = "获取角色数据")
    @GetMapping("/role/get/{roleId}")
    public ResponseDTO<RoleVO> getRole(@PathVariable("roleId") Long roleId) {
        AccessRoleResult<AccessRole> result = roleLifecycleFacade.get(roleId);
        if (!result.successful()) {
            return errorResponse(result);
        }
        return ResponseDTO.ok(toLegacyRole(result.data()));
    }

    @Operation(summary = "获取所有角色")
    @GetMapping("/role/getAll")
    public ResponseDTO<List<RoleVO>> getAllRole() {
        return ResponseDTO.ok(roleLifecycleFacade.list().stream()
                .map(this::toLegacyRole)
                .toList());
    }

    private ResponseDTO<String> toMutationResponse(AccessRoleResult<?> result) {
        return result.successful() ? ResponseDTO.ok() : errorResponse(result);
    }

    private <T> ResponseDTO<T> errorResponse(AccessRoleResult<?> result) {
        AccessRoleFailure failure = result.failure();
        if (failure == AccessRoleFailure.ROLE_NOT_FOUND) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (failure == AccessRoleFailure.ROLE_HAS_EMPLOYEES) {
            return ResponseDTO.error(UserErrorCode.ALREADY_EXIST, result.message());
        }
        return ResponseDTO.userErrorParam(result.message());
    }

    private RoleVO toLegacyRole(AccessRole role) {
        RoleVO roleVO = new RoleVO();
        roleVO.setRoleId(role.roleId());
        roleVO.setRoleName(role.roleName());
        roleVO.setRoleCode(role.roleCode());
        roleVO.setRemark(role.remark());
        return roleVO;
    }

}
