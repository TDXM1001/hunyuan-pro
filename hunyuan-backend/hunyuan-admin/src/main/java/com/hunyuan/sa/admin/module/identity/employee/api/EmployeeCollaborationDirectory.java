package com.hunyuan.sa.admin.module.identity.employee.api;

import java.util.List;
import java.util.Optional;

/** Narrow employee collaboration queries for consumers that must not load the directory facade. */
public interface EmployeeCollaborationDirectory {

    Optional<EmployeeCollaborationProfile> findCollaborationProfileById(Long employeeId);

    List<Long> findNonDeletedEmployeeIdsByDepartmentIds(List<Long> departmentIds);
}
