package com.hunyuan.sa.admin.module.system.department.api;

import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationDepartmentCachePort;
import com.hunyuan.sa.admin.module.system.department.manager.DepartmentCacheManager;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class LegacyDepartmentCacheAdapter implements OrganizationDepartmentCachePort {

    @Resource
    private DepartmentCacheManager departmentCacheManager;

    @Override
    public void clearDepartmentCaches() {
        departmentCacheManager.clearCache();
    }
}
