package com.hunyuan.sa.admin.module.organization.department.domain;

import java.time.LocalDateTime;

/** Domain model for an organization department. */
public record Department(
        Long departmentId,
        String departmentName,
        Long managerId,
        Long parentId,
        Integer sort,
        String managerName,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    public Department normalized() {
        return new Department(
                departmentId,
                departmentName == null ? null : departmentName.trim(),
                managerId,
                parentId == null ? 0L : parentId,
                sort == null ? 0 : sort,
                managerName,
                createTime,
                updateTime);
    }
}
