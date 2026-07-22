package com.hunyuan.sa.admin.module.system.role.service;

import com.hunyuan.sa.admin.module.identity.employee.application.port.EmployeeRoleAssignmentPort;
import com.hunyuan.sa.admin.module.system.role.dao.RoleEmployeeDao;
import com.hunyuan.sa.admin.module.system.role.domain.entity.RoleEmployeeEntity;
import com.hunyuan.sa.admin.module.system.role.manager.RoleEmployeeManager;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IdentityEmployeeRoleAssignmentAdapter implements EmployeeRoleAssignmentPort {

    @Resource
    private RoleEmployeeDao roleEmployeeDao;

    @Resource
    private RoleEmployeeManager roleEmployeeManager;

    @Override
    public void replaceRoles(Long employeeId, List<Long> roleIds) {
        roleEmployeeDao.deleteByEmployeeId(employeeId);
        if (roleIds.isEmpty()) {
            return;
        }
        roleEmployeeManager.saveBatch(roleIds.stream()
                .map(roleId -> new RoleEmployeeEntity(roleId, employeeId))
                .toList());
    }
}
