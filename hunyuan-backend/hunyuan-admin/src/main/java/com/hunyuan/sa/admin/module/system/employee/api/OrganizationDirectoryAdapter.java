package com.hunyuan.sa.admin.module.system.employee.api;

import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationDirectoryPort;
import com.hunyuan.sa.admin.module.organization.department.domain.OrganizationMember;
import com.hunyuan.sa.admin.module.system.employee.dao.EmployeeDao;
import com.hunyuan.sa.admin.module.system.employee.domain.vo.EmployeeVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrganizationDirectoryAdapter implements OrganizationDirectoryPort {

    @Resource
    private EmployeeDao employeeDao;

    @Override
    public boolean employeeExists(Long employeeId) {
        return employeeId != null && employeeDao.selectById(employeeId) != null;
    }

    @Override
    public int countActiveEmployees(Long departmentId) {
        return employeeDao.countByDepartmentId(departmentId, Boolean.FALSE);
    }

    @Override
    public List<OrganizationMember> listActiveEmployees() {
        return employeeDao.selectEmployeeByDisabledAndDeleted(Boolean.FALSE, Boolean.FALSE).stream()
                .map(this::toMember)
                .toList();
    }

    private OrganizationMember toMember(EmployeeVO employee) {
        return new OrganizationMember(employee.getEmployeeId(), employee.getActualName(), employee.getDepartmentId());
    }
}
