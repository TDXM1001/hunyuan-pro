package com.hunyuan.sa.admin.module.access.capability.api;

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
 * 角色能力授权稳定管理接口。
 */
@RestController
@RequestMapping("/api/admin/v1/access/roles/{roleId}/capabilities")
@Tag(name = "访问控制 - 角色能力授权")
public class AccessCapabilityGrantController {

    @Resource
    private AccessCapabilityGrantFacade capabilityGrantFacade;

    @GetMapping
    @Operation(operationId = "accessRoleCapabilityGet", summary = "查询角色能力授权")
    @SaCheckPermission("access.capability.read")
    public ResponseDTO<AccessRoleCapabilityGrant> get(@PathVariable Long roleId) {
        return ResponseDTO.ok(capabilityGrantFacade.getRoleCapabilities(roleId));
    }

    @PutMapping
    @Operation(operationId = "accessRoleCapabilityReplace", summary = "全量替换角色能力授权")
    @SaCheckPermission("access.capability.grant")
    public ResponseDTO<String> replace(
            @PathVariable Long roleId,
            @Valid @RequestBody ReplaceRoleCapabilitiesRequest request) {
        AccessCapabilityGrantResult<Void> result = capabilityGrantFacade.replaceRoleCapabilities(
                new ReplaceRoleCapabilitiesCommand(roleId, request.capabilityIds()));
        if (result.successful()) {
            return ResponseDTO.ok();
        }
        if (result.failure() == AccessCapabilityGrantFailure.ROLE_NOT_FOUND) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return ResponseDTO.userErrorParam("角色能力授权失败");
    }

    /**
     * 全量替换角色能力授权的请求体。
     */
    public record ReplaceRoleCapabilitiesRequest(
            @NotNull(message = "能力ID集合不能为空")
            List<Long> capabilityIds
    ) {
    }
}
