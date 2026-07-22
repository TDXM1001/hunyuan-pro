package com.hunyuan.sa.admin.module.access.role.api;

/**
 * 角色生命周期用例的稳定失败原因。
 */
public enum AccessRoleFailure {
    ROLE_NOT_FOUND,
    ROLE_NAME_DUPLICATED,
    ROLE_CODE_DUPLICATED,
    ROLE_HAS_EMPLOYEES
}
