package com.hunyuan.sa.admin.module.identity.employee.api;

import com.hunyuan.sa.base.common.domain.ResponseDTO;

import java.util.List;

public interface EmployeeAdministrationFacade {

    ResponseDTO<EmployeeOneTimeCredential> create(EmployeeCreateCommand command);

    ResponseDTO<EmployeeOneTimeCredential> createWithLegacyRoles(
            EmployeeCreateCommand command, List<Long> roleIds);

    ResponseDTO<String> update(EmployeeUpdateCommand command);

    ResponseDTO<String> updateWithLegacyRoles(
            EmployeeUpdateCommand command, Boolean disabled, List<Long> roleIds);

    ResponseDTO<String> enable(Long employeeId);

    ResponseDTO<String> disable(Long employeeId);

    ResponseDTO<String> toggleDisabledForLegacy(Long employeeId);

    ResponseDTO<String> assignDepartment(EmployeeDepartmentAssignmentCommand command);

    ResponseDTO<String> delete(EmployeeDeleteCommand command);

    ResponseDTO<EmployeeOneTimeCredential> resetPassword(Long employeeId, Long operatorEmployeeId);
}
