package com.hunyuan.sa.admin.module.access.role.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色生命周期稳定管理接口。
 */
@RestController
@RequestMapping("/api/admin/v1/access/roles")
@Tag(name = "访问控制 - 角色")
public class AccessRoleController {

    @Resource
    private AccessRoleLifecycleFacade roleLifecycleFacade;

    @GetMapping
    @Operation(operationId = "accessRoleList", summary = "查询角色列表")
    @SaCheckPermission("access.role.read")
    public ResponseDTO<List<AccessRole>> list() {
        return ResponseDTO.ok(roleLifecycleFacade.list());
    }

    @GetMapping("/{roleId}")
    @Operation(operationId = "accessRoleGet", summary = "查询角色详情")
    @SaCheckPermission("access.role.read")
    public ResponseDTO<AccessRole> get(@PathVariable Long roleId) {
        return toResponse(roleLifecycleFacade.get(roleId));
    }

    @PostMapping
    @Operation(operationId = "accessRoleCreate", summary = "创建角色")
    @SaCheckPermission("access.role.create")
    public ResponseDTO<Long> create(@Valid @RequestBody AccessRoleRequest request) {
        return toResponse(roleLifecycleFacade.create(new CreateAccessRoleCommand(
                request.roleName(),
                request.roleCode(),
                request.remark())));
    }

    @PutMapping("/{roleId}")
    @Operation(operationId = "accessRoleUpdate", summary = "更新角色")
    @SaCheckPermission("access.role.update")
    public ResponseDTO<String> update(
            @PathVariable Long roleId,
            @Valid @RequestBody AccessRoleRequest request) {
        return toMutationResponse(roleLifecycleFacade.update(new UpdateAccessRoleCommand(
                roleId,
                request.roleName(),
                request.roleCode(),
                request.remark())));
    }

    @DeleteMapping("/{roleId}")
    @Operation(operationId = "accessRoleDelete", summary = "删除角色")
    @SaCheckPermission("access.role.delete")
    public ResponseDTO<String> delete(@PathVariable Long roleId) {
        return toMutationResponse(roleLifecycleFacade.delete(roleId));
    }

    private ResponseDTO<String> toMutationResponse(AccessRoleResult<?> result) {
        return result.successful() ? ResponseDTO.ok() : errorResponse(result);
    }

    private <T> ResponseDTO<T> toResponse(AccessRoleResult<T> result) {
        return result.successful() ? ResponseDTO.ok(result.data()) : errorResponse(result);
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

    /**
     * 创建和更新角色共用的请求体。
     */
    public record AccessRoleRequest(
            @NotBlank(message = "角色名称不能为空")
            @Size(max = 20, message = "角色名称最多 20 个字符")
            String roleName,
            @NotBlank(message = "角色编码不能为空")
            @Size(max = 20, message = "角色编码最多 20 个字符")
            String roleCode,
            @Size(max = 255, message = "角色备注最多 255 个字符")
            String remark
    ) {
    }
}
