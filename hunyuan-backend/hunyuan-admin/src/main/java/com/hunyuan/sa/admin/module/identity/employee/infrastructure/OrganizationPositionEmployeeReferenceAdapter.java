package com.hunyuan.sa.admin.module.identity.employee.infrastructure;

import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeRepository;
import com.hunyuan.sa.admin.module.organization.position.domain.PositionEmployeeReferencePort;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 由 identity.employee 提供岗位员工引用的只读适配器。
 */
@Component
public class OrganizationPositionEmployeeReferenceAdapter implements PositionEmployeeReferencePort {

    @Resource
    private EmployeeRepository employeeRepository;

    @Override
    public int countNonDeletedEmployees(Long positionId) {
        return employeeRepository.countNonDeletedByPositionId(positionId);
    }
}
