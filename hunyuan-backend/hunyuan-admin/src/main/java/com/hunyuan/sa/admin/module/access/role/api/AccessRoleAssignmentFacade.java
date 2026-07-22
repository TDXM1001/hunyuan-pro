package com.hunyuan.sa.admin.module.access.role.api;

/**
 * Public role membership command boundary.
 */
public interface AccessRoleAssignmentFacade {

    void assignEmployees(AssignRoleEmployeesCommand command);

    void removeEmployees(RemoveRoleEmployeesCommand command);

    void replaceEmployeeRoles(ReplaceEmployeeRolesCommand command);
}
