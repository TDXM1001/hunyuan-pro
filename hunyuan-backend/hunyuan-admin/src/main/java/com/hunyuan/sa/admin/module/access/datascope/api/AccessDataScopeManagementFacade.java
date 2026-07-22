package com.hunyuan.sa.admin.module.access.datascope.api;

import java.util.List;

/**
 * 数据范围稳定管理边界。
 */
public interface AccessDataScopeManagementFacade {

    /**
     * 查询系统支持的数据范围目录。
     */
    List<AccessDataScopeDefinition> listDataScopes();

    /**
     * 查询指定角色的数据范围配置。
     */
    AccessDataScopeManagementResult<AccessRoleDataScopes> getRoleDataScopes(Long roleId);

    /**
     * 全量替换指定角色的数据范围配置，空集合表示清空配置。
     */
    AccessDataScopeManagementResult<Void> replaceRoleDataScopes(
            ReplaceAccessRoleDataScopesCommand command);
}
