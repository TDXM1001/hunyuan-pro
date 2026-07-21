package com.hunyuan.sa.admin.module.organization.department.domain;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository {

    List<Department> findAll();

    Optional<Department> findById(Long departmentId);

    boolean exists(Long departmentId);

    int countChildren(Long departmentId);

    Long insert(Department department);

    void update(Department department);

    void delete(Long departmentId);
}
