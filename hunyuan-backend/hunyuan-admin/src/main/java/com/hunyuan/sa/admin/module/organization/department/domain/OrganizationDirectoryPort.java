package com.hunyuan.sa.admin.module.organization.department.domain;

import java.util.List;

/** Public organization dependencies supplied by the identity/employee module. */
public interface OrganizationDirectoryPort {

    boolean employeeExists(Long employeeId);

    int countActiveEmployees(Long departmentId);

    List<OrganizationMember> listActiveEmployees();
}
