package com.hunyuan.sa.admin.module.organization.department.application;

import com.hunyuan.sa.admin.module.organization.department.domain.Department;
import com.hunyuan.sa.admin.module.organization.department.domain.DepartmentCommand;
import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationMember;
import com.hunyuan.sa.base.common.domain.ResponseDTO;

import java.util.List;
import java.util.Optional;

public interface OrganizationDepartmentFacade {

    List<Department> list();

    Department get(Long departmentId);

    List<OrganizationMember> listManagerOptions();

    ResponseDTO<Long> create(DepartmentCommand command);

    ResponseDTO<String> update(Long departmentId, DepartmentCommand command);

    ResponseDTO<String> delete(Long departmentId);

    /**
     * Compatibility access for the legacy system department API. These methods keep
     * the old entry points available while sharing the same write implementation.
     */
    List<Department> listForCompatibility();

    Optional<Department> findForCompatibility(Long departmentId);

    ResponseDTO<Long> createForCompatibility(DepartmentCommand command);

    ResponseDTO<String> updateForCompatibility(Long departmentId, DepartmentCommand command);

    ResponseDTO<String> deleteForCompatibility(Long departmentId);
}
