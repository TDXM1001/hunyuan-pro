package com.hunyuan.sa.admin.module.system.employee.controller;

import com.hunyuan.sa.admin.constant.AdminSwaggerTagConst;
import com.hunyuan.sa.admin.module.system.employee.domain.form.EmployeeUpdateAvatarForm;
import com.hunyuan.sa.admin.module.system.employee.domain.form.EmployeeUpdateCenterForm;
import com.hunyuan.sa.admin.module.system.employee.domain.form.EmployeeUpdatePasswordForm;
import com.hunyuan.sa.admin.module.system.employee.service.EmployeeService;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import com.hunyuan.sa.base.module.support.apiencrypt.annotation.ApiDecrypt;
import com.hunyuan.sa.base.module.support.securityprotect.service.Level3ProtectConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Legacy employee self-service endpoints retained until the account-center boundary migrates.
 */
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_EMPLOYEE)
public class EmployeeController {

    @Resource
    private EmployeeService employeeService;

    @Resource
    private Level3ProtectConfigService level3ProtectConfigService;

    @Operation(summary = "Update the current employee profile")
    @PostMapping("/employee/update/center")
    public ResponseDTO<String> updateCenter(@Valid @RequestBody EmployeeUpdateCenterForm updateCenterForm) {
        updateCenterForm.setEmployeeId(SmartRequestUtil.getRequestUserId());
        return employeeService.updateCenter(updateCenterForm);
    }

    @Operation(summary = "Update the current employee avatar")
    @PostMapping("/employee/update/avatar")
    public ResponseDTO<String> updateAvatar(@Valid @RequestBody EmployeeUpdateAvatarForm employeeUpdateAvatarForm) {
        employeeUpdateAvatarForm.setEmployeeId(SmartRequestUtil.getRequestUserId());
        return employeeService.updateAvatar(employeeUpdateAvatarForm);
    }

    @Operation(summary = "Update the current employee password")
    @PostMapping("/employee/update/password")
    @ApiDecrypt
    public ResponseDTO<String> updatePassword(@Valid @RequestBody EmployeeUpdatePasswordForm updatePasswordForm) {
        updatePasswordForm.setEmployeeId(SmartRequestUtil.getRequestUserId());
        return employeeService.updatePassword(SmartRequestUtil.getRequestUser(), updatePasswordForm);
    }

    @Operation(summary = "Get password complexity configuration")
    @GetMapping("/employee/getPasswordComplexityEnabled")
    @ApiDecrypt
    public ResponseDTO<Boolean> getPasswordComplexityEnabled() {
        return ResponseDTO.ok(level3ProtectConfigService.isPasswordComplexityEnabled());
    }
}
