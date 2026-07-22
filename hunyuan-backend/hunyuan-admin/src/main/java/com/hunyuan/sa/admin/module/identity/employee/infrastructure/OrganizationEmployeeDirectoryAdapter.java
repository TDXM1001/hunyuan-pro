package com.hunyuan.sa.admin.module.identity.employee.infrastructure;

import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeSummary;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeRepository;
import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationDirectoryPort;
import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationMember;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrganizationEmployeeDirectoryAdapter implements OrganizationDirectoryPort {

    @Resource
    private EmployeeRepository employeeRepository;

    @Override
    public boolean employeeExists(Long employeeId) {
        return employeeId != null && employeeRepository.exists(employeeId);
    }

    @Override
    public int countActiveEmployees(Long departmentId) {
        return departmentId == null ? 0 : employeeRepository.countNonDeletedByDepartmentId(departmentId);
    }

    @Override
    public List<OrganizationMember> listActiveEmployees() {
        return employeeRepository.findActive().stream()
                .map(this::toMember)
                .toList();
    }

    private OrganizationMember toMember(EmployeeSummary employee) {
        return new OrganizationMember(employee.employeeId(), employee.actualName(), employee.departmentId());
    }
}
