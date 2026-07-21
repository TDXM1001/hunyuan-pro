package com.hunyuan.sa.admin.module.organization.department.domain;

public interface OrganizationDepartmentScopePort {

    boolean canAccess(Long departmentId);

    boolean canCreateUnder(Long parentId);
}
