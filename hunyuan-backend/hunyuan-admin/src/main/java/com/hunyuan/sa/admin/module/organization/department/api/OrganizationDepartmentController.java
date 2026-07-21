package com.hunyuan.sa.admin.module.organization.department.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.admin.module.organization.department.application.OrganizationDepartmentFacade;
import com.hunyuan.sa.admin.module.organization.department.domain.Department;
import com.hunyuan.sa.admin.module.organization.department.domain.DepartmentCommand;
import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationMember;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/v1/organization/departments")
@Tag(name = "组织目录 - 部门")
public class OrganizationDepartmentController {

    @Resource
    private OrganizationDepartmentFacade facade;

    @GetMapping
    @Operation(operationId = "organizationDepartmentList", summary = "查询部门目录")
    @SaCheckPermission("organization.department.read")
    public ResponseDTO<List<Department>> list() {
        return ResponseDTO.ok(facade.list());
    }

    @GetMapping("/{departmentId}")
    @Operation(operationId = "organizationDepartmentGet", summary = "查询部门详情")
    @SaCheckPermission("organization.department.read")
    public ResponseDTO<Department> get(@PathVariable Long departmentId) {
        return ResponseDTO.ok(facade.get(departmentId));
    }

    @GetMapping("/manager-options")
    @Operation(operationId = "organizationDepartmentManagerOptions", summary = "查询部门负责人候选")
    @SaCheckPermission("organization.department.read")
    public ResponseDTO<List<OrganizationMember>> managerOptions() {
        return ResponseDTO.ok(facade.listManagerOptions());
    }

    @PostMapping
    @Operation(operationId = "organizationDepartmentCreate", summary = "创建部门")
    @SaCheckPermission("organization.department.create")
    public ResponseDTO<Long> create(@Valid @RequestBody DepartmentRequest request) {
        return facade.create(request.toCommand());
    }

    @PutMapping("/{departmentId}")
    @Operation(operationId = "organizationDepartmentUpdate", summary = "更新部门")
    @SaCheckPermission("organization.department.update")
    public ResponseDTO<String> update(@PathVariable Long departmentId, @Valid @RequestBody DepartmentRequest request) {
        return facade.update(departmentId, request.toCommand());
    }

    @DeleteMapping("/{departmentId}")
    @Operation(operationId = "organizationDepartmentDelete", summary = "删除部门")
    @SaCheckPermission("organization.department.delete")
    public ResponseDTO<String> delete(@PathVariable Long departmentId) {
        return facade.delete(departmentId);
    }

    @Data
    public static class DepartmentRequest {
        @NotBlank(message = "部门名称不能为空")
        private String departmentName;
        private Long managerId;
        private Long parentId;
        @NotNull
        @Min(value = 0, message = "排序值不能小于 0")
        private Integer sort;

        DepartmentCommand toCommand() {
            return new DepartmentCommand(departmentName, managerId, parentId, sort);
        }
    }
}
