package com.hunyuan.sa.admin.module.identity.employee.application.port;

import java.util.List;

public interface EmployeeRoleAssignmentPort {

    void replaceRoles(Long employeeId, List<Long> roleIds);
}
