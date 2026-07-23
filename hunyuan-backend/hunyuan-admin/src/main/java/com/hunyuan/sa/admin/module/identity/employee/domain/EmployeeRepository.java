package com.hunyuan.sa.admin.module.identity.employee.domain;

import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeAuthenticationAccount;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeCollaborationProfile;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeQuery;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeSummary;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {

    Optional<EmployeeAuthenticationAccount> findAuthenticationAccountByLoginName(String loginName);

    Optional<EmployeeAuthenticationAccount> findAuthenticationAccountById(Long employeeId);

    Optional<EmployeeSummary> findSummaryById(Long employeeId);

    Optional<EmployeeCollaborationProfile> findCollaborationProfileById(Long employeeId);

    List<EmployeeCollaborationProfile> findCollaborationProfilesByIds(List<Long> employeeIds);

    List<Long> findNonDeletedEmployeeIdsByDepartmentIds(List<Long> departmentIds);

    Optional<Long> findIdByLoginName(String loginName);

    Optional<Long> findIdByPhone(String phone);

    Optional<Long> findIdByEmail(String email);

    List<EmployeeAuthenticationAccount> findAuthenticationAccountsByIds(List<Long> employeeIds);

    EmployeePage query(EmployeeQuery query, List<Long> departmentIds);

    Long create(EmployeeCreateDraft draft);

    void updateProfile(EmployeeProfileUpdate update);

    void updateDisabled(Long employeeId, boolean disabled);

    void assignDepartment(List<Long> employeeIds, Long departmentId);

    void markDeleted(List<Long> employeeIds);

    void updatePassword(Long employeeId, String passwordHash);

    boolean exists(Long employeeId);

    int countNonDeletedByDepartmentId(Long departmentId);

    int countNonDeletedByPositionId(Long positionId);

    List<EmployeeSummary> findActive();
}
