package com.hunyuan.sa.admin.module.access.datascope.api;

import java.util.List;

/**
 * 全量替换角色数据范围命令。
 */
public record ReplaceAccessRoleDataScopesCommand(
        Long roleId,
        List<AccessDataScopeSetting> dataScopes
) {
}
