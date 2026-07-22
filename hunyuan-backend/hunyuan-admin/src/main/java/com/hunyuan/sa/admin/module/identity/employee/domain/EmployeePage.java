package com.hunyuan.sa.admin.module.identity.employee.domain;

import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeSummary;

import java.util.List;

public record EmployeePage(
        long pageNum,
        long pageSize,
        long total,
        long pages,
        List<EmployeeSummary> employees
) {
}
