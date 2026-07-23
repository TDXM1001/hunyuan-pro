package com.hunyuan.sa.admin.module.organization.position.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.admin.module.organization.position.application.OrganizationPositionFacade;
import com.hunyuan.sa.admin.module.organization.position.domain.Position;
import com.hunyuan.sa.admin.module.organization.position.domain.PositionCommand;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
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
 * 岗位目录稳定管理接口。
 */
@RestController
@RequestMapping("/api/admin/v1/organization/positions")
@Tag(name = "组织目录 - 岗位")
public class OrganizationPositionController {

    @Resource
    private OrganizationPositionFacade facade;

    @GetMapping
    @Operation(operationId = "organizationPositionList", summary = "查询岗位目录")
    @SaCheckPermission("organization.position.read")
    public ResponseDTO<List<Position>> list() {
        return ResponseDTO.ok(facade.list());
    }

    @GetMapping("/{positionId}")
    @Operation(operationId = "organizationPositionGet", summary = "查询岗位详情")
    @SaCheckPermission("organization.position.read")
    public ResponseDTO<Position> get(@PathVariable Long positionId) {
        return ResponseDTO.ok(facade.get(positionId));
    }

    @PostMapping
    @Operation(operationId = "organizationPositionCreate", summary = "创建岗位")
    @SaCheckPermission("organization.position.create")
    public ResponseDTO<Long> create(@Valid @RequestBody PositionRequest request) {
        return facade.create(request.toCommand());
    }

    @PutMapping("/{positionId}")
    @Operation(operationId = "organizationPositionUpdate", summary = "更新岗位")
    @SaCheckPermission("organization.position.update")
    public ResponseDTO<String> update(
            @PathVariable Long positionId,
            @Valid @RequestBody PositionRequest request) {
        return facade.update(positionId, request.toCommand());
    }

    @DeleteMapping("/{positionId}")
    @Operation(operationId = "organizationPositionDelete", summary = "删除岗位")
    @SaCheckPermission("organization.position.delete")
    public ResponseDTO<String> delete(@PathVariable Long positionId) {
        return facade.delete(positionId);
    }

    @Data
    public static class PositionRequest {

        @NotBlank(message = "岗位名称不能为空")
        private String positionName;

        private String positionLevel;

        @NotNull(message = "排序不能为空")
        @Min(value = 0, message = "排序值不能小于 0")
        private Integer sort;

        private String remark;

        PositionCommand toCommand() {
            return new PositionCommand(positionName, positionLevel, sort, remark);
        }
    }
}
