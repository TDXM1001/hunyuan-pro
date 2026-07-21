package com.hunyuan.sa.admin.module.organization.department.domain;

/** Input for create/update use cases. */
public record DepartmentCommand(String departmentName, Long managerId, Long parentId, Integer sort) {
}
