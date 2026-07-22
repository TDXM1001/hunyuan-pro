package com.hunyuan.sa.admin.module.access.datascope.api;

/**
 * 部门数据范围公开查询边界。
 */
public interface AccessDepartmentScopeFacade {

    /**
     * 查询员工在组织目录中的部门访问范围。
     */
    AccessDepartmentScope resolveOrganizationDirectoryScope(Long employeeId);
}
