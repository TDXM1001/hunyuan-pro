package com.hunyuan.sa.admin.module.identity.employee.api;

import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.domain.ResponseDTO;

/**
 * 认证账号与当前登录员工自助操作的公开协作端口。
 */
public interface EmployeeAccountFacade {

    ResponseDTO<String> updateSelfProfile(EmployeeSelfProfileUpdateCommand command);

    ResponseDTO<String> updateAvatar(Long employeeId, String avatar);

    ResponseDTO<String> changePassword(
            RequestUser requestUser, EmployeePasswordChangeCommand command);

    boolean passwordComplexityEnabled();
}
