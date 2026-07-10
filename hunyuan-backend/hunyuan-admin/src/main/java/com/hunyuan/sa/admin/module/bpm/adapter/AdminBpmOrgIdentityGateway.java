package com.hunyuan.sa.admin.module.bpm.adapter;

import com.hunyuan.sa.admin.module.system.department.domain.vo.DepartmentVO;
import com.hunyuan.sa.admin.module.system.department.service.DepartmentService;
import com.hunyuan.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import com.hunyuan.sa.admin.module.system.employee.service.EmployeeService;
import com.hunyuan.sa.admin.module.system.role.service.RoleEmployeeService;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 基于当前 admin 组织体系实现 BPM 身份解析。
 */
@Component
public class AdminBpmOrgIdentityGateway implements BpmOrgIdentityGateway {

    private final EmployeeService employeeService;

    private final DepartmentService departmentService;

    private final RoleEmployeeService roleEmployeeService;

    public AdminBpmOrgIdentityGateway(
            EmployeeService employeeService,
            DepartmentService departmentService,
            RoleEmployeeService roleEmployeeService
    ) {
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.roleEmployeeService = roleEmployeeService;
    }

    @Override
    public BpmEmployeeSnapshot requireEmployee(Long employeeId) {
        EmployeeEntity employeeEntity = employeeService.getById(employeeId);
        if (employeeEntity == null) {
            throw new IllegalArgumentException("员工不存在，employeeId=" + employeeId);
        }
        if (Boolean.TRUE.equals(employeeEntity.getDisabledFlag())) {
            throw new IllegalArgumentException("员工已禁用，employeeId=" + employeeId);
        }
        if (Boolean.TRUE.equals(employeeEntity.getDeletedFlag())) {
            throw new IllegalArgumentException("员工已删除，employeeId=" + employeeId);
        }
        DepartmentVO department = employeeEntity.getDepartmentId() == null
                ? null
                : departmentService.getDepartmentById(employeeEntity.getDepartmentId());
        return new BpmEmployeeSnapshot(
                employeeEntity.getEmployeeId(),
                employeeEntity.getActualName(),
                employeeEntity.getDepartmentId(),
                department == null ? null : department.getDepartmentName(),
                employeeEntity.getPhone(),
                employeeEntity.getEmail()
        );
    }

    @Override
    public Long resolveDepartmentManagerEmployeeId(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        DepartmentVO department = departmentService.getDepartmentById(departmentId);
        return department == null ? null : department.getManagerId();
    }

    @Override
    public List<Long> listEmployeeIdsByRoleId(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        return roleEmployeeService.getAllEmployeeByRoleId(roleId)
                .stream()
                .map(item -> item.getEmployeeId())
                .toList();
    }
}
