package com.hunyuan.sa.admin.module.access.role.api;

import java.util.List;

/**
 * 角色生命周期公开用例边界。
 */
public interface AccessRoleLifecycleFacade {

    AccessRoleResult<Long> create(CreateAccessRoleCommand command);

    AccessRoleResult<Void> update(UpdateAccessRoleCommand command);

    AccessRoleResult<Void> delete(Long roleId);

    AccessRoleResult<AccessRole> get(Long roleId);

    List<AccessRole> list();
}
