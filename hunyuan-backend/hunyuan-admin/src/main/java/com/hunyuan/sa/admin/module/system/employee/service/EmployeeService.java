package com.hunyuan.sa.admin.module.system.employee.service;

import com.hunyuan.sa.admin.module.identity.employee.api.EmployeePasswordSalt;
import com.hunyuan.sa.admin.module.system.employee.dao.EmployeeDao;
import com.hunyuan.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import com.hunyuan.sa.admin.module.system.employee.domain.form.EmployeeUpdateAvatarForm;
import com.hunyuan.sa.admin.module.system.employee.domain.form.EmployeeUpdateCenterForm;
import com.hunyuan.sa.admin.module.system.employee.domain.form.EmployeeUpdatePasswordForm;
import com.hunyuan.sa.admin.module.system.login.service.LoginService;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.module.support.securityprotect.service.SecurityPasswordService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Compatibility service for employee self-service operations.
 */
@Service
public class EmployeeService {

    @Resource
    private EmployeeDao employeeDao;

    @Resource
    private SecurityPasswordService securityPasswordService;

    @Resource
    @Lazy
    private LoginService loginService;

    public EmployeeEntity getById(Long employeeId) {
        return employeeDao.selectById(employeeId);
    }

    public ResponseDTO<String> updateCenter(EmployeeUpdateCenterForm updateCenterForm) {
        Long employeeId = updateCenterForm.getEmployeeId();
        EmployeeEntity employeeEntity = employeeDao.selectById(employeeId);
        if (employeeEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        ResponseDTO<String> checkResponse =
                checkUniqueness(employeeId, "", updateCenterForm.getPhone(), updateCenterForm.getEmail());
        if (!checkResponse.getOk()) {
            return checkResponse;
        }

        EmployeeEntity employee = SmartBeanUtil.copy(updateCenterForm, EmployeeEntity.class);
        employee.setLoginPwd(null);
        employeeDao.updateById(employee);
        loginService.clearLoginEmployeeCache(employeeId);
        return ResponseDTO.ok();
    }

    private ResponseDTO<String> checkUniqueness(Long employeeId, String loginName, String phone, String email) {
        EmployeeEntity existEntity = employeeDao.getByLoginName(loginName, null);
        if (existEntity != null && !Objects.equals(existEntity.getEmployeeId(), employeeId)) {
            return ResponseDTO.userErrorParam("登录名重复");
        }

        existEntity = employeeDao.getByPhone(phone, null);
        if (existEntity != null && !Objects.equals(existEntity.getEmployeeId(), employeeId)) {
            return ResponseDTO.userErrorParam("手机号已存在");
        }

        existEntity = employeeDao.getByEmail(email, null);
        if (existEntity != null && !Objects.equals(existEntity.getEmployeeId(), employeeId)) {
            return ResponseDTO.userErrorParam("邮箱账号已存在");
        }

        return ResponseDTO.ok();
    }

    public ResponseDTO<String> updateAvatar(EmployeeUpdateAvatarForm employeeUpdateAvatarForm) {
        Long employeeId = employeeUpdateAvatarForm.getEmployeeId();
        if (employeeDao.selectById(employeeId) == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        EmployeeEntity updateEntity = new EmployeeEntity();
        updateEntity.setEmployeeId(employeeId);
        updateEntity.setAvatar(employeeUpdateAvatarForm.getAvatar());
        employeeDao.updateById(updateEntity);
        loginService.clearLoginEmployeeCache(employeeId);
        return ResponseDTO.ok();
    }

    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<String> updatePassword(
            RequestUser requestUser,
            EmployeeUpdatePasswordForm updatePasswordForm) {
        Long employeeId = updatePasswordForm.getEmployeeId();
        EmployeeEntity employeeEntity = employeeDao.selectById(employeeId);
        if (employeeEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        String oldSaltedPassword =
                generateSaltPassword(updatePasswordForm.getOldPassword(), employeeEntity.getEmployeeUid());
        if (!SecurityPasswordService.matchesPwd(oldSaltedPassword, employeeEntity.getLoginPwd())) {
            return ResponseDTO.userErrorParam("原密码有误，请重新输入");
        }

        if (Objects.equals(updatePasswordForm.getOldPassword(), updatePasswordForm.getNewPassword())) {
            return ResponseDTO.userErrorParam("新密码与原始密码相同，请重新输入");
        }

        ResponseDTO<String> complexityResponse =
                securityPasswordService.validatePasswordComplexity(updatePasswordForm.getNewPassword());
        if (!complexityResponse.getOk()) {
            return complexityResponse;
        }

        String newSaltedPassword =
                generateSaltPassword(updatePasswordForm.getNewPassword(), employeeEntity.getEmployeeUid());
        ResponseDTO<String> repeatResponse =
                securityPasswordService.validatePasswordRepeatTimes(requestUser, newSaltedPassword);
        if (!repeatResponse.getOk()) {
            return ResponseDTO.error(repeatResponse);
        }

        String newEncryptedPassword = SecurityPasswordService.getEncryptPwd(newSaltedPassword);
        EmployeeEntity updateEntity = new EmployeeEntity();
        updateEntity.setEmployeeId(employeeId);
        updateEntity.setLoginPwd(newEncryptedPassword);
        employeeDao.updateById(updateEntity);
        securityPasswordService.saveUserChangePasswordLog(
                requestUser, newEncryptedPassword, employeeEntity.getLoginPwd());
        return ResponseDTO.ok();
    }

    public EmployeeEntity getByLoginName(String loginName) {
        return employeeDao.getByLoginName(loginName, false);
    }

    public String generateSaltPassword(String password, String employeeUid) {
        return EmployeePasswordSalt.apply(password, employeeUid);
    }
}
