package com.hunyuan.sa.admin.module.identity.employee.api;

import com.hunyuan.sa.admin.constant.AdminSwaggerTagConst;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import com.hunyuan.sa.base.module.support.apiencrypt.annotation.ApiDecrypt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证账号与当前登录员工自助 HTTP 接口。
 */
@RestController
@RequestMapping("/api/admin/v1/identity/account")
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_EMPLOYEE)
public class EmployeeAccountController {

    @Resource
    private EmployeeAccountFacade accountFacade;

    @Resource
    private EmployeeDirectoryFacade directoryFacade;

    @Operation(summary = "获取当前登录员工资料")
    @GetMapping("/me")
    public ResponseDTO<EmployeeSummary> getCurrentProfile() {
        return directoryFacade.findSummaryById(SmartRequestUtil.getRequestUserId())
                .map(ResponseDTO::ok)
                .orElseGet(() -> ResponseDTO.error(
                        com.hunyuan.sa.base.common.code.UserErrorCode.DATA_NOT_EXIST));
    }

    @Operation(summary = "更新当前登录员工资料")
    @PutMapping("/me/profile")
    public ResponseDTO<String> updateProfile(
            @Valid @RequestBody EmployeeSelfProfileRequest request) {
        return accountFacade.updateSelfProfile(
                request.toCommand(SmartRequestUtil.getRequestUserId()));
    }

    @Operation(summary = "更新当前登录员工头像")
    @PutMapping("/me/avatar")
    public ResponseDTO<String> updateAvatar(@Valid @RequestBody EmployeeAvatarRequest request) {
        return accountFacade.updateAvatar(
                SmartRequestUtil.getRequestUserId(), request.avatar());
    }

    @Operation(summary = "修改当前登录员工密码")
    @PostMapping("/me/password")
    @ApiDecrypt
    public ResponseDTO<String> changePassword(
            @Valid @RequestBody EmployeePasswordChangeRequest request) {
        return accountFacade.changePassword(
                SmartRequestUtil.getRequestUser(),
                request.toCommand(SmartRequestUtil.getRequestUserId()));
    }

    @Operation(summary = "获取密码复杂度配置")
    @GetMapping("/me/password-policy")
    @ApiDecrypt
    public ResponseDTO<Boolean> getPasswordPolicy() {
        return ResponseDTO.ok(accountFacade.passwordComplexityEnabled());
    }
}
