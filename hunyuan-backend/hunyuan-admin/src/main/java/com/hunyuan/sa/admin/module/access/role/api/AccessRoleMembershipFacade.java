package com.hunyuan.sa.admin.module.access.role.api;

import com.hunyuan.sa.base.common.domain.PageResult;

import java.util.List;

/**
 * 角色成员关系公开查询边界。
 */
public interface AccessRoleMembershipFacade {

    PageResult<AccessRoleMember> queryMembers(AccessRoleMemberQuery query);

    PageResult<AccessRoleMember> queryCandidates(AccessRoleMemberQuery query);

    List<AccessRoleMember> listMembers(Long roleId);

    List<AccessRoleSelection> listEmployeeRoleSelections(Long employeeId);

    List<AccessRole> listEmployeeRoles(Long employeeId);
}
