package com.hunyuan.sa.admin.module.identity.employee.api;

import com.hunyuan.sa.base.common.domain.PageResult;

import java.util.List;
import java.util.Optional;

public interface EmployeeDirectoryFacade {

    Optional<EmployeeAuthenticationAccount> findAuthenticationAccountByLoginName(String loginName);

    Optional<EmployeeAuthenticationAccount> findAuthenticationAccountById(Long employeeId);

    Optional<EmployeeSummary> findSummaryById(Long employeeId);

    Optional<EmployeeCollaborationProfile> findCollaborationProfileById(Long employeeId);

    List<EmployeeCollaborationProfile> findCollaborationProfilesByIds(List<Long> employeeIds);

    List<Long> findNonDeletedEmployeeIdsByDepartmentIds(List<Long> departmentIds);

    PageResult<EmployeeSummary> query(EmployeeQuery query);

    boolean employeeExists(Long employeeId);

    int countActiveEmployees(Long departmentId);

    List<EmployeeSummary> listActiveEmployees();
}
