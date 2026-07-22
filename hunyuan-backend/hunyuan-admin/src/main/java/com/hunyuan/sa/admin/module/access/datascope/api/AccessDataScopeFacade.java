package com.hunyuan.sa.admin.module.access.datascope.api;

/**
 * 数据范围公开查询边界。
 */
public interface AccessDataScopeFacade {

    /**
     * 查询员工在指定数据范围类型下配置的最强可见级别。
     *
     * @return 稳定的可见级别值；未分配角色或未配置数据范围时返回“仅本人”
     */
    Integer resolveEmployeeViewType(Long employeeId, Integer dataScopeType);
}
