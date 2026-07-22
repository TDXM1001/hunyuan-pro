package com.hunyuan.sa.admin.module.access.datascope.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据范围稳定管理接口。
 */
@RestController
@RequestMapping("/api/admin/v1/access")
@Tag(name = "访问控制 - 数据范围")
public class AccessDataScopeManagementController {

    @Resource
    private AccessDataScopeManagementFacade dataScopeManagementFacade;

    @GetMapping("/data-scopes")
    @Operation(operationId = "accessDataScopeList", summary = "查询数据范围目录")
    @SaCheckPermission("access.data-scope.read")
    public ResponseDTO<List<AccessDataScopeDefinition>> list() {
        return ResponseDTO.ok(dataScopeManagementFacade.listDataScopes());
    }

    @GetMapping("/roles/{roleId}/data-scopes")
    @Operation(operationId = "accessRoleDataScopeGet", summary = "查询角色数据范围")
    @SaCheckPermission("access.data-scope.read")
    public ResponseDTO<AccessRoleDataScopes> get(@PathVariable Long roleId) {
        return toResponse(dataScopeManagementFacade.getRoleDataScopes(roleId));
    }

    @PutMapping("/roles/{roleId}/data-scopes")
    @Operation(operationId = "accessRoleDataScopeReplace", summary = "全量替换角色数据范围")
    @SaCheckPermission("access.data-scope.update")
    public ResponseDTO<String> replace(
            @PathVariable Long roleId,
            @Valid @RequestBody ReplaceRoleDataScopesRequest request) {
        List<AccessDataScopeSetting> dataScopes = request.dataScopes().stream()
                .map(item -> new AccessDataScopeSetting(item.dataScopeType(), item.viewType()))
                .toList();
        AccessDataScopeManagementResult<Void> result =
                dataScopeManagementFacade.replaceRoleDataScopes(
                        new ReplaceAccessRoleDataScopesCommand(roleId, dataScopes));
        return result.successful() ? ResponseDTO.ok() : errorResponse(result);
    }

    private <T> ResponseDTO<T> toResponse(AccessDataScopeManagementResult<T> result) {
        return result.successful() ? ResponseDTO.ok(result.data()) : errorResponse(result);
    }

    private <T> ResponseDTO<T> errorResponse(AccessDataScopeManagementResult<?> result) {
        if (result.failure() == AccessDataScopeManagementFailure.ROLE_NOT_FOUND) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return ResponseDTO.userErrorParam(result.message());
    }

    /**
     * 全量替换角色数据范围的请求体。
     */
    public record ReplaceRoleDataScopesRequest(
            @NotNull(message = "数据范围配置集合不能为空")
            List<@Valid DataScopeSettingRequest> dataScopes
    ) {
    }

    /**
     * 单项数据范围配置请求。
     */
    public record DataScopeSettingRequest(
            @NotNull(message = "数据范围类型不能为空")
            Integer dataScopeType,
            @NotNull(message = "可见范围不能为空")
            Integer viewType
    ) {
    }
}
