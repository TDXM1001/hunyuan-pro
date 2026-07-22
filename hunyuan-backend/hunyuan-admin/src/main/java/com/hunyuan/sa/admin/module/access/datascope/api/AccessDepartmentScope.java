package com.hunyuan.sa.admin.module.access.datascope.api;

import java.util.List;

/**
 * 部门访问范围快照。
 *
 * @param unrestricted 是否允许访问全部部门
 * @param departmentIds 受限时允许访问的部门 ID
 */
public record AccessDepartmentScope(boolean unrestricted, List<Long> departmentIds) {

    public AccessDepartmentScope {
        departmentIds = departmentIds == null ? List.of() : List.copyOf(departmentIds);
    }

    public static AccessDepartmentScope allDepartments() {
        return new AccessDepartmentScope(true, List.of());
    }

    public static AccessDepartmentScope restricted(List<Long> departmentIds) {
        return new AccessDepartmentScope(false, departmentIds);
    }

    public boolean allows(Long departmentId) {
        return departmentId != null && (unrestricted || departmentIds.contains(departmentId));
    }
}
