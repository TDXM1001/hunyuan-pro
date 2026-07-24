package com.hunyuan.sa.admin.module.identity.employee.application;

import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeAccountFacade;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeAuthenticationAccount;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeePasswordChangeCommand;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeePasswordSalt;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeSelfProfileUpdateCommand;
import com.hunyuan.sa.admin.module.identity.employee.application.port.EmployeeSessionPort;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeRepository;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeSelfProfileUpdate;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.securityprotect.service.Level3ProtectConfigService;
import com.hunyuan.sa.base.module.support.securityprotect.service.SecurityPasswordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/**
 * 认证账号与个人自助应用服务。
 */
@Service
public class EmployeeAccountApplicationService implements EmployeeAccountFacade {

    @Resource
    private EmployeeRepository employeeRepository;

    @Resource
    private SecurityPasswordService securityPasswordService;

    @Resource
    private Level3ProtectConfigService level3ProtectConfigService;

    @Resource
    private EmployeeSessionPort employeeSessionPort;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<String> updateSelfProfile(EmployeeSelfProfileUpdateCommand command) {
        Optional<EmployeeAuthenticationAccount> account = activeAccount(command.employeeId());
        if (account.isEmpty()) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        ResponseDTO<String> uniqueness = validateUniqueness(
                command.employeeId(), command.phone(), command.email());
        if (uniqueness != null) {
            return uniqueness;
        }

        employeeRepository.updateSelfProfile(new EmployeeSelfProfileUpdate(
                command.employeeId(),
                command.actualName(),
                command.gender(),
                command.phone(),
                command.email(),
                command.positionId(),
                command.avatar(),
                command.remark()
        ));
        employeeSessionPort.clearCache(command.employeeId());
        return ResponseDTO.ok();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<String> updateAvatar(Long employeeId, String avatar) {
        if (activeAccount(employeeId).isEmpty()) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        employeeRepository.updateAvatar(employeeId, avatar);
        employeeSessionPort.clearCache(employeeId);
        return ResponseDTO.ok();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<String> changePassword(
            RequestUser requestUser, EmployeePasswordChangeCommand command) {
        Optional<EmployeeAuthenticationAccount> account = activeAccount(command.employeeId());
        if (account.isEmpty()) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        String oldSaltedPassword =
                EmployeePasswordSalt.apply(command.oldPassword(), account.get().employeeUid());
        if (!SecurityPasswordService.matchesPwd(oldSaltedPassword, account.get().passwordHash())) {
            return ResponseDTO.userErrorParam("原密码有误，请重新输入");
        }
        if (Objects.equals(command.oldPassword(), command.newPassword())) {
            return ResponseDTO.userErrorParam("新密码与原始密码相同，请重新输入");
        }

        ResponseDTO<String> complexityResponse =
                securityPasswordService.validatePasswordComplexity(command.newPassword());
        if (!complexityResponse.getOk()) {
            return complexityResponse;
        }

        String newSaltedPassword =
                EmployeePasswordSalt.apply(command.newPassword(), account.get().employeeUid());
        ResponseDTO<String> repeatResponse =
                securityPasswordService.validatePasswordRepeatTimes(requestUser, newSaltedPassword);
        if (!repeatResponse.getOk()) {
            return ResponseDTO.error(repeatResponse);
        }

        String newEncryptedPassword = SecurityPasswordService.getEncryptPwd(newSaltedPassword);
        employeeRepository.updatePassword(command.employeeId(), newEncryptedPassword);
        securityPasswordService.saveUserChangePasswordLog(
                requestUser, newEncryptedPassword, account.get().passwordHash());
        employeeSessionPort.clearCache(command.employeeId());
        return ResponseDTO.ok();
    }

    @Override
    public boolean passwordComplexityEnabled() {
        return level3ProtectConfigService.isPasswordComplexityEnabled();
    }

    private Optional<EmployeeAuthenticationAccount> activeAccount(Long employeeId) {
        if (employeeId == null) {
            return Optional.empty();
        }
        return employeeRepository.findAuthenticationAccountById(employeeId)
                .filter(account -> !Boolean.TRUE.equals(account.deleted()));
    }

    private ResponseDTO<String> validateUniqueness(Long employeeId, String phone, String email) {
        if (belongsToAnotherEmployee(employeeRepository.findIdByPhone(phone), employeeId)) {
            return ResponseDTO.userErrorParam("手机号已存在");
        }
        if (belongsToAnotherEmployee(employeeRepository.findIdByEmail(email), employeeId)) {
            return ResponseDTO.userErrorParam("邮箱账号已存在");
        }
        return null;
    }

    private boolean belongsToAnotherEmployee(Optional<Long> existingId, Long employeeId) {
        return existingId.isPresent() && !Objects.equals(existingId.get(), employeeId);
    }
}
