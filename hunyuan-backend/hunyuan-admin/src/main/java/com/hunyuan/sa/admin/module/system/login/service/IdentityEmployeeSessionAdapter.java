package com.hunyuan.sa.admin.module.system.login.service;

import cn.dev33.satoken.stp.StpUtil;
import com.hunyuan.sa.admin.module.identity.employee.application.port.EmployeeSessionPort;
import com.hunyuan.sa.base.common.constant.StringConst;
import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class IdentityEmployeeSessionAdapter implements EmployeeSessionPort {

    @Resource
    private LoginService loginService;

    @Override
    public void clearCache(Long employeeId) {
        loginService.clearLoginEmployeeCache(employeeId);
    }

    @Override
    public void logout(Long employeeId) {
        StpUtil.logout(UserTypeEnum.ADMIN_EMPLOYEE.getValue() + StringConst.COLON + employeeId);
    }
}
