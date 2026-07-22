package com.hunyuan.sa.admin.module.access.role.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 角色成员关系稳定管理接口。
 */
@RestController
@RequestMapping("/api/admin/v1/access")
@Tag(name = "访问控制 - 角色成员")
public class AccessRoleMembershipController {

    @Resource
    private AccessRoleMembershipFacade roleMembershipFacade;

    @Resource
    private AccessRoleAssignmentFacade roleAssignmentFacade;

    @PostMapping("/roles/{roleId}/members/query")
    @Operation(operationId = "accessRoleMemberQuery", summary = "分页查询角色成员")
    @SaCheckPermission("access.role.employee.read")
    public ResponseDTO<PageResult<AccessRoleMember>> queryMembers(
            @PathVariable Long roleId,
            @Valid @RequestBody AccessRoleMemberQuery query) {
        query.setRoleId(String.valueOf(roleId));
        return ResponseDTO.ok(roleMembershipFacade.queryMembers(query));
    }

    @PostMapping("/roles/{roleId}/member-candidates/query")
    @Operation(operationId = "accessRoleMemberCandidateQuery", summary = "分页查询角色候选成员")
    @SaCheckPermission("access.role.employee.read")
    public ResponseDTO<PageResult<AccessRoleMember>> queryCandidates(
            @PathVariable Long roleId,
            @Valid @RequestBody AccessRoleMemberQuery query) {
        query.setRoleId(String.valueOf(roleId));
        return ResponseDTO.ok(roleMembershipFacade.queryCandidates(query));
    }

    @GetMapping("/roles/{roleId}/members")
    @Operation(operationId = "accessRoleMemberList", summary = "查询角色全部成员")
    @SaCheckPermission("access.role.employee.read")
    public ResponseDTO<List<AccessRoleMember>> listMembers(@PathVariable Long roleId) {
        return ResponseDTO.ok(roleMembershipFacade.listMembers(roleId));
    }

    @PostMapping("/roles/{roleId}/members")
    @Operation(operationId = "accessRoleMemberAssign", summary = "批量分配角色成员")
    @SaCheckPermission("access.role.employee.assign")
    public ResponseDTO<String> assignMembers(
            @PathVariable Long roleId,
            @Valid @RequestBody RoleEmployeesRequest request) {
        roleAssignmentFacade.assignEmployees(
                new AssignRoleEmployeesCommand(roleId, request.employeeIds()));
        return ResponseDTO.ok();
    }

    @DeleteMapping("/roles/{roleId}/members")
    @Operation(operationId = "accessRoleMemberRemove", summary = "批量移除角色成员")
    @SaCheckPermission("access.role.employee.remove")
    public ResponseDTO<String> removeMembers(
            @PathVariable Long roleId,
            @Valid @RequestBody RoleEmployeesRequest request) {
        roleAssignmentFacade.removeEmployees(
                new RemoveRoleEmployeesCommand(roleId, request.employeeIds()));
        return ResponseDTO.ok();
    }

    @GetMapping("/employees/{employeeId}/roles")
    @Operation(operationId = "accessEmployeeRoleSelectionList", summary = "查询员工角色选择状态")
    @SaCheckPermission("access.role.employee.read")
    public ResponseDTO<List<AccessRoleSelection>> listEmployeeRoles(
            @PathVariable Long employeeId) {
        return ResponseDTO.ok(roleMembershipFacade.listEmployeeRoleSelections(employeeId));
    }

    /**
     * 批量分配或移除角色成员的请求体。
     */
    public record RoleEmployeesRequest(
            @NotEmpty(message = "员工ID集合不能为空")
            Set<Long> employeeIds
    ) {
    }
}
