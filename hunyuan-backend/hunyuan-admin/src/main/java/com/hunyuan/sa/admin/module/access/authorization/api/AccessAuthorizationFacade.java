package com.hunyuan.sa.admin.module.access.authorization.api;

/**
 * Public authorization query boundary consumed by authentication and other modules.
 */
public interface AccessAuthorizationFacade {

    AccessAuthorizationSnapshot loadEmployeeAuthorization(Long employeeId, Boolean administratorFlag);
}
