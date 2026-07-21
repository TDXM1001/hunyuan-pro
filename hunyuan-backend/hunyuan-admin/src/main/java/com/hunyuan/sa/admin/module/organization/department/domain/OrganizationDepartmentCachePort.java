package com.hunyuan.sa.admin.module.organization.department.domain;

/** Invalidates legacy organization read caches during the compatibility window. */
public interface OrganizationDepartmentCachePort {

    void clearDepartmentCaches();
}
