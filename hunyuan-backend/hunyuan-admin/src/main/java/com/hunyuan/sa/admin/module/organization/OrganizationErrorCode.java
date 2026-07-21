package com.hunyuan.sa.admin.module.organization;

import com.hunyuan.sa.base.common.code.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrganizationErrorCode implements ErrorCode {
    MODULE_DISABLED(41001, "组织目录模块未启用"),
    DEPARTMENT_NOT_FOUND(41002, "部门不存在"),
    INVALID_DEPARTMENT(41003, "部门参数不合法"),
    DEPARTMENT_NOT_EMPTY(41004, "部门仍有下级或在职员工"),
    EMPLOYEE_NOT_FOUND(41005, "部门负责人不存在");

    private final int code;
    private final String msg;
    private final String level = LEVEL_USER;
}
