package com.hunyuan.sa.admin.module.access.datascope.api;

import java.util.List;

/**
 * 角色数据范围配置快照。
 */
public record AccessRoleDataScopes(
        Long roleId,
        List<AccessDataScopeSetting> dataScopes
) {
}
