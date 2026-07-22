package com.hunyuan.sa.admin.module.identity.employee.infrastructure;

import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeCollaborationDirectory;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeCollaborationProfile;
import com.hunyuan.sa.admin.module.identity.employee.domain.EmployeeRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EmployeeCollaborationDirectoryAdapter implements EmployeeCollaborationDirectory {

    @Resource
    private EmployeeRepository employeeRepository;

    @Override
    public Optional<EmployeeCollaborationProfile> findCollaborationProfileById(Long employeeId) {
        return employeeId == null
                ? Optional.empty()
                : employeeRepository.findCollaborationProfileById(employeeId);
    }

    @Override
    public List<Long> findNonDeletedEmployeeIdsByDepartmentIds(List<Long> departmentIds) {
        return departmentIds == null || departmentIds.isEmpty()
                ? List.of()
                : employeeRepository.findNonDeletedEmployeeIdsByDepartmentIds(departmentIds);
    }
}
