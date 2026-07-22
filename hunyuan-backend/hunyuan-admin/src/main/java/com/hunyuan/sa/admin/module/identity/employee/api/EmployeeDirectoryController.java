package com.hunyuan.sa.admin.module.identity.employee.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/v1/identity/employees")
@Tag(name = "身份目录 - 员工")
public class EmployeeDirectoryController {

    @Resource
    private EmployeeDirectoryFacade facade;

    @Resource
    private EmployeeAdministrationFacade administrationFacade;

    @PostMapping("/query")
    @Operation(
            operationId = "identityEmployeeQuery",
            summary = "分页查询员工",
            description = "返回员工管理公开摘要，不包含密码、员工UID、删除标记或超级管理员标记。")
    @SaCheckPermission("identity.employee.read")
    public ResponseDTO<PageResult<EmployeeSummary>> query(@Valid @RequestBody EmployeeQuery query) {
        return ResponseDTO.ok(facade.query(query));
    }

    @PostMapping
    @Operation(
            operationId = "identityEmployeeCreate",
            summary = "创建员工",
            description = "创建员工并仅在本次禁止缓存的响应中返回临时密码。")
    @SaCheckPermission("identity.employee.create")
    public ResponseDTO<EmployeeOneTimeCredential> create(
            @Valid @RequestBody EmployeeCreateCommand command,
            HttpServletResponse response) {
        disableCredentialCaching(response);
        return administrationFacade.create(command);
    }

    @PutMapping("/{employeeId}")
    @Operation(
            operationId = "identityEmployeeUpdate",
            summary = "更新员工资料",
            description = "路径员工ID必须与请求体一致；账号状态和角色分配由独立用例处理。")
    @SaCheckPermission("identity.employee.update")
    public ResponseDTO<String> update(
            @PathVariable Long employeeId,
            @Valid @RequestBody EmployeeUpdateCommand command) {
        if (!employeeId.equals(command.employeeId())) {
            return ResponseDTO.userErrorParam("路径员工id与请求体不一致");
        }
        return administrationFacade.update(command);
    }

    @PostMapping("/{employeeId}/enable")
    @Operation(
            operationId = "identityEmployeeEnable",
            summary = "启用员工",
            description = "显式启用员工；重复启用保持幂等，已删除员工不能重新启用。")
    @SaCheckPermission("identity.employee.enable")
    public ResponseDTO<String> enable(@PathVariable Long employeeId) {
        return administrationFacade.enable(employeeId);
    }

    @PostMapping("/{employeeId}/disable")
    @Operation(
            operationId = "identityEmployeeDisable",
            summary = "禁用员工",
            description = "显式禁用员工并使现有会话失效；重复禁用保持幂等。")
    @SaCheckPermission("identity.employee.disable")
    public ResponseDTO<String> disable(@PathVariable Long employeeId) {
        return administrationFacade.disable(employeeId);
    }

    @PostMapping("/department-assignment")
    @Operation(
            operationId = "identityEmployeeAssignDepartment",
            summary = "批量调整员工部门",
            description = "目标部门与全部员工校验通过后统一调整部门。")
    @SaCheckPermission("identity.employee.department.assign")
    public ResponseDTO<String> assignDepartment(
            @Valid @RequestBody EmployeeDepartmentAssignmentCommand command) {
        return administrationFacade.assignDepartment(command);
    }

    @PostMapping("/delete")
    @Operation(
            operationId = "identityEmployeeDelete",
            summary = "批量删除员工",
            description = "执行兼容软删除并使现有会话失效；超级管理员受保护。")
    @SaCheckPermission("identity.employee.delete")
    public ResponseDTO<String> delete(@Valid @RequestBody EmployeeDeleteCommand command) {
        return administrationFacade.delete(command);
    }

    @PostMapping("/{employeeId}/password/reset")
    @Operation(
            operationId = "identityEmployeeResetPassword",
            summary = "重置员工密码",
            description = "由授权管理员重置密码，并仅在本次禁止缓存的响应中返回临时密码。")
    @SaCheckPermission("identity.employee.password.reset")
    public ResponseDTO<EmployeeOneTimeCredential> resetPassword(
            @PathVariable Long employeeId,
            HttpServletResponse response) {
        disableCredentialCaching(response);
        return administrationFacade.resetPassword(
                employeeId, com.hunyuan.sa.base.common.util.SmartRequestUtil.getRequestUserId());
    }

    private void disableCredentialCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
    }
}
